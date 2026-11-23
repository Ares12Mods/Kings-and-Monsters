package com.kingsandmonsters.world;

import com.kingsandmonsters.compat.wildscapes.WildscapesStructureCompat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Random-spread placement for structures that belong near a real Ogre King's Fort but never right
 * on top of it. A candidate chunk only counts when a Fort that will actually generate (the same
 * authoritative swamp test the Fort itself uses) sits between {@code min_distance_chunks} and
 * {@code max_distance_chunks} away, and no Fort at all is closer than the minimum.
 *
 * <p>The Fort grid values are copied in as plain numbers, like {@link FortTerritoryStructurePlacement}
 * does, so this never calls into the Fort's own set. Keep them in sync with ogre_kings_fort.json.
 * Only chunks that already won the random-spread roll pay for the Fort lookup, so locate stays cheap.</p>
 */
@SuppressWarnings("deprecation")
public final class FortNeighborStructurePlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<FortNeighborStructurePlacement> CODEC =
            RecordCodecBuilder.<FortNeighborStructurePlacement>mapCodec(instance -> instance.group(
                            Codec.INT.fieldOf("salt").forGetter(FortNeighborStructurePlacement::ownSalt),
                            Codec.intRange(1, 4096).fieldOf("spacing")
                                    .forGetter(RandomSpreadStructurePlacement::spacing),
                            Codec.intRange(0, 4095).fieldOf("separation")
                                    .forGetter(RandomSpreadStructurePlacement::separation),
                            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F)
                                    .forGetter(FortNeighborStructurePlacement::ownFrequency),
                            StructurePlacement.ExclusionZone.CODEC.listOf()
                                    .optionalFieldOf("exclusion_zones", List.of())
                                    .forGetter(FortNeighborStructurePlacement::exclusionZones),
                            Codec.intRange(1, 4096).fieldOf("fort_spacing")
                                    .forGetter(FortNeighborStructurePlacement::fortSpacing),
                            Codec.intRange(0, 4095).fieldOf("fort_separation")
                                    .forGetter(FortNeighborStructurePlacement::fortSeparation),
                            Codec.INT.fieldOf("fort_salt")
                                    .forGetter(FortNeighborStructurePlacement::fortSalt),
                            Codec.intRange(0, 4096).fieldOf("fort_spawn_exclusion_chunks")
                                    .forGetter(FortNeighborStructurePlacement::fortSpawnExclusionChunks),
                            Codec.intRange(0, 4096).fieldOf("fort_minimum_spacing_chunks")
                                    .forGetter(FortNeighborStructurePlacement::fortMinimumSpacingChunks),
                            Codec.intRange(0, 1024).fieldOf("min_distance_chunks")
                                    .forGetter(FortNeighborStructurePlacement::minDistanceChunks),
                            Codec.intRange(1, 2048).fieldOf("max_distance_chunks")
                                    .forGetter(FortNeighborStructurePlacement::maxDistanceChunks)
                    ).apply(instance, FortNeighborStructurePlacement::new))
                    .validate(FortNeighborStructurePlacement::validate);

    private final int ownSalt;
    private final float ownFrequency;
    private final List<StructurePlacement.ExclusionZone> exclusionZones;
    private final int fortSpacing;
    private final int fortSeparation;
    private final int fortSalt;
    private final int fortSpawnExclusionChunks;
    private final int fortMinimumSpacingChunks;
    private final int minDistanceChunks;
    private final int maxDistanceChunks;

    public FortNeighborStructurePlacement(
            int salt,
            int spacing,
            int separation,
            float frequency,
            List<ExclusionZone> exclusionZones,
            int fortSpacing,
            int fortSeparation,
            int fortSalt,
            int fortSpawnExclusionChunks,
            int fortMinimumSpacingChunks,
            int minDistanceChunks,
            int maxDistanceChunks) {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, frequency, salt, Optional.empty(),
                spacing, separation, RandomSpreadType.LINEAR);
        this.ownSalt = salt;
        this.ownFrequency = frequency;
        this.exclusionZones = List.copyOf(exclusionZones);
        this.fortSpacing = fortSpacing;
        this.fortSeparation = fortSeparation;
        this.fortSalt = fortSalt;
        this.fortSpawnExclusionChunks = fortSpawnExclusionChunks;
        this.fortMinimumSpacingChunks = fortMinimumSpacingChunks;
        this.minDistanceChunks = minDistanceChunks;
        this.maxDistanceChunks = maxDistanceChunks;
    }

    private static DataResult<FortNeighborStructurePlacement> validate(FortNeighborStructurePlacement placement) {
        if (placement.spacing() <= placement.separation()) {
            return DataResult.error(() -> "Spacing has to be larger than separation");
        }
        if (placement.fortSpacing <= placement.fortSeparation) {
            return DataResult.error(() -> "fort_spacing has to be larger than fort_separation");
        }
        if (placement.minDistanceChunks >= placement.maxDistanceChunks) {
            return DataResult.error(() -> "min_distance_chunks must be less than max_distance_chunks");
        }
        return DataResult.success(placement);
    }

    private int ownSalt() {
        return ownSalt;
    }

    private float ownFrequency() {
        return ownFrequency;
    }

    public List<ExclusionZone> exclusionZones() {
        return exclusionZones;
    }

    public int fortSpacing() {
        return fortSpacing;
    }

    public int fortSeparation() {
        return fortSeparation;
    }

    public int fortSalt() {
        return fortSalt;
    }

    public int fortSpawnExclusionChunks() {
        return fortSpawnExclusionChunks;
    }

    public int fortMinimumSpacingChunks() {
        return fortMinimumSpacingChunks;
    }

    public int minDistanceChunks() {
        return minDistanceChunks;
    }

    public int maxDistanceChunks() {
        return maxDistanceChunks;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        // The cheap random-spread roll first; only winners go on to look for a Fort.
        if (!super.isPlacementChunk(state, chunkX, chunkZ)) {
            return false;
        }
        Map<Long, Boolean> swampMemo = new HashMap<>();
        FortGridPlacement.SwampOracle swamp = (x, z) -> swampMemo.computeIfAbsent(
                ChunkPos.asLong(x, z),
                ignored -> state.biomeSource.getNoiseBiome(
                        QuartPos.fromBlock(new ChunkPos(x, z).getMiddleBlockX()),
                        QuartPos.fromBlock(64),
                        QuartPos.fromBlock(new ChunkPos(x, z).getMiddleBlockZ()),
                        state.randomState().sampler()).is(Biomes.SWAMP));
        return hasFortInRing(state.getLevelSeed(), chunkX, chunkZ, swamp);
    }

    /** True when a real Fort lies inside the ring and none lies inside the minimum distance. */
    private boolean hasFortInRing(long seed, int chunkX, int chunkZ, FortGridPlacement.SwampOracle swamp) {
        long minSq = (long) minDistanceChunks * minDistanceChunks;
        long maxSq = (long) maxDistanceChunks * maxDistanceChunks;
        int minRegionX = Math.floorDiv(chunkX - maxDistanceChunks, fortSpacing) - 1;
        int maxRegionX = Math.floorDiv(chunkX + maxDistanceChunks, fortSpacing) + 1;
        int minRegionZ = Math.floorDiv(chunkZ - maxDistanceChunks, fortSpacing) - 1;
        int maxRegionZ = Math.floorDiv(chunkZ + maxDistanceChunks, fortSpacing) + 1;
        boolean found = false;
        for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
            for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                ChunkPos fort = FortGridPlacement.candidateChunkAt(
                        seed, regionX, regionZ, fortSpacing, fortSeparation, fortSalt);
                long dx = (long) fort.x - chunkX;
                long dz = (long) fort.z - chunkZ;
                long distSq = dx * dx + dz * dz;
                if (distSq > maxSq) {
                    continue;
                }
                if (!FortGridPlacement.isAuthoritativeCandidate(seed, fort.x, fort.z, fortSpacing,
                        fortSeparation, fortSalt, fortSpawnExclusionChunks, fortMinimumSpacingChunks, swamp)) {
                    continue;
                }
                if (distSq < minSq) {
                    return false;
                }
                found = true;
            }
        }
        return found;
    }

    @Override
    public boolean applyInteractionsWithOtherStructures(
            ChunkGeneratorStructureState structureState,
            int chunkX,
            int chunkZ) {
        if (!super.applyInteractionsWithOtherStructures(structureState, chunkX, chunkZ)) {
            return false;
        }
        if (!WildscapesStructureCompat.isClear(structureState, chunkX, chunkZ)) {
            return false;
        }
        for (ExclusionZone zone : exclusionZones) {
            if (structureState.hasStructureChunkInRange(
                    zone.otherSet(), chunkX, chunkZ, zone.chunkCount())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModStructures.FORT_NEIGHBOR_RANDOM_SPREAD.get();
    }
}
