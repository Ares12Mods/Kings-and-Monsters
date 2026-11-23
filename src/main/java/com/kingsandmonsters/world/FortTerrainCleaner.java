package com.kingsandmonsters.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Map;
import java.util.Set;

/**
 * Grades terrain per column using each column's own structure extent (from FortFootprintProcessor)
 * instead of one bounding-box-wide floor and roof. The foundation height varies around the
 * perimeter (the entrance is aligned separately in OgreKingsFortStructure.findGenerationPoint), so
 * a uniform cut would bury shorter walls or leave taller ones floating.
 *
 * <p>For each occupied column, terrain above its highest structure block is cleared so a hillside
 * can't drape over a short wall, and any air gap below its lowest structure block is filled with
 * locally sampled terrain. Columns outside the footprint are never touched; block_ignore on the
 * template already stops the paste overwriting terrain there.</p>
 */
public final class FortTerrainCleaner {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int OVERHANG_CLEARANCE = 24;
    // The fill stops at the first solid block, so this is only a ceiling. It used to be 8, which left
    // the downhill side of a Fort on a steep slope hanging over open air; now it reaches the ground.
    private static final int FOUNDATION_DEPTH = 40;

    private FortTerrainCleaner() {
    }

    public static void grade(
            WorldGenLevel level,
            BoundingBox chunkBox,
            Map<Long, int[]> columnExtents,
            Set<Long> authoredBlocks) {
        grade(level, chunkBox, columnExtents, authoredBlocks, false);
    }

    /**
     * {@code fillThroughFluid} lets the foundation fill replace water below a column, so a structure
     * built on a swamp's shallows gets solid ground under it instead of floating on the water.
     */
    public static void grade(
            WorldGenLevel level,
            BoundingBox chunkBox,
            Map<Long, int[]> columnExtents,
            Set<Long> authoredBlocks,
            boolean fillThroughFluid) {
        if (columnExtents.isEmpty()) {
            return;
        }

        int worldMinY = level.getMinBuildHeight();
        int worldMaxY = level.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (Map.Entry<Long, int[]> entry : columnExtents.entrySet()) {
            long packedColumn = entry.getKey();
            int x = BlockPos.getX(packedColumn);
            int z = BlockPos.getZ(packedColumn);
            if (x < chunkBox.minX() || x > chunkBox.maxX() || z < chunkBox.minZ() || z > chunkBox.maxZ()) {
                continue;
            }

            int columnMinY = entry.getValue()[0];
            int columnMaxY = entry.getValue()[1];

            for (int y = columnMinY; y <= columnMaxY; y++) {
                pos.set(x, y, z);
                if (!authoredBlocks.contains(pos.asLong()) && isNaturalTerrain(level.getBlockState(pos))) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                }
            }

            int clearTo = Math.min(worldMaxY, columnMaxY + OVERHANG_CLEARANCE);
            for (int y = clearTo; y > columnMaxY; y--) {
                pos.set(x, y, z);
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                }
            }

            int fillTo = Math.max(worldMinY, columnMinY - FOUNDATION_DEPTH);
            BlockState foundationFill = sampleFoundationFill(level, pos, x, z, columnMinY, fillTo);
            for (int y = columnMinY - 1; y >= fillTo; y--) {
                pos.set(x, y, z);
                BlockState below = level.getBlockState(pos);
                if (!below.isAir() && !(fillThroughFluid && !below.getFluidState().isEmpty())) {
                    break;
                }
                level.setBlock(pos, foundationFill, UPDATE_FLAGS);
            }
        }
    }

    private static BlockState sampleFoundationFill(
            WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int fromY, int minimumY) {
        for (int y = fromY - 1; y >= minimumY; y--) {
            pos.set(x, y, z);
            BlockState sampled = level.getBlockState(pos);
            if (sampled.isAir() || !sampled.getFluidState().isEmpty()) continue;
            if (sampled.is(Blocks.GRASS_BLOCK) || sampled.is(Blocks.COARSE_DIRT)
                    || sampled.is(Blocks.PODZOL) || sampled.is(Blocks.ROOTED_DIRT)) {
                return Blocks.DIRT.defaultBlockState();
            }
            if (isNaturalTerrain(sampled)) return sampled;
            break;
        }
        return Blocks.DIRT.defaultBlockState();
    }

    private static boolean isNaturalTerrain(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || OptionalTerrainCompat.isCreateAsurine(state)
                || state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD);
    }
}
