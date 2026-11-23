package com.kingsandmonsters.client.particle;

import com.kingsandmonsters.effect.BogrotMechanics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** An animated fly whose orbit remains centered on its afflicted owner. */
public final class BogrotFlyParticle extends TextureSheetParticle {
    private static final int FRAME_COUNT = 4;
    private static final int FRAME_TICKS = 2;
    private static SpriteSet activeSprites;
    private final SpriteSet sprites;
    private final int ownerId;
    private final int generation = BogrotParticleBudget.generation();
    private final float phase;
    private final float speed;
    private final float radiusScale;
    private final float heightScale;
    private boolean released;

    private BogrotFlyParticle(ClientLevel level, LivingEntity owner, SpriteSet sprites, int orbitIndex) {
        super(level, owner.getX(), owner.getY() + owner.getBbHeight() * 0.65, owner.getZ());
        this.sprites = sprites;
        this.ownerId = owner.getId();
        this.phase = random.nextFloat() * Mth.TWO_PI + orbitIndex * Mth.PI;
        this.speed = (random.nextBoolean() ? 1.0F : -1.0F) * (0.105F + random.nextFloat() * 0.035F);
        // Extra distance beyond the model edge; the particle's scaled visual radius is added too.
        this.radiusScale = 0.08F + random.nextFloat() * 0.16F;
        // Keep the orbit high: each fly spirals from the shoulders to just above the head.
        this.heightScale = 0.72F + random.nextFloat() * 0.12F;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.quadSize = 0.15F * modelScale(owner);
        this.lifetime = 45 + random.nextInt(11);
        setSprite(sprites.get(0, FRAME_COUNT - 1));
        BogrotParticleBudget.addFly(ownerId);
        updatePosition(owner);
    }

    public static void spawn(ClientLevel level, LivingEntity owner, int desired, int orbitIndex) {
        if (activeSprites == null || !BogrotParticleBudget.canAddFly(owner.getId(), desired)) return;
        Minecraft.getInstance().particleEngine.add(new BogrotFlyParticle(level, owner, activeSprites, orbitIndex));
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        Entity found = level.getEntity(ownerId);
        if (!(found instanceof LivingEntity owner) || !owner.isAlive()
                || !BogrotMechanics.showsVisuals(owner)) {
            remove();
            return;
        }
        setSprite(sprites.get((age / FRAME_TICKS) % FRAME_COUNT, FRAME_COUNT - 1));
        float fade = Math.min(age / 4.0F, (lifetime - age) / 4.0F);
        alpha = Mth.clamp(fade, 0.0F, 1.0F);
        updatePosition(owner);
    }

    private void updatePosition(LivingEntity owner) {
        float angle = phase + age * speed;
        // Orbit far enough out that the whole sprite clears the hitbox, not just its center.
        double radius = Math.max(0.28,
                owner.getBbWidth() * 0.5 + quadSize + owner.getBbWidth() * radiusScale)
                * (1.0 + Math.sin(age * 0.08 + phase) * 0.08);
        // Slow vertical wave so the orbit reads as a spiral rather than a flat ring.
        double spiral = (Math.sin(age * 0.115 + phase) + 1.0) * 0.5;
        double spiralHeight = spiral * Math.max(0.18, owner.getBbHeight() * 0.24);
        double bob = Math.sin(age * 0.29 + phase) * Math.max(0.025, owner.getBbHeight() * 0.025);
        setPos(owner.getX() + Math.cos(angle) * radius,
                owner.getY() + owner.getBbHeight() * heightScale + spiralHeight + bob,
                owner.getZ() + Math.sin(angle) * radius);
    }

    /** Player-sized creatures are the baseline; unusual models scale smoothly but stay readable. */
    private static float modelScale(LivingEntity owner) {
        double widthRatio = owner.getBbWidth() / 0.6;
        double heightRatio = owner.getBbHeight() / 1.8;
        return Mth.clamp((float) Math.sqrt(Math.max(0.01, widthRatio * heightRatio)), 0.7F, 2.25F);
    }

    @Override public void remove() {
        super.remove();
        if (!released) { released = true; BogrotParticleBudget.removeFly(ownerId, generation); }
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        public Provider(SpriteSet sprites) { activeSprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xd, double yd, double zd) {
            return null; // Owner-aware flies are created by BogrotParticleEmitter.
        }
    }
}
