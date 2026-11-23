package com.kingsandmonsters.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** A short-lived fleck of turf timed to one jaw contact in the Mudsnout grazing clip. */
public final class GrazingCrumbParticle extends TextureSheetParticle {
    private GrazingCrumbParticle(ClientLevel level, double x, double y, double z,
                                 double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        lifetime = 6;
        gravity = 0.9F;
        friction = 0.72F;
        hasPhysics = true;
        quadSize = 0.055F + random.nextFloat() * 0.045F;
        this.xd = xd * 0.7;
        this.yd = Math.abs(yd) * 0.45 + 0.035;
        this.zd = zd * 0.7;
        setColor(0.48F + random.nextFloat() * 0.12F,
                0.58F + random.nextFloat() * 0.14F,
                0.24F + random.nextFloat() * 0.08F);
        pickSprite(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new GrazingCrumbParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
