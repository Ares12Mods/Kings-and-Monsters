package com.kingsandmonsters.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * Optional vegetation recognized by structure cleanup without linking against other mods. Blocks are
 * looked up by id in the live registry, so everything here is simply empty when the other mod is
 * absent and structures behave exactly as they do without it.
 */
final class OptionalVegetationCompat {
    private static final ResourceLocation NO_MANS_LAND_CUT_VINE =
            new ResourceLocation("nomansland", "cut_vine");
    private static final ResourceLocation NO_MANS_LAND_SHELF_MUSHROOM_BLOCK =
            new ResourceLocation("nomansland", "shelf_mushroom_block");

    /**
     * Wildscapes decorates the vanilla swamp after structures are placed: duckweed and rushes along
     * the water, hanging moss and moss carpet, glowing mushrooms, and cypress trees standing on land
     * and in the shallows. Small plants are cleared like any other undergrowth near a structure,
     * and cypress is cleared like any other tree.
     */
    private static final String WILDSCAPES = "wildscapes";
    private static final String[] WILDSCAPES_PLANTS = {
            "duckweed", "rushes", "short_rushes", "moss_carpet", "hanging_moss",
            "luminous_mushroom", "shelf_luminous_mushroom", "cypress_sapling"
    };
    private static final String[] WILDSCAPES_TREE_BLOCKS = {
            "cypress_log", "cypress_wood", "stripped_cypress_log", "stripped_cypress_wood", "cypress_leaves"
    };

    private static volatile boolean resolved;
    private static Block noMansLandCutVine;
    private static Block noMansLandShelfMushroomBlock;
    private static Set<Block> wildscapesPlants = Set.of();
    private static Set<Block> wildscapesTreeBlocks = Set.of();

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

    /** Rushes, duckweed, moss, glowing mushrooms and saplings from Wildscapes. */
    static boolean isWildscapesPlant(BlockState state) {
        resolveBlocks();
        return !wildscapesPlants.isEmpty() && wildscapesPlants.contains(state.getBlock());
    }

    /** Cypress logs, wood and leaves from Wildscapes. */
    static boolean isWildscapesTreeBlock(BlockState state) {
        resolveBlocks();
        return !wildscapesTreeBlocks.isEmpty() && wildscapesTreeBlocks.contains(state.getBlock());
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
                wildscapesPlants = lookup(WILDSCAPES_PLANTS);
                wildscapesTreeBlocks = lookup(WILDSCAPES_TREE_BLOCKS);
                resolved = true;
            }
        }
    }

    private static Set<Block> lookup(String[] paths) {
        Set<Block> found = new HashSet<>();
        for (String path : paths) {
            BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(WILDSCAPES, path))
                    .ifPresent(found::add);
        }
        return Set.copyOf(found);
    }
}
