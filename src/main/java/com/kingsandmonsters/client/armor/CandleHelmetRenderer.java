package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.item.CandleHelmetItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** Standard GeckoLib armor renderer: the geo's {@code armorHead} bone is GeckoLib's default head bone. */
public class CandleHelmetRenderer extends GeoArmorRenderer<CandleHelmetItem> {
    public CandleHelmetRenderer() {
        super(new CandleHelmetModel());
    }

    @Override
    public void actuallyRender(PoseStack poseStack, CandleHelmetItem animatable, BakedGeoModel model,
                               @Nullable RenderType renderType, MultiBufferSource bufferSource,
                               @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, int colour) {
        // The server-side light block hops between cells as its wearer moves, and light propagation
        // can briefly report a lower value in between, making the metal flash. Clamp to the light
        // this helmet creates while keeping brighter block light and the real sky light.
        boolean lit = CandleHelmetItem.isLit(getCurrentStack());
        int stableLight = LightTexture.pack(
                lit ? Math.max(LightTexture.block(packedLight), CandleHelmetItem.BASE_LIGHT_LEVEL)
                        : LightTexture.block(packedLight),
                LightTexture.sky(packedLight));
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, stableLight, packedOverlay, colour);
    }
}
