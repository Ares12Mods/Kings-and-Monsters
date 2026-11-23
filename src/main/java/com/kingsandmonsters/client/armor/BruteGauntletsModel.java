package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.BruteGauntletsItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class BruteGauntletsModel extends GeoModel<BruteGauntletsItem> {
    private static final ResourceLocation MODEL = id("geo/armor/brute_gauntlets.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/armor/brute_gauntlets.png");
    private static final ResourceLocation ANIMATION = id("animations/armor/bog_iron.animation.json");

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(BruteGauntletsItem animatable) {
        return MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(BruteGauntletsItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BruteGauntletsItem animatable) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, path);
    }
}
