package com.kingsandmonsters.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Optional vegetation recognized by structure cleanup without linking against other mods. */
final class OptionalVegetationCompat {
    private static final ResourceLocation NO_MANS_LAND_CUT_VINE =
            ResourceLocation.fromNamespaceAndPath("nomansland", "cut_vine");
    private static final ResourceLocation NO_MANS_LAND_SHELF_MUSHROOM_BLOCK =
            ResourceLocation.fromNamespaceAndPath("nomansland", "shelf_mushroom_block");

    private static volatile boolean resolved;
    private static Block noMansLandCutVine;
    private static Block noMansLandShelfMushroomBlock;

    private OptionalVegetationCompat() {
    }

    static boolean isCutVine(BlockState state) {
        resolveBlocks();
        return noMansLandCutVine != null && state.is(noMansLandCutVine);
    }

    static boolean isShelfMushroomBlock(BlockState state) {
        resolveBlocks();
        return noMansLandShelfMushroomBlock != null && state.is(noMansLandShelfMushroomBlock);
    }

    private static void resolveBlocks() {
        if (resolved) {
            return;
        }
        synchronized (OptionalVegetationCompat.class) {
            if (!resolved) {
                noMansLandCutVine = BuiltInRegistries.BLOCK
                        .getOptional(NO_MANS_LAND_CUT_VINE)
                        .orElse(null);
                noMansLandShelfMushroomBlock = BuiltInRegistries.BLOCK
                        .getOptional(NO_MANS_LAND_SHELF_MUSHROOM_BLOCK)
                        .orElse(null);
                resolved = true;
            }
        }
    }
}
