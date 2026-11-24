package com.kingsandmonsters.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Optional;
import java.util.Arrays;

/**
 * A Mudsnout wallow. It is a structure only so that it can be found with locate; the building is
 * done by {@link MudWallowGenerator}, which settles it into the terrain and spawns its boars.
 */
public final class MudWallowStructure extends Structure {
    public static final Codec<MudWallowStructure> CODEC = simpleCodec(MudWallowStructure::new);

    private static final int REACH = MudWallowGenerator.HALF_SIZE + MudWallowGenerator.BLEND_WIDTH;

    public MudWallowStructure(StructureSettings settings) {
        super(settings);
    }

    /**
     * Locate asks this for every candidate chunk within a hundred chunks, so it has to be cheap: the
     * biome is checked first, and the footprint is sampled after that.
     */
    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int centerX = chunk.getMinBlockX() + 8;
        int centerZ = chunk.getMinBlockZ() + 8;
        int seaLevel = context.chunkGenerator().getSeaLevel();

        Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(centerX),
                QuartPos.fromBlock(seaLevel), QuartPos.fromBlock(centerZ), context.randomState().sampler());
        if (!context.validBiome().test(biome)) {
            return Optional.empty();
        }

        int half = MudWallowGenerator.HALF_SIZE;
        // Each height lookup runs the terrain noise, and locate asks about every candidate chunk, so
        // most candidates are turned away on nine points before any of the 121 are sampled.
        if (!isFlatEnough(context, centerX, centerZ, half, half, seaLevel, null)) {
            return Optional.empty();
        }
        // Then the whole footprint. Nine points alone allowed narrow ridges and gullies between them,
        // which were only discovered after locate had already accepted the structure start.
        int[] heights = new int[121];
        if (!isFlatEnough(context, centerX, centerZ, half, 3, seaLevel, heights)) {
            return Optional.empty();
        }
        int heightIndex = 121;

        Arrays.sort(heights, 0, heightIndex);
        int groundY = heights[heightIndex / 2] - 1;
        BlockPos center = new BlockPos(centerX, groundY, centerZ);
        BoundingBox box = new BoundingBox(centerX - REACH, groundY - 8, centerZ - REACH,
                centerX + REACH, groundY + 12, centerZ + REACH);
        Rotation rotation = Rotation.getRandom(context.random());
        int variant = context.random().nextInt(MudWallowGenerator.variantCount());
        return Optional.of(new GenerationStub(center,
                builder -> builder.addPiece(new Piece(center, box, rotation, variant))));
    }

    /**
     * Samples the footprint on a grid of the given step and accepts it when it is dry and within the
     * allowed relief. The heights are stored when an array is given.
     */
    private static boolean isFlatEnough(GenerationContext context, int centerX, int centerZ, int half, int step,
                                        int seaLevel, int[] heights) {
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        int index = 0;
        for (int dx = -half; dx <= half; dx += step) {
            for (int dz = -half; dz <= half; dz += step) {
                int surface = context.chunkGenerator().getBaseHeight(centerX + dx, centerZ + dz,
                        Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                if (surface <= seaLevel) {
                    return false;
                }
                if (heights != null) {
                    heights[index++] = surface;
                }
                low = Math.min(low, surface);
                high = Math.max(high, surface);
                if (high - low > MudWallowGenerator.MAX_RELIEF) {
                    return false;
                }
            }
        }
        return true;
    }
    @Override
    public StructureType<?> type() {
        return ModStructures.MUD_WALLOW.get();
    }

    public static final class Piece extends StructurePiece {
        private static final String CENTER_TAG = "Center";
        private static final String ROTATION_TAG = "Rotation";
        private static final String VARIANT_TAG = "Variant";
        private final BlockPos center;
        private final Rotation rotation;
        private final int variant;

        public Piece(BlockPos center, BoundingBox box, Rotation rotation, int variant) {
            super(ModStructures.MUD_WALLOW_PIECE.get(), 0, box);
            this.center = center;
            this.rotation = rotation;
            this.variant = variant;
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.MUD_WALLOW_PIECE.get(), tag);
            this.center = BlockPos.of(tag.getLong(CENTER_TAG));
            Rotation loadedRotation;
            try {
                loadedRotation = Rotation.valueOf(tag.getString(ROTATION_TAG));
            } catch (IllegalArgumentException ignored) {
                loadedRotation = Rotation.NONE;
            }
            this.rotation = loadedRotation;
            this.variant = tag.getInt(VARIANT_TAG);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putLong(CENTER_TAG, center.asLong());
            tag.putString(ROTATION_TAG, rotation.name());
            tag.putInt(VARIANT_TAG, variant);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                                RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            // Clip placement and grading to this chunk, matching vanilla structure processing. This
            // prevents later-generated neighbour chunks from erasing the wallow's transition bank.
            MudWallowGenerator.placeChunk(level, random, center, rotation, variant, chunkBox);
        }
    }
}
