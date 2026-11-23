package com.kingsandmonsters.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.List;
import java.util.Optional;

/**
 * Places Boar Traps a few chunks from a Trapper Hut. It is a random-spread placement on the hut's
 * own grid, so {@code /locate} understands it, but the chunk it offers for each grid cell is the
 * one {@link TrapperSites#trapChunk} derives from that cell's hut candidate, and it only accepts a
 * chunk when the hut candidate is a real hut start (its own frequency and exclusions included).
 * Nothing here touches terrain.
 */
@SuppressWarnings("deprecation")
public final class TrapperPairedStructurePlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<TrapperPairedStructurePlacement> CODEC =
            RecordCodecBuilder.mapCodec(instance -> placementCodec(instance)
                    .and(StructurePlacement.ExclusionZone.CODEC.listOf()
                            .optionalFieldOf("exclusion_zones", List.of())
                            .forGetter(TrapperPairedStructurePlacement::exclusionZones))
                    .and(StructurePlacement.ExclusionZone.CODEC.fieldOf("partner")
                            .forGetter(TrapperPairedStructurePlacement::partner))
                    .apply(instance, TrapperPairedStructurePlacement::new));

    private final List<ExclusionZone> exclusionZones;
    private final ExclusionZone partner;

    public TrapperPairedStructurePlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod,
                                           float frequency, int salt, Optional<ExclusionZone> exclusionZone,
                                           List<ExclusionZone> exclusionZones, ExclusionZone partner) {
        // The grid, and so the salt, belong to the hut: the JSON salt is read but never used.
        super(locateOffset, frequencyReductionMethod, frequency, TrapperSites.GRID_SALT, exclusionZone,
                TrapperSites.GRID_SPACING, TrapperSites.GRID_SEPARATION, RandomSpreadType.LINEAR);
        this.exclusionZones = List.copyOf(exclusionZones);
        this.partner = partner;
    }

    public List<ExclusionZone> exclusionZones() {
        return exclusionZones;
    }

    /** The hut's structure set. Only its set is used; the chunk count is ignored. */
    public ExclusionZone partner() {
        return partner;
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int chunkX, int chunkZ) {
        return TrapperSites.trapChunk(seed, chunkX, chunkZ);
    }

    @Override
    public boolean applyInteractionsWithOtherStructures(
            ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        ChunkPos hut = TrapperSites.hutChunk(state.getLevelSeed(), chunkX, chunkZ);
        // Range 0 asks the hut's own placement about exactly that one chunk.
        if (!state.hasStructureChunkInRange(partner.otherSet(), hut.x, hut.z, 0)) {
            return false;
        }
        for (ExclusionZone zone : exclusionZones) {
            if (state.hasStructureChunkInRange(zone.otherSet(), chunkX, chunkZ, zone.chunkCount())) {
                return false;
            }
        }
        return super.applyInteractionsWithOtherStructures(state, chunkX, chunkZ);
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModStructures.TRAPPER_PAIRED_SPREAD.get();
    }
}
