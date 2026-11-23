package com.kingsandmonsters.tribute;

import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.entity.PlagueOgre;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Rare infected wanderers at the edge of inhabited Ogre outposts. */
public final class OutpostPlagueSpawner {
    private static final int CHECK_INTERVAL_TICKS = 60 * 20;
    private static final int CHANCE_DENOMINATOR = 20;
    /** Plains outposts check at roughly a quarter of the swamp rate, with a hard local cap. */
    private static final int WANDERER_CHANCE_DENOMINATOR = 90;
    private static final int WANDERER_MAX_NEARBY = 2;
    private static final int WANDERER_COUNT_RADIUS = 64;
    private static final int MIN_EDGE_DISTANCE = 10;
    private static final int MAX_EDGE_DISTANCE = 24;
    private static final int SPAWN_ATTEMPTS = 12;
    private static final double PLAYER_ACTIVATION_DISTANCE_SQR = 128.0 * 128.0;
    private static final double PLAYER_MIN_SPAWN_DISTANCE_SQR = 24.0 * 24.0;

    private OutpostPlagueSpawner() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (level.getDifficulty() == Difficulty.PEACEFUL) continue;

            for (TributeSavedData.OutpostPopulationState outpost
                    : TributeManager.getOutpostPopulationStates(level)) {
                if (!outpost.hasOutpostBounds() || !hasNearbyPlayer(level, outpost.campOrigin())) continue;
                boolean swamp = PlagueOgre.isSwampBiome(level, outpost.campOrigin());
                if (level.random.nextInt(swamp ? CHANCE_DENOMINATOR : WANDERER_CHANCE_DENOMINATOR) != 0) continue;
                if (swamp) {
                    trySpawnAtEdge(level, outpost, false);
                } else {
                    // Strays from the swamp: usually one, sometimes a pair, never past the local cap.
                    int room = WANDERER_MAX_NEARBY - countNearbyPlagueOgres(level, outpost);
                    int wanderers = Math.min(room, level.random.nextInt(3) == 0 ? 2 : 1);
                    for (int i = 0; i < wanderers; i++) trySpawnAtEdge(level, outpost, true);
                }
            }
        }
    }

    private static boolean hasNearbyPlayer(ServerLevel level, BlockPos origin) {
        Vec3 center = Vec3.atCenterOf(origin);
        return !level.getPlayers(player -> !player.isSpectator() && !player.isCreative()
                && player.distanceToSqr(center) <= PLAYER_ACTIVATION_DISTANCE_SQR).isEmpty();
    }

    private static int countNearbyPlagueOgres(ServerLevel level, TributeSavedData.OutpostPopulationState outpost) {
        AABB area = new AABB(outpost.outpostMinX(), outpost.outpostGroundY() - 16, outpost.outpostMinZ(),
                outpost.outpostMaxX() + 1, outpost.outpostGroundY() + 32, outpost.outpostMaxZ() + 1)
                .inflate(WANDERER_COUNT_RADIUS, 0, WANDERER_COUNT_RADIUS);
        return level.getEntitiesOfClass(PlagueOgre.class, area, Entity::isAlive).size();
    }

    private static boolean canWanderTo(ServerLevel level, BlockPos pos) {
        // Same footing rules as swamp spawns, minus the swamp-biome requirement.
        return level.getDifficulty() != Difficulty.PEACEFUL
                && Mob.checkMobSpawnRules(ModEntities.PLAGUE_OGRE.get(), level, MobSpawnType.NATURAL, pos, level.random);
    }

    private static void trySpawnAtEdge(ServerLevel level, TributeSavedData.OutpostPopulationState outpost,
                                       boolean wanderer) {
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            int side = level.random.nextInt(4);
            int distance = level.random.nextIntBetweenInclusive(MIN_EDGE_DISTANCE, MAX_EDGE_DISTANCE);
            int x = side < 2
                    ? (side == 0 ? outpost.outpostMinX() - distance : outpost.outpostMaxX() + distance)
                    : level.random.nextIntBetweenInclusive(outpost.outpostMinX(), outpost.outpostMaxX());
            int z = side >= 2
                    ? (side == 2 ? outpost.outpostMinZ() - distance : outpost.outpostMaxZ() + distance)
                    : level.random.nextIntBetweenInclusive(outpost.outpostMinZ(), outpost.outpostMaxZ());
            if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) continue;

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - outpost.outpostGroundY()) > 10) continue;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getWorldBorder().isWithinBounds(pos)
                    || !level.getFluidState(pos).isEmpty()
                    || !level.getFluidState(pos.below()).isEmpty()
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    || !level.noCollision(ModEntities.PLAGUE_OGRE.get().getSpawnAABB(x + 0.5, y, z + 0.5))
                    || !(wanderer ? canWanderTo(level, pos)
                    : PlagueOgre.checkPlagueOgreSpawnRules(ModEntities.PLAGUE_OGRE.get(), level,
                    MobSpawnType.NATURAL, pos, level.random))) continue;

            Vec3 spawnCenter = Vec3.atBottomCenterOf(pos);
            if (level.getPlayers(player -> !player.isSpectator()
                    && player.distanceToSqr(spawnCenter) < PLAYER_MIN_SPAWN_DISTANCE_SQR).size() > 0) continue;
            ModEntities.PLAGUE_OGRE.get().spawn(level, pos, MobSpawnType.NATURAL);
            return;
        }
    }
}
