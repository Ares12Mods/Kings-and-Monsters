package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.PoisonFogCloud;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class PoisonFogCloudRenderer extends EntityRenderer<PoisonFogCloud> {
    private static final ResourceLocation WARNING_CIRCLE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "textures/entity/spell/poison_warning_circle.png");
    private static final ResourceLocation CLOUD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "textures/entity/spell/poison_cloud.png");
    private static final float CIRCLE_SIZE = PoisonFogCloud.VISUAL_RING_RADIUS * 2.0F;
    private static final float CLOUD_GROWTH_FRACTION = 0.25F;
    private static final float CLOUD_MIN_SCALE = 0.15F;
    private static final int CLOUD_FRAME_COUNT = 25;
    private static final int CLOUD_LOOP_START_FRAME = 7;
    private static final int CLOUD_LOOP_END_FRAME = 19;
    private static final int CLOUD_DISSIPATION_START_FRAME = 19;
    private static final int CLOUD_INTRO_TICKS_PER_FRAME = 3;
    private static final int CLOUD_LOOP_TICKS_PER_FRAME = 4;
    private static final int CLOUD_DISSIPATION_TICKS_PER_FRAME = 3;
    private static final boolean USE_PUFF_CLUSTER = true;
    private static final float SINGLE_CLOUD_SIZE = 5.6F;
    private static final float CLOUD_HORIZONTAL_SPREAD = 1.10F;
    private static final float CLOUD_PUFF_SIZE_SCALE = 1.28F;

    // A dense center and irregular outer puffs form one low, rolling cloud while
    // remaining inside the poison zone's 3.2-block warning circle.
    private static final CloudPuff[] CLOUD_PUFFS = {
            new CloudPuff(0.00F, 0.55F, 0.00F, 1.70F, 0),
            new CloudPuff(-0.62F, 0.42F, 0.28F, 1.48F, 3),
            new CloudPuff(0.58F, 0.38F, -0.35F, 1.52F, 7),
            new CloudPuff(0.30F, 0.62F, 0.72F, 1.35F, 10),
            new CloudPuff(-0.28F, 0.58F, -0.78F, 1.38F, 5),
            new CloudPuff(-1.18F, 0.30F, -0.42F, 1.30F, 8),
            new CloudPuff(1.22F, 0.27F, 0.38F, 1.34F, 2),
            new CloudPuff(-0.96F, 0.26F, 0.96F, 1.22F, 6),
            new CloudPuff(0.92F, 0.32F, -1.08F, 1.28F, 11),
            new CloudPuff(0.10F, 0.24F, 1.55F, 1.18F, 4),
            new CloudPuff(-0.12F, 0.22F, -1.62F, 1.16F, 9),
            new CloudPuff(-1.72F, 0.18F, 0.48F, 1.10F, 1),
            new CloudPuff(1.78F, 0.20F, -0.38F, 1.12F, 7),
            new CloudPuff(-1.52F, 0.16F, -1.24F, 1.02F, 10),
            new CloudPuff(1.48F, 0.17F, 1.28F, 1.06F, 3),
            new CloudPuff(-0.62F, 0.14F, 2.05F, 0.98F, 6),
            new CloudPuff(0.72F, 0.15F, -2.12F, 1.00F, 0),
            new CloudPuff(-2.18F, 0.12F, 0.02F, 0.94F, 8),
            new CloudPuff(2.22F, 0.13F, 0.18F, 0.96F, 4)
    };

    public PoisonFogCloudRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(PoisonFogCloud entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (entity.isWarningPhase()) {
            renderGroundSprite(WARNING_CIRCLE_TEXTURE, poseStack, bufferSource, packedLight, CIRCLE_SIZE, 0.95F);
        } else {
            float progress = entity.getVisualProgress(partialTick);
            float ringFade = Math.max(0.0F, 1.0F - progress / 0.1F);
            if (ringFade > 0.0F) {
                renderGroundSprite(WARNING_CIRCLE_TEXTURE, poseStack, bufferSource, packedLight, CIRCLE_SIZE, 0.35F * ringFade);
            }
            renderCloud(entity, partialTick, poseStack, bufferSource, packedLight, progress);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PoisonFogCloud entity) {
        return entity.isWarningPhase() ? WARNING_CIRCLE_TEXTURE : CLOUD_TEXTURE;
    }

    private void renderGroundSprite(ResourceLocation texture, PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, float size, float alpha) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.035, 0.0);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        float halfSize = size * 0.5F;

        addVertex(consumer, matrix, -halfSize, 0.0F, -halfSize, 0.0F, 1.0F, alpha, packedLight);
        addVertex(consumer, matrix, halfSize, 0.0F, -halfSize, 1.0F, 1.0F, alpha, packedLight);
        addVertex(consumer, matrix, halfSize, 0.0F, halfSize, 1.0F, 0.0F, alpha, packedLight);
        addVertex(consumer, matrix, -halfSize, 0.0F, halfSize, 0.0F, 0.0F, alpha, packedLight);
        poseStack.popPose();
    }

    private void renderBillboard(ResourceLocation texture, PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, float size, float alpha, int frameIndex) {
        poseStack.pushPose();
        poseStack.translate(0.0, size * 0.42F, 0.0);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        float halfSize = size * 0.5F;
        float frameHeight = 1.0F / CLOUD_FRAME_COUNT;
        float minV = frameIndex * frameHeight;
        float maxV = minV + frameHeight;

        addVertex(consumer, matrix, -halfSize, -halfSize, 0.0F, 0.0F, maxV, alpha, packedLight);
        addVertex(consumer, matrix, halfSize, -halfSize, 0.0F, 1.0F, maxV, alpha, packedLight);
        addVertex(consumer, matrix, halfSize, halfSize, 0.0F, 1.0F, minV, alpha, packedLight);
        addVertex(consumer, matrix, -halfSize, halfSize, 0.0F, 0.0F, minV, alpha, packedLight);
        poseStack.popPose();
    }

    private void renderCloud(PoisonFogCloud entity, float partialTick, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, float progress) {
        float growth = Math.min(1.0F, progress / CLOUD_GROWTH_FRACTION);
        float eased = 1.0F - (1.0F - growth) * (1.0F - growth);
        float scale = Math.max(CLOUD_MIN_SCALE, eased);
        float alpha = 0.45F + Math.min(1.0F, progress / CLOUD_GROWTH_FRACTION) * 0.45F;

        float time = entity.tickCount + partialTick;
        float bob = (float) Math.sin(time * 0.06F) * 0.08F;

        poseStack.pushPose();
        poseStack.translate(0.0F, bob, 0.0F);
        if (USE_PUFF_CLUSTER) {
            for (CloudPuff puff : CLOUD_PUFFS) {
                poseStack.pushPose();
                poseStack.translate(
                        puff.x * scale * CLOUD_HORIZONTAL_SPREAD,
                        puff.y * scale,
                        puff.z * scale * CLOUD_HORIZONTAL_SPREAD);
                renderBillboard(CLOUD_TEXTURE, poseStack, bufferSource, packedLight,
                        puff.size * scale * CLOUD_PUFF_SIZE_SCALE, alpha * 0.95F,
                        getCloudFrame(entity, partialTick, puff.loopOffset));
                poseStack.popPose();
            }
        } else {
            renderBillboard(CLOUD_TEXTURE, poseStack, bufferSource, packedLight,
                    SINGLE_CLOUD_SIZE * scale, alpha,
                    getCloudFrame(entity, partialTick, 0));
        }
        poseStack.popPose();
    }

    private int getCloudFrame(PoisonFogCloud entity, float partialTick, int loopOffset) {
        int activeTick = Math.max(0, (int) (entity.tickCount + partialTick - PoisonFogCloud.WARNING_TICKS));
        int introDuration = CLOUD_LOOP_START_FRAME * CLOUD_INTRO_TICKS_PER_FRAME;
        if (activeTick < introDuration) {
            return Math.min(CLOUD_LOOP_START_FRAME - 1, activeTick / CLOUD_INTRO_TICKS_PER_FRAME);
        }

        if (entity.isDissipating()) {
            int dissipationTick = Math.max(
                    0,
                    (int) (entity.tickCount + partialTick
                            - PoisonFogCloud.WARNING_TICKS
                            - PoisonFogCloud.ACTIVE_TICKS));
            return Math.min(
                    CLOUD_FRAME_COUNT - 1,
                    CLOUD_DISSIPATION_START_FRAME
                            + dissipationTick / CLOUD_DISSIPATION_TICKS_PER_FRAME);
        }

        int loopFrameCount = CLOUD_LOOP_END_FRAME - CLOUD_LOOP_START_FRAME;
        int loopTick = activeTick - introDuration;
        return CLOUD_LOOP_START_FRAME
                + (loopTick / CLOUD_LOOP_TICKS_PER_FRAME + loopOffset) % loopFrameCount;
    }

    private void addVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                           float u, float v, float alpha, int packedLight) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, (int) (alpha * 255.0F))
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private record CloudPuff(float x, float y, float z, float size, int loopOffset) {
    }
}
