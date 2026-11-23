package com.kingsandmonsters.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Tells each spawner in an encounter template what it spawns, by template-local position. The
 * saved spawners only carry vanilla defaults (or nothing at all), so this is the single place that
 * decides the roster.
 *
 * <p>Trial spawners only get their entity id here; the usual {@code ogre_trial_spawner} processor
 * that follows in the list turns that into the full wave config and cooldown. Regular spawners get
 * a slow, small trickle so they can't be farmed the way a vanilla default spawner can.</p>
 */
public final class EncounterSpawnerProcessor extends StructureProcessor {
    public static final MapCodec<EncounterSpawnerProcessor> CODEC =
            Codec.STRING.optionalFieldOf("variant", "supply_1")
                    .xmap(EncounterSpawnerProcessor::new, processor -> processor.variant);

    private static final String GRUNT = "kingsandmonsters:ogre_grunt";
    private static final String ARCHER = "kingsandmonsters:ogre_archer";
    private static final String PLAGUE_OGRE = "kingsandmonsters:plague_ogre";

    /** One spawner block: what it spawns and how fast. */
    private record Trickle(String entityId, int minDelay, int maxDelay, int maxNearby, int playerRange) {
    }

    // supply_camp_2.nbt: the caravan's spare guard, one grunt every minute or two.
    private static final Map<BlockPos, Trickle> SUPPLY_SPAWNERS = Map.of(
            new BlockPos(3, 5, 20), new Trickle(GRUNT, 1200, 2400, 2, 20));
    // abandoned_ogre_camp.nbt: two graves the infection keeps crawling out of.
    private static final Map<BlockPos, Trickle> ABANDONED_SPAWNERS = Map.of(
            new BlockPos(17, 1, 5), new Trickle(PLAGUE_OGRE, 900, 1800, 2, 24),
            new BlockPos(17, 1, 7), new Trickle(PLAGUE_OGRE, 900, 1800, 2, 24));
    // moving_camp.nbt and ogre_kings_quarry.nbt: archer trials in the lookouts and towers.
    private static final Map<BlockPos, String> MOVING_TRIALS = Map.of(
            new BlockPos(14, 8, 17), ARCHER);
    private static final Map<BlockPos, String> QUARRY_TRIALS = Map.of(
            new BlockPos(42, 44, 46), ARCHER,
            new BlockPos(6, 47, 31), ARCHER);

    private final String variant;

    public EncounterSpawnerProcessor(String variant) {
        this.variant = variant;
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
        if (relativeInfo.state().is(Blocks.SPAWNER)) {
            Trickle trickle = switch (variant) {
                case "supply_2" -> SUPPLY_SPAWNERS.get(rawInfo.pos());
                case "abandoned" -> ABANDONED_SPAWNERS.get(rawInfo.pos());
                default -> null;
            };
            return trickle == null ? relativeInfo : withNbt(relativeInfo, spawnerNbt(relativeInfo, trickle));
        }
        if (relativeInfo.state().is(Blocks.TRIAL_SPAWNER)) {
            String entityId = switch (variant) {
                case "moving" -> MOVING_TRIALS.get(rawInfo.pos());
                case "quarry" -> QUARRY_TRIALS.get(rawInfo.pos());
                default -> null;
            };
            return entityId == null ? relativeInfo : withNbt(relativeInfo, trialNbt(relativeInfo, entityId));
        }
        return relativeInfo;
    }

    private static CompoundTag spawnerNbt(StructureTemplate.StructureBlockInfo info, Trickle trickle) {
        CompoundTag nbt = info.nbt() == null ? new CompoundTag() : info.nbt().copy();
        CompoundTag data = entityData(trickle.entityId());
        nbt.put("SpawnData", data.copy());
        CompoundTag potential = new CompoundTag();
        potential.put("data", data);
        potential.putInt("weight", 1);
        ListTag potentials = new ListTag();
        potentials.add(potential);
        nbt.put("SpawnPotentials", potentials);
        nbt.putShort("MinSpawnDelay", (short) trickle.minDelay());
        nbt.putShort("MaxSpawnDelay", (short) trickle.maxDelay());
        nbt.putShort("SpawnCount", (short) 1);
        nbt.putShort("MaxNearbyEntities", (short) trickle.maxNearby());
        nbt.putShort("RequiredPlayerRange", (short) trickle.playerRange());
        nbt.putShort("SpawnRange", (short) 5);
        nbt.putShort("Delay", (short) trickle.minDelay());
        return nbt;
    }

    private static CompoundTag trialNbt(StructureTemplate.StructureBlockInfo info, String entityId) {
        CompoundTag nbt = info.nbt() == null ? new CompoundTag() : info.nbt().copy();
        nbt.put("spawn_data", entityData(entityId));
        return nbt;
    }

    private static CompoundTag entityData(String entityId) {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", entityId);
        CompoundTag data = new CompoundTag();
        data.put("entity", entity);
        return data;
    }

    private static StructureTemplate.StructureBlockInfo withNbt(
            StructureTemplate.StructureBlockInfo info, CompoundTag nbt) {
        return new StructureTemplate.StructureBlockInfo(info.pos(), info.state(), nbt);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructures.ENCOUNTER_SPAWNER.get();
    }
}
