package com.kingsandmonsters.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.kingsandmonsters.entity.OgreGuard;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.cache.object.GeoBone;

public class OgreGuardRenderer extends GeoEntityRenderer<OgreGuard> {

    public OgreGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new OgreGuardModel());
        this.shadowRadius = 0.55f;
        addRenderLayer(new GuardSpearLayer(this));
    }

    private static class GuardSpearLayer extends BlockAndItemGeoLayer<OgreGuard> {
        private GuardSpearLayer(GeoEntityRenderer<OgreGuard> renderer) {
            super(renderer, (bone, guard) -> "right_hand_item".equals(bone.getName()) && guard.shouldRenderHeldSpear()
                    ? guard.getMainHandItem() : null, (bone, guard) -> null);
        }

        @Override
        protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, OgreGuard guard) {
            // Use the raw item quad so the spear can lie through the fist instead of standing upright.
            return ItemDisplayContext.NONE;
        }

        @Override
        protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, OgreGuard guard, MultiBufferSource bufferSource,
                                          float partialTick, int packedLight, int packedOverlay) {
            if (stack.isEmpty()) {
                return;
            }

            // Stay neutral through the windup, then ease the spear 20% of a quarter-turn downward
            // from 0.58s to 0.71s before returning smoothly during recovery.
            float stabTilt = guard.getStabSpearTiltDegrees(partialTick);
            if (stabTilt != 0.0F) {
                poseStack.mulPose(Axis.XP.rotationDegrees(stabTilt));
            }
            // Facing a target within reach he holds the spear for a stab: the same pose turned half way round
            // the hand, so the tip points forward instead of trailing behind.
            if (guard.isSpearStabPose()) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            // Offset the raw quad so the authored grip sits at the hand-bone pivot.
            poseStack.translate(0.0F, 0.0F, 0.15F);
            // Remove the icon's baked diagonal, then lay the shaft front-to-back through the hand.
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            // Roll around the shaft so the spearhead remains vertical.
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            // Keep the tip trailing behind the Guard.
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
            // Use a slim profile while retaining enough depth to remain visible edge-on.
            poseStack.scale(1.4F, 1.4F, 0.8F);
            super.renderStackForBone(poseStack, bone, stack, guard, bufferSource, partialTick, packedLight, packedOverlay);
        }
    }
}
