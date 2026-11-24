package com.kingsandmonsters.client;

import com.kingsandmonsters.ClientConfig;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.entity.OgreLord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import com.kingsandmonsters.event.ClientTickEvent;

import java.util.Comparator;

/** Adaptive Ogre King score sequencer driven by the encounter and synced King phase state. */
public final class BossMusicController {
    private static final double ENTRY_RADIUS = 96.0;
    private static final double EXIT_RADIUS = 112.0;

    private static OgreKingBossMusicSound activeSound;
    private static ClientLevel activeLevel;
    private static boolean endingPlayed;
    private static final RandomSource RANDOM = RandomSource.create();

    enum MusicSegment {
        INTRO,
        PHASE_ONE_ONE,
        PHASE_ONE_TWO,
        PHASE_ONE_THREE,
        TRANSITION_FILL,
        PHASE_TWO_ONE,
        PHASE_TWO_TWO,
        PHASE_TWO_THREE,
        END;

        /**
         * Start the next authored movement one second early so adjacent tails and attacks overlap
         * as intended by the composer.
         */
        int handoffTick() {
            return switch (this) {
                case INTRO -> 320;
                case PHASE_ONE_ONE, PHASE_ONE_TWO, PHASE_ONE_THREE -> 640;
                case TRANSITION_FILL -> 160;
                case PHASE_TWO_ONE, PHASE_TWO_TWO, PHASE_TWO_THREE -> 640;
                case END -> Integer.MAX_VALUE;
            };
        }
    }

    private BossMusicController() {}

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;

        if (!ClientConfig.bossMusicEnabled()) {
            // Switched off, possibly mid-fight: cut anything already playing and leave vanilla
            // music alone.
            if (activeSound != null || endingPlayed) {
                stopImmediately(minecraft);
            }
            return;
        }

        if (level == null || player == null || activeLevel != null && activeLevel != level) {
            stopImmediately(minecraft);
        }
        activeLevel = level;
        if (level == null || player == null || !player.isAlive()) {
            beginFade();
            suppressVanillaMusicWhileOwned(minecraft);
            clearFinished(minecraft);
            return;
        }

        OgreLord dyingKing = nearestKing(level, player, EXIT_RADIUS,
                king -> !king.isRemoved() && king.isDyingAnimation());
        if (dyingKing != null) {
            if (!endingPlayed && dyingKing.isDyingAnimationFinishing()) {
                switchImmediately(minecraft, MusicSegment.END);
                endingPlayed = true;
            }
            suppressVanillaMusicWhileOwned(minecraft);
            clearFinished(minecraft);
            return;
        }
        // The King is removed immediately after the animation's final frame. Keep ownership of
        // the ending after that removal so the three-second cadence is not treated as abandoned
        // encounter music and faded out.
        if (endingPlayed) {
            suppressVanillaMusicWhileOwned(minecraft);
            clearFinished(minecraft);
            if (activeSound == null) endingPlayed = false;
            return;
        }

        double radius = activeSound == null ? ENTRY_RADIUS : EXIT_RADIUS;
        OgreLord king = nearestKing(level, player, radius,
                candidate -> candidate.isAlive()
                        && !candidate.isRemoved()
                        && !candidate.isDyingAnimation()
                        && candidate.isFortEncounterMusicActive());

        if (king == null) {
            beginFade();
        } else {
            endingPlayed = false;
            advanceEncounterScore(minecraft, king);
        }
        suppressVanillaMusicWhileOwned(minecraft);
        clearFinished(minecraft);
    }

    private static OgreLord nearestKing(ClientLevel level, Player player, double radius,
                                         java.util.function.Predicate<OgreLord> predicate) {
        double radiusSqr = radius * radius;
        return level.getEntitiesOfClass(OgreLord.class,
                        new AABB(player.blockPosition()).inflate(radius),
                        king -> predicate.test(king) && king.distanceToSqr(player) <= radiusSqr)
                .stream()
                .min(Comparator.comparingDouble(king -> king.distanceToSqr(player)))
                .orElse(null);
    }

    private static void advanceEncounterScore(Minecraft minecraft, OgreLord king) {
        if (activeSound == null || activeSound.isStopped()) {
            play(minecraft, king.isPhaseTwo() ? MusicSegment.PHASE_TWO_ONE : MusicSegment.INTRO);
            return;
        }

        activeSound.cancelFade();
        if (king.isPhaseTwo() && isPhaseOneMovement(activeSound.segment())) {
            // The health threshold can occur anywhere inside a long Phase 1 movement. Start the
            // authored fill immediately and let the outgoing movement clear beneath it instead of
            // delaying Phase 2 music by as much as an entire 33-second file.
            activeSound.fadeOut(10);
            play(minecraft, MusicSegment.TRANSITION_FILL);
            return;
        }
        if (activeSound.ticksPlayed() >= activeSound.segment().handoffTick()) {
            play(minecraft, nextSegment(activeSound.segment(), king.isPhaseTwo()));
            return;
        }
        if (playbackFinished(minecraft)) {
            play(minecraft, nextSegment(activeSound.segment(), king.isPhaseTwo()));
        }
    }

    private static boolean isPhaseOneMovement(MusicSegment segment) {
        return segment == MusicSegment.INTRO
                || segment == MusicSegment.PHASE_ONE_ONE
                || segment == MusicSegment.PHASE_ONE_TWO
                || segment == MusicSegment.PHASE_ONE_THREE;
    }

    private static MusicSegment nextSegment(MusicSegment segment, boolean phaseTwo) {
        if (phaseTwo && isPhaseOneMovement(segment)) return MusicSegment.TRANSITION_FILL;
        return switch (segment) {
            case INTRO -> MusicSegment.PHASE_ONE_ONE;
            case PHASE_ONE_ONE -> RANDOM.nextBoolean() ? MusicSegment.PHASE_ONE_TWO : MusicSegment.PHASE_ONE_THREE;
            case PHASE_ONE_TWO, PHASE_ONE_THREE -> MusicSegment.PHASE_ONE_ONE;
            case TRANSITION_FILL -> MusicSegment.PHASE_TWO_ONE;
            case PHASE_TWO_ONE -> RANDOM.nextBoolean() ? MusicSegment.PHASE_TWO_TWO : MusicSegment.PHASE_TWO_THREE;
            case PHASE_TWO_TWO, PHASE_TWO_THREE -> MusicSegment.PHASE_TWO_ONE;
            case END -> MusicSegment.END;
        };
    }

    private static void switchImmediately(Minecraft minecraft, MusicSegment segment) {
        if (activeSound != null) minecraft.getSoundManager().stop(activeSound);
        play(minecraft, segment);
    }

    private static void play(Minecraft minecraft, MusicSegment segment) {
        SoundEvent event = switch (segment) {
            case INTRO -> ModSoundEvents.OGRE_KING_INTRO.get();
            case PHASE_ONE_ONE -> ModSoundEvents.OGRE_KING_PHASE_ONE_ONE.get();
            case PHASE_ONE_TWO -> ModSoundEvents.OGRE_KING_PHASE_ONE_TWO.get();
            case PHASE_ONE_THREE -> ModSoundEvents.OGRE_KING_PHASE_ONE_THREE.get();
            case TRANSITION_FILL -> ModSoundEvents.OGRE_KING_TRANSITION_FILL.get();
            case PHASE_TWO_ONE -> ModSoundEvents.OGRE_KING_PHASE_TWO_ONE.get();
            case PHASE_TWO_TWO -> ModSoundEvents.OGRE_KING_PHASE_TWO_TWO.get();
            case PHASE_TWO_THREE -> ModSoundEvents.OGRE_KING_PHASE_TWO_THREE.get();
            case END -> ModSoundEvents.OGRE_KING_END.get();
        };
        activeSound = new OgreKingBossMusicSound(event, segment);
        minecraft.getSoundManager().play(activeSound);
    }

    private static void beginFade() {
        if (activeSound != null) activeSound.fadeOut();
    }

    private static void suppressVanillaMusicWhileOwned(Minecraft minecraft) {
        if (activeSound != null && !activeSound.isStopped()) minecraft.getMusicManager().stopPlaying();
    }

    private static void clearFinished(Minecraft minecraft) {
        if (activeSound != null && (activeSound.isStopped() || playbackFinished(minecraft))) {
            minecraft.getSoundManager().stop(activeSound);
            activeSound = null;
        }
    }

    private static boolean playbackFinished(Minecraft minecraft) {
        return activeSound != null
                && activeSound.hasHadTimeToStart()
                && !minecraft.getSoundManager().isActive(activeSound);
    }

    private static void stopImmediately(Minecraft minecraft) {
        if (activeSound != null) {
            minecraft.getSoundManager().stop(activeSound);
            activeSound = null;
        }
        endingPlayed = false;
        activeLevel = null;
    }
}
