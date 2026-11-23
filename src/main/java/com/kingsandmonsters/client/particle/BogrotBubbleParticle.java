package com.kingsandmonsters.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** One-shot form, float, swell, pop, and decay animation. */
public final class BogrotBubbleParticle extends TextureSheetParticle {
    private static final int FRAME_COUNT = 5;
    private static SpriteSet activeSprites;
    private final SpriteSet sprites;
    private final int generation = BogrotParticleBudget.generation();
    private final int formTicks;
    private final int swellTicks;
    private final float baseSize;
    private final float wobblePhase;
    private boolean released;

    private BogrotBubbleParticle(ClientLevel level, double x, double y, double z,
                                 float ownerScale, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        // Give the intact bubble time to hover where the player can actually read it before it pops.
        this.formTicks = 9 + random.nextInt(5);
        this.swellTicks = 6 + random.nextInt(4);
        this.lifetime = formTicks + swellTicks + 2 + 2 + 1 + random.nextInt(2);
        this.baseSize = 0.18F * ownerScale * (0.8F + random.nextFloat() * 0.4F);
        this.quadSize = baseSize;
        this.wobblePhase = random.nextFloat() * Mth.TWO_PI;
        this.yd = 0.012 + random.nextDouble() * 0.005;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        setSprite(sprites.get(0, FRAME_COUNT - 1));
        BogrotParticleBudget.addBubble();
    }

    public static void spawn(ClientLevel level, double x, double y, double z, float ownerScale) {
        if (activeSprites == null || !BogrotParticleBudget.canAddBubble()) return;
        Minecraft.getInstance().particleEngine.add(
                new BogrotBubbleParticle(level, x, y, z, ownerScale, activeSprites));
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        int popStart = formTicks + swellTicks;
        int frame;
        if (age < formTicks) frame = 0;
        else if (age < popStart) frame = 1;
        else if (age < popStart + 2) frame = 2;
        else if (age < popStart + 4) frame = 3;
        else frame = 4;
        setSprite(sprites.get(frame, FRAME_COUNT - 1));

        if (age < popStart) {
            double slowdown = 1.0 - 0.55 * age / (double) popStart;
            // A slightly wider, slow corkscrew keeps it floating around the body instead of rising
            // as a perfectly straight column. Stop all travel as soon as the pop begins.
            double wobble = Math.sin(age * 0.27 + wobblePhase) * 0.0032;
            move(wobble, yd * slowdown, Math.cos(age * 0.24 + wobblePhase) * 0.0032);
            quadSize = baseSize * Mth.lerp(age / (float) popStart, 0.9F, 1.16F);
        } else {
            xd = yd = zd = 0.0;
            quadSize = baseSize * (frame == 2 ? 1.2F : 1.0F);
        }
        alpha = frame == 4 ? Math.max(0.0F, (lifetime - age) / 2.0F) : 1.0F;
    }

    @Override public void remove() {
        super.remove();
        if (!released) { released = true; BogrotParticleBudget.removeBubble(generation); }
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        public Provider(SpriteSet sprites) { activeSprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xd, double yd, double zd) {
            return new BogrotBubbleParticle(level, x, y, z, 1.0F, activeSprites);
        }
    }
}
