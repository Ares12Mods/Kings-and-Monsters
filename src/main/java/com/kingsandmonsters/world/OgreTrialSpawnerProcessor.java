package com.kingsandmonsters.world;

import com.kingsandmonsters.entity.OgreGrunt;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
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
 * Turns the trial-spawner markers saved in the Ogre structures into ordinary monster spawners,
 * each configured for its role. The three royal trials are led by the Mage, Brute, and Grunt
 * Captains; Ogre Guards are reserved for protecting the sleeping King. Minecraft 1.20.1 has no
 * trial spawners, so the saved block comes through as air and is recognised by its block entity id.
 */
public final class OgreTrialSpawnerProcessor extends StructureProcessor {
    public static final Codec<OgreTrialSpawnerProcessor> CODEC =
            Codec.unit(OgreTrialSpawnerProcessor::new);
    private static final String TRIAL_SPAWNER_ID = "minecraft:trial_spawner";
    private static final BlockPos ROYAL_MAGE_TRIAL_LOCAL_POS = new BlockPos(30, 3, 9);
    private static final BlockPos ROYAL_BRUTE_TRIAL_LOCAL_POS = new BlockPos(31, 4, 90);
    private static final BlockPos ROYAL_GRUNT_TRIAL_LOCAL_POS = new BlockPos(12, 3, 65);
    private static final BlockPos ROYAL_WEST_ARCHER_TRIAL_LOCAL_POS = new BlockPos(43, 11, 7);
    private static final BlockPos ROYAL_EAST_ARCHER_TRIAL_LOCAL_POS = new BlockPos(52, 11, 80);
    private static final Vec3i ROYAL_FORT_TEMPLATE_SIZE = new Vec3i(67, 27, 103);
    private static final Vec3i OUTPOST_TEMPLATE_SIZE = new Vec3i(34, 16, 26);
    public static final String ROYAL_MAGE_SQUAD_TAG = "KingsAndMonstersRoyalMageSquad";

    /** How a spawner behaves: the trial numbers become spawn count, cap and delays of a monster spawner. */
    private record Profile(int spawnRange, int spawnCount, int maxNearby, int minDelay, int maxDelay,
                           int requiredPlayerRange) {
    }

    // The royal trial leaders come one at a time; each brings its own squad (see OgreGrunt and OgreMage).
    private static final Profile ROYAL_LEADER_PROFILE = new Profile(5, 1, 1, 100, 200, 3);
    private static final Profile OUTPOST_REINFORCEMENT_PROFILE = new Profile(6, 1, 3, 600, 1200, 10);

    private static final Map<String, Profile> PROFILES = Map.of(
            "kingsandmonsters:ogre_grunt", new Profile(5, 2, 4, 400, 1000, 14),
            "kingsandmonsters:ogre_archer", new Profile(6, 2, 3, 600, 1200, 24),
            "kingsandmonsters:ogre_mage", ROYAL_LEADER_PROFILE,
            "kingsandmonsters:ogre_brute", ROYAL_LEADER_PROFILE,
            "kingsandmonsters:ogre_guard", new Profile(4, 1, 2, 800, 1600, 14),
            "kingsandmonsters:ogre_grunt_captain", ROYAL_LEADER_PROFILE);

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
        if (relativeInfo.nbt() == null || !TRIAL_SPAWNER_ID.equals(relativeInfo.nbt().getString("id"))) {
            return relativeInfo;
        }

        CompoundTag source = relativeInfo.nbt();
        boolean royalFortTemplate = template.getSize().equals(ROYAL_FORT_TEMPLATE_SIZE);
        boolean outpostTemplate = template.getSize().equals(OUTPOST_TEMPLATE_SIZE);
        String entityId = readEntityId(source);
        if (outpostTemplate) {
            entityId = "kingsandmonsters:ogre_grunt";
        } else if (entityId.isEmpty() && royalFortTemplate) {
            entityId = royalFortEntityId(rawInfo.pos());
        }
        // Older fort templates used an Ogre Guard marker for this trial. Force that fixed royal
        // position to the dedicated Grunt Captain so both old and newly rebuilt structures use the
        // correct leader while ordinary Guards remain free to protect the sleeping King.
        if (royalFortTemplate && rawInfo.pos().equals(ROYAL_GRUNT_TRIAL_LOCAL_POS)) {
            entityId = "kingsandmonsters:ogre_grunt_captain";
        }
        Profile profile = PROFILES.get(entityId);
        if (profile == null) {
            return relativeInfo;
        }

        boolean royalMageTrial = royalFortTemplate && "kingsandmonsters:ogre_mage".equals(entityId)
                && rawInfo.pos().equals(ROYAL_MAGE_TRIAL_LOCAL_POS);
        boolean royalBruteTrial = royalFortTemplate && "kingsandmonsters:ogre_brute".equals(entityId)
                && rawInfo.pos().equals(ROYAL_BRUTE_TRIAL_LOCAL_POS);
        boolean royalGruntTrial = royalFortTemplate && "kingsandmonsters:ogre_grunt_captain".equals(entityId)
                && rawInfo.pos().equals(ROYAL_GRUNT_TRIAL_LOCAL_POS);
        boolean royalPatrolTrial = royalBruteTrial || royalGruntTrial;
        if (outpostTemplate) {
            profile = OUTPOST_REINFORCEMENT_PROFILE;
        } else if (royalMageTrial || royalPatrolTrial) {
            profile = ROYAL_LEADER_PROFILE;
        }

        CompoundTag entity = new CompoundTag();
        entity.putString("id", entityId);
        if (royalFortTemplate) {
            // The royal trials are real fights, whatever the tribe thinks of the player.
            entity.putBoolean(OgreGrunt.ROYAL_TRIAL_OGRE_TAG, true);
        }
        if (royalMageTrial) {
            entity.putBoolean(ROYAL_MAGE_SQUAD_TAG, true);
        }
        if (royalPatrolTrial) {
            entity.putBoolean(OgreGrunt.ROYAL_PATROL_SQUAD_TAG, true);
        }
        if (outpostTemplate) {
            entity.putBoolean(OgreGrunt.OUTPOST_REINFORCEMENT_SQUAD_TAG, true);
        }

        CompoundTag spawnData = new CompoundTag();
        spawnData.put("entity", entity);
        CompoundTag potential = new CompoundTag();
        potential.putInt("weight", 1);
        potential.put("data", spawnData.copy());
        ListTag potentials = new ListTag();
        potentials.add(potential);

        CompoundTag spawner = new CompoundTag();
        spawner.putString("id", "minecraft:mob_spawner");
        spawner.put("SpawnData", spawnData);
        spawner.put("SpawnPotentials", potentials);
        spawner.putShort("SpawnRange", (short) profile.spawnRange());
        spawner.putShort("SpawnCount", (short) profile.spawnCount());
        spawner.putShort("MaxNearbyEntities", (short) profile.maxNearby());
        spawner.putShort("RequiredPlayerRange", (short) profile.requiredPlayerRange());
        spawner.putShort("MinSpawnDelay", (short) profile.minDelay());
        spawner.putShort("MaxSpawnDelay", (short) profile.maxDelay());
        spawner.putShort("Delay", (short) 20);
        return new StructureTemplate.StructureBlockInfo(
                relativeInfo.pos(), Blocks.SPAWNER.defaultBlockState(), spawner);
    }

    private static String readEntityId(CompoundTag nbt) {
        if (!nbt.contains("spawn_data")) {
            return "";
        }
        return nbt.getCompound("spawn_data").getCompound("entity").getString("id");
    }

    private static String royalFortEntityId(BlockPos localPos) {
        if (localPos.equals(ROYAL_MAGE_TRIAL_LOCAL_POS)) {
            return "kingsandmonsters:ogre_mage";
        }
        if (localPos.equals(ROYAL_BRUTE_TRIAL_LOCAL_POS)) {
            return "kingsandmonsters:ogre_brute";
        }
        if (localPos.equals(ROYAL_GRUNT_TRIAL_LOCAL_POS)) {
            return "kingsandmonsters:ogre_grunt_captain";
        }
        if (localPos.equals(ROYAL_WEST_ARCHER_TRIAL_LOCAL_POS)
                || localPos.equals(ROYAL_EAST_ARCHER_TRIAL_LOCAL_POS)) {
            return "kingsandmonsters:ogre_archer";
        }
        return "";
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructures.OGRE_TRIAL_SPAWNER.get();
    }
}
