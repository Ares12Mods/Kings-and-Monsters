package com.kingsandmonsters.entity;

import com.kingsandmonsters.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Keeps swamp water populated with Bog Poles near players. Vanilla's natural spawning picks a
 * random height anywhere in the column, so a swamp's two-block-deep water almost never gets
 * chosen. This looks for real water instead: every few seconds it picks a spot in swamp water a
 * little way from each player and, if there are few poles nearby, spawns a small shoal there.
 */
public final class BogPoleSwampSpawner {
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final float SPAWN_CHANCE = 0.5F;
    private static final double MIN_DISTANCE = 24.0;
    private static final double MAX_DISTANCE = 48.0;
    private static final int NEARBY_CAP = 6;
    private static final double NEARBY_RADIUS = 56.0;

    private BogPoleSwampSpawner() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && level.random.nextFloat() < SPAWN_CHANCE) {
                    trySpawnShoal(level, player);
                }
            }
        }
    }

    private static void trySpawnShoal(ServerLevel level, ServerPlayer player) {
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        double distance = MIN_DISTANCE + level.random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
        if (!level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight(), z))) {
            return;
        }

        BlockPos origin = waterSpot(level, x, z);
        if (origin == null) {
            return;
        }
        AABB nearby = new AABB(origin).inflate(NEARBY_RADIUS);
        if (level.getEntitiesOfClass(BogPole.class, nearby).size() >= NEARBY_CAP) {
            return;
        }

        int shoal = 1 + level.random.nextInt(2);
        for (int i = 0; i < shoal; i++) {
            BlockPos pos = i == 0 ? origin : waterSpot(level, x + level.random.nextInt(7) - 3,
                    z + level.random.nextInt(7) - 3);
            if (pos != null) {
                ModEntities.BOG_POLE.get().spawn(level, pos, MobSpawnType.NATURAL);
            }
        }
    }

    /** A spot just under the surface of swamp water at this column, or null if it isn't suitable. */
    private static BlockPos waterSpot(ServerLevel level, int x, int z) {
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        BlockPos pos = new BlockPos(x, surface - 2, z);
        boolean swamp = level.getBiome(pos).is(Biomes.SWAMP) || level.getBiome(pos).is(Biomes.MANGROVE_SWAMP);
        if (!swamp
                || !level.getFluidState(pos).is(FluidTags.WATER)
                || !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            return null;
        }
        return pos;
    }
}
