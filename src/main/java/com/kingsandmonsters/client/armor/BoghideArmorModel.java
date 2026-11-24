package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.BoghideArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class BoghideArmorModel extends GeoModel<BoghideArmorItem> {
    private static final ResourceLocation LAYER_1_MODEL = id("geo/armor/boghide_layer_1.geo.json");
    private static final ResourceLocation LAYER_2_MODEL = id("geo/armor/boghide_layer_2.geo.json");
    // Both layers share one 64x64 texture sheet.
    private static final ResourceLocation TEXTURE = id("textures/armor/boghide.png");
    private static final ResourceLocation ANIMATION = id("animations/armor/bog_iron.animation.json");

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(BoghideArmorItem item) {
        return item.usesLeggingsModel() ? LAYER_2_MODEL : LAYER_1_MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(BoghideArmorItem item) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BoghideArmorItem item) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(KingsAndMonsters.MODID, path);
    }
}
