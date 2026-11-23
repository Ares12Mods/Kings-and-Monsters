package com.kingsandmonsters.compat.wildscapes;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/** Optional structure spacing shared with Wildscapes. No Wildscapes classes are linked. */
public final class WildscapesStructureCompat {
    private static final ResourceLocation SWAMP_VILLAGE =
            ResourceLocation.fromNamespaceAndPath("wildscapes", "swamp_village");
    private static final ResourceLocation WITCH_CAMP =
            ResourceLocation.fromNamespaceAndPath("wildscapes", "witch_camp");

    private WildscapesStructureCompat() {
    }

    /**
     * Uses the live worldgen registry, so the check is automatically inactive when Wildscapes is
     * absent. Candidate checks do not load chunks or depend on which structure generates first.
     */
    public static boolean isClear(ChunkGeneratorStructureState state, int chunkX, int chunkZ,
                                  int villageDistance, int witchCampDistance) {
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            int distance = set.is(SWAMP_VILLAGE) ? villageDistance
                    : set.is(WITCH_CAMP) ? witchCampDistance : 0;
            if (distance > 0 && state.hasStructureChunkInRange(set, chunkX, chunkZ, distance)) {
                return false;
            }
        }
        return true;
    }

    /** Standard breathing room for camps, outposts, wallows, and other smaller sites. */
    public static boolean isClear(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        return isClear(state, chunkX, chunkZ, 10, 8);
    }
}
