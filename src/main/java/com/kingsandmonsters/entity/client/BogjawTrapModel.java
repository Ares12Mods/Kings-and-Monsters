package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.BogjawTrap;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BogjawTrapModel extends GeoModel<BogjawTrap> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(KingsAndMonsters.MODID, "geo/bogjaw_trap.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(KingsAndMonsters.MODID, "textures/entity/bogjaw_trap.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(KingsAndMonsters.MODID, "animations/bogjaw_trap.animation.json");

    @Override
    public ResourceLocation getModelResource(BogjawTrap trap) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BogjawTrap trap) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BogjawTrap trap) {
        return ANIMATION;
    }
}
