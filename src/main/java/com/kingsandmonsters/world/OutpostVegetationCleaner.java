package com.kingsandmonsters.world;

import com.kingsandmonsters.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import com.kingsandmonsters.event.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Removes decoration-stage vegetation while protecting every authored tree-like
 * block captured immediately after the outpost template is placed.
 */
public final class OutpostVegetationCleaner {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int VEGETATION_BUFFER = 3;
    private static final int TREE_SEARCH_BUFFER = 8;
    private static final int BELOW_CLEARANCE = 4;
    private static final int CANOPY_CLEARANCE = 24;
    private static final int MAX_CONNECTED_TREE_BLOCKS = 2048;
    private static final ConcurrentMap<CleanupKey, CleanupTask> PENDING = new ConcurrentHashMap<>();

    private OutpostVegetationCleaner() {
    }

    public static void reset() {
        PENDING.clear();
    }

    public static void captureStructureBlocks(
            WorldGenLevel level,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            PiecesContainer pieces,
            Set<Long> authoredBlocks,
            boolean swamp) {
        captureStructureBlocks(level, chunkBox, chunkPos, pieces, authoredBlocks, swamp, false, false,
                VEGETATION_BUFFER, TREE_SEARCH_BUFFER);
    }

    /**
     * The Boar Trap's site. As {@link #captureStructureBlocks} does, except that natural ground keeps its
     * plants and the moss carpet the
     * template lays over its pit stays: it hangs there only because of the log beside it, and
     * breaking that log is what drops the cover.
     */
    public static void captureTrapperTrap(
            WorldGenLevel level,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            PiecesContainer pieces,
            Set<Long> authoredBlocks) {
        captureStructureBlocks(level, chunkBox, chunkPos, pieces, authoredBlocks, false, true, true,
                VEGETATION_BUFFER, TREE_SEARCH_BUFFER);
    }

    /**
     * The Trapper Hut's site. Plants growing on the natural ground around and between the authored
     * parts are left to grow, so the clearing reads as lived-in rather than mown; only plants
     * standing on the template's own floor are cleared.
     */
    public static void captureTrapperHut(
            WorldGenLevel level,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            PiecesContainer pieces,
            Set<Long> authoredBlocks) {
        captureStructureBlocks(level, chunkBox, chunkPos, pieces, authoredBlocks, false, false, true,
                VEGETATION_BUFFER, TREE_SEARCH_BUFFER);
    }

    /** Wider connected-tree cleanup for an intentionally cleared woodland site. */
    public static void captureForestClearing(
            WorldGenLevel level,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            PiecesContainer pieces,
            Set<Long> authoredBlocks) {
        captureStructureBlocks(level, chunkBox, chunkPos, pieces, authoredBlocks, false, false, false, 10, 32);
    }

    private static void captureStructureBlocks(
            WorldGenLevel level,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            PiecesContainer pieces,
            Set<Long> authoredBlocks,
            boolean swamp,
            boolean keepAuthoredMoss,
            boolean keepNaturalGround,
            int vegetationBuffer,
            int treeSearchBuffer) {
        BoundingBox structureBox = pieces.calculateBoundingBox();
        BoundingBox seedBox = clippedHorizontal(
                expanded(structureBox, vegetationBuffer, BELOW_CLEARANCE, CANOPY_CLEARANCE),
                chunkBox,
                level);
        BoundingBox searchBox = clippedHorizontal(
                expanded(structureBox, treeSearchBuffer, BELOW_CLEARANCE, CANOPY_CLEARANCE),
                chunkBox,
                level);
        if (seedBox == null || searchBox == null) {
            return;
        }

        Set<Long> protectedBlocks = new HashSet<>(authoredBlocks);
        Set<Long> keptCarpets = new HashSet<>();
        if (keepAuthoredMoss) {
            for (long packed : authoredBlocks) {
                if (level.getBlockState(BlockPos.of(packed)).is(Blocks.MOSS_CARPET)) {
                    keptCarpets.add(packed);
                }
            }
        }

        CleanupKey key = new CleanupKey(level.getLevel().dimension(), chunkPos.toLong());
        PENDING.merge(
                key,
                new CleanupTask(seedBox, searchBox, structureBox, protectedBlocks, keptCarpets, keepNaturalGround, swamp),
                CleanupTask::merge);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (CleanupKey key : Set.copyOf(PENDING.keySet())) {
            ServerLevel level = server.getLevel(key.dimension());
            ChunkPos chunkPos = new ChunkPos(key.chunkPos());
            if (level == null || !level.hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }

            CleanupTask task = PENDING.remove(key);
            if (task != null) {
                clear(level, task);
            }
        }
    }

    private static void clear(ServerLevel level, CleanupTask task) {
        Set<Long> removableTrees = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        forEach(task.seedBox(), cursor, pos -> {
            long packed = pos.asLong();
            // Moss carpet is biome decoration, not part of the outpost palette. Remove it even
            // when terrain/template overlap caused the footprint capture to mark the position as
            // authored.
            BlockState initialState = level.getBlockState(pos);
            // The Boar Trap's carpet over its pit is authored: it is what the pit hangs from.
            if (initialState.is(Blocks.MOSS_CARPET) && task.keptCarpets().contains(packed)) {
                return;
            }
            if (initialState.is(Blocks.MOSS_CARPET)
                    || initialState.is(Blocks.VINE)
                    || OptionalVegetationCompat.isCutVine(initialState)
                    || OptionalVegetationCompat.isWildscapesPlant(initialState)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                return;
            }
            if (task.protectedBlocks().contains(packed)) {
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (task.structureBox().isInside(pos) && isStructureIntrusion(state)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
            } else if (isTreePart(state, task.swamp())) {
                if (removableTrees.add(packed)) {
                    open.add(pos.immutable());
                }
            } else if (OutpostTerrainCleaner.isSmallVegetation(state)
                    && !(task.keepNaturalGround() && !task.protectedBlocks().contains(pos.below().asLong()))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
            }
        });

        while (!open.isEmpty() && removableTrees.size() < MAX_CONNECTED_TREE_BLOCKS) {
            BlockPos current = open.removeFirst();
            for (BlockPos neighbor : sixNeighbors(current)) {
                if (!task.searchBox().isInside(neighbor)) {
                    continue;
                }
                long packed = neighbor.asLong();
                if (task.protectedBlocks().contains(packed) || removableTrees.contains(packed)) {
                    continue;
                }
                if (isTreePart(level.getBlockState(neighbor), task.swamp())) {
                    removableTrees.add(packed);
                    open.add(neighbor);
                }
            }
        }

        for (long packed : removableTrees) {
            BlockPos pos = BlockPos.of(packed);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }

        // Acorns are placed beneath natural oak leaves during decoration. When those leaves are
        // removed for a clearing, clean up only the acorns that have lost their support.
        forEach(task.searchBox(), cursor, pos -> {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.ACORN.get()) && !state.canSurvive(level, pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
            }
        });
    }

    private static boolean isStructureIntrusion(BlockState state) {
        return OptionalTerrainCompat.isCreateAsurine(state);
    }

    private static boolean isTreePart(BlockState state, boolean swamp) {
        return state.is(BlockTags.LOGS)
                || OptionalVegetationCompat.isWildscapesTreeBlock(state)
                || state.is(BlockTags.LEAVES)
                || state.is(Blocks.VINE)
                || swamp && (OptionalVegetationCompat.isCutVine(state)
                || OptionalVegetationCompat.isShelfMushroomBlock(state)
                || state.is(Blocks.MUSHROOM_STEM)
                || state.is(Blocks.BROWN_MUSHROOM_BLOCK))
                || state.is(Blocks.MANGROVE_ROOTS)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS)
                || state.is(Blocks.HANGING_ROOTS);
    }

    private static BoundingBox expanded(BoundingBox box, int horizontal, int below, int above) {
        return new BoundingBox(
                box.minX() - horizontal, box.minY() - below, box.minZ() - horizontal,
                box.maxX() + horizontal, box.maxY() + above, box.maxZ() + horizontal);
    }

    private static BoundingBox clippedHorizontal(
            BoundingBox box,
            BoundingBox chunkBox,
            WorldGenLevel level) {
        int minX = Math.max(box.minX(), chunkBox.minX());
        int maxX = Math.min(box.maxX(), chunkBox.maxX());
        int minZ = Math.max(box.minZ(), chunkBox.minZ());
        int maxZ = Math.min(box.maxZ(), chunkBox.maxZ());
        if (minX > maxX || minZ > maxZ) {
            return null;
        }
        return new BoundingBox(
                minX, Math.max(level.getMinBuildHeight(), box.minY()), minZ,
                maxX, Math.min(level.getMaxBuildHeight() - 1, box.maxY()), maxZ);
    }

    private static BlockPos[] sixNeighbors(BlockPos pos) {
        return new BlockPos[]{
                pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()
        };
    }

    private static void forEach(BoundingBox box, BlockPos.MutableBlockPos cursor, PositionConsumer consumer) {
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    cursor.set(x, y, z);
                    consumer.accept(cursor);
                }
            }
        }
    }

    @FunctionalInterface
    private interface PositionConsumer {
        void accept(BlockPos pos);
    }

    private record CleanupKey(ResourceKey<Level> dimension, long chunkPos) {
    }

    private record CleanupTask(
            BoundingBox seedBox,
            BoundingBox searchBox,
            BoundingBox structureBox,
            Set<Long> protectedBlocks,
            Set<Long> keptCarpets,
            boolean keepNaturalGround,
            boolean swamp) {
        private CleanupTask merge(CleanupTask other) {
            Set<Long> keptCombined = new HashSet<>(keptCarpets);
            keptCombined.addAll(other.keptCarpets);
            Set<Long> protectedCombined = new HashSet<>(protectedBlocks);
            protectedCombined.addAll(other.protectedBlocks);
            return new CleanupTask(
                    encompass(seedBox, other.seedBox),
                    encompass(searchBox, other.searchBox),
                    encompass(structureBox, other.structureBox),
                    protectedCombined,
                    keptCombined,
                    keepNaturalGround || other.keepNaturalGround,
                    swamp || other.swamp);
        }

        private static BoundingBox encompass(BoundingBox first, BoundingBox second) {
            return new BoundingBox(
                    Math.min(first.minX(), second.minX()),
                    Math.min(first.minY(), second.minY()),
                    Math.min(first.minZ(), second.minZ()),
                    Math.max(first.maxX(), second.maxX()),
                    Math.max(first.maxY(), second.maxY()),
                    Math.max(first.maxZ(), second.maxZ()));
        }
    }
}
