package com.kingsandmonsters.world;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

import java.util.Map;

/**
 * A generation-only, exact-footprint perimeter blend for accepted forts.
 * It tapers abrupt terrain contacts over at most five blocks and extends local
 * terrain where a foundation meets lower ground. Candidate search,
 * biome validation and structure placement never call this class.
 */
public final class FortCliffGuard {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int MAX_BLEND_DISTANCE = 5;
    private static final int MAX_CUT_PER_COLUMN = 12;
    private static final int MAX_FILL_PER_COLUMN = 6;
    private static final int MAX_WATER_FILL = 24;
    private static final int TERRACE_BLEND_DISTANCE = 12;
    private static final int TERRACE_MAX_FILL = 14;
    private static final int CUT_THRESHOLD = 2;
    private static final int WALL_CLEARANCE = 1;
    private static final int OUTWARD_PROBE_DISTANCE = 5;
    private static final double TAPER_NOISE_SCALE = 1.0 / 9.0;

    private FortCliffGuard() {
    }

    public static void guard(WorldGenLevel level, BoundingBox chunkBox, Map<Long, int[]> columnExtents) {
        guard(level, chunkBox, columnExtents, false, false);
    }

    public static void guard(
            WorldGenLevel level, BoundingBox chunkBox, Map<Long, int[]> columnExtents, boolean fillThroughFluid) {
        guard(level, chunkBox, columnExtents, fillThroughFluid, false);
    }

    /**
     * {@code fillThroughFluid} builds a bank that stands in water up from the bed, instead of
     * stacking it on the water surface where it would hover over the water.
     * {@code terraced} lets a foundation on a steep slope or biome edge step down in wide landings
     * over a longer reach, so the hill doesn't drop away from under the walls.
     */
    public static void guard(
            WorldGenLevel level,
            BoundingBox chunkBox,
            Map<Long, int[]> columnExtents,
            boolean fillThroughFluid,
            boolean terraced) {
        if (columnExtents.isEmpty()) {
            return;
        }
        int blendDistance = terraced ? TERRACE_BLEND_DISTANCE : MAX_BLEND_DISTANCE;
        int maxFill = terraced ? TERRACE_MAX_FILL : MAX_FILL_PER_COLUMN;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        ImprovedNoise taperNoise = new ImprovedNoise(
                RandomSource.create(level.getSeed() ^ 0xD1B54A32D192ED03L));

        for (int x = chunkBox.minX(); x <= chunkBox.maxX(); x++) {
            for (int z = chunkBox.minZ(); z <= chunkBox.maxZ(); z++) {
                if (columnExtents.containsKey(BlockPos.asLong(x, 0, z))) {
                    continue;
                }

                NearestPerimeter nearest = nearestPerimeterFoundation(
                        x, z, columnExtents, blendDistance);
                if (nearest == null || !opensAwayFromFootprint(x, z, nearest, columnExtents)) {
                    continue;
                }

                int variation = taperNoise.noise(
                        x * TAPER_NOISE_SCALE, 0.0, z * TAPER_NOISE_SCALE) >= 0.0 ? 1 : 0;
                int surfaceY = level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

                // High terrain is allowed to rise by roughly two blocks per
                // ring away from the wall, producing a short natural taper.
                int maximumNaturalTop = nearest.foundationY()
                        + WALL_CLEARANCE
                        + nearest.distance() * 2
                        + variation;
                if (surfaceY - maximumNaturalTop >= CUT_THRESHOLD) {
                    int cutTo = Math.max(maximumNaturalTop, surfaceY - MAX_CUT_PER_COLUMN);
                    carveNaturalColumn(level, pos, x, z, surfaceY, cutTo);
                    continue;
                }

                // On the low side, descend one block per ring. Only short gaps
                // are filled, continuing the local terrain instead of exposing
                // an artificial stone retaining wall around the fort.
                int drop = terraced
                        ? terraceDrop(nearest.distance())
                        : supportDrop(taperNoise, x, z, nearest.distance(), fillThroughFluid);
                int minimumSupportedTop = nearest.foundationY() - drop;
                if (minimumSupportedTop > surfaceY) {
                    int fillTo = Math.min(minimumSupportedTop, surfaceY + maxFill);
                    fillRetainingColumn(level, pos, x, z, surfaceY, fillTo, fillThroughFluid);
                }
            }
        }
    }

    /**
     * How far the bank has dropped this many rings out. The Fort keeps a strict one block per ring;
     * the quarry's bank varies, so the shore doesn't read as the same staircase all the way round:
     * broad noise swings the slope from gentle to steep and fine noise adds the odd extra notch.
     */
    private static int supportDrop(ImprovedNoise noise, int x, int z, int distance, boolean varied) {
        if (!varied) {
            return distance;
        }
        double slope = 1.15 + noise.noise(x * 0.07, 7.0, z * 0.07) * 0.5;
        int drop = (int) Math.round(distance * slope);
        if (noise.noise(x * 0.45, 3.0, z * 0.45) > 0.35) {
            drop++;
        }
        return Math.max(drop, 0);
    }

    /** Flat landings three blocks wide, each one two blocks lower than the last. */
    private static int terraceDrop(int distance) {
        return 2 * ((distance + 2) / 3);
    }

    private static void carveNaturalColumn(
            WorldGenLevel level,
            BlockPos.MutableBlockPos pos,
            int x,
            int z,
            int surfaceY,
            int cutTo) {
        pos.set(x, surfaceY, z);
        BlockState originalSurface = level.getBlockState(pos);

        for (int y = surfaceY; y > cutTo; y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!isNaturalTerrain(state) && !OutpostTerrainCleaner.isSmallVegetation(state)) {
                return;
            }
        }
        for (int y = surfaceY; y > cutTo; y--) {
            pos.set(x, y, z);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }

        pos.set(x, cutTo, z);
        BlockState exposed = level.getBlockState(pos);
        if (!exposed.isAir() && exposed.getFluidState().isEmpty() && isNaturalSurface(originalSurface)) {
            level.setBlock(pos, originalSurface, UPDATE_FLAGS);
        }
    }

    private static void fillRetainingColumn(
            WorldGenLevel level,
            BlockPos.MutableBlockPos pos,
            int x,
            int z,
            int surfaceY,
            int fillTo,
            boolean fillThroughFluid) {
        pos.set(x, surfaceY, z);
        BlockState originalSurface = level.getBlockState(pos);
        int bedY = surfaceY;
        BlockState bed = originalSurface;
        if (fillThroughFluid && !originalSurface.getFluidState().isEmpty()) {
            // Standing in water: find the bed and build the bank up from it.
            int lowest = Math.max(level.getMinBuildHeight(), surfaceY - MAX_WATER_FILL);
            boolean found = false;
            for (int y = surfaceY - 1; y >= lowest; y--) {
                pos.set(x, y, z);
                BlockState below = level.getBlockState(pos);
                if (!below.isAir() && below.getFluidState().isEmpty()) {
                    bedY = y;
                    bed = below;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return;
            }
        }
        BlockState subsurface = naturalSubsurface(bed);
        for (int y = surfaceY + 1; y <= fillTo; y++) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty()
                    && !OutpostTerrainCleaner.isSmallVegetation(state)) {
                return;
            }
        }

        for (int y = bedY + 1; y <= fillTo; y++) {
            pos.set(x, y, z);
            boolean top = y == fillTo;
            BlockState fill = top ? naturalSurface(originalSurface) : subsurface;
            level.setBlock(pos, fill, UPDATE_FLAGS);
        }
    }

    private static BlockState naturalSurface(BlockState sampled) {
        return isNaturalSurface(sampled) ? sampled : Blocks.GRASS_BLOCK.defaultBlockState();
    }

    private static BlockState naturalSubsurface(BlockState sampled) {
        if (sampled.is(Blocks.MUD)) return Blocks.MUD.defaultBlockState();
        if (sampled.is(Blocks.CLAY)) return Blocks.CLAY.defaultBlockState();
        if (sampled.is(Blocks.GRAVEL)) return Blocks.GRAVEL.defaultBlockState();
        if (sampled.is(Blocks.SAND)) return Blocks.SAND.defaultBlockState();
        if (sampled.is(Blocks.RED_SAND)) return Blocks.RED_SAND.defaultBlockState();
        if (sampled.is(Blocks.SNOW_BLOCK)) return Blocks.SNOW_BLOCK.defaultBlockState();
        if (sampled.is(BlockTags.BASE_STONE_OVERWORLD)) return sampled;
        return Blocks.DIRT.defaultBlockState();
    }

    private static NearestPerimeter nearestPerimeterFoundation(
            int x,
            int z,
            Map<Long, int[]> footprint,
            int maxDistance) {
        NearestPerimeter best = null;
        for (int dx = -maxDistance; dx <= maxDistance; dx++) {
            for (int dz = -maxDistance; dz <= maxDistance; dz++) {
                int distance = Math.max(Math.abs(dx), Math.abs(dz));
                if (distance == 0 || distance > maxDistance
                        || best != null && distance > best.distance()) {
                    continue;
                }

                int wallX = x + dx;
                int wallZ = z + dz;
                int[] extent = footprint.get(BlockPos.asLong(wallX, 0, wallZ));
                if (extent == null || !isPerimeterColumn(wallX, wallZ, footprint)) {
                    continue;
                }

                // Prefer the lowest foundation among equally close columns;
                // decorative spikes or hanging details must not set the grade.
                if (best == null || distance < best.distance() || extent[0] < best.foundationY()) {
                    best = new NearestPerimeter(wallX, wallZ, extent[0], distance);
                }
            }
        }
        return best;
    }

    private static boolean isPerimeterColumn(int x, int z, Map<Long, int[]> footprint) {
        return !footprint.containsKey(BlockPos.asLong(x + 1, 0, z))
                || !footprint.containsKey(BlockPos.asLong(x - 1, 0, z))
                || !footprint.containsKey(BlockPos.asLong(x, 0, z + 1))
                || !footprint.containsKey(BlockPos.asLong(x, 0, z - 1));
    }

    /** Avoids grading enclosed footprint gaps by requiring an open ray away from the wall. */
    private static boolean opensAwayFromFootprint(
            int x,
            int z,
            NearestPerimeter nearest,
            Map<Long, int[]> footprint) {
        int stepX = Integer.signum(x - nearest.x());
        int stepZ = Integer.signum(z - nearest.z());
        if (stepX == 0 && stepZ == 0) {
            return false;
        }
        for (int step = 1; step <= OUTWARD_PROBE_DISTANCE; step++) {
            if (footprint.containsKey(BlockPos.asLong(x + stepX * step, 0, z + stepZ * step))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNaturalTerrain(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK);
    }

    private static boolean isNaturalSurface(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.SNOW_BLOCK);
    }

    private record NearestPerimeter(int x, int z, int foundationY, int distance) {
    }
}
