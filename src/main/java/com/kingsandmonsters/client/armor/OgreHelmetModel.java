package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.OgreHelmetItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class OgreHelmetModel extends GeoModel<OgreHelmetItem> {
    private static final ResourceLocation MODEL = id("geo/armor/ogre_helmet.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/armor/ogre_helmet.png");
    private static final ResourceLocation ANIMATION = id("animations/armor/bog_iron.animation.json");

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(OgreHelmetItem animatable) {
        return MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(OgreHelmetItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(OgreHelmetItem animatable) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, path);
    }
}
