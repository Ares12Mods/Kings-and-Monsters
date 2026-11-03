package com.kingsandmonsters.client.block;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.block.entity.OgreKingsCrownBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class OgreKingsCrownBlockModel extends GeoModel<OgreKingsCrownBlockEntity> {
    private static final ResourceLocation MODEL = id("geo/block/ogre_kings_crown.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/armor/ogre_kings_crown.png");
    private static final ResourceLocation ANIMATION = id("animations/block/ogre_kings_crown.animation.json");

    @Override
    public ResourceLocation getModelResource(OgreKingsCrownBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(OgreKingsCrownBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(OgreKingsCrownBlockEntity animatable) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, path);
    }
}
