package com.kingsandmonsters.tribute;

import com.kingsandmonsters.Config;
import com.kingsandmonsters.entity.OgreGrunt;
import com.kingsandmonsters.entity.PlagueOgre;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class OutpostEvents {

    private OutpostEvents() {}

    public static void onTributeChestOpened(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = event.getPos();

        if (!level.getBlockState(pos).is(Blocks.TRAPPED_CHEST)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RandomizableContainerBlockEntity)) return;

        // If this chest outpost already unpacked its loot table (LootTable tag gone) before its
        // Fort target resolved, the guaranteed map never generated; deliver it now, just before the
        // GUI opens. Does nothing unless every precondition holds (see FortMapDeliveryService).
        TributeManager.findOutpostCampOrigin(level, pos)
                .ifPresent(campOrigin -> com.kingsandmonsters.world.FortMapDeliveryService.tryDeliver(level, campOrigin));

        // Only trigger the rest (anger/patrol) on first open — loot table tag is present before loot generates
        CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
        if (!tag.contains("LootTable")) return;

        int campRadius = Config.TRIBUTE_CAMP_KILL_RADIUS.get();
        TributeManager.findNearestCamp(level, pos, campRadius * 4)
            .ifPresent(campOrigin -> punishTheft(level, player, pos, campOrigin));
    }

    /**
     * Taking what is on display in a camp is theft like looting a tribute chest: it angers the camp,
     * costs favor and ends any peace or paid passage. Only the first grab of an episode counts, so
     * clearing a hut of frames isn't a stack of penalties.
     */
    public static void onItemFrameTaken(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof ItemFrame frame) || frame.getItem().isEmpty()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || player.isCreative() || TributeManager.isProvoked(level, player.getUUID())) {
            return;
        }
        TributeManager.findNearestCamp(level, frame.blockPosition(), Config.TRIBUTE_CAMP_KILL_RADIUS.get())
                .ifPresent(campOrigin -> punishTheft(level, player, frame.blockPosition(), campOrigin));
    }

    private static void punishTheft(ServerLevel level, ServerPlayer player, BlockPos pos, BlockPos campOrigin) {
        // Nobody is left in an abandoned camp to mind: taking from it costs no favor and angers no one.
        if (TributeManager.isAbandonedCamp(level, campOrigin)) {
            return;
        }
        boolean angerApplied = TributeManager.recordTributeChestLooted(player, pos, campOrigin,
                Config.TRIBUTE_CHEST_ANGER_DELTA.get());

        // Anger is immediate, but retaliation is not: arm the player single pending timer
        // instead of spawning a patrol on the spot, so the tribe has time to learn what
        // happened first. Provoking again during the wait just refreshes the timer.
        if (angerApplied && level.random.nextFloat() < Config.TRIBUTE_PATROL_CHANCE.get()) {
            long eligibleGameTime = level.getGameTime() + PatrolManager.PATROL_RETALIATION_DELAY_TICKS;
            TributeManager.armPatrolRetaliation(level, player.getUUID(), campOrigin, eligibleGameTime);
        }
    }

    private static final int OGRE_KILL_FAVOR_LOSS = 5;
    // Ogre kills per player inside the last KILL_WINDOW_TICKS; the second one angers the whole tribe.
    private static final long KILL_WINDOW_TICKS = 20L * 60 * 5;
    private static final int KILLS_BEFORE_TRIBE_TURNS = 2;
    private static final java.util.Map<java.util.UUID, java.util.ArrayDeque<Long>> RECENT_KILLS =
            new java.util.HashMap<>();

    /** Forgets recent kills when a world closes. */
    public static void resetState() {
        RECENT_KILLS.clear();
    }
    private static final int FRIENDLY_FIRE_FAVOR_LOSS = 1;
    private static final String STRUCK_BY_FRIEND_TAG = "KingsAndMonstersStruckByFriend";

    /** Striking the tribe ends any peace the player had, for every ogre and not just the one hit. */
    public static void onOgreHurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof OgreGrunt ogre && !(ogre instanceof PlagueOgre)
                && !ogre.isArmyAlly()
                && !ogre.isRoyalTrialOgre()
                && ogre.level() instanceof ServerLevel level
                && event.getSource().getEntity() instanceof ServerPlayer attacker
                && !isDueling(level, attacker)) {
            if (TributeManager.isAppeased(level, attacker.getUUID())) {
                // Someone in the tribe's good graces who strikes an ogre by accident answers only to that
                // ogre, and pays a small fee for the first blow on each one, so one stray fight can't ruin them.
                if (!ogre.getPersistentData().getBoolean(STRUCK_BY_FRIEND_TAG)) {
                    ogre.getPersistentData().putBoolean(STRUCK_BY_FRIEND_TAG, true);
                    TributeManager.addFavorQuietly(level, attacker, -FRIENDLY_FIRE_FAVOR_LOSS);
                }
            } else {
                TributeManager.provoke(level, attacker);
            }
        }
    }

    /**
     * A duel is a fair fight, so nothing struck while it lasts breaks the peace: not the King, and not
     * a bystander caught by a swing.
     */
    private static boolean isDueling(ServerLevel level, ServerPlayer player) {
        return !level.getEntitiesOfClass(com.kingsandmonsters.entity.OgreLord.class,
                player.getBoundingBox().inflate(64.0), king -> king.isDuelingWith(player)).isEmpty();
    }

    public static void onOgreKilled(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof OgreGrunt ogre)) return;
        if (ogre.isArmyAlly()) return;
        if (ogre.level().isClientSide()) return;
        if (!(ogre.level() instanceof ServerLevel level)) return;

        CompoundTag data = ogre.getPersistentData();

        if (ogre.isOutpostCaptain() && ogre.getOutpostHome() != null) {
            TributeManager.markOutpostCaptainKilled(
                    level,
                    ogre.getOutpostHome(),
                    level.getGameTime() + OutpostPopulationManager.captainRespawnTicks(level, ogre.getOutpostHome()));
        } else if (ogre.isOutpostResident() && ogre.getOutpostHome() != null) {
            OutpostPopulationManager.scheduleResidentReplacement(level, ogre.getOutpostHome());
        }

        // Plague Ogres are the tribe's lost, not its own; clearing them out never angers the faction.
        if (ogre instanceof PlagueOgre) {
            return;
        }

        // A king beaten in a duel was beaten fairly: no favor lost, and the tribe is not angered.
        if (ogre instanceof com.kingsandmonsters.entity.OgreLord king && king.isDuel()) {
            if (event.getSource().getEntity() instanceof ServerPlayer winner && king.isDuelist(winner)) {
                com.kingsandmonsters.advancement.RoyalAdvancementEvents.awardPath(winner, "tribute/fair_fight");
            }
            return;
        }

        if (!ogre.isRoyalTrialOgre() && event.getSource().getEntity() instanceof ServerPlayer killer
                && !isDueling(level, killer)) {
            TributeManager.addFavorQuietly(level, killer, -OGRE_KILL_FAVOR_LOSS);
            // One stray death is forgiven to the appeased, but a second within the window shows intent:
            // the whole tribe turns on them, as if they had struck the camp.
            java.util.ArrayDeque<Long> recent = RECENT_KILLS.computeIfAbsent(
                    killer.getUUID(), id -> new java.util.ArrayDeque<>());
            long now = level.getGameTime();
            while (!recent.isEmpty() && now - recent.peekFirst() > KILL_WINDOW_TICKS) {
                recent.pollFirst();
            }
            recent.addLast(now);
            if (recent.size() >= KILLS_BEFORE_TRIBE_TURNS) {
                TributeManager.provoke(level, killer);
            }
        }

        if (data.contains("PatrolCampPos")) {
            // Patrol ogre killed — reduce anger
            BlockPos campPos = BlockPos.of(data.getLong("PatrolCampPos"));
            int reduction = data.getInt("PatrolAngerReduction");
            TributeManager.getOrCreate(level, campPos).incrementAnger(-reduction);
        } else {
            // Check if killed near a known camp — increase anger
            int radius = Config.TRIBUTE_CAMP_KILL_RADIUS.get();
            TributeManager.findNearestCamp(level, ogre.blockPosition(), radius)
                .ifPresent(campOrigin ->
                    TributeManager.getOrCreate(level, campOrigin)
                        .incrementAnger(Config.TRIBUTE_CAMP_KILL_ANGER.get()));
        }
    }
}
