package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.BogfumeBolt;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BogfumeBoltRenderer extends EntityRenderer<BogfumeBolt> {
    private static final ResourceLocation EMPTY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "textures/entity/spell/bogfume_bolt.png");

    public BogfumeBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BogfumeBolt entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    }

    @Override
    public ResourceLocation getTextureLocation(BogfumeBolt entity) {
        return EMPTY_TEXTURE;
    }
}
