package com.kingsandmonsters.client.block;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.block.HaunchPlatterBlock;
import com.kingsandmonsters.block.entity.HaunchPlatterBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Picks the model and texture for how much of the roast is left: four servings is stage 1, an empty plate stage 5. */
public final class HaunchPlatterBlockModel extends GeoModel<HaunchPlatterBlockEntity> {
    private static final ResourceLocation ANIMATION = id("animations/block/haunch_platter.animation.json");

    @Override
    public ResourceLocation getModelResource(HaunchPlatterBlockEntity animatable) {
        return id("geo/block/haunch_platter_" + stage(animatable) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HaunchPlatterBlockEntity animatable) {
        return id("textures/block/haunch_platter_" + stage(animatable) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(HaunchPlatterBlockEntity animatable) {
        return ANIMATION;
    }

    private static int stage(HaunchPlatterBlockEntity platter) {
        return 1 + HaunchPlatterBlock.MAX_SERVINGS - platter.getBlockState().getValue(HaunchPlatterBlock.SERVINGS);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, path);
    }
}
