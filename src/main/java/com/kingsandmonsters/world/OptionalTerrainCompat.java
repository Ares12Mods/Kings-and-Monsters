package com.kingsandmonsters.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Optional terrain recognized by structure cleanup without requiring the owning mod. */
final class OptionalTerrainCompat {
    private static final ResourceLocation CREATE_ASURINE =
            ResourceLocation.fromNamespaceAndPath("create", "asurine");

    private static volatile boolean resolved;
    private static Block createAsurine;

    private OptionalTerrainCompat() {
    }

    static boolean isCreateAsurine(BlockState state) {
        resolveBlocks();
        return createAsurine != null && state.is(createAsurine);
    }

    private static void resolveBlocks() {
        if (resolved) {
            return;
        }
        synchronized (OptionalTerrainCompat.class) {
            if (!resolved) {
                createAsurine = BuiltInRegistries.BLOCK
                        .getOptional(CREATE_ASURINE)
                        .orElse(null);
                resolved = true;
            }
        }
    }
}
