package com.kingsandmonsters.client.item;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.OgreKingsClubItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class OgreKingsClubModel extends GeoModel<OgreKingsClubItem> {
    private static final ResourceLocation MODEL = id("geo/item/ogre_kings_club.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/item/ogre_kings_club.png");
    private static final ResourceLocation ANIMATION = id("animations/item/ogre_kings_club.animation.json");

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(OgreKingsClubItem animatable) {
        return MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(OgreKingsClubItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(OgreKingsClubItem animatable) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, path);
    }
}
