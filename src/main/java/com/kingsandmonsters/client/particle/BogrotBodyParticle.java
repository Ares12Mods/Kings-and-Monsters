package com.kingsandmonsters.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Motion for Bogrot's four static sprites. The texture itself never animates. */
public final class BogrotBodyParticle extends TextureSheetParticle {
    public enum Kind { MOTE, FLY, FLIES, BUBBLE }

    private final Kind kind;
    private final float baseSize;
    private final float driftPhase;

    private BogrotBodyParticle(ClientLevel level, double x, double y, double z,
                               double xd, double yd, double zd, SpriteSet sprites, Kind kind) {
        // The velocity-taking vanilla constructor adds a large random launch and +0.1 upward
        // motion. Start stationary and apply only Bogrot's deliberately tiny drift values.
        super(level, x, y, z);
        setParticleSpeed(xd, yd, zd);
        this.kind = kind;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.driftPhase = random.nextFloat() * Mth.TWO_PI;
        this.baseSize = switch (kind) {
            case MOTE -> 0.075F;
            case FLY -> 0.13F;
            case FLIES -> 0.22F;
            case BUBBLE -> 0.22F;
        };
        this.quadSize = baseSize;
        this.lifetime = switch (kind) {
            case MOTE -> 34 + random.nextInt(11);
            case FLY -> 42 + random.nextInt(15);
            case FLIES -> 38 + random.nextInt(13);
            case BUBBLE -> 38 + random.nextInt(13);
        };
        this.friction = switch (kind) {
            case FLY -> 0.90F;
            case FLIES -> 0.94F;
            default -> 0.97F;
        };
        setSprite(sprites.get(0, 1));
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;

        float progress = age / (float) lifetime;
        alpha = Mth.clamp(Math.min(progress * 5.0F, (1.0F - progress) * 5.0F), 0.0F, 1.0F);
        switch (kind) {
            case MOTE -> {
                xd += Mth.sin(age * 0.22F + driftPhase) * 0.00018;
                zd += Mth.cos(age * 0.19F + driftPhase) * 0.00018;
                yd += progress < 0.45F ? 0.00025 : -0.0007;
            }
            case FLY -> {
                // A small looping path reads like one fly circling instead of being sprayed away.
                xd = Mth.sin(age * 0.42F + driftPhase) * 0.0045;
                yd = Mth.cos(age * 0.29F + driftPhase) * 0.0015;
                zd = Mth.cos(age * 0.38F + driftPhase) * 0.0045;
            }
            case FLIES -> {
                xd += Mth.sin(age * 0.34F + driftPhase) * 0.0025;
                yd += Mth.cos(age * 0.27F + driftPhase) * 0.0015;
                zd += Mth.cos(age * 0.31F + driftPhase) * 0.0025;
            }
            case BUBBLE -> {
                xd += Mth.sin(age * 0.16F + driftPhase) * 0.00012;
                zd += Mth.cos(age * 0.14F + driftPhase) * 0.00012;
                yd += progress < 0.55F ? 0.0003 : -0.00065;
                quadSize = baseSize * Mth.lerp(progress, 0.88F, 1.08F);
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new BogrotBodyParticle(level, x, y, z, xd, yd, zd, sprites, kind);
        }
    }
}
