package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.entity.OgreTrapper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class OgreTrapperRenderer extends GeoEntityRenderer<OgreTrapper> {
    private static final float KNIFE_SCALE = 1.75F;

    public OgreTrapperRenderer(EntityRendererProvider.Context context) {
        super(context, new OgreTrapperModel());
        this.shadowRadius = 0.8f;
        addRenderLayer(new KnifeLayer(this));
    }

    /** The knife sprite in his right fist, from the moment he draws it. */
    private static final class KnifeLayer extends BlockAndItemGeoLayer<OgreTrapper> {
        private static final ItemStack KNIFE = new ItemStack(ModItems.TRAPPER_KNIFE.get());

        private KnifeLayer(GeoEntityRenderer<OgreTrapper> renderer) {
            super(renderer,
                    (bone, trapper) -> "right_hand_item".equals(bone.getName()) && trapper.isKnifeOut() ? KNIFE : null,
                    (bone, trapper) -> null);
        }

        @Override
        protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, OgreTrapper trapper) {
            return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }

        @Override
        protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, OgreTrapper trapper,
                                          MultiBufferSource bufferSource, float partialTick, int packedLight,
                                          int packedOverlay) {
            // Same wrist roll as the Captain's sword: the blade points forward from the fist, not down the arm.
            poseStack.translate(0.0F, 0.0F, -0.12F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.scale(KNIFE_SCALE, KNIFE_SCALE, KNIFE_SCALE);
            super.renderStackForBone(poseStack, bone, stack, trapper, bufferSource, partialTick, packedLight,
                    packedOverlay);
        }
    }
}
