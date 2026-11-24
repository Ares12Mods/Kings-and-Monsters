package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.CandleHelmetItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CandleHelmetModel extends GeoModel<CandleHelmetItem> {
    private static final ResourceLocation MODEL = id("geo/armor/candle_helmet.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/armor/candle_helmet.png");
    private static final ResourceLocation ANIMATION = id("animations/armor/candle_helmet.animation.json");

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(CandleHelmetItem animatable) {
        return MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(CandleHelmetItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CandleHelmetItem animatable) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(KingsAndMonsters.MODID, path);
    }
}
