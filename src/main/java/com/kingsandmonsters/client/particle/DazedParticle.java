package com.kingsandmonsters.client.particle;

import com.kingsandmonsters.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * One small yellow swirl that circles a Dazed entity's head and follows it while it moves.
 */
public final class DazedParticle extends TextureSheetParticle {
    private static final float BASE_SIZE = 0.07F;
    private static final float SPIN_PER_TICK = 0.22F;
    private static SpriteSet activeSprites;

    private final SpriteSet sprites;
    private final int entityId;
    private final float baseSize;
    private final double startHeight;
    private final double orbitRadius;
    private final float startAngle;
    private final float spinDirection;
    private final double wobble;

    private DazedParticle(ClientLevel level, LivingEntity entity, float scale, SpriteSet sprites) {
        super(level, entity.getX(), entity.getY(), entity.getZ());
        this.sprites = sprites;
        this.entityId = entity.getId();
        this.baseSize = BASE_SIZE * scale;
        this.lifetime = Integer.MAX_VALUE;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.xd = this.yd = this.zd = 0.0;
        this.startHeight = Mth.lerp(random.nextDouble(), 0.92, 1.02);
        // Keep the restrained status cue around the head instead of filling the whole body.
        this.orbitRadius = entity.getBbWidth() * 0.32 + 0.05 * scale + random.nextDouble() * 0.06;
        this.startAngle = random.nextFloat() * Mth.TWO_PI;
        this.spinDirection = random.nextBoolean() ? 1.0F : -1.0F;
        this.wobble = random.nextDouble() * Mth.TWO_PI;
        this.quadSize = baseSize;
        setAnimatedSprite();
        place(entity, 0.0F);
    }

    private void setAnimatedSprite() {
        int frame = (age / 4) % 4;
        setSprite(sprites.get(frame, 3));
    }

    private void place(LivingEntity entity, float progress) {
        float angle = startAngle + spinDirection * SPIN_PER_TICK * age;
        double height = entity.getBbHeight() * (startHeight + 0.025 * Math.sin(age * 0.16 + wobble));
        double radius = orbitRadius * (1.0 + 0.12 * Math.sin(age * 0.4 + wobble));
        setPos(entity.getX() + Mth.cos(angle) * radius, entity.getY() + height,
                entity.getZ() + Mth.sin(angle) * radius);
    }

    @Override
    public void tick() {
        if (!(level.getEntity(entityId) instanceof LivingEntity entity)
                || !entity.isAlive() || !entity.hasEffect(ModMobEffects.DAZED)) {
            remove();
            return;
        }
        xo = x;
        yo = y;
        zo = z;
        age++;
        setAnimatedSprite();
        float progress = (age % 80) / 79.0F;
        place(entity, progress);
        alpha = 1.0F;
        quadSize = baseSize * (0.9F + 0.1F * Mth.sin(age * 0.18F));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Added straight to the engine so particle-setting throttling cannot swallow the status visual. */
    public static DazedParticle spawn(ClientLevel level, LivingEntity entity, float scale) {
        if (activeSprites == null) return null;
        DazedParticle particle = new DazedParticle(level, entity, scale, activeSprites);
        Minecraft.getInstance().particleEngine.add(particle);
        return particle;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        public Provider(SpriteSet sprites) {
            activeSprites = sprites;
        }

        /** Not used in play: the swirls are spawned by the emitter, which knows the afflicted entity. */
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return null;
        }
    }
}
