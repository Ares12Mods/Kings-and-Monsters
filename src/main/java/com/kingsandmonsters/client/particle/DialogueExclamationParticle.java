package com.kingsandmonsters.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** The supplied pixel-art reaction that builds, holds, and disappears above a dialogue giver. */
public final class DialogueExclamationParticle extends TextureSheetParticle {
    private static final int[] FRAME_TICKS = {2, 2, 2, 3, 4, 4, 3, 2, 2, 2};
    private static final int LIFETIME = java.util.Arrays.stream(FRAME_TICKS).sum();
    private static final float SIZE = 0.42F;

    private final SpriteSet sprites;

    private DialogueExclamationParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = LIFETIME;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.quadSize = SIZE;
        setSprite(sprites.get(0, FRAME_TICKS.length - 1));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        int frame = 0;
        for (int elapsed = FRAME_TICKS[0]; age >= elapsed && frame < FRAME_TICKS.length - 1; ) {
            elapsed += FRAME_TICKS[++frame];
        }
        setSprite(sprites.get(frame, FRAME_TICKS.length - 1));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new DialogueExclamationParticle(level, x, y, z, sprites);
        }
    }
}
