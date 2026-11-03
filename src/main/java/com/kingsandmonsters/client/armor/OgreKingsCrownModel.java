package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.OgreKingsCrownItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OgreKingsCrownModel extends GeoModel<OgreKingsCrownItem> {
    private static final ResourceLocation MODEL = id("geo/armor/ogre_kings_crown.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/armor/ogre_kings_crown.png");
    private static final ResourceLocation ANIMATION = id("animations/armor/ogre_kings_crown.animation.json");

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(OgreKingsCrownItem animatable) {
        return MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(OgreKingsCrownItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(OgreKingsCrownItem animatable) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, path);
    }
}
