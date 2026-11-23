package com.kingsandmonsters.world;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Where Trapper Huts and Boar Traps go, and how the two are tied together.
 *
 * <p>A Boar Trap is its own structure, so {@code /locate} finds it where it really is. Its start
 * chunk is not rolled independently: it is derived from the Trapper Hut candidate that shares its
 * grid cell, a few chunks away. That is pure arithmetic (no terrain, no jigsaw), which keeps
 * {@code /locate} cheap, and the trap placement only accepts a chunk whose hut candidate really is
 * a hut start, so a trap never appears without a hut to belong to.</p>
 *
 * <p>The grid constants must match {@code structure_set/trapper_hut.json}.</p>
 */
final class TrapperSites {
    static final int GRID_SPACING = 30;
    static final int GRID_SEPARATION = 11;
    static final int GRID_SALT = 1947362811;

    /** A trap sits 3 to 6 chunks from its hut (the length is rolled up to 5.3, plus rounding): 48 to 96 blocks. */
    private static final int MIN_PAIR_CHUNKS = 3;
    private static final double MAX_PAIR_CHUNKS = 5.3;
    private static final long PAIR_SALT = 0x5DEECE66DL;

    private TrapperSites() {
    }

    /** The hut candidate of the grid cell that holds this chunk. */
    static ChunkPos hutChunk(long seed, int chunkX, int chunkZ) {
        // The same formula vanilla's linear random spread uses, which the hut's own placement runs.
        return FortGridPlacement.candidateChunkAt(seed, Math.floorDiv(chunkX, GRID_SPACING),
                Math.floorDiv(chunkZ, GRID_SPACING), GRID_SPACING, GRID_SEPARATION, GRID_SALT);
    }

    /** The trap candidate belonging to the hut of the grid cell that holds this chunk. */
    static ChunkPos trapChunk(long seed, int chunkX, int chunkZ) {
        ChunkPos hut = hutChunk(seed, chunkX, chunkZ);
        RandomSource random = RandomSource.create(seed ^ PAIR_SALT ^ ((long) hut.x * 341873128712L)
                ^ ((long) hut.z * 132897987541L));
        double angle = random.nextDouble() * Math.PI * 2.0;
        double length = MIN_PAIR_CHUNKS + random.nextDouble() * (MAX_PAIR_CHUNKS - MIN_PAIR_CHUNKS);
        int dx = (int) Math.round(Math.cos(angle) * length);
        int dz = (int) Math.round(Math.sin(angle) * length);
        if (Math.max(Math.abs(dx), Math.abs(dz)) < MIN_PAIR_CHUNKS) {
            if (Math.abs(dx) >= Math.abs(dz)) {
                dx = dx < 0 ? -MIN_PAIR_CHUNKS : MIN_PAIR_CHUNKS;
            } else {
                dz = dz < 0 ? -MIN_PAIR_CHUNKS : MIN_PAIR_CHUNKS;
            }
        }
        // Stay in the hut's grid cell, so the trap's own cell lookup finds the same hut. The hut
        // sits in the first 19 chunks of a 30 chunk cell, and the offset is at most 6.
        int cellX = Math.floorDiv(chunkX, GRID_SPACING) * GRID_SPACING;
        int cellZ = Math.floorDiv(chunkZ, GRID_SPACING) * GRID_SPACING;
        int x = hut.x + dx;
        if (x < cellX || x >= cellX + GRID_SPACING) {
            x = hut.x - dx;
        }
        int z = hut.z + dz;
        if (z < cellZ || z >= cellZ + GRID_SPACING) {
            z = hut.z - dz;
        }
        return new ChunkPos(x, z);
    }

    /**
     * Cheap, terrain-only site test shared by huts and traps (the biome is checked by the caller
     * first). It uses worldgen heights only, one sample per point: a five by five grid across the
     * footprint must be dry and no more than {@code maxRelief} blocks apart, and a ring of points
     * beyond it must be dry too. Gentle slopes are fine because the site is graded afterwards.
     */
    static boolean isViableSite(Structure.GenerationContext context, int centerX, int centerZ, int maxRelief,
                                int dryRadius) {
        int seaLevel = context.chunkGenerator().getSeaLevel();
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int dx = -12; dx <= 12; dx += 6) {
            for (int dz = -12; dz <= 12; dz += 6) {
                int height = surface(context, centerX + dx, centerZ + dz);
                if (height <= seaLevel + 1) {
                    return false;
                }
                low = Math.min(low, height);
                high = Math.max(high, height);
                if (high - low > maxRelief) {
                    return false;
                }
            }
        }
        for (int step = 0; step < 8; step++) {
            double angle = step * (Math.PI / 4.0);
            int height = surface(context, centerX + (int) Math.round(Math.cos(angle) * dryRadius),
                    centerZ + (int) Math.round(Math.sin(angle) * dryRadius));
            if (height <= seaLevel + 1) {
                return false;
            }
        }
        return true;
    }

    private static int surface(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
    }
}
