package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.BogsteelChestplateItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class BogsteelChestplateModel extends GeoModel<BogsteelChestplateItem> {
    private static final ResourceLocation MODEL = id("geo/armor/bogsteel_chestplate.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/armor/bogsteel_chestplate.png");
    private static final ResourceLocation ANIMATION = id("animations/armor/bog_iron.animation.json");

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(BogsteelChestplateItem animatable) {
        return MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(BogsteelChestplateItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BogsteelChestplateItem animatable) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, path);
    }
}
