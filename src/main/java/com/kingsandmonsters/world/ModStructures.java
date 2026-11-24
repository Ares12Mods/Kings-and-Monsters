package com.kingsandmonsters.world;

import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public class ModStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, KingsAndMonsters.MODID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, KingsAndMonsters.MODID);

    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSOR_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, KingsAndMonsters.MODID);

    public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, KingsAndMonsters.MODID);

    public static final RegistryObject<StructureType<OgreOutpostStructure>> OGRE_OUTPOST =
            STRUCTURE_TYPES.register("ogre_outpost", () -> () -> OgreOutpostStructure.CODEC);

    public static final RegistryObject<StructureType<OgreKingsFortStructure>> OGRE_KINGS_FORT =
            STRUCTURE_TYPES.register("ogre_kings_fort", () -> () -> OgreKingsFortStructure.CODEC);

    public static final RegistryObject<StructureType<OgreEncounterStructure>> OGRE_ENCOUNTER =
            STRUCTURE_TYPES.register("ogre_encounter", () -> () -> OgreEncounterStructure.CODEC);

    public static final RegistryObject<StructureType<MudWallowStructure>> MUD_WALLOW =
            STRUCTURE_TYPES.register("mud_wallow", () -> () -> MudWallowStructure.CODEC);

    public static final RegistryObject<StructurePieceType> TRAPPER_GRADING_PIECE =
            STRUCTURE_PIECE_TYPES.register("trapper_grading",
                    () -> (StructurePieceType.ContextlessType) TrapperGradingPiece::new);

    public static final RegistryObject<StructurePieceType> MUD_WALLOW_PIECE =
            STRUCTURE_PIECE_TYPES.register("mud_wallow",
                    () -> (StructurePieceType.ContextlessType) MudWallowStructure.Piece::new);

    public static final RegistryObject<StructureProcessorType<EncounterAirProcessor>> ENCOUNTER_AIR =
            STRUCTURE_PROCESSOR_TYPES.register("encounter_air", () -> () -> EncounterAirProcessor.CODEC);

    public static final RegistryObject<StructureProcessorType<EncounterLootProcessor>> ENCOUNTER_LOOT =
            STRUCTURE_PROCESSOR_TYPES.register("encounter_loot", () -> () -> EncounterLootProcessor.CODEC);

    public static final RegistryObject<StructureProcessorType<EncounterSpawnerProcessor>> ENCOUNTER_SPAWNER =
            STRUCTURE_PROCESSOR_TYPES.register("encounter_spawner", () -> () -> EncounterSpawnerProcessor.CODEC);

    public static final RegistryObject<StructureProcessorType<FortFootprintProcessor>> FORT_FOOTPRINT_RECORDER =
            STRUCTURE_PROCESSOR_TYPES.register("fort_footprint_recorder", () -> () -> FortFootprintProcessor.CODEC);

    public static final RegistryObject<StructureProcessorType<FortChestLootProcessor>> FORT_CHEST_LOOT =
            STRUCTURE_PROCESSOR_TYPES.register("fort_chest_loot", () -> () -> FortChestLootProcessor.CODEC);

    public static final RegistryObject<StructureProcessorType<OutpostMobLootProcessor>> OUTPOST_MOB_LOOT =
            STRUCTURE_PROCESSOR_TYPES.register("outpost_mob_loot", () -> () -> OutpostMobLootProcessor.CODEC);

    public static final RegistryObject<StructureProcessorType<OgreTrialSpawnerProcessor>> OGRE_TRIAL_SPAWNER =
            STRUCTURE_PROCESSOR_TYPES.register("ogre_trial_spawner", () -> () -> OgreTrialSpawnerProcessor.CODEC);

    public static final RegistryObject<StructurePlacementType<MultiExclusionRandomSpreadStructurePlacement>>
            MULTI_EXCLUSION_RANDOM_SPREAD =
            STRUCTURE_PLACEMENT_TYPES.register(
                    "multi_exclusion_random_spread",
                    () -> () -> MultiExclusionRandomSpreadStructurePlacement.CODEC);

    public static final RegistryObject<StructurePlacementType<FortTerritoryStructurePlacement>>
            FORT_TERRITORY_RANDOM_SPREAD =
            STRUCTURE_PLACEMENT_TYPES.register(
                    "fort_territory_random_spread",
                    () -> () -> FortTerritoryStructurePlacement.CODEC);

    public static final RegistryObject<StructurePlacementType<FortNeighborStructurePlacement>>
            FORT_NEIGHBOR_RANDOM_SPREAD =
            STRUCTURE_PLACEMENT_TYPES.register(
                    "fort_neighbor_random_spread",
                    () -> () -> FortNeighborStructurePlacement.CODEC);

    public static final RegistryObject<StructurePlacementType<SwampFortStructurePlacement>>
            SWAMP_FORT_RANDOM_SPREAD =
            STRUCTURE_PLACEMENT_TYPES.register(
                    "swamp_fort_random_spread",
                    () -> () -> SwampFortStructurePlacement.CODEC);

    public static final RegistryObject<StructurePlacementType<TrapperPairedStructurePlacement>>
            TRAPPER_PAIRED_SPREAD =
            STRUCTURE_PLACEMENT_TYPES.register(
                    "trapper_paired_spread",
                    () -> () -> TrapperPairedStructurePlacement.CODEC);

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECE_TYPES.register(modEventBus);
        STRUCTURE_PROCESSOR_TYPES.register(modEventBus);
        STRUCTURE_PLACEMENT_TYPES.register(modEventBus);
    }
}
