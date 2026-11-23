package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.entity.OgreGrunt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class OgreGruntRenderer extends GeoEntityRenderer<OgreGrunt> {
    private static final float BONE_CLUB_SCALE = 1.5F;

    public OgreGruntRenderer(EntityRendererProvider.Context context) {
        super(context, new OgreGruntModel());
        this.shadowRadius = 0.7f;
        addRenderLayer(new BoneClubLayer(this));
    }

    private static final class BoneClubLayer extends BlockAndItemGeoLayer<OgreGrunt> {
        private BoneClubLayer(GeoEntityRenderer<OgreGrunt> renderer) {
            super(renderer,
                    (bone, grunt) -> "right_hand_item".equals(bone.getName())
                            && grunt.getMainHandItem().is(ModItems.BONE_CLUB.get())
                            ? grunt.getMainHandItem() : null,
                    (bone, grunt) -> null);
        }

        @Override
        protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, OgreGrunt grunt) {
            return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }

        @Override
        protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                          OgreGrunt grunt, MultiBufferSource bufferSource,
                                          float partialTick, int packedLight, int packedOverlay) {
            if (stack.isEmpty()) {
                return;
            }
            poseStack.translate(0.0F, 0.0F, -0.12F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.scale(BONE_CLUB_SCALE, BONE_CLUB_SCALE, BONE_CLUB_SCALE);
            super.renderStackForBone(poseStack, bone, stack, grunt, bufferSource,
                    partialTick, packedLight, packedOverlay);
        }
    }
}
