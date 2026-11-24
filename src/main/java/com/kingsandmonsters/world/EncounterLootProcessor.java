package com.kingsandmonsters.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Hands out loot to the storage containers in the encounter templates, by template-local position
 * (same approach as {@link OutpostMobLootProcessor}). Almost every barrel in these builds is wall,
 * roof or tower detailing, so only the ones listed here are real storage. Trapped chests are not
 * handled here: they're the tribute chests and get their loot from the rule in the processor list.
 */
public final class EncounterLootProcessor extends StructureProcessor {
    public static final Codec<EncounterLootProcessor> CODEC =
            Codec.STRING.optionalFieldOf("variant", "supply_1")
                    .xmap(EncounterLootProcessor::new, processor -> processor.variant).codec();

    private static final String SUPPLY = "kingsandmonsters:chests/ogre_supply_camp";
    private static final String SUPPLY_LARDER = "kingsandmonsters:chests/ogre_supply_camp_larder";
    private static final String ABANDONED = "kingsandmonsters:chests/ogre_abandoned_camp";
    private static final String ABANDONED_SCRAPS = "kingsandmonsters:chests/ogre_abandoned_camp_barrel";
    private static final String QUARRY = "kingsandmonsters:chests/ogre_kings_quarry";
    private static final String TRAPPER_TRIBUTE = "kingsandmonsters:chests/ogre_trapper_tribute_exports";
    private static final String TRAPPER_CACHE = "kingsandmonsters:chests/ogre_trapper_hunter_cache";

    // supply_camp_1.nbt and supply_camp_2.nbt share one layout for their storage.
    private static final Map<BlockPos, String> SUPPLY_CAMP = Map.of(
            new BlockPos(8, 5, 4), SUPPLY,
            new BlockPos(4, 5, 20), SUPPLY_LARDER);
    // abandoned_ogre_camp.nbt: the one barrel with an open top, left on the lookout platform.
    private static final Map<BlockPos, String> ABANDONED_CAMP = Map.of(
            new BlockPos(20, 7, 27), ABANDONED);
    // ogre_kings_quarry.nbt: the foreman's double chest and the single chest by the ore carts.
    private static final Map<BlockPos, String> QUARRY_CHESTS = Map.of(
            new BlockPos(31, 39, 6), QUARRY,
            new BlockPos(31, 39, 7), QUARRY,
            new BlockPos(37, 40, 14), QUARRY);
    private static final Map<BlockPos, String> TRAPPER_HUT = Map.of(
            new BlockPos(12, 4, 8), TRAPPER_TRIBUTE,
            new BlockPos(15, 4, 6), TRAPPER_CACHE,
            new BlockPos(15, 4, 10), TRAPPER_CACHE,
            new BlockPos(18, 4, 13), TRAPPER_CACHE,
            new BlockPos(22, 4, 13), TRAPPER_CACHE);
    private static final Map<BlockPos, String> BOAR_TRAP_2 = Map.of(
            new BlockPos(7, 5, 7), TRAPPER_CACHE);

    private final String variant;

    public EncounterLootProcessor(String variant) {
        this.variant = variant;
    }

    private Map<BlockPos, String> containers() {
        return switch (variant) {
            case "supply_1", "supply_2" -> SUPPLY_CAMP;
            case "abandoned" -> ABANDONED_CAMP;
            case "quarry" -> QUARRY_CHESTS;
            case "trapper_hut_1", "trapper_hut_2" -> TRAPPER_HUT;
            case "boar_trap_2" -> BOAR_TRAP_2;
            default -> Map.of();
        };
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo process(
            LevelReader level,
            BlockPos offset,
            BlockPos pos,
            StructureTemplate.StructureBlockInfo rawInfo,
            StructureTemplate.StructureBlockInfo relativeInfo,
            StructurePlaceSettings settings,
            StructureTemplate template) {
        if (!relativeInfo.state().is(Blocks.CHEST) && !relativeInfo.state().is(Blocks.TRAPPED_CHEST)
                && !relativeInfo.state().is(Blocks.BARREL)) {
            return relativeInfo;
        }
        String lootTable = containers().get(rawInfo.pos());
        if (lootTable == null && variant.equals("abandoned") && relativeInfo.state().is(Blocks.BARREL)) {
            // The camp's other barrels hold what the plague left behind.
            lootTable = ABANDONED_SCRAPS;
        }
        if (lootTable == null) {
            return relativeInfo;
        }

        CompoundTag nbt = relativeInfo.nbt() == null ? new CompoundTag() : relativeInfo.nbt().copy();
        nbt.putString("LootTable", lootTable);
        nbt.remove("Items");
        return new StructureTemplate.StructureBlockInfo(relativeInfo.pos(), relativeInfo.state(), nbt);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructures.ENCOUNTER_LOOT.get();
    }
}
