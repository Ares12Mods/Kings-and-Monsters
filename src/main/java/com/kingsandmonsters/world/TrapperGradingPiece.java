package com.kingsandmonsters.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

/**
 * Levels the ground under a Trapper Hut or Boar Trap and feathers it into the surrounding terrain.
 *
 * <p>It is a piece of its own, placed ahead of the template, so that every chunk the site touches
 * grades the same columns by the same rule before the authored blocks go on top. Each column's
 * result depends only on its own x and z, the site's footprint and plane, and a noise that is
 * seeded from the world seed, so chunk borders can never disagree. Inside the footprint the ground
 * becomes the template's ground plane. Outside it, the surface eases from the plane back to the
 * natural height over a distance that wanders with noise (and the distance itself is warped), so no
 * ring, rectangle or contour line is left behind. It reads the worldgen heightmap, which ignores
 * trees and plants, and never touches a column outside the chunk being processed.</p>
 */
public final class TrapperGradingPiece extends StructurePiece {
    public static final int SHAPE_ELLIPSE = 0;
    public static final int SHAPE_RECTANGLE = 1;
    /** Widest the eased bank can ever reach; the piece's bounding box adds this to the footprint. */
    public static final int MAX_REACH = 10;
    private static final int MIN_REACH = 4;
    private static final int MAX_BANK_CUT = 5;
    private static final int MAX_BANK_FILL = 4;
    private static final int MAX_FLOOR_CHANGE = 9;
    private static final int CANOPY_CLEARANCE = 16;

    private static final String CORE_TAG = "Core";
    private static final String PLANE_TAG = "Plane";
    private static final String SHAPE_TAG = "Shape";

    private final BoundingBox core;
    private final int planeY;
    private final int shape;

    public TrapperGradingPiece(BoundingBox core, int planeY, int shape) {
        super(ModStructures.TRAPPER_GRADING_PIECE.get(), 0, bankBox(core, planeY));
        this.core = core;
        this.planeY = planeY;
        this.shape = shape;
    }

    public TrapperGradingPiece(CompoundTag tag) {
        super(ModStructures.TRAPPER_GRADING_PIECE.get(), tag);
        int[] bounds = tag.getIntArray(CORE_TAG);
        this.core = bounds.length == 6
                ? new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5])
                : this.boundingBox;
        this.planeY = tag.getInt(PLANE_TAG);
        this.shape = tag.getInt(SHAPE_TAG);
    }

    private static BoundingBox bankBox(BoundingBox core, int planeY) {
        return new BoundingBox(core.minX() - MAX_REACH - 1, planeY - 12, core.minZ() - MAX_REACH - 1,
                core.maxX() + MAX_REACH + 1, planeY + 14, core.maxZ() + MAX_REACH + 1);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putIntArray(CORE_TAG, new int[]{core.minX(), core.minY(), core.minZ(), core.maxX(), core.maxY(), core.maxZ()});
        tag.putInt(PLANE_TAG, planeY);
        tag.putInt(SHAPE_TAG, shape);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        // Seeded from the world, never from the chunk, so neighbouring chunks sample the same noise.
        RandomSource noiseRandom = RandomSource.create(level.getSeed() ^ 0x7A3C5E1F9B2D4687L);
        ImprovedNoise reachNoise = new ImprovedNoise(noiseRandom);
        ImprovedNoise warpNoise = new ImprovedNoise(noiseRandom);

        int minX = Math.max(chunkBox.minX(), boundingBox.minX());
        int maxX = Math.min(chunkBox.maxX(), boundingBox.maxX());
        int minZ = Math.max(chunkBox.minZ(), boundingBox.minZ());
        int maxZ = Math.min(chunkBox.maxZ(), boundingBox.maxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double outside = distanceOutside(x, z);
                if (outside <= 0.0) {
                    grade(level, x, z, planeY, true);
                    continue;
                }
                // How far the bank reaches changes slowly along the edge, and the distance itself is
                // warped a little, so the bank is never a constant width or a clean offset curve.
                double reachSample = (reachNoise.noise(x / 17.0, 0.0, z / 17.0) + 1.0) * 0.5;
                double reach = MIN_REACH + Mth.clamp(reachSample, 0.0, 1.0) * (MAX_REACH - MIN_REACH);
                double warped = outside + warpNoise.noise(x / 6.0, 7.3, z / 6.0) * 1.6;
                if (warped >= reach) {
                    continue;
                }
                double progress = 1.0 - Math.max(0.0, warped) / reach;
                double blend = progress * progress * (3.0 - 2.0 * progress);
                BlockPos natural = naturalGround(level, x, z);
                int target = Mth.floor(Mth.lerp(blend, natural.getY(), planeY) + 0.5);
                target = Mth.clamp(target, natural.getY() - MAX_BANK_CUT, natural.getY() + MAX_BANK_FILL);
                if (target != natural.getY()) {
                    settle(level, natural, target, false);
                }
            }
        }
    }

    /** Distance in blocks from the footprint, zero inside it. */
    private double distanceOutside(int x, int z) {
        if (shape == SHAPE_ELLIPSE) {
            double halfX = (core.maxX() - core.minX() + 1) / 2.0;
            double halfZ = (core.maxZ() - core.minZ() + 1) / 2.0;
            double u = (x + 0.5 - (core.minX() + halfX)) / halfX;
            double v = (z + 0.5 - (core.minZ() + halfZ)) / halfZ;
            double radius = Math.sqrt(u * u + v * v);
            return radius <= 1.0 ? 0.0 : (radius - 1.0) * Math.min(halfX, halfZ);
        }
        double dx = Math.max(Math.max(core.minX() - x, 0), x - core.maxX());
        double dz = Math.max(Math.max(core.minZ() - z, 0), z - core.maxZ());
        return Math.hypot(dx, dz);
    }

    private static BlockPos naturalGround(WorldGenLevel level, int x, int z) {
        // The worldgen heightmap exists for every chunk from the terrain pass on, and it ignores
        // trees, so a canopy hanging over from a neighbour cannot be mistaken for ground.
        return new BlockPos(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1, z);
    }

    private static void grade(WorldGenLevel level, int x, int z, int targetY, boolean interior) {
        BlockPos natural = naturalGround(level, x, z);
        int target = interior
                ? Mth.clamp(targetY, natural.getY() - MAX_FLOOR_CHANGE, natural.getY() + MAX_FLOOR_CHANGE)
                : targetY;
        settle(level, natural, target, interior);
    }

    /**
     * Raises or lowers one column to {@code target}. Inside the footprint everything above the new
     * surface goes, including canopy and trunks that overhang from neighbouring chunks; on the bank
     * only plants and leaves are cleared, and a column with water above it is left alone.
     */
    private static void settle(WorldGenLevel level, BlockPos natural, int target, boolean interior) {
        int x = natural.getX();
        int z = natural.getZ();
        if (!interior && (!level.getFluidState(natural.above()).isEmpty()
                || level.getBlockState(natural.above()).is(BlockTags.LOGS))) {
            return;
        }
        if (natural.getY() < target) {
            level.setBlock(natural, Blocks.DIRT.defaultBlockState(), 2);
            for (int y = natural.getY() + 1; y <= target; y++) {
                level.setBlock(new BlockPos(x, y, z), y == target
                        ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
            }
        } else if (natural.getY() > target) {
            for (int y = natural.getY(); y > target; y--) {
                level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
            level.setBlock(new BlockPos(x, target, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        }
        int clearTop = Math.max(natural.getY(), target) + (interior ? CANOPY_CLEARANCE : 4);
        for (int y = target + 1; y <= clearTop; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            boolean removable = state.is(BlockTags.REPLACEABLE)
                    || state.getBlock() instanceof net.minecraft.world.level.block.BushBlock
                    || state.is(BlockTags.LEAVES)
                    || (interior && state.is(BlockTags.LOGS));
            if (removable) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }
}
