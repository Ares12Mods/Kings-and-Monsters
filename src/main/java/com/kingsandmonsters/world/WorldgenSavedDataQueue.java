package com.kingsandmonsters.world;

import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.entity.MudsnoutBoar;
import com.kingsandmonsters.tribute.OutpostCaptainType;
import com.kingsandmonsters.tribute.OutpostProfile;
import com.kingsandmonsters.tribute.PitZone;
import com.kingsandmonsters.tribute.TributeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Moves worldgen discoveries onto the owning server thread and deduplicates per-chunk afterPlace calls. */
public final class WorldgenSavedDataQueue {
    private static final Set<RegistrationKey> FORTS = ConcurrentHashMap.newKeySet();
    private static final Set<RegistrationKey> OUTPOSTS = ConcurrentHashMap.newKeySet();
    private static final Set<RegistrationKey> MAP_CHESTS = ConcurrentHashMap.newKeySet();
    private static final Set<RegistrationKey> TRAPPER_BOAR_FAMILIES = ConcurrentHashMap.newKeySet();
    private static final String TRAPPER_PEN_KEY = "KingsAndMonstersTrapperPen";

    private WorldgenSavedDataQueue() {
    }

    public static void registerFort(ServerLevel level, BlockPos origin, BoundingBox bounds) {
        RegistrationKey key = key(level, origin);
        if (!FORTS.add(key)) {
            return;
        }
        BoundingBox immutableBounds = new BoundingBox(
                bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
        level.getServer().execute(() -> TributeManager.registerFort(level, origin.immutable(), immutableBounds));
    }

    public static void registerOutpost(
            ServerLevel level,
            BlockPos origin,
            OutpostCaptainType captainType,
            BoundingBox bounds,
            BlockPos fortTarget) {
        RegistrationKey key = key(level, origin);
        if (!OUTPOSTS.add(key)) {
            return;
        }
        BoundingBox immutableBounds = new BoundingBox(
                bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
        level.getServer().execute(() -> {
            TributeManager.registerOutpost(level, origin.immutable(), captainType, immutableBounds);
            if (fortTarget != null) {
                TributeManager.setOutpostFortMapTarget(level, origin, fortTarget.immutable());
            }
        });
    }

    public static void registerEncounter(
            ServerLevel level,
            BlockPos origin,
            OutpostProfile profile,
            OutpostCaptainType captainType,
            BoundingBox bounds,
            int groundY,
            PitZone pit) {
        RegistrationKey key = key(level, origin);
        if (!OUTPOSTS.add(key)) {
            return;
        }
        BoundingBox immutableBounds = new BoundingBox(
                bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
        level.getServer().execute(() ->
                TributeManager.registerEncounter(
                        level, origin.immutable(), profile, captainType, immutableBounds, groundY, pit));
    }

    public static void registerMapChest(ServerLevel level, BlockPos origin, BlockPos chestPos) {
        RegistrationKey key = key(level, origin);
        if (!MAP_CHESTS.add(key)) {
            return;
        }
        level.getServer().execute(() ->
                TributeManager.setOutpostMapChestPosIfAbsent(level, origin.immutable(), chestPos.immutable()));
    }

    /** Creates the hut's penned family once, after all rotated template blocks have been placed. */
    public static void spawnTrapperBoarFamily(ServerLevel level, BlockPos origin, List<BlockPos> spawnPositions) {
        if (spawnPositions.size() < 4 || !TRAPPER_BOAR_FAMILIES.add(key(level, origin))) {
            return;
        }
        List<BlockPos> immutablePositions = spawnPositions.stream().map(BlockPos::immutable).toList();
        level.getServer().execute(() -> {
            long site = origin.asLong();
            BlockPos center = immutablePositions.getFirst();
            boolean alreadySpawned = !level.getEntitiesOfClass(MudsnoutBoar.class,
                    new AABB(center).inflate(16, 8, 16),
                    boar -> boar.getPersistentData().getLong(TRAPPER_PEN_KEY) == site).isEmpty();
            if (alreadySpawned) {
                return;
            }
            int adults = 1 + Math.floorMod(Long.hashCode(site ^ 0x5DEECE66DL), 2);
            int babies = 1 + Math.floorMod(Long.hashCode(site ^ 0x9E3779B97F4A7C15L), 2);
            int familySize = adults + babies;
            for (int index = 0; index < familySize; index++) {
                BlockPos position = immutablePositions.get(index);
                MudsnoutBoar boar = ModEntities.MUDSNOUT_BOAR.get().create(level);
                if (boar == null) {
                    return;
                }
                boar.moveTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5,
                        level.random.nextFloat() * 360.0F, 0.0F);
                boar.finalizeSpawn(level, level.getCurrentDifficultyAt(position), MobSpawnType.STRUCTURE, null);
                boar.setBaby(index >= adults);
                boar.setPersistenceRequired();
                boar.restrictTo(center, 8);
                boar.getPersistentData().putLong(TRAPPER_PEN_KEY, site);
                if (!level.noCollision(boar, boar.getBoundingBox())) {
                    boar.discard();
                    continue;
                }
                level.addFreshEntityWithPassengers(boar);
            }
        });
    }

    public static void reset() {
        FORTS.clear();
        OUTPOSTS.clear();
        MAP_CHESTS.clear();
        TRAPPER_BOAR_FAMILIES.clear();
    }

    private static RegistrationKey key(ServerLevel level, BlockPos origin) {
        return new RegistrationKey(level.getServer(), level.dimension(), origin.asLong());
    }

    private record RegistrationKey(MinecraftServer server, ResourceKey<Level> dimension, long origin) {
    }
}
