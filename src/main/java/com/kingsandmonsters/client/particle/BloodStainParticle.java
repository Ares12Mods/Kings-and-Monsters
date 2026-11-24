package com.kingsandmonsters.client.particle;

import com.kingsandmonsters.client.BloodParticleBudget;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A small blood spot lying flat on the ground: it grows to the artwork selected by the impacting
 * droplet's size tier, holds, then fades. Smaller spots retain the intentionally smaller painted
 * silhouettes instead of producing the largest art on a physically downscaled quad.
 * Rendered as a horizontal quad (never camera-facing), lit by world light, nearest-neighbor via the
 * particle atlas. Created only by {@link BloodDropletParticle} on valid ground.
 */
public final class BloodStainParticle extends TextureSheetParticle {
    private static final int FRAME_COUNT = 4;
    private static final int MIN_LIFETIME_TICKS = 120;
    private static final int LIFETIME_RANGE_TICKS = 61; // 120..180
    private static final int FADE_TICKS = 20;
    private static final float DECREASED_LIFETIME_SCALE = 0.5F;
    private static final float BASE_HALF_SIZE = 0.20F;
    private static final int SUPPORT_CHECK_INTERVAL_TICKS = 10;
    private static SpriteSet activeSprites;

    private final SpriteSet sprites;
    private final int generation = BloodParticleBudget.generation();
    private final float rotationCos;
    private final float rotationSin;
    private final BlockPos support;
    private final int finalFrame;
    private int ownerId = BloodParticleBudget.NO_OWNER;
    private boolean released;

    private BloodStainParticle(ClientLevel level, double x, double y, double z,
                               SpriteSet sprites, int sizeTier, float sizeMultiplier) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        float lifetimeScale = BloodParticleBudget.status() == ParticleStatus.ALL ? 1.0F : DECREASED_LIFETIME_SCALE;
        this.lifetime = (int) ((MIN_LIFETIME_TICKS + random.nextInt(LIFETIME_RANGE_TICKS)) * lifetimeScale);
        this.quadSize = BASE_HALF_SIZE * sizeMultiplier;
        // The four sprites are different painted puddle sizes. Small drops end on one of the two
        // small ones (0 or 1), medium on 2, large on 3; the quad size is the same for all of them.
        this.finalFrame = sizeTier <= 0 ? random.nextInt(2) : Math.min(sizeTier + 1, FRAME_COUNT - 1);
        float angle = random.nextFloat() * Mth.TWO_PI;
        this.rotationCos = Mth.cos(angle);
        this.rotationSin = Mth.sin(angle);
        this.support = BlockPos.containing(x, y - 0.05, z);
        setSprite(sprites.get(0, FRAME_COUNT - 1));
        BloodParticleBudget.stainAdded();
    }

    public void setOwner(int ownerId) {
        this.ownerId = ownerId;
        BloodParticleBudget.stainOwned(ownerId);
    }

    static void spawn(ClientLevel level, double x, double y, double z,
                      int ownerId, int sizeTier, float sizeMultiplier) {
        if (activeSprites == null || !BloodParticleBudget.canSpawnStain(ownerId)) return;
        BloodStainParticle stain = new BloodStainParticle(
                level, x, y, z, activeSprites, sizeTier, sizeMultiplier);
        stain.setOwner(ownerId);
        Minecraft.getInstance().particleEngine.add(stain);
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
        if (age % SUPPORT_CHECK_INTERVAL_TICKS == 0
                && (level.getBlockState(support).getCollisionShape(level, support).isEmpty()
                || !level.getFluidState(support.above()).isEmpty())) {
            remove();
            return;
        }
        setSprite(sprites.get(frameForAge(age), FRAME_COUNT - 1));
        alpha = Math.min(1.0F, (lifetime - age) / (float) FADE_TICKS);
    }

    private int frameForAge(int age) {
        // Reaches finalFrame on the same tick the droplet's last (blank) frame is reached.
        return Math.min(finalFrame, age * (finalFrame + 1) / BloodDropletParticle.IMPACT_TICKS);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float cx = (float) (x - cam.x);
        float cy = (float) (y - cam.y);
        float cz = (float) (z - cam.z);
        float s = quadSize;
        int light = getLightColor(partialTicks);
        float u0 = getU0();
        float u1 = getU1();
        float v0 = getV0();
        float v1 = getV1();
        // Corners counter-clockwise seen from above (+Y): north-west, south-west, south-east, north-east.
        corner(buffer, cx, cy, cz, -s, -s, u0, v0, light);
        corner(buffer, cx, cy, cz, -s, s, u0, v1, light);
        corner(buffer, cx, cy, cz, s, s, u1, v1, light);
        corner(buffer, cx, cy, cz, s, -s, u1, v0, light);
    }

    private void corner(VertexConsumer buffer, float cx, float cy, float cz,
                        float lx, float lz, float u, float v, int light) {
        float rx = lx * rotationCos - lz * rotationSin;
        float rz = lx * rotationSin + lz * rotationCos;
        buffer.vertex(cx + rx, cy, cz + rz)
                .uv(u, v)
                .color(rCol, gCol, bCol, alpha)
                .uv2(light)
                .endVertex();
    }

    @Override
    public void remove() {
        super.remove();
        if (!released) {
            released = true;
            BloodParticleBudget.stainRemoved(generation, ownerId);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
            activeSprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new BloodStainParticle(level, x, y, z, sprites, 1, 1.0F);
        }
    }
}
