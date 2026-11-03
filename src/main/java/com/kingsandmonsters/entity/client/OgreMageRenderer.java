package com.kingsandmonsters.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.kingsandmonsters.entity.OgreMage;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class OgreMageRenderer extends GeoEntityRenderer<OgreMage> {

    public OgreMageRenderer(EntityRendererProvider.Context context) {
        super(context, new OgreMageModel());
        this.shadowRadius = 0.65f;
        addRenderLayer(new MagePotionLayer(this));
    }

    private static class MagePotionLayer extends BlockAndItemGeoLayer<OgreMage> {
        private MagePotionLayer(GeoEntityRenderer<OgreMage> renderer) {
            super(renderer, (bone, mage) -> "left_hand_item".equals(bone.getName()) ? mage.getVisiblePotionStack() : null, (bone, mage) -> null);
        }

        @Override
        protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, OgreMage mage) {
            // Render directly around the authored hand-bone pivot. The vanilla third-person hand
            // transform adds a humanoid-arm offset that detaches the bottle from this larger model.
            return ItemDisplayContext.NONE;
        }

        @Override
        protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, OgreMage mage, MultiBufferSource bufferSource,
                                          float partialTick, int packedLight, int packedOverlay) {
            if (stack.isEmpty()) {
                return;
            }

            poseStack.scale(0.8F, 0.8F, 0.8F);
            super.renderStackForBone(poseStack, bone, stack, mage, bufferSource, partialTick, packedLight, packedOverlay);
        }
    }
}
