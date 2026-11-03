package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreSpear;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Renders the flying spear as its own flat 2D item art (unlike vanilla's 3D-modelled
 * {@code ThrownTridentRenderer}) but oriented like a trident/arrow in flight — tip pointing
 * along the direction of travel — rather than using vanilla's {@code ThrownItemRenderer}, which
 * just billboards flat items to always face the camera (fine for a snowball, wrong for a spear).
 */
public class OgreSpearRenderer extends EntityRenderer<OgreSpear> {
    // Keep the flying weapon the same physical size as the spear held by the Ogre Guard.
    private static final float SCALE = 1.4F;

    public OgreSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(OgreSpear entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // Same yaw/pitch-from-velocity formula ThrownTridentRenderer and vanilla arrows use to
        // point a shaft-shaped model along the direction of travel.
        float yaw = entity.hasStablePostHitRotation()
                ? entity.getStablePostHitYaw()
                : Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = entity.hasStablePostHitRotation()
                ? entity.getStablePostHitPitch()
                : Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + 90.0F));
        // The spear texture is a diagonal generated-item icon (grip bottom-left, tip top-right,
        // ~45 degrees) — same correction used for the held pose in OgreGuardRenderer — so this
        // must run first (it's the last mulPose call, applied first to the raw sprite) to turn
        // that diagonal into a shaft pointing along local Y before the travel-direction rotations
        // above carry it the rest of the way. 225 (45 + 180), not 45: the yaw/pitch formula above
        // expects the model's tip at -Y, not +Y as it was for the held pose — confirmed backwards
        // (grip leading, tip trailing) in testing, so this is flipped from the held-pose value.
        poseStack.mulPose(Axis.ZP.rotationDegrees(225.0F));
        // Use the same slim-depth profile as the player-held and guard-held spear.
        poseStack.scale(SCALE, SCALE, 0.8F);
        Minecraft.getInstance().getItemRenderer().renderStatic(entity.getItem(), ItemDisplayContext.NONE, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(OgreSpear entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
