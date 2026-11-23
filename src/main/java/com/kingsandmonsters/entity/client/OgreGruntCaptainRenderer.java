package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.ModItems;
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
    private static final float IRON_SWORD_SCALE = 1.75F;
    private static final float BOG_IRON_SWORD_SCALE = 1.85F;

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
            boolean bogIronSword = stack.is(ModItems.BOG_IRON_SWORD.get());
            poseStack.translate(0.0F, 0.0F, bogIronSword ? -0.15F : -0.12F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            // The Bog Iron Sword's painted silhouette is smaller than vanilla's Iron Sword.
            // Give only that variant a modest boost so its handguard clears the captain's fist
            // while preserving the exact same hand anchor and rotation.
            float swordScale = bogIronSword ? BOG_IRON_SWORD_SCALE : IRON_SWORD_SCALE;
            poseStack.scale(swordScale, swordScale, swordScale);
            super.renderStackForBone(poseStack, bone, stack, captain, bufferSource,
                    partialTick, packedLight, packedOverlay);
        }
    }
}
