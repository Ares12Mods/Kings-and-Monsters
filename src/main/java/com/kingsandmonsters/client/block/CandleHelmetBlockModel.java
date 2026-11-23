package com.kingsandmonsters.client.block;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.block.entity.CandleHelmetBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Reuses the worn helmet geometry and texture for the placed block. */
public final class CandleHelmetBlockModel extends GeoModel<CandleHelmetBlockEntity> {
    private static final ResourceLocation MODEL = id("geo/armor/candle_helmet.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/armor/candle_helmet.png");
    private static final ResourceLocation ANIMATION = id("animations/armor/candle_helmet.animation.json");

    @Override
    public ResourceLocation getModelResource(CandleHelmetBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CandleHelmetBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CandleHelmetBlockEntity animatable) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, path);
    }
}
