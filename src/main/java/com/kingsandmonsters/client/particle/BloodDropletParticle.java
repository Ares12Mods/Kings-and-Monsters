package com.kingsandmonsters.client.particle;

import com.kingsandmonsters.client.BloodParticleBudget;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A single airborne blood drop. Arcs outward, falls under its own gravity with block collision, and
 * on landing on a valid top surface turns into one flat {@link BloodStainParticle}. Purely cosmetic.
 */
public final class BloodDropletParticle extends TextureSheetParticle {
    // Frame 0 is the complete droplet and is the only airborne frame. Frames 1-4 are the droplet
    // shrinking away (frame 4 is empty) and play only after confirmed ground contact.
    private static final int FRAME_COUNT = 5;
    private static final int IMPACT_FIRST_FRAME = 1;
    private static final int IMPACT_FRAME_TICKS = 2;
    /** Frame 4 is blank, so the droplet is removed as it would be shown; the puddle completes on this tick. */
    public static final int IMPACT_TICKS = (FRAME_COUNT - IMPACT_FIRST_FRAME - 1) * IMPACT_FRAME_TICKS;
    /** Lowest painted pixel row (0 = top) of frames 0-3; the art sinks toward the bottom of the 16px canvas. */
    private static final int[] FRAME_BOTTOM_ROW = {8, 11, 14, 15};
    private static final float CANVAS_HALF_ROWS = 8.0F;
    private static final float FORM_START = 0.35F;
    private static final float FORM_PER_TICK = 0.16F;
    private static final float NEAR_FADE_START = 0.5F;
    private static final float NEAR_FADE_END = 1.5F;
    private static final double GRAVITY_PER_TICK = 0.014;
    private static final double AIR_DRAG = 0.90;
    private static final int MAX_AIRBORNE_TICKS = 48;
    // TextureSheetParticle uses this as a half-size. 0.05 made the few painted pixels in the
    // 16x16 artwork effectively invisible at ordinary camera distances.
    private static final float BASE_QUAD_SIZE = 0.26F;
    private static final float SMALL_SIZE_SCALE = 0.75F;
    private static final float MEDIUM_SIZE_SCALE = 1.0F;
    private static final float LARGE_SIZE_SCALE = 1.25F;
    private static final float COLLISION_SIZE = 0.1F;
    /** The stain is only placed if the collision top is this close to where the droplet came to rest. */
    private static final double SURFACE_TOLERANCE = 0.35;
    private static final double STAIN_LIFT = 0.012;
    private static final double STAIN_LIFT_JITTER = 0.004;
    private static SpriteSet activeSprites;

    private final SpriteSet sprites;
    private final int sizeTier;
    private final float sizeScale;
    private final int generation = BloodParticleBudget.generation();
    private int ownerId = BloodParticleBudget.NO_OWNER;
    private float entitySizeScale = 1.0F;
    private boolean impacting;
    private int impactAge;
    private double groundY;
    private boolean released;
    private final float naturalTilt;
    private final float settledTiltRetention;
    private final boolean angledFlight;

    private BloodDropletParticle(ClientLevel level, double x, double y, double z,
                                 double xd, double yd, double zd, SpriteSet sprites, int severity,
                                 boolean angledFlight) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.angledFlight = angledFlight;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.hasPhysics = true;
        this.gravity = 0.0F;
        this.lifetime = MAX_AIRBORNE_TICKS;
        // Distinct tiers make the landing result readable: the exact same tier is handed to the
        // puddle, so large drops never create tiny stains and small drops never create huge ones.
        this.sizeTier = chooseSizeTier(Mth.clamp(severity, 0, 2));
        this.sizeScale = sizeScaleForTier(sizeTier);
        this.quadSize = BASE_QUAD_SIZE * sizeScale;
        this.naturalTilt = Mth.lerp(random.nextFloat(), -0.18F, 0.18F);
        this.settledTiltRetention = random.nextBoolean()
                ? 0.0F : Mth.lerp(random.nextFloat(), 0.2F, 0.4F);
        this.roll = naturalTilt;
        this.oRoll = roll;
        setSize(COLLISION_SIZE, COLLISION_SIZE);
        setPos(x, y, z);
        setSprite(sprites.get(0, FRAME_COUNT - 1));
        BloodParticleBudget.dropletAdded();
    }

    public void setOwner(int ownerId) {
        this.ownerId = ownerId;
    }

    /** Spawns exactly once while preserving the bleeding entity for its per-owner stain budget. */
    public static void spawn(ClientLevel level, double x, double y, double z,
                             double xd, double yd, double zd, int ownerId, int severity, float sizeMultiplier,
                             boolean angledFlight) {
        if (activeSprites == null || !BloodParticleBudget.canSpawnDroplet()) return;
        BloodDropletParticle droplet = new BloodDropletParticle(
                level, x, y, z, xd, yd, zd, activeSprites, severity, angledFlight);
        droplet.entitySizeScale = sizeMultiplier;
        droplet.quadSize *= sizeMultiplier;
        droplet.setOwner(ownerId);
        Minecraft.getInstance().particleEngine.add(droplet);
    }

    private int chooseSizeTier(int severity) {
        float roll = random.nextFloat();
        return switch (severity) {
            // Lesser: 70% small, 25% medium, 5% large.
            case 0 -> roll < 0.70F ? 0 : roll < 0.95F ? 1 : 2;
            // Greater: 15% small, 40% medium, 45% large.
            case 2 -> roll < 0.15F ? 0 : roll < 0.55F ? 1 : 2;
            // Normal: 30% small, 50% medium, 20% large.
            default -> roll < 0.30F ? 0 : roll < 0.80F ? 1 : 2;
        };
    }

    private static float sizeScaleForTier(int sizeTier) {
        return switch (sizeTier) {
            case 0 -> SMALL_SIZE_SCALE;
            case 2 -> LARGE_SIZE_SCALE;
            default -> MEDIUM_SIZE_SCALE;
        };
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (impacting) {
            if (++impactAge >= IMPACT_TICKS) {
                remove();
                return;
            }
            applyImpactFrame();
            return;
        }
        if (age++ >= lifetime) {
            remove();
            return;
        }
        // The complete droplet stays on frame 0 for the whole flight.
        setSprite(sprites.get(0, FRAME_COUNT - 1));

        yd -= GRAVITY_PER_TICK;
        move(xd, yd, zd);
        xd *= AIR_DRAG;
        yd *= AIR_DRAG;
        zd *= AIR_DRAG;

        if (!level.getFluidState(BlockPos.containing(x, y, z)).isEmpty()) {
            remove();
            return;
        }
        if (onGround) {
            if (land()) {
                impacting = true;
                impactAge = 0;
                hasPhysics = false;
                xd = yd = zd = 0.0;
                applyImpactFrame();
            } else {
                remove();
            }
        }
    }

    /** Shrinking frame for the current impact tick, with its lowest painted row resting on the ground. */
    private void applyImpactFrame() {
        int frame = IMPACT_FIRST_FRAME + impactAge / IMPACT_FRAME_TICKS;
        setSprite(sprites.get(frame, FRAME_COUNT - 1));
        setPos(x, groundY + quadSize * (FRAME_BOTTOM_ROW[frame] + 1 - CANVAS_HALF_ROWS) / CANVAS_HALF_ROWS, z);
    }

    /** Converts the droplet into a stain, but only on a real, dry, upward-facing collision surface. */
    private boolean land() {
        if (!BloodParticleBudget.canSpawnStain(ownerId)) return false;
        Surface surface = findLandingSurface();
        if (surface == null) return false;
        double topY = surface.topY();
        BlockPos at = BlockPos.containing(x, topY + STAIN_LIFT, z);
        if (!level.getFluidState(at).isEmpty()
                || !level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
            return false;
        }
        BloodStainParticle.spawn(level,
                x, topY + STAIN_LIFT + random.nextDouble() * STAIN_LIFT_JITTER, z,
                ownerId, sizeTier, entitySizeScale);
        groundY = topY;
        return true;
    }

    /** Particle collision can stop a small quad slightly above a slab or uneven collision shape. */
    private Surface findLandingSurface() {
        BlockPos start = BlockPos.containing(x, y + 0.15, z);
        for (int offset = 0; offset <= 2; offset++) {
            BlockPos candidate = start.below(offset);
            BlockState floor = level.getBlockState(candidate);
            if (!floor.getFluidState().isEmpty()) continue;
            VoxelShape shape = floor.getCollisionShape(level, candidate);
            if (shape.isEmpty()) continue;
            double topY = candidate.getY() + shape.max(Direction.Axis.Y);
            if (y >= topY - SURFACE_TOLERANCE && y <= topY + SURFACE_TOLERANCE) {
                return new Surface(candidate, topY);
            }
        }
        return null;
    }

    private record Surface(BlockPos pos, double topY) {}

    @Override
    public void remove() {
        super.remove();
        if (!released) {
            released = true;
            BloodParticleBudget.dropletRemoved(generation);
        }
    }

    /** Your own drops shrink to nothing as they near the camera, so one passing the lens never fills the screen. */
    @Override
    public float getQuadSize(float partialTicks) {
        if (ownerId == Minecraft.getInstance().player.getId()
                && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return 0.0F;
        }
        // A drop swells as it forms before it lets go.
        float formed = impacting ? 1.0F : Math.min(1.0F, FORM_START + (age + partialTicks) * FORM_PER_TICK);
        float size = quadSize * formed;
        if (ownerId != Minecraft.getInstance().player.getId()) return size;
        Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = Mth.lerp(partialTicks, xo, x) - eye.x;
        double dy = Mth.lerp(partialTicks, yo, y) - eye.y;
        double dz = Mth.lerp(partialTicks, zo, z) - eye.z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return size * Mth.clamp((distance - NEAR_FADE_START) / (NEAR_FADE_END - NEAR_FADE_START), 0.0F, 1.0F);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (!impacting) {
            var left = camera.getLeftVector();
            var up = camera.getUpVector();
            double screenRight = -(xd * left.x() + yd * left.y() + zd * left.z());
            double screenUp = xd * up.x() + yd * up.y() + zd * up.z();
            // Frame zero is painted pointing down. Rotate that axis onto the visible flight path.
            float pathAngle = (float) Math.atan2(screenRight, -screenUp);
            float minimumAngle = angledFlight ? 0.26F * Mth.clamp(1.0F - age / 8.0F, 0.0F, 1.0F) : 0.0F;
            if (Math.abs(pathAngle) < minimumAngle) {
                pathAngle = screenRight < 0.0 || screenRight == 0.0 && naturalTilt < 0.0F
                        ? -minimumAngle : minimumAngle;
            }
            float tiltFade = settledTiltRetention
                    + (1.0F - settledTiltRetention) * Mth.clamp(1.0F - age / 10.0F, 0.0F, 1.0F);
            roll = pathAngle + naturalTilt * tiltFade;
            oRoll = roll;
        }
        super.render(buffer, camera, partialTicks);
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
            return new BloodDropletParticle(level, x, y, z, xd, yd, zd, sprites, 1, false);
        }
    }
}
