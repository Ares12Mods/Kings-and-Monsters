package com.kingsandmonsters.tribute;

import com.kingsandmonsters.Config;
import com.kingsandmonsters.api.event.AngerLevelChangedEvent;
import com.kingsandmonsters.api.tribute.ITributeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class TributeSavedData extends SavedData {
    public static final String FILE_ID = "kingsandmonsters_tribute";
    public static final int MAX_FAVOR = 100;

    private final ResourceKey<Level> dimension;
    private final Map<Long, CampTributeData> camps = new HashMap<>();
    private final Map<UUID, Set<Long>> discoveredStructures = new HashMap<>();
    // One pending retaliation per player, keyed by UUID — a repeat provocation during the grace
    // period overwrites this rather than queuing a second one.
    private final Map<UUID, PendingRetaliation> pendingPatrolRetaliations = new HashMap<>();
    // Patrols still inside their cooldown, saved so a restart doesn't let another camp pile on.
    private final List<RecentPatrol> recentPatrols = new ArrayList<>();
    private int factionAngerLevel;
    private final Map<UUID, Integer> playerFavor = new HashMap<>();
    private final Map<UUID, QuestRecord> playerQuests = new HashMap<>();
    /** Per player: quest ordinal -> game time when it may be offered again after being finished. */
    private final Map<UUID, Map<Integer, Long>> questLockouts = new HashMap<>();

    public TributeSavedData(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public static TributeSavedData load(ResourceKey<Level> dimension, CompoundTag tag, HolderLookup.Provider registries) {
        TributeSavedData data = new TributeSavedData(dimension);
        ListTag campsTag = tag.getList("camps", Tag.TAG_COMPOUND);
        int legacyAngerTotal = 0;

        for (int i = 0; i < campsTag.size(); i++) {
            CompoundTag campTag = campsTag.getCompound(i);
            long posKey = campTag.getLong("pos");
            CampTributeData camp = data.new CampTributeData(BlockPos.of(posKey));
            legacyAngerTotal += campTag.getInt("angerLevel");
            camp.chiefAlive = !campTag.contains("chiefAlive") || campTag.getBoolean("chiefAlive");
            camp.nextPatrolGameTime = campTag.getLong("nextPatrolGameTime");
            camp.populationManaged = campTag.getBoolean("populationManaged");
            camp.captainType = OutpostCaptainType.fromSerializedName(campTag.getString("captainType"));
            camp.profile = OutpostProfile.byId(campTag.getString("profile"));
            camp.populationInitialized = campTag.getBoolean("populationInitialized");
            camp.nextResidentSpawnGameTime = campTag.getLong("nextResidentSpawnGameTime");
            camp.captainRespawnGameTime = campTag.getLong("captainRespawnGameTime");
            camp.jockeyTarget = campTag.contains("jockeyTarget") ? campTag.getInt("jockeyTarget") : -1;
            camp.jockeyPopulationInitialized = campTag.getBoolean("jockeyPopulationInitialized");
            camp.nextJockeySpawnGameTime = campTag.getLong("nextJockeySpawnGameTime");
            camp.structureKind = StructureKind.fromSerializedName(campTag.getString("structureKind"));
            if (campTag.contains("fortMapTarget")) {
                camp.fortMapTarget = BlockPos.of(campTag.getLong("fortMapTarget"));
            }
            if (campTag.contains("mapChestPos")) {
                camp.mapChestPos = BlockPos.of(campTag.getLong("mapChestPos"));
            }
            camp.mapRewardFulfilled = campTag.getBoolean("mapRewardFulfilled");
            if (campTag.contains("outpostMinX")) {
                camp.outpostMinX = campTag.getInt("outpostMinX");
                camp.outpostMaxX = campTag.getInt("outpostMaxX");
                camp.outpostMinZ = campTag.getInt("outpostMinZ");
                camp.outpostMaxZ = campTag.getInt("outpostMaxZ");
                camp.outpostGroundY = campTag.getInt("outpostGroundY");
                if (campTag.contains("pitX")) {
                    camp.pit = new PitZone(campTag.getInt("pitX"), campTag.getInt("pitZ"),
                            campTag.getInt("pitAvoid"), campTag.getInt("pitKeep"));
                }
                camp.hasOutpostBounds = true;
            }
            if (camp.structureKind == StructureKind.UNKNOWN) {
                camp.structureKind = camp.populationManaged ? StructureKind.OUTPOST : StructureKind.FORT;
            }
            data.camps.put(posKey, camp);
        }

        ListTag discoveriesTag = tag.getList("structureDiscoveries", Tag.TAG_COMPOUND);
        for (int i = 0; i < discoveriesTag.size(); i++) {
            CompoundTag playerTag = discoveriesTag.getCompound(i);
            if (!playerTag.hasUUID("player")) {
                continue;
            }
            Set<Long> positions = new HashSet<>();
            for (long position : playerTag.getLongArray("positions")) {
                positions.add(position);
            }
            data.discoveredStructures.put(playerTag.getUUID("player"), positions);
        }

        // Older saves stored anger separately on every camp. Preserve the total
        // hostility those camps generated when migrating to faction-wide anger.
        ListTag favorTag = tag.getList("playerFavor", Tag.TAG_COMPOUND);
        for (int i = 0; i < favorTag.size(); i++) {
            CompoundTag entry = favorTag.getCompound(i);
            int favor = Math.clamp(entry.getInt("favor"), 0, MAX_FAVOR);
            if (entry.hasUUID("player") && favor > 0) {
                data.playerFavor.put(entry.getUUID("player"), favor);
            }
        }
        ListTag questTag = tag.getList("playerQuests", Tag.TAG_COMPOUND);
        for (int i = 0; i < questTag.size(); i++) {
            CompoundTag entry = questTag.getCompound(i);
            if (entry.hasUUID("player")) {
                data.playerQuests.put(entry.getUUID("player"), new QuestRecord(entry.getInt("quest"),
                        entry.getInt("progress"), entry.getLong("cooldownUntil"), entry.getInt("completed")));
            }
        }
        ListTag lockTag = tag.getList("questLockouts", Tag.TAG_COMPOUND);
        for (int i = 0; i < lockTag.size(); i++) {
            CompoundTag entry = lockTag.getCompound(i);
            if (entry.hasUUID("player")) {
                data.questLockouts.computeIfAbsent(entry.getUUID("player"), id -> new HashMap<>())
                        .put(entry.getInt("quest"), entry.getLong("until"));
            }
        }
        data.factionAngerLevel = tag.contains("factionAngerLevel")
                ? data.clampAnger(tag.getInt("factionAngerLevel"))
                : data.clampAnger(legacyAngerTotal);

        ListTag retaliationsTag = tag.getList("patrolRetaliations", Tag.TAG_COMPOUND);
        for (int i = 0; i < retaliationsTag.size(); i++) {
            CompoundTag retaliationTag = retaliationsTag.getCompound(i);
            if (!retaliationTag.hasUUID("player")) {
                continue;
            }
            data.pendingPatrolRetaliations.put(retaliationTag.getUUID("player"), new PendingRetaliation(
                    BlockPos.of(retaliationTag.getLong("campOrigin")),
                    retaliationTag.getLong("eligibleGameTime")));
        }

        ListTag recentTag = tag.getList("recentPatrols", Tag.TAG_COMPOUND);
        for (int i = 0; i < recentTag.size(); i++) {
            CompoundTag entry = recentTag.getCompound(i);
            if (entry.hasUUID("player")) {
                data.recentPatrols.add(new RecentPatrol(entry.getUUID("player"),
                        BlockPos.of(entry.getLong("pos")), entry.getLong("time")));
            }
        }

        // Legacy reservedForts/pendingForts tags are intentionally ignored. They described
        // speculative targets that are not authoritative under the unconditional grid architecture.
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag campsTag = new ListTag();

        for (CampTributeData camp : camps.values()) {
            CompoundTag campTag = new CompoundTag();
            campTag.putLong("pos", camp.campOrigin.asLong());
            campTag.putBoolean("chiefAlive", camp.chiefAlive);
            campTag.putLong("nextPatrolGameTime", camp.nextPatrolGameTime);
            campTag.putBoolean("populationManaged", camp.populationManaged);
            campTag.putString("captainType", camp.captainType.serializedName());
            campTag.putString("profile", camp.profile.id());
            campTag.putBoolean("populationInitialized", camp.populationInitialized);
            campTag.putLong("nextResidentSpawnGameTime", camp.nextResidentSpawnGameTime);
            campTag.putLong("captainRespawnGameTime", camp.captainRespawnGameTime);
            campTag.putInt("jockeyTarget", camp.jockeyTarget);
            campTag.putBoolean("jockeyPopulationInitialized", camp.jockeyPopulationInitialized);
            campTag.putLong("nextJockeySpawnGameTime", camp.nextJockeySpawnGameTime);
            campTag.putString("structureKind", camp.structureKind.serializedName);
            if (camp.fortMapTarget != null) {
                campTag.putLong("fortMapTarget", camp.fortMapTarget.asLong());
            }
            if (camp.mapChestPos != null) {
                campTag.putLong("mapChestPos", camp.mapChestPos.asLong());
            }
            campTag.putBoolean("mapRewardFulfilled", camp.mapRewardFulfilled);
            if (camp.hasOutpostBounds) {
                campTag.putInt("outpostMinX", camp.outpostMinX);
                campTag.putInt("outpostMaxX", camp.outpostMaxX);
                campTag.putInt("outpostMinZ", camp.outpostMinZ);
                campTag.putInt("outpostMaxZ", camp.outpostMaxZ);
                campTag.putInt("outpostGroundY", camp.outpostGroundY);
                if (camp.pit != null) {
                    campTag.putInt("pitX", camp.pit.x());
                    campTag.putInt("pitZ", camp.pit.z());
                    campTag.putInt("pitAvoid", camp.pit.avoidRadius());
                    campTag.putInt("pitKeep", camp.pit.keepRadius());
                }
            }
            campsTag.add(campTag);
        }

        ListTag discoveriesTag = new ListTag();
        discoveredStructures.forEach((playerId, positions) -> {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player", playerId);
            playerTag.putLongArray("positions", positions.stream().mapToLong(Long::longValue).toArray());
            discoveriesTag.add(playerTag);
        });

        ListTag retaliationsTag = new ListTag();
        pendingPatrolRetaliations.forEach((playerId, retaliation) -> {
            CompoundTag retaliationTag = new CompoundTag();
            retaliationTag.putUUID("player", playerId);
            retaliationTag.putLong("campOrigin", retaliation.campOrigin().asLong());
            retaliationTag.putLong("eligibleGameTime", retaliation.eligibleGameTime());
            retaliationsTag.add(retaliationTag);
        });

        tag.putInt("factionAngerLevel", factionAngerLevel);
        ListTag favorTag = new ListTag();
        playerFavor.forEach((playerId, favor) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", playerId);
            entry.putInt("favor", favor);
            favorTag.add(entry);
        });
        tag.put("playerFavor", favorTag);
        ListTag questTag = new ListTag();
        playerQuests.forEach((playerId, record) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", playerId);
            entry.putInt("quest", record.quest());
            entry.putInt("progress", record.progress());
            entry.putLong("cooldownUntil", record.cooldownUntil());
            entry.putInt("completed", record.completed());
            questTag.add(entry);
        });
        tag.put("playerQuests", questTag);
        ListTag lockTag = new ListTag();
        questLockouts.forEach((playerId, locks) -> locks.forEach((quest, until) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", playerId);
            entry.putInt("quest", quest);
            entry.putLong("until", until);
            lockTag.add(entry);
        }));
        tag.put("questLockouts", lockTag);
        tag.put("camps", campsTag);
        tag.put("structureDiscoveries", discoveriesTag);
        tag.put("patrolRetaliations", retaliationsTag);
        ListTag recentTag = new ListTag();
        for (RecentPatrol patrol : recentPatrols) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", patrol.playerId());
            entry.putLong("pos", patrol.pos().asLong());
            entry.putLong("time", patrol.time());
            recentTag.add(entry);
        }
        tag.put("recentPatrols", recentTag);
        return tag;
    }

    public ITributeData getOrCreate(BlockPos campOrigin) {
        long posKey = campOrigin.asLong();
        return camps.computeIfAbsent(posKey, ignored -> {
            setDirty();
            return new CampTributeData(campOrigin);
        });
    }

    public ITributeData get(BlockPos campOrigin) {
        return camps.get(campOrigin.asLong());
    }

    public List<CampPatrolState> getPatrolStates() {
        return camps.values().stream()
                .map(camp -> new CampPatrolState(
                        camp.campOrigin,
                        getFactionAngerLevel(),
                        camp.chiefAlive,
                        camp.nextPatrolGameTime))
                .toList();
    }

    public Optional<BlockPos> findNearestCamp(BlockPos pos, int maxRadius) {
        long maxRadiusSq = (long) maxRadius * maxRadius;
        BlockPos nearest = null;
        long nearestDistSq = Long.MAX_VALUE;

        for (CampTributeData camp : camps.values()) {
            long dx = camp.campOrigin.getX() - pos.getX();
            long dz = camp.campOrigin.getZ() - pos.getZ();
            long distSq = dx * dx + dz * dz;
            if (distSq <= maxRadiusSq && distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = camp.campOrigin;
            }
        }
        return Optional.ofNullable(nearest);
    }

    public Optional<BlockPos> findNearestFort(BlockPos pos) {
        return getAllConfirmedForts().stream().min(nearestTo(pos));
    }

    /** Every Fort that has already, successfully generated. */
    public List<BlockPos> getAllConfirmedForts() {
        return camps.values().stream()
                .filter(camp -> camp.structureKind == StructureKind.FORT)
                .map(camp -> camp.campOrigin)
                .toList();
    }

    private static java.util.Comparator<BlockPos> nearestTo(BlockPos pos) {
        return java.util.Comparator.comparingLong(candidate -> {
            long dx = candidate.getX() - pos.getX();
            long dz = candidate.getZ() - pos.getZ();
            return dx * dx + dz * dz;
        });
    }

    public Optional<BlockPos> findOutpostFortMapTarget(BlockPos pos) {
        return findOwningOutpostCamp(pos)
                .filter(camp -> camp.fortMapTarget != null)
                .map(camp -> camp.fortMapTarget);
    }

    /** True for a camp the plague took: nothing of the tribe is left in it to be robbed. */
    public boolean isAbandonedCamp(BlockPos campOrigin) {
        CampTributeData camp = camps.get(campOrigin.asLong());
        return camp != null && camp.profile == OutpostProfile.ABANDONED_CAMP;
    }

    /** Only the full outposts, the King's fort and the moving camps send patrols; every other camp stays quiet. */
    public boolean canSendPatrols(BlockPos campOrigin) {
        CampTributeData camp = camps.get(campOrigin.asLong());
        if (camp == null) {
            return false;
        }
        return camp.structureKind == StructureKind.OUTPOST
                || camp.structureKind == StructureKind.FORT
                || (camp.structureKind == StructureKind.ENCOUNTER && camp.profile == OutpostProfile.MOVING_CAMP);
    }

    public Optional<BlockPos> findOutpostCampOrigin(BlockPos pos) {
        return findOwningOutpostCamp(pos).map(camp -> camp.campOrigin);
    }

    /** The Outpost (if any) whose structure bounds contain {@code pos}; the nearest origin wins on overlap. */
    private Optional<CampTributeData> findOwningOutpostCamp(BlockPos pos) {
        return camps.values().stream()
                .filter(camp -> camp.structureKind == StructureKind.OUTPOST
                        && camp.hasOutpostBounds
                        && pos.getX() >= camp.outpostMinX && pos.getX() <= camp.outpostMaxX
                        && pos.getZ() >= camp.outpostMinZ && pos.getZ() <= camp.outpostMaxZ)
                .min(java.util.Comparator.comparingLong(camp -> {
                    long dx = camp.campOrigin.getX() - pos.getX();
                    long dz = camp.campOrigin.getZ() - pos.getZ();
                    return dx * dx + dz * dz;
                }));
    }

    /**
     * Records the single physical chest that owes this Outpost's guaranteed Fort Map, the first
     * time it's found. Idempotent: a later call for the same Outpost (afterPlace runs once per
     * chunk intersecting the structure) never overwrites an already-recorded chest.
     */
    public void setOutpostMapChestPosIfAbsent(BlockPos campOrigin, BlockPos chestPos) {
        CampTributeData camp = (CampTributeData) getOrCreate(campOrigin);
        if (camp.mapChestPos == null) {
            camp.mapChestPos = chestPos.immutable();
            setDirty();
        }
    }

    public Optional<BlockPos> getOutpostMapChestPos(BlockPos campOrigin) {
        CampTributeData camp = camps.get(campOrigin.asLong());
        return camp == null ? Optional.empty() : Optional.ofNullable(camp.mapChestPos);
    }

    public Optional<BlockPos> getOutpostFortMapTarget(BlockPos campOrigin) {
        CampTributeData camp = camps.get(campOrigin.asLong());
        return camp == null ? Optional.empty() : Optional.ofNullable(camp.fortMapTarget);
    }

    public boolean isMapRewardFulfilled(BlockPos campOrigin) {
        CampTributeData camp = camps.get(campOrigin.asLong());
        return camp != null && camp.mapRewardFulfilled;
    }

    /** @return true if this call is the one that first marked the reward fulfilled. */
    public boolean markMapRewardFulfilled(BlockPos campOrigin) {
        CampTributeData camp = (CampTributeData) getOrCreate(campOrigin);
        if (camp.mapRewardFulfilled) {
            return false;
        }
        camp.mapRewardFulfilled = true;
        setDirty();
        return true;
    }

    public boolean isMapRewardFulfilledNear(BlockPos pos) {
        return findOwningOutpostCamp(pos).map(camp -> camp.mapRewardFulfilled).orElse(false);
    }

    /** @return true if this call is the one that first marked the reward fulfilled. */
    public boolean markMapRewardFulfilledNear(BlockPos pos) {
        return findOwningOutpostCamp(pos).map(camp -> {
            if (camp.mapRewardFulfilled) {
                return false;
            }
            camp.mapRewardFulfilled = true;
            setDirty();
            return true;
        }).orElse(false);
    }

    public int getFactionAngerLevel() {
        return Math.max(factionAngerLevel, angerFloor());
    }

    public int getFavor(UUID playerId) {
        return playerFavor.getOrDefault(playerId, 0);
    }

    /** A player's errand for the tribe: the active quest id (-1 for none), its progress, and when the next may be offered. */
    public record QuestRecord(int quest, int progress, long cooldownUntil, int completed) {
        public static final QuestRecord NONE = new QuestRecord(-1, 0, 0L, 0);
    }

    public QuestRecord getQuest(UUID playerId) {
        return playerQuests.getOrDefault(playerId, QuestRecord.NONE);
    }

    public void setQuest(UUID playerId, QuestRecord record) {
        playerQuests.put(playerId, record);
        setDirty();
    }

    /** Game time before which this player is not offered the quest again (0 when it is free). */
    public long questLockedUntil(UUID playerId, int quest) {
        Map<Integer, Long> locks = questLockouts.get(playerId);
        return locks == null ? 0L : locks.getOrDefault(quest, 0L);
    }

    public void lockQuest(UUID playerId, int quest, long until) {
        questLockouts.computeIfAbsent(playerId, id -> new HashMap<>()).put(quest, until);
        setDirty();
    }

    public boolean isAppeased(UUID playerId) {
        return getFavor(playerId) >= Config.TRIBUTE_APPEASED_FAVOR.get();
    }

    /** The tribe's mood is shared and starts out angry; each player's favor is their own. */
    private int angerFloor() {
        return Config.TRIBUTE_BASELINE_ANGER.get();
    }

    /** Returns true when the change moved this player into or out of the appeased state. */
    public boolean addFavor(UUID playerId, int delta) {
        boolean wasAppeased = isAppeased(playerId);
        int oldFavor = getFavor(playerId);
        int newFavor = (int) Math.clamp((long) oldFavor + delta, 0L, (long) MAX_FAVOR);
        if (newFavor == oldFavor) {
            return false;
        }
        if (newFavor == 0) {
            playerFavor.remove(playerId);
        } else {
            playerFavor.put(playerId, newFavor);
        }
        setDirty();
        return isAppeased(playerId) != wasAppeased;
    }

    public void registerOutpost(
            BlockPos campOrigin, OutpostCaptainType captainType, BoundingBox structureBounds) {
        CampTributeData camp = (CampTributeData) getOrCreate(campOrigin);
        if (camp.populationManaged && camp.captainType == captainType
                && camp.structureKind == StructureKind.OUTPOST
                && camp.hasOutpostBounds
                && camp.outpostMinX == structureBounds.minX()
                && camp.outpostMaxX == structureBounds.maxX()
                && camp.outpostMinZ == structureBounds.minZ()
                && camp.outpostMaxZ == structureBounds.maxZ()
                && camp.outpostGroundY == structureBounds.minY()) {
            return;
        }
        camp.populationManaged = true;
        camp.captainType = captainType;
        camp.structureKind = StructureKind.OUTPOST;
        camp.outpostMinX = structureBounds.minX();
        camp.outpostMaxX = structureBounds.maxX();
        camp.outpostMinZ = structureBounds.minZ();
        camp.outpostMaxZ = structureBounds.maxZ();
        camp.outpostGroundY = structureBounds.minY();
        camp.hasOutpostBounds = true;
        if (camp.jockeyTarget < 0) camp.jockeyTarget = Math.floorMod(Long.hashCode(campOrigin.asLong()), 3);
        setDirty();
    }

    /**
     * Registers a camp or the quarry. Unlike {@link #registerOutpost}, {@code groundY} is the real
     * ground height (the quarry's bounding box reaches far below it) and the roster comes from the
     * profile. Registering them as camps is what lets their trapped chests raise anger.
     */
    public void registerEncounter(
            BlockPos campOrigin, OutpostProfile profile, OutpostCaptainType captainType,
            BoundingBox structureBounds, int groundY, @org.jetbrains.annotations.Nullable PitZone pit) {
        CampTributeData camp = (CampTributeData) getOrCreate(campOrigin);
        if (camp.jockeyTarget < 0) {
            camp.jockeyTarget = encounterJockeyTarget(profile, campOrigin);
            setDirty();
        }
        if (camp.populationManaged && camp.profile == profile
                && camp.structureKind == StructureKind.ENCOUNTER
                && camp.hasOutpostBounds
                && camp.outpostGroundY == groundY
                && java.util.Objects.equals(camp.pit, pit)) {
            return;
        }
        camp.populationManaged = true;
        camp.profile = profile;
        camp.captainType = captainType;
        camp.structureKind = StructureKind.ENCOUNTER;
        camp.outpostMinX = structureBounds.minX();
        camp.outpostMaxX = structureBounds.maxX();
        camp.outpostMinZ = structureBounds.minZ();
        camp.outpostMaxZ = structureBounds.maxZ();
        camp.outpostGroundY = groundY;
        camp.pit = pit;
        camp.hasOutpostBounds = true;
        setDirty();
    }

    /** The quarry keeps one or two boar riders; one supply camp in four has a single one. */
    private static int encounterJockeyTarget(OutpostProfile profile, BlockPos origin) {
        return profile.jockeyTarget(origin);
    }

    public void registerFort(BlockPos fortOrigin, BoundingBox structureBounds) {
        CampTributeData camp = (CampTributeData) getOrCreate(fortOrigin);
        camp.structureKind = StructureKind.FORT;
        camp.outpostMinX = structureBounds.minX();
        camp.outpostMaxX = structureBounds.maxX();
        camp.outpostMinZ = structureBounds.minZ();
        camp.outpostMaxZ = structureBounds.maxZ();
        camp.outpostGroundY = structureBounds.minY();
        camp.hasOutpostBounds = true;
        if (camp.jockeyTarget < 0) camp.jockeyTarget = 1 + Math.floorMod(Long.hashCode(fortOrigin.asLong()), 2);
        setDirty();
    }

    public void setOutpostFortMapTarget(BlockPos campOrigin, BlockPos fortTarget) {
        CampTributeData camp = (CampTributeData) getOrCreate(campOrigin);
        BlockPos immutableTarget = fortTarget.immutable();
        if (!immutableTarget.equals(camp.fortMapTarget)) {
            camp.fortMapTarget = immutableTarget;
            setDirty();
        }
    }

    public List<DiscoverySite> getDiscoverySites() {
        // Reservations are not real, physically generated structures yet — a player walking near
        // one must not trigger a discovery overlay for something that isn't there.
        return camps.values().stream()
                .filter(camp -> camp.structureKind == StructureKind.OUTPOST
                        || camp.structureKind == StructureKind.FORT)
                .map(camp -> new DiscoverySite(camp.campOrigin, camp.structureKind))
                .toList();
    }

    public boolean markDiscovered(UUID playerId, BlockPos structureOrigin) {
        boolean added = discoveredStructures
                .computeIfAbsent(playerId, ignored -> new HashSet<>())
                .add(structureOrigin.asLong());
        if (added) {
            setDirty();
        }
        return added;
    }

    public List<OutpostPopulationState> getOutpostPopulationStates() {
        return camps.values().stream()
                .filter(camp -> camp.populationManaged)
                .map(camp -> new OutpostPopulationState(
                        camp.campOrigin,
                        camp.captainType,
                        camp.populationInitialized,
                        camp.nextResidentSpawnGameTime,
                        camp.captainRespawnGameTime,
                        camp.hasOutpostBounds,
                        camp.outpostMinX,
                        camp.outpostMaxX,
                        camp.outpostMinZ,
                        camp.outpostMaxZ,
                        camp.outpostGroundY,
                        camp.profile,
                        camp.pit))
                .toList();
    }

    public List<JockeyPopulationState> getJockeyPopulationStates() {
        return camps.values().stream()
                .filter(camp -> (camp.structureKind == StructureKind.OUTPOST || camp.structureKind == StructureKind.FORT
                        || camp.structureKind == StructureKind.ENCOUNTER)
                        && camp.hasOutpostBounds && camp.jockeyTarget >= 0)
                .map(camp -> new JockeyPopulationState(camp.campOrigin, camp.structureKind,
                        camp.jockeyTarget, camp.jockeyPopulationInitialized, camp.nextJockeySpawnGameTime,
                        camp.outpostMinX, camp.outpostMaxX, camp.outpostMinZ, camp.outpostMaxZ,
                        camp.outpostGroundY,
                        // The fort and the quarry keep their riders in any biome; other camps keep to boar country.
                        camp.structureKind == StructureKind.FORT || camp.profile == OutpostProfile.QUARRY))
                .toList();
    }

    public void updateJockeyPopulation(BlockPos origin, boolean initialized, long nextSpawnTime) {
        CampTributeData camp = camps.get(origin.asLong());
        if (camp == null) return;
        camp.jockeyPopulationInitialized = initialized;
        camp.nextJockeySpawnGameTime = nextSpawnTime;
        setDirty();
    }

    public void markPopulationInitialized(BlockPos campOrigin, long nextResidentSpawnGameTime) {
        CampTributeData camp = camps.get(campOrigin.asLong());
        if (camp == null) return;
        camp.populationInitialized = true;
        camp.nextResidentSpawnGameTime = nextResidentSpawnGameTime;
        camp.chiefAlive = true;
        setDirty();
    }

    public void setNextResidentSpawnGameTime(BlockPos campOrigin, long gameTime) {
        CampTributeData camp = camps.get(campOrigin.asLong());
        if (camp == null || camp.nextResidentSpawnGameTime == gameTime) return;
        camp.nextResidentSpawnGameTime = gameTime;
        setDirty();
    }

    public void markOutpostCaptainKilled(BlockPos campOrigin, long respawnGameTime) {
        CampTributeData camp = camps.get(campOrigin.asLong());
        if (camp == null || !camp.populationManaged) return;
        camp.chiefAlive = false;
        camp.captainRespawnGameTime = respawnGameTime;
        setDirty();
    }

    public void markOutpostCaptainAlive(BlockPos campOrigin) {
        CampTributeData camp = camps.get(campOrigin.asLong());
        if (camp == null || !camp.populationManaged) return;
        camp.chiefAlive = true;
        camp.captainRespawnGameTime = 0L;
        setDirty();
    }

    public void incrementFactionAnger(int delta, BlockPos sourceCamp) {
        setFactionAnger(getFactionAngerLevel() + delta, sourceCamp);
    }

    private void setFactionAnger(int level, BlockPos sourceCamp) {
        int oldAngerLevel = getFactionAngerLevel();
        int newAngerLevel = clampAnger(level);

        if (oldAngerLevel == newAngerLevel) {
            return;
        }

        factionAngerLevel = newAngerLevel;
        setDirty();
        NeoForge.EVENT_BUS.post(new AngerLevelChangedEvent(
                sourceCamp, dimension, oldAngerLevel, newAngerLevel));
    }

    /** Arms (or refreshes) this player's single pending retaliation — overwrites any existing one. */
    public void armPatrolRetaliation(UUID playerId, BlockPos campOrigin, long eligibleGameTime) {
        pendingPatrolRetaliations.put(playerId, new PendingRetaliation(campOrigin.immutable(), eligibleGameTime));
        setDirty();
    }

    /**
     * Returns pending retaliations whose delay has elapsed, without removing them — a caller that
     * can't currently deliver one (player offline/elsewhere) should leave it pending rather than
     * losing it, so this only reports readiness. Pair with {@link #clearPatrolRetaliation}.
     */
    public List<DuePatrolRetaliation> peekDuePatrolRetaliations(long gameTime) {
        List<DuePatrolRetaliation> due = new ArrayList<>();
        pendingPatrolRetaliations.forEach((playerId, retaliation) -> {
            if (gameTime >= retaliation.eligibleGameTime()) {
                due.add(new DuePatrolRetaliation(playerId, retaliation.campOrigin()));
            }
        });
        return due;
    }

    public void clearPatrolRetaliation(UUID playerId) {
        if (pendingPatrolRetaliations.remove(playerId) != null) {
            setDirty();
        }
    }

    /** True when a patrol after {@code since} hunted this player, or anyone within {@code radius} of {@code pos}. */
    public boolean wasHuntedSince(UUID playerId, BlockPos pos, double radius, long since) {
        return recentPatrols.stream().anyMatch(patrol -> patrol.time() > since
                && (patrol.playerId().equals(playerId) || patrol.pos().distSqr(pos) <= radius * radius));
    }

    /** Remembers a patrol for {@code keepTicks}, dropping the ones older than that. */
    public void recordPatrol(UUID playerId, BlockPos pos, long gameTime, long keepTicks) {
        recentPatrols.removeIf(patrol -> gameTime - patrol.time() >= keepTicks);
        recentPatrols.add(new RecentPatrol(playerId, pos.immutable(), gameTime));
        setDirty();
    }

    private record RecentPatrol(UUID playerId, BlockPos pos, long time) {
    }

    public void setNextPatrolGameTime(BlockPos campOrigin, long gameTime) {
        CampTributeData camp = camps.get(campOrigin.asLong());
        if (camp == null || camp.nextPatrolGameTime == gameTime) {
            return;
        }

        camp.nextPatrolGameTime = gameTime;
        setDirty();
    }

    private int maxAngerLevel() {
        return Config.TRIBUTE_MAX_ANGER.get();
    }

    private int clampAnger(int angerLevel) {
        return Math.clamp(angerLevel, Math.min(angerFloor(), maxAngerLevel()), maxAngerLevel());
    }

    private final class CampTributeData implements ITributeData {
        private final BlockPos campOrigin;
        private boolean chiefAlive = true;
        private long nextPatrolGameTime;
        private boolean populationManaged;
        private OutpostCaptainType captainType = OutpostCaptainType.GRUNT;
        private OutpostProfile profile = OutpostProfile.OUTPOST;
        private boolean populationInitialized;
        private long nextResidentSpawnGameTime;
        private long captainRespawnGameTime;
        private int jockeyTarget = -1;
        private boolean jockeyPopulationInitialized;
        private long nextJockeySpawnGameTime;
        private StructureKind structureKind = StructureKind.UNKNOWN;
        private BlockPos fortMapTarget;
        // The one chest chosen to receive this outpost guaranteed Fort Map: the first TRAPPED_CHEST
        // found for the loot table that carries the map reward, captured in
        // OgreOutpostStructure.afterPlace. Only used for deferred insertion; see mapRewardFulfilled.
        private BlockPos mapChestPos;
        // True once a Fort Map has been handed out for this outpost, by the normal loot roll
        // (CachedFortMapFunction) or a deferred insertion (FortMapDeliveryService). Stops a second
        // trapped chest at the same outpost (grunt_outpost.nbt has two) from producing a duplicate
        // map, and stops deferred delivery from inserting again after the reward landed.
        private boolean mapRewardFulfilled;
        private boolean hasOutpostBounds;
        private int outpostMinX;
        private int outpostMaxX;
        private int outpostMinZ;
        private int outpostMaxZ;
        private int outpostGroundY;
        private PitZone pit;

        private CampTributeData(BlockPos campOrigin) {
            this.campOrigin = campOrigin.immutable();
        }

        @Override
        public int getAngerLevel() {
            return getFactionAngerLevel();
        }

        @Override
        public void setAngerLevel(int level) {
            setFactionAnger(level, campOrigin);
        }

        @Override
        public void incrementAnger(int delta) {
            incrementFactionAnger(delta, campOrigin);
        }

        @Override
        public int getMaxAngerLevel() {
            return maxAngerLevel();
        }

        @Override
        public boolean isChiefAlive() {
            return chiefAlive;
        }

        @Override
        public void setChiefAlive(boolean alive) {
            if (chiefAlive == alive) {
                return;
            }

            chiefAlive = alive;
            setDirty();
        }
    }

    public record CampPatrolState(BlockPos campOrigin, int angerLevel, boolean chiefAlive, long nextPatrolGameTime) {
    }

    private record PendingRetaliation(BlockPos campOrigin, long eligibleGameTime) {
    }

    public record DuePatrolRetaliation(UUID playerId, BlockPos campOrigin) {
    }

    public record OutpostPopulationState(
            BlockPos campOrigin,
            OutpostCaptainType captainType,
            boolean populationInitialized,
            long nextResidentSpawnGameTime,
            long captainRespawnGameTime,
            boolean hasOutpostBounds,
            int outpostMinX,
            int outpostMaxX,
            int outpostMinZ,
            int outpostMaxZ,
            int outpostGroundY,
            OutpostProfile profile,
            @org.jetbrains.annotations.Nullable PitZone pit) {
    }

    public record DiscoverySite(BlockPos origin, StructureKind kind) {}

    public record JockeyPopulationState(BlockPos origin, StructureKind kind, int target,
                                        boolean initialized, long nextSpawnGameTime,
                                        int minX, int maxX, int minZ, int maxZ, int groundY,
                                        boolean anyBiome) {}

    public enum StructureKind {
        UNKNOWN("unknown"),
        OUTPOST("outpost"),
        FORT("fort"),
        /** Camps and the quarry: they hold tribute and a population but aren't discovery sites. */
        ENCOUNTER("encounter");

        private final String serializedName;

        StructureKind(String serializedName) {
            this.serializedName = serializedName;
        }

        static StructureKind fromSerializedName(String name) {
            for (StructureKind kind : values()) {
                if (kind.serializedName.equals(name)) {
                    return kind;
                }
            }
            return UNKNOWN;
        }
    }
}
