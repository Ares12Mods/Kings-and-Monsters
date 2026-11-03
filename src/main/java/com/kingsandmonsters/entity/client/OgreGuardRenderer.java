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
            // NONE gives the raw, uncentered generated-item quad instead of Mojang's built-in
            // "held upright like a sword" hand transform — we want the spear laid through the
            // fist with the tip trailing behind, not standing up like a sword.
            return ItemDisplayContext.NONE;
        }

        @Override
        protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, OgreGuard guard, MultiBufferSource bufferSource,
                                          float partialTick, int packedLight, int packedOverlay) {
            if (stack.isEmpty()) {
                return;
            }

            // right_hand_item's pivot sits at the fist, and a generated item's raw quad is
            // centered on its own origin, so a zero translate lands the sprite's center on the
            // hand ("through" it). Nudged slightly along local Z (bone-local depth axis, applied
            // here before the rotations below so it shifts the already-oriented spear rigidly
            // rather than interacting with its own rotation) to bring the hand fist onto the tan
            // grip section of the shaft instead of sitting at dead center. Sign is a first guess —
            // if this moves the grip further from the hand instead of onto it, flip to -0.15F.
            poseStack.translate(0.0F, 0.0F, 0.15F);
            // The spear texture is a diagonal icon (grip bottom-left, tip top-right, ~45
            // degrees): ZP undoes that baked diagonal so the shaft runs along local Y (straight
            // up in the sprite's own plane), then XP tips that vertical shaft back so it runs
            // front-to-back through the hand with the tip pointing behind the character. ZP must
            // be +45 (not -45) here: aligning to vertical is a symmetric goal for either arm, not
            // something that needs mirroring, and -45 was aligning the shaft to the local X axis
            // instead of Y — X-rotation can't fix that (it never touches a vector already on the
            // X axis), which is why it rendered as a flat horizontal line.
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            // Roll around the aligned shaft so the spearhead stands vertically instead of
            // lying flat and parallel to the ground. This does not alter the shaft direction.
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            // Correct the diagonal artwork while keeping the tip posterior, trailing behind the
            // Guard. The Y roll above still keeps the spearhead vertical.
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
            // Generated item sprites render at roughly one block long by default. Kept notably
            // below the trident-matching 2.0 used for the flying/held-in-hand versions — at that
            // size the spear reads as comically oversized against the guard's own body.
            // Slim-depth extrusion matches the vanilla spear profile while retaining
            // enough side surface to stay visible when viewed edge-on.
            poseStack.scale(1.4F, 1.4F, 0.8F);
            super.renderStackForBone(poseStack, bone, stack, guard, bufferSource, partialTick, packedLight, packedOverlay);
        }
    }
}
