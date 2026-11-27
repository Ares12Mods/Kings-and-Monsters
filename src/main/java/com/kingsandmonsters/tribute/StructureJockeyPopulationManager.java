package com.kingsandmonsters.tribute;

import com.kingsandmonsters.entity.OgreGrunt;
import com.kingsandmonsters.entity.OgreLord;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.entity.MudsnoutBoar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.kingsandmonsters.event.ServerTickEvent;

import java.util.List;

/** Keeps the mounted archer patrols attached to generated outposts and the Ogre King's fort. */
public final class StructureJockeyPopulationManager {
    private static final String SITE_KEY = "KingsAndMonstersJockeySite";
    private static final int CHECK_INTERVAL = 40;
    private static final int ACTIVATION_RADIUS = 128;
    private static final int RESPAWN_BLOCK_RADIUS = 64;
    private static final int RESPAWN_MIN = 20 * 60 * 20;
    private static final int RESPAWN_RANGE = 10 * 60 * 20;
    private static final int RETRY_DELAY = 20 * 20;

    private StructureJockeyPopulationManager() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL != 0) return;
        for (ServerLevel level : server.getAllLevels()) tickLevel(level);
    }

    private static void tickLevel(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        long now = level.getGameTime();
        for (TributeSavedData.JockeyPopulationState site : TributeManager.getJockeyPopulationStates(level)) {
            if (!active(level, site.origin()) || !site.anyBiome() && !boarCountry(level, site.origin())) continue;
            List<MudsnoutBoar> boars = siteBoars(level, site);
            int mounted = (int) boars.stream().filter(MudsnoutBoar::hasOgreRider).count();

            if (!site.initialized()) {
                boolean complete = true;
                while (mounted < site.target()) {
                    if (!restoreOrSpawn(level, site, boars)) {
                        complete = false;
                        break;
                    }
                    mounted++;
                }
                TributeManager.updateJockeyPopulation(level, site.origin(), complete,
                        complete ? 0L : now + RETRY_DELAY);
                continue;
            }

            if (mounted >= site.target()) {
                if (site.nextSpawnGameTime() != 0L) {
                    TributeManager.updateJockeyPopulation(level, site.origin(), true, 0L);
                }
                continue;
            }
            if (site.nextSpawnGameTime() == 0L) {
                TributeManager.updateJockeyPopulation(level, site.origin(), true, nextRespawn(level, now));
            } else if (now >= site.nextSpawnGameTime() && !hasPlayerWithin(level, site.origin(), RESPAWN_BLOCK_RADIUS)) {
                boolean spawned = restoreOrSpawn(level, site, boars);
                TributeManager.updateJockeyPopulation(level, site.origin(), true,
                        spawned && mounted + 1 >= site.target() ? 0L
                                : spawned ? nextRespawn(level, now) : now + RETRY_DELAY);
            }
        }
    }

    private static boolean active(ServerLevel level, BlockPos origin) {
        if (!level.hasChunk(SectionPos.blockToSectionCoord(origin.getX()),
                SectionPos.blockToSectionCoord(origin.getZ()))) return false;
        return hasPlayerWithin(level, origin, ACTIVATION_RADIUS);
    }

    /** Riders serve plains strongholds and the Trappers' oak-forest hunting camps. */
    static boolean boarCountry(ServerLevel level, BlockPos origin) {
        var biome = level.getBiome(origin);
        return biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS)
                || biome.is(Biomes.FOREST) || biome.is(Biomes.FLOWER_FOREST);
    }

    private static boolean hasPlayerWithin(ServerLevel level, BlockPos origin, int radius) {
        double range = (double) radius * radius;
        return !level.getPlayers(player -> !player.isSpectator()
                && player.distanceToSqr(Vec3.atCenterOf(origin)) <= range).isEmpty();
    }

    private static List<MudsnoutBoar> siteBoars(ServerLevel level, TributeSavedData.JockeyPopulationState site) {
        AABB area = new AABB(site.minX() - 24, site.groundY() - 8, site.minZ() - 24,
                site.maxX() + 25, site.groundY() + 32, site.maxZ() + 25);
        long key = site.origin().asLong();
        return level.getEntitiesOfClass(MudsnoutBoar.class, area, boar -> boar.isAlive()
                && boar.getPersistentData().getLong(SITE_KEY) == key);
    }

    private static boolean restoreOrSpawn(ServerLevel level, TributeSavedData.JockeyPopulationState site,
                                          List<MudsnoutBoar> existing) {
        for (MudsnoutBoar boar : existing) {
            if (!boar.hasOgreRider()) {
                boar.addJockey(level, level.getCurrentDifficultyAt(boar.blockPosition()));
                return boar.hasOgreRider();
            }
        }
        // Outpost riders patrol the ground outside the walls; the fort, quarry and camps have room
        // inside, so theirs start there.
        boolean outside = site.kind() == TributeSavedData.StructureKind.OUTPOST;
        BlockPos center = new BlockPos((site.minX() + site.maxX()) / 2, site.groundY(), (site.minZ() + site.maxZ()) / 2);
        int halfExtent = Math.max(site.maxX() - site.minX(), site.maxZ() - site.minZ()) / 2;
        for (int attempt = 0; attempt < 32; attempt++) {
            BlockPos pos = outside ? outsideSpot(level, site)
                    : site.kind() == TributeSavedData.StructureKind.FORT ? fortSpot(level, site)
                    : insideSpot(level, site);
            if (pos == null) continue;
            BlockState floor = level.getBlockState(pos.below());
            if (!floor.isFaceSturdy(level, pos.below(), Direction.UP)
                    || !level.getFluidState(pos).isEmpty()) continue;
            MudsnoutBoar boar = ModEntities.MUDSNOUT_BOAR.get().spawn(level, pos, MobSpawnType.STRUCTURE);
            if (boar == null) continue;
            if (!level.noCollision(boar)) {
                boar.discard();
                continue;
            }
            boar.setPersistenceRequired();
            // Outpost riders circle the ground around the walls; the rest keep to the site itself.
            boar.restrictTo(center, outside ? halfExtent + 28 : halfExtent);
            boar.getPersistentData().putLong(SITE_KEY, site.origin().asLong());
            boar.addJockey(level, level.getCurrentDifficultyAt(pos));
            if (boar.hasOgreRider()) return true;
            boar.discard();
        }
        return false;
    }

    /** A random spot in the band five to fourteen blocks outside the walls, on ground near the site's level. */
    private static BlockPos outsideSpot(ServerLevel level, TributeSavedData.JockeyPopulationState site) {
        int x = randomBetween(level, site.minX() - 14, site.maxX() + 14);
        int z = randomBetween(level, site.minZ() - 14, site.maxZ() + 14);
        boolean nearWalls = x >= site.minX() - 5 && x <= site.maxX() + 5 && z >= site.minZ() - 5 && z <= site.maxZ() + 5;
        if (nearWalls || !level.hasChunk(x >> 4, z >> 4)) return null;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z));
        return Math.abs(top.getY() - site.groundY()) <= 10 ? top : null;
    }

    private static final String FORT_RESIDENT_TAG_PREFIX = "KingsAndMonstersFortResident_";

    /**
     * A spot inside the fort's walls. The fort's bounding box is bigger than its walls, so a random point in it
     * could land outside, where a rider can neither get in nor be got at. The King and the fort's residents are
     * placed inside the compound by hand, so riders start a few blocks from one of them, on the same level.
     */
    private static BlockPos fortSpot(ServerLevel level, TributeSavedData.JockeyPopulationState site) {
        AABB area = new AABB(site.minX(), site.groundY() - 8, site.minZ(), site.maxX() + 1, site.groundY() + 32,
                site.maxZ() + 1);
        List<OgreGrunt> anchors = level.getEntitiesOfClass(
                OgreGrunt.class, area,
                ogre -> ogre.isAlive() && ogre.getVehicle() == null
                        && (ogre instanceof OgreLord
                        || ogre.getTags().stream().anyMatch(tag -> tag.startsWith(FORT_RESIDENT_TAG_PREFIX))));
        if (anchors.isEmpty()) {
            return null;
        }
        BlockPos anchor = anchors.get(level.random.nextInt(anchors.size())).blockPosition();
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        double distance = 3.0 + level.random.nextDouble() * 4.0;
        int x = anchor.getX() + (int) Math.round(Math.cos(angle) * distance);
        int z = anchor.getZ() + (int) Math.round(Math.sin(angle) * distance);
        if (x < site.minX() || x > site.maxX() || z < site.minZ() || z > site.maxZ()) {
            return null;
        }
        for (int y = anchor.getY() + 2; y >= anchor.getY() - 2; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    private static BlockPos insideSpot(ServerLevel level, TributeSavedData.JockeyPopulationState site) {
        int x = randomBetween(level, site.minX() + 3, site.maxX() - 3);
        int z = randomBetween(level, site.minZ() + 3, site.maxZ() - 3);
        for (int y = site.groundY(); y <= site.groundY() + 12; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    private static int randomBetween(ServerLevel level, int min, int max) {
        if (max <= min) return (min + max) / 2;
        return min + level.random.nextInt(max - min + 1);
    }

    private static long nextRespawn(ServerLevel level, long now) {
        return now + RESPAWN_MIN + level.random.nextInt(RESPAWN_RANGE + 1);
    }
}
