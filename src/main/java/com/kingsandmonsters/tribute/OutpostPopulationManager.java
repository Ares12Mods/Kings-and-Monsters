package com.kingsandmonsters.tribute;

import org.jetbrains.annotations.Nullable;
import com.kingsandmonsters.entity.OgreGrunt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Keeps each registered camp staffed according to its {@link OutpostProfile}: the classic outposts
 * hold 3 Grunts, 2 Archers and a captain, while supply camps, the moving camp, the abandoned camp
 * and the quarry each have their own crew.
 */
public final class OutpostPopulationManager {
    private static final int CHECK_INTERVAL_TICKS = 40;
    private static final int INITIAL_RETRY_TICKS = 20 * 20;
    private static final int ACTIVATION_RADIUS = 128;
    private static final int COUNT_RADIUS = 48;
    private static final int FOOTPRINT_INSET = 4;
    private static final int SPAWN_HEIGHT_BAND = 8;
    private static final int CAPTAIN_CENTER_SEARCH_RADIUS = 5;
    /** How far a spawn spot looks each way for the fence of a pen. */
    private static final int PEN_MAX_CELLS = 160;
    private static final int SPAWN_ATTEMPTS = 24;

    private OutpostPopulationManager() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;

        for (ServerLevel level : server.getAllLevels()) {
            tickLevel(level);
        }
    }

    private static void tickLevel(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;

        for (TributeSavedData.OutpostPopulationState outpost
                : TributeManager.getOutpostPopulationStates(level)) {
            BlockPos origin = outpost.campOrigin();
            if (!hasNearbyPlayer(level, origin)
                    || !level.hasChunk(SectionPos.blockToSectionCoord(origin.getX()),
                    SectionPos.blockToSectionCoord(origin.getZ()))) continue;

            Population population = countPopulation(level, origin);
            long gameTime = level.getGameTime();

            if (!outpost.populationInitialized()) {
                if (gameTime < outpost.nextResidentSpawnGameTime()) continue;
                initializePopulation(level, outpost, population, gameTime);
                continue;
            }

            maintainResidents(level, outpost, population, gameTime);
            maintainCaptain(level, outpost, population, gameTime);
        }
    }

    private static boolean hasNearbyPlayer(ServerLevel level, BlockPos origin) {
        double radiusSqr = (double) ACTIVATION_RADIUS * ACTIVATION_RADIUS;
        return level.getPlayers(player -> !player.isSpectator()
                && player.distanceToSqr(Vec3.atCenterOf(origin)) <= radiusSqr).size() > 0;
    }

    private static Population countPopulation(ServerLevel level, BlockPos origin) {
        AABB bounds = new AABB(origin).inflate(COUNT_RADIUS, 24, COUNT_RADIUS);
        Map<EntityType<?>, Integer> residents = new HashMap<>();
        int captains = 0;

        for (OgreGrunt ogre : level.getEntitiesOfClass(
                OgreGrunt.class, bounds, ogre -> ogre.isAlive() && ogre.isOutpostResidentAt(origin))) {
            if (ogre.isOutpostCaptain()) {
                captains++;
            } else {
                residents.merge(ogre.getType(), 1, Integer::sum);
            }
        }
        return new Population(residents, captains);
    }

    private static void initializePopulation(
            ServerLevel level,
            TributeSavedData.OutpostPopulationState outpost,
            Population population,
            long gameTime) {
        OutpostProfile profile = outpost.profile();
        boolean crewComplete = true;
        for (OutpostProfile.Slot slot : profile.slots()) {
            EntityType<? extends OgreGrunt> type = slot.type().get();
            int target = profile.residentTarget(slot, outpost.campOrigin());
            int have = population.count(type);
            while (have < target && spawnResident(level, outpost, type, false)) {
                have++;
            }
            crewComplete &= have >= target;
        }

        int captains = population.captains();
        if (profile.hasCaptain() && captains < 1
                && spawnResident(level, outpost, outpost.captainType().entityType(), true)) {
            captains++;
        }

        if (crewComplete && (!profile.hasCaptain() || captains >= 1)) {
            TributeManager.markPopulationInitialized(
                    level, outpost.campOrigin(), nextResidentRespawnTime(level, profile, gameTime));
            if (profile.hasCaptain()) {
                TributeManager.markOutpostCaptainAlive(level, outpost.campOrigin());
            }
        } else {
            TributeManager.setNextResidentSpawnGameTime(
                    level, outpost.campOrigin(), gameTime + INITIAL_RETRY_TICKS);
        }
    }

    private static void maintainResidents(
            ServerLevel level,
            TributeSavedData.OutpostPopulationState outpost,
            Population population,
            long gameTime) {
        EntityType<? extends OgreGrunt> missingType = null;
        for (OutpostProfile.Slot slot : outpost.profile().slots()) {
            if (population.count(slot.type().get())
                    < outpost.profile().residentTarget(slot, outpost.campOrigin())) {
                missingType = slot.type().get();
                break;
            }
        }
        if (missingType == null || gameTime < outpost.nextResidentSpawnGameTime()) return;

        if (spawnResident(level, outpost, missingType, false)) {
            TributeManager.setNextResidentSpawnGameTime(
                    level, outpost.campOrigin(), nextResidentRespawnTime(level, outpost.profile(), gameTime));
        } else {
            TributeManager.setNextResidentSpawnGameTime(
                    level, outpost.campOrigin(), gameTime + INITIAL_RETRY_TICKS);
        }
    }

    private static void maintainCaptain(
            ServerLevel level,
            TributeSavedData.OutpostPopulationState outpost,
            Population population,
            long gameTime) {
        if (!outpost.profile().hasCaptain()) return;

        if (population.captains() > 0) {
            if (outpost.captainRespawnGameTime() > 0L) {
                TributeManager.markOutpostCaptainAlive(level, outpost.campOrigin());
            }
            return;
        }

        if (outpost.captainRespawnGameTime() <= 0L) {
            TributeManager.markOutpostCaptainKilled(
                    level, outpost.campOrigin(), gameTime + outpost.profile().captainRespawnTicks());
            return;
        }
        if (gameTime < outpost.captainRespawnGameTime()) return;

        if (spawnResident(level, outpost, outpost.captainType().entityType(), true)) {
            TributeManager.markOutpostCaptainAlive(level, outpost.campOrigin());
        }
    }

    private static long nextResidentRespawnTime(ServerLevel level, OutpostProfile profile, long gameTime) {
        int range = profile.maxRespawnTicks() - profile.minRespawnTicks();
        return gameTime + profile.minRespawnTicks() + level.random.nextInt(range + 1);
    }

    private static OutpostProfile profileOf(ServerLevel level, BlockPos campOrigin) {
        for (TributeSavedData.OutpostPopulationState state : TributeManager.getOutpostPopulationStates(level)) {
            if (state.campOrigin().equals(campOrigin)) {
                return state.profile();
            }
        }
        return OutpostProfile.OUTPOST;
    }

    /** How long this camp goes without a captain after losing one. */
    public static int captainRespawnTicks(ServerLevel level, BlockPos campOrigin) {
        return profileOf(level, campOrigin).captainRespawnTicks();
    }

    public static void scheduleResidentReplacement(ServerLevel level, BlockPos campOrigin) {
        TributeManager.setNextResidentSpawnGameTime(
                level, campOrigin,
                nextResidentRespawnTime(level, profileOf(level, campOrigin), level.getGameTime()));
    }

    private static boolean spawnResident(
            ServerLevel level,
            TributeSavedData.OutpostPopulationState outpost,
            EntityType<? extends OgreGrunt> entityType,
            boolean captain) {
        BlockPos origin = outpost.campOrigin();
        SpawnArea area = spawnArea(level, outpost);
        // A camp with a pit has its middle open, so even the captain spawns in the ring around it.
        Optional<BlockPos> spawnPos = captain && outpost.pit() == null
                ? findCaptainSpawnPos(level, origin, area, entityType)
                : findSpawnPos(level, area, entityType);
        if (spawnPos.isEmpty()) return false;

        OgreGrunt ogre = entityType.spawn(level, spawnPos.get(), MobSpawnType.STRUCTURE);
        if (ogre == null) return false;
        ogre.assignOutpostResident(origin, captain);
        if (outpost.pit() != null) {
            // Stay out of the hole: pathfinding treats everything inside the avoid radius as solid.
            ogre.setNoGoZone(outpost.pit().x(), outpost.pit().z(), outpost.pit().avoidRadius() - 1);
        }
        return true;
    }

    /**
     * Captains are the visual anchor of an outpost, so they spawn at its X/Z
     * center instead of sharing the residents' 6-24 block random ring. If the
     * exact center is obstructed, search only the nearest five blocks and pick
     * the first safe position; this keeps the captain inside the courtyard.
     */
    private static Optional<BlockPos> findCaptainSpawnPos(
            ServerLevel level,
            BlockPos origin,
            SpawnArea area,
            EntityType<? extends OgreGrunt> entityType) {
        for (int radius = 0; radius <= CAPTAIN_CENTER_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    Optional<BlockPos> candidate = safeSurfaceSpawn(
                            level, origin.getX() + dx, origin.getZ() + dz, area, entityType);
                    if (candidate.isPresent()) {
                        return candidate;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> findSpawnPos(
            ServerLevel level,
            SpawnArea area,
            EntityType<? extends OgreGrunt> entityType) {
        RandomSource random = level.random;
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            int x = random.nextIntBetweenInclusive(area.minX(), area.maxX());
            int z = random.nextIntBetweenInclusive(area.minZ(), area.maxZ());
            Optional<BlockPos> candidate = safeSurfaceSpawn(level, x, z, area, entityType);
            if (candidate.isPresent()) return candidate;
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> safeSurfaceSpawn(
            ServerLevel level,
            int x,
            int z,
            SpawnArea area,
            EntityType<? extends OgreGrunt> entityType) {
        if (!area.contains(x, z)) return Optional.empty();
        if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
            return Optional.empty();
        }

        for (int y = area.minY(); y <= area.maxY(); y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (isSafeGroundSpawn(level, pos, entityType) && hasWalkableExit(level, pos, area, entityType)) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    private static boolean isSafeGroundSpawn(
            ServerLevel level, BlockPos pos, EntityType<? extends OgreGrunt> entityType) {
        BlockState floor = level.getBlockState(pos.below());
        if (!level.getWorldBorder().isWithinBounds(pos)
                || !level.getFluidState(pos).isEmpty()
                || isRejectedFloor(floor)
                || isFencedIn(level, pos)
                || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                || !floor.isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }

        AABB spawnBox = entityType.getSpawnAABB(
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return level.noCollision(spawnBox);
    }

    private static boolean hasWalkableExit(
            ServerLevel level,
            BlockPos pos,
            SpawnArea area,
            EntityType<? extends OgreGrunt> entityType) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = pos.relative(direction);
            if (area.contains(adjacent.getX(), adjacent.getZ())
                    && isSafeGroundSpawn(level, adjacent, entityType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * An animal pen: a walkable area, give or take a one-block step, that is shut in on all sides by fences, walls,
     * gates or raised ground. The fill gives up after {@code PEN_MAX_CELLS} cells, because open
     * ground just keeps going, so only a small closed area counts and no resident spawns in it.
     */
    private static boolean isFencedIn(ServerLevel level, BlockPos pos) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(pos);
        queue.add(pos);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                // Ground is rarely level: a one-block step up or down is still the same open ground.
                BlockPos side = current.relative(direction);
                BlockPos next = null;
                for (int step : new int[]{0, 1, -1}) {
                    BlockPos candidate = side.above(step);
                    if (isOpenFloor(level, candidate)) {
                        next = candidate;
                        break;
                    }
                }
                if (next == null || seen.contains(next)) {
                    continue;
                }
                if (seen.size() >= PEN_MAX_CELLS) {
                    return false;
                }
                seen.add(next);
                queue.add(next);
            }
        }
        return true;
    }

    private static boolean isOpenFloor(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static boolean isRejectedFloor(BlockState floor) {
        return floor.is(Blocks.POINTED_DRIPSTONE)
                || floor.is(BlockTags.FENCES)
                || floor.is(BlockTags.WALLS)
                || floor.is(BlockTags.LEAVES)
                || floor.is(BlockTags.LOGS)
                || floor.getBlock() instanceof FenceGateBlock
                || floor.is(Blocks.CAMPFIRE)
                || floor.is(Blocks.SOUL_CAMPFIRE)
                || floor.is(Blocks.MAGMA_BLOCK)
                || floor.is(Blocks.CACTUS)
                || floor.is(Blocks.SWEET_BERRY_BUSH)
                || floor.getBlock() instanceof BaseFireBlock;
    }

    private static SpawnArea spawnArea(
            ServerLevel level, TributeSavedData.OutpostPopulationState outpost) {
        if (outpost.hasOutpostBounds()) {
            int minX = outpost.outpostMinX() + FOOTPRINT_INSET;
            int maxX = outpost.outpostMaxX() - FOOTPRINT_INSET;
            int minZ = outpost.outpostMinZ() + FOOTPRINT_INSET;
            int maxZ = outpost.outpostMaxZ() - FOOTPRINT_INSET;
            return new SpawnArea(minX, maxX, minZ, maxZ,
                    outpost.outpostGroundY(), outpost.outpostGroundY() + SPAWN_HEIGHT_BAND, outpost.pit());
        }

        // Migration fallback for outposts saved before footprint bounds existed.
        // A low percentile ignores roofs while keeping the search finite and local.
        BlockPos origin = outpost.campOrigin();
        int minX = origin.getX() - 20;
        int maxX = origin.getX() + 20;
        int minZ = origin.getZ() - 20;
        int maxZ = origin.getZ() + 20;
        int[] samples = new int[25];
        int index = 0;
        for (int dx = -16; dx <= 16; dx += 8) {
            for (int dz = -16; dz <= 16; dz += 8) {
                samples[index++] = level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        origin.getX() + dx,
                        origin.getZ() + dz);
            }
        }
        Arrays.sort(samples);
        int groundY = samples[samples.length / 4] - 2;
        return new SpawnArea(minX, maxX, minZ, maxZ, groundY, groundY + SPAWN_HEIGHT_BAND, null);
    }

    /** {@code pit}, when set, narrows the area to the ring inside the walls and clear of the hole. */
    private record SpawnArea(int minX, int maxX, int minZ, int maxZ, int minY, int maxY,
                             @Nullable PitZone pit) {
        private boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ
                    && (pit == null || pit.isSpawnRing(x, z));
        }
    }

    private record Population(Map<EntityType<?>, Integer> residents, int captains) {
        private int count(EntityType<?> type) {
            return residents.getOrDefault(type, 0);
        }
    }
}
