package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.CandleHelmetItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Emissive flame. Hooks {@link GeoRenderLayer#renderForBone}, which GeckoLib calls while the pose
 * stack is already inside the bone's (animated, head-attached) transform, and re-draws only the
 * cubes of the {@code flame} bone with {@link RenderType#eyes} at full brightness, using a mask
 * texture that contains only the flame pixels. No second full-model pass is made.
 * This affects looks only; it does not light the surroundings.
 */
public class CandleHelmetFlameGlowLayer extends GeoRenderLayer<CandleHelmetItem> {
    private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KingsAndMonsters.MODID, "textures/armor/candle_helmet_flame_glow.png");

    public CandleHelmetFlameGlowLayer(GeoRenderer<CandleHelmetItem> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, CandleHelmetItem animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        if (!"flame".equals(bone.getName())) return;
        VertexConsumer glowBuffer = bufferSource.getBuffer(RenderType.eyes(GLOW_TEXTURE));
        getRenderer().renderCubesOfBone(poseStack, bone, glowBuffer, LightTexture.FULL_BRIGHT, packedOverlay, 0xFFFFFFFF);
    }
}
