package com.kingsandmonsters.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.kingsandmonsters.entity.OgreArcher;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.cache.object.GeoBone;

public class OgreArcherRenderer extends GeoEntityRenderer<OgreArcher> {

    public OgreArcherRenderer(EntityRendererProvider.Context context) {
        super(context, new OgreArcherModel());
        this.shadowRadius = 0.55f;
        addRenderLayer(new ArcherBowLayer(this));
    }

    private static class ArcherBowLayer extends BlockAndItemGeoLayer<OgreArcher> {
        private ArcherBowLayer(GeoEntityRenderer<OgreArcher> renderer) {
            super(renderer, (bone, archer) -> "right_hand_item".equals(bone.getName()) ? archer.getMainHandItem() : null, (bone, archer) -> null);
        }

        @Override
        protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, OgreArcher archer) {
            return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }

        @Override
        protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, OgreArcher archer, MultiBufferSource bufferSource,
                                          float partialTick, int packedLight, int packedOverlay) {
            if (stack.isEmpty()) {
                return;
            }

            poseStack.translate(0.0F, 0.0F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-78.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(174.0F));
            poseStack.scale(1.05F, 1.05F, 1.05F);
            super.renderStackForBone(poseStack, bone, stack, archer, bufferSource, partialTick, packedLight, packedOverlay);
        }
    }
}
