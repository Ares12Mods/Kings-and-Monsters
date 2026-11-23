package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.BlackhideArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class BlackhideArmorModel extends GeoModel<BlackhideArmorItem> {
    private static final ResourceLocation LAYER_1_MODEL = id("geo/armor/blackhide_layer_1.geo.json");
    private static final ResourceLocation LAYER_2_MODEL = id("geo/armor/blackhide_layer_2.geo.json");
    // Both layers share one 128x128 texture sheet.
    private static final ResourceLocation TEXTURE = id("textures/armor/blackhide.png");
    private static final ResourceLocation ANIMATION = id("animations/armor/bog_iron.animation.json");

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(BlackhideArmorItem item) {
        return item.usesLeggingsModel() ? LAYER_2_MODEL : LAYER_1_MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(BlackhideArmorItem item) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BlackhideArmorItem item) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, path);
    }
}
