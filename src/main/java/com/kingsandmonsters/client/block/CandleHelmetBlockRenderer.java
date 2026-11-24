package com.kingsandmonsters.client.block;

import com.kingsandmonsters.block.entity.CandleHelmetBlockEntity;
import com.kingsandmonsters.block.CandleHelmetBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class CandleHelmetBlockRenderer extends GeoBlockRenderer<CandleHelmetBlockEntity> {

    public CandleHelmetBlockRenderer() {
        super(new CandleHelmetBlockModel());
    }

    @Override
    public void actuallyRender(PoseStack poseStack, CandleHelmetBlockEntity animatable, BakedGeoModel model,
                               @Nullable RenderType renderType, MultiBufferSource bufferSource,
                               @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate(0.0, CandleHelmetBlock.DROP_TO_FLOOR, 0.0);
        switch (animatable.getBlockState().getValue(CandleHelmetBlock.FACING)) {
            case NORTH -> poseStack.translate(0.0, 0.0, CandleHelmetBlock.BACKWARD_OFFSET);
            case SOUTH -> poseStack.translate(0.0, 0.0, -CandleHelmetBlock.BACKWARD_OFFSET);
            case EAST -> poseStack.translate(-CandleHelmetBlock.BACKWARD_OFFSET, 0.0, 0.0);
            case WEST -> poseStack.translate(CandleHelmetBlock.BACKWARD_OFFSET, 0.0, 0.0);
            default -> { }
        }
        // GeoBlockRenderer applies the block's yaw later. Rotate around the matching world-space
        // horizontal axis here so this remains a forward pitch for every placement direction,
        // rather than becoming a sideways roll for east/west-facing helmets.
        switch (animatable.getBlockState().getValue(CandleHelmetBlock.FACING)) {
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-CandleHelmetBlock.FORWARD_TILT_DEGREES));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(CandleHelmetBlock.FORWARD_TILT_DEGREES));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-CandleHelmetBlock.FORWARD_TILT_DEGREES));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(CandleHelmetBlock.FORWARD_TILT_DEGREES));
            default -> { }
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }
}
