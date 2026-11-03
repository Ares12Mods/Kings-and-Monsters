package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.BogIronArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BogIronArmorModel extends GeoModel<BogIronArmorItem> {
    private static final ResourceLocation LAYER_1_MODEL = id("geo/armor/bog_iron_layer_1.geo.json");
    private static final ResourceLocation LAYER_2_MODEL = id("geo/armor/bog_iron_layer_2.geo.json");
    private static final ResourceLocation LAYER_1_TEXTURE = id("textures/armor/bog_iron_layer_1.png");
    private static final ResourceLocation LAYER_2_TEXTURE = id("textures/armor/bog_iron_layer_2.png");
    private static final ResourceLocation ANIMATION = id("animations/armor/bog_iron.animation.json");

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(BogIronArmorItem animatable) {
        return animatable.usesLeggingsModel() ? LAYER_2_MODEL : LAYER_1_MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(BogIronArmorItem animatable) {
        return animatable.usesLeggingsModel() ? LAYER_2_TEXTURE : LAYER_1_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BogIronArmorItem animatable) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, path);
    }
}
