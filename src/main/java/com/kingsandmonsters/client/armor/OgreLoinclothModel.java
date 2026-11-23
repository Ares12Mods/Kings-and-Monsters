package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.OgreLoinclothItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class OgreLoinclothModel extends GeoModel<OgreLoinclothItem> {
    private static final ResourceLocation MODEL = id("geo/armor/ogre_loincloth.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/armor/ogre_loincloth.png");
    private static final ResourceLocation ANIMATION = id("animations/armor/bog_iron.animation.json");

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(OgreLoinclothItem animatable) {
        return MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(OgreLoinclothItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(OgreLoinclothItem animatable) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, path);
    }
}
