package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.OgreMage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Lights the green stone on his wand at full brightness while a spell is charging. */
public class MageStoneGlowLayer extends GeoRenderLayer<OgreMage> {
    // Transparent everywhere except the stone's pixels, so redrawing the whole model only lights the stone.
    private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KingsAndMonsters.MODID, "textures/entity/ogre_mage_glow.png");

    public MageStoneGlowLayer(GeoRenderer<OgreMage> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, OgreMage mage, BakedGeoModel model, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        if (!mage.isStoneGlowing()) {
            return;
        }
        RenderType glow = RenderType.eyes(GLOW_TEXTURE);
        getRenderer().reRender(model, poseStack, bufferSource, mage, glow, bufferSource.getBuffer(glow),
                partialTick, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
