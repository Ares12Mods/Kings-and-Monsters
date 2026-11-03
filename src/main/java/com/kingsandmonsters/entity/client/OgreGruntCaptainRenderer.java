package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreGruntCaptain;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class OgreGruntCaptainRenderer extends GeoEntityRenderer<OgreGruntCaptain> {
    private static final float CAPTAIN_SCALE = 1.05F;

    public OgreGruntCaptainRenderer(EntityRendererProvider.Context context) {
        super(context, new OgreGruntCaptainModel());
        this.shadowRadius = 0.74F;
        addRenderLayer(new CaptainSwordLayer(this));
    }

    @Override
    public void render(OgreGruntCaptain captain, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(CAPTAIN_SCALE, CAPTAIN_SCALE, CAPTAIN_SCALE);
        super.render(captain, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    private static final class CaptainSwordLayer extends BlockAndItemGeoLayer<OgreGruntCaptain> {
        private CaptainSwordLayer(GeoEntityRenderer<OgreGruntCaptain> renderer) {
            super(renderer,
                    (bone, captain) -> "right_hand_item".equals(bone.getName())
                            ? captain.getMainHandItem() : null,
                    (bone, captain) -> null);
        }

        @Override
        protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack,
                                                               OgreGruntCaptain captain) {
            return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }

        @Override
        protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                          OgreGruntCaptain captain, MultiBufferSource bufferSource,
                                          float partialTick, int packedLight, int packedOverlay) {
            if (stack.isEmpty()) {
                return;
            }

            // The vanilla hand transform points the blade down this model's long forearm.
            // Roll it forward at the wrist so the sword projects anteriorly instead.
            poseStack.translate(0.0F, 0.0F, -0.12F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.scale(1.75F, 1.75F, 1.75F);
            super.renderStackForBone(poseStack, bone, stack, captain, bufferSource,
                    partialTick, packedLight, packedOverlay);
        }
    }
}
