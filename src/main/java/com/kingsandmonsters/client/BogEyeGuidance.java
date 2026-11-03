package com.kingsandmonsters.client;

import com.kingsandmonsters.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Client-only visual feedback for a successful Bog Eye Charm use: a magical particle trail plus
 * an outline highlight on the found chest. Both persist for as long as the player keeps holding
 * the charm and the chest remains unopened, rather than for a short fixed window. The server has
 * already done all the real chest-search work by the time {@link #start(BlockPos)} runs — this
 * class only ever reads the known target position, never rescans or pathfinds.
 */
public final class BogEyeGuidance {
    // Guidance now persists as long as the player keeps holding the Bog Eye Charm and the chest
    // stays unopened — not for a short fixed window. SAFETY_CAP_TICKS is just a generous fallback
    // in case something else fails to clear it; in normal play, holding the charm or opening the
    // chest is what actually ends the effect.
    private static final int SAFETY_CAP_TICKS = 20 * 60 * 20; // 20 minutes
    private static final int TRAIL_PARTICLE_INTERVAL_TICKS = 6;
    private static final int MAX_TRAIL_STEPS = 9;
    private static final int VALIDITY_CHECK_INTERVAL_TICKS = 10;
    private static final int HORIZONTAL_RANGE = 24;
    private static final int VERTICAL_RANGE = 12;

    private static final float OUTLINE_R = 0.88F;
    private static final float OUTLINE_G = 0.95F;
    private static final float OUTLINE_B = 1.0F;
    private static final float OUTLINE_A = 0.82F;
    private static final RenderType SEE_THROUGH_LINES = new RenderType(
            "kingsandmonsters_bog_eye_see_through_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            () -> {
                RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableCull();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
            },
            () -> {
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
            }) {
    };
    private static final RandomSource RANDOM = RandomSource.create();

    private static BlockPos target;
    private static ResourceKey<Level> targetDimension;
    private static int guidanceTicksRemaining;
    private static int validityCheckCooldown;
    private static double trailSeed;

    private BogEyeGuidance() {
    }

    public static void start(BlockPos targetPos) {
        Minecraft minecraft = Minecraft.getInstance();
        target = targetPos;
        targetDimension = minecraft.level == null ? null : minecraft.level.dimension();
        guidanceTicksRemaining = SAFETY_CAP_TICKS;
        validityCheckCooldown = VALIDITY_CHECK_INTERVAL_TICKS;
        trailSeed = RANDOM.nextDouble() * 1000.0;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (target == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || targetDimension != level.dimension()) {
            clear();
            return;
        }

        if (!isHoldingBogEye(minecraft.player)) {
            clear();
            return;
        }

        if (!level.isLoaded(target)) {
            // Never force-load target data or retain a ghost marker for an unloaded chunk.
            clear();
            return;
        }

        BlockPos playerPos = minecraft.player.blockPosition();
        int dx = target.getX() - playerPos.getX();
        int dy = target.getY() - playerPos.getY();
        int dz = target.getZ() - playerPos.getZ();
        if (Math.abs(dy) > VERTICAL_RANGE || dx * dx + dz * dz > HORIZONTAL_RANGE * HORIZONTAL_RANGE) {
            clear();
            return;
        }

        if (--validityCheckCooldown <= 0) {
            validityCheckCooldown = VALIDITY_CHECK_INTERVAL_TICKS;
            if (!isStillValid(level)) {
                clear();
                return;
            }
        }

        if (guidanceTicksRemaining > 0) {
            guidanceTicksRemaining--;
            if (guidanceTicksRemaining % TRAIL_PARTICLE_INTERVAL_TICKS == 0) {
                spawnTrailParticles(level, minecraft.player.getEyePosition());
            }
        } else {
            clear();
        }
    }

    private static boolean isHoldingBogEye(Player player) {
        return player.getMainHandItem().is(ModItems.BOG_EYE_CHARM.get())
                || player.getOffhandItem().is(ModItems.BOG_EYE_CHARM.get());
    }

    public static void render(RenderLevelStageEvent event) {
        if (target == null || guidanceTicksRemaining <= 0
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !level.isLoaded(target)) {
            return;
        }

        AABB bounds = highlightBounds(level, target).inflate(0.02);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(SEE_THROUGH_LINES),
                bounds, OUTLINE_R, OUTLINE_G, OUTLINE_B, OUTLINE_A);
        poseStack.popPose();
        buffers.endBatch(SEE_THROUGH_LINES);
    }

    private static void spawnTrailParticles(ClientLevel level, Vec3 eye) {
        Vec3 targetCenter = Vec3.atCenterOf(target);
        Vec3 toTarget = targetCenter.subtract(eye);
        double distance = toTarget.length();
        if (distance < 0.5) {
            return;
        }
        Vec3 direction = toTarget.scale(1.0 / distance);
        Vec3 perpendicular = perpendicular(direction);

        int steps = Math.min(MAX_TRAIL_STEPS, Math.max(3, (int) (distance / 2.0)));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / (steps + 1);
            Vec3 point = eye.add(direction.scale(distance * t));

            // Cheap deterministic wobble so the trail reads as drifting swamp magic rather than
            // a straight line — no pathfinding, just a sine offset perpendicular to the line.
            double wobble = Math.sin(trailSeed + t * Math.PI * 3.0) * 0.35 * Math.sin(t * Math.PI);
            point = point.add(perpendicular.scale(wobble));

            level.addParticle(ParticleTypes.CLOUD, point.x, point.y, point.z, 0.0, 0.008, 0.0);
        }
    }

    private static Vec3 perpendicular(Vec3 direction) {
        Vec3 helper = Math.abs(direction.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        return direction.cross(helper).normalize();
    }

    private static boolean isStillValid(ClientLevel level) {
        BlockEntity blockEntity = level.getBlockEntity(target);
        return blockEntity instanceof RandomizableContainerBlockEntity container
                && container.getLootTable() != null;
    }

    private static AABB highlightBounds(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Direction connected = ChestBlock.getConnectedDirection(state);
            Vec3i delta = pos.relative(connected).subtract(pos);
            return new AABB(pos).expandTowards(delta.getX(), delta.getY(), delta.getZ());
        }
        return new AABB(pos);
    }

    private static void clear() {
        target = null;
        targetDimension = null;
        guidanceTicksRemaining = 0;
    }
}
