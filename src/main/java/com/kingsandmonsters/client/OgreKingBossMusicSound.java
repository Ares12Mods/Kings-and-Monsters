package com.kingsandmonsters.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** One streamed score movement whose lifetime and fade are owned by the encounter controller. */
final class OgreKingBossMusicSound extends AbstractTickableSoundInstance {
    private static final float FADE_STEP = 1.0F / 50.0F;
    private final BossMusicController.MusicSegment segment;
    private boolean fadingOut;
    private float fadeStep = FADE_STEP;
    private int ticksPlayed;

    OgreKingBossMusicSound(SoundEvent soundEvent, BossMusicController.MusicSegment segment) {
        super(soundEvent, SoundSource.MUSIC, RandomSource.create());
        this.segment = segment;
        looping = false;
        delay = 0;
        relative = true;
        attenuation = Attenuation.NONE;
        volume = 1.0F;
        pitch = 1.0F;
    }

    void fadeOut() {
        fadingOut = true;
    }

    void fadeOut(int ticks) {
        fadingOut = true;
        fadeStep = 1.0F / Math.max(1, ticks);
    }

    void cancelFade() {
        fadingOut = false;
        fadeStep = FADE_STEP;
        volume = 1.0F;
    }

    BossMusicController.MusicSegment segment() {
        return segment;
    }

    boolean hasHadTimeToStart() {
        return ticksPlayed > 2;
    }

    int ticksPlayed() {
        return ticksPlayed;
    }

    @Override
    public void tick() {
        ticksPlayed++;
        if (fadingOut) {
            volume = Math.max(0.0F, volume - fadeStep);
            if (volume <= 0.0F) stop();
        }
    }
}
