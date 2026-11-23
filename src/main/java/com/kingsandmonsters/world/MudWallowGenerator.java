package com.kingsandmonsters.world;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.entity.MudsnoutBoar;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Places the authored Mudsnout wallow and settles it naturally into plains terrain. */
public final class MudWallowGenerator {
    private static final ResourceLocation[] TEMPLATES = {
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "mud_wallow/boar_wallow"),
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "mud_wallow/boar_wallow_2")
    };
    /** Half the template's footprint; the structure's bounding box adds the terrain blend to it. */
    public static final int HALF_SIZE = 15;
    public static final int BLEND_WIDTH = 8;
    public static final int MAX_RELIEF = 3;
    /** Only the central basin may use template air to excavate terrain. */
    private static final int BASIN_CARVE_RADIUS = 10;
    /**
     * Each template's grass and path layer, the layer that has to line up with the ground. The second
     * template starts with an extra foundation layer, so its surface is one layer higher up.
     */
    private static final int[] SURFACE_LAYERS = {3, 4};
    private static final java.util.Map<Integer, boolean[][]> COLUMN_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private MudWallowGenerator() {
    }

    /**
     * Builds the wallow around center. The structure calls this for the one chunk that holds the
     * centre, so the 31-block footprint stays within that chunk and its neighbours.
     */
    public static int variantCount() {
        return TEMPLATES.length;
    }

    public static boolean placeChunk(WorldGenLevel level, RandomSource random, BlockPos center,
                                     Rotation rotation, int variant, BoundingBox chunkBox) {
        int templateIndex = Mth.clamp(variant, 0, TEMPLATES.length - 1);
        StructureTemplate template = level.getLevel().getStructureManager().get(TEMPLATES[templateIndex]).orElse(null);
        if (template == null || template.getSize().getX() != 31 || template.getSize().getZ() != 31) {
            return false;
        }

        int groundY = center.getY();
        blendTerrain(level, center, groundY, chunkBox);
        BlockPos templateOrigin = new BlockPos(center.getX() - HALF_SIZE, groundY - SURFACE_LAYERS[templateIndex],
                center.getZ() - HALF_SIZE);
        int carveMinX = Math.max(chunkBox.minX(), center.getX() - BASIN_CARVE_RADIUS);
        int carveMaxX = Math.min(chunkBox.maxX(), center.getX() + BASIN_CARVE_RADIUS);
        int carveMinZ = Math.max(chunkBox.minZ(), center.getZ() - BASIN_CARVE_RADIUS);
        int carveMaxZ = Math.min(chunkBox.maxZ(), center.getZ() + BASIN_CARVE_RADIUS);

        // Structure saves contain air throughout their rectangular bounds. Pasting all lower-half
        // air excavated a 31x31 quarry. Only the central basin is allowed to use authored air; every
        // other column places solids while preserving the naturally graded ground underneath.
        if (carveMinX <= carveMaxX && carveMinZ <= carveMaxZ) {
            BoundingBox basin = new BoundingBox(carveMinX, level.getMinBuildHeight(), carveMinZ,
                    carveMaxX, groundY, carveMaxZ);
            StructurePlaceSettings carving = new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(rotation)
                    .setRotationPivot(new BlockPos(HALF_SIZE, 0, HALF_SIZE))
                    .setIgnoreEntities(true)
                    .setBoundingBox(basin)
                    .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
            template.placeInWorld(level, templateOrigin, templateOrigin, carving, random, 2);
        }

        BoundingBox solidsBox = new BoundingBox(chunkBox.minX(), level.getMinBuildHeight(), chunkBox.minZ(),
                chunkBox.maxX(), level.getMaxBuildHeight() - 1, chunkBox.maxZ());
        StructurePlaceSettings solids = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(rotation)
                .setRotationPivot(new BlockPos(HALF_SIZE, 0, HALF_SIZE))
                .setIgnoreEntities(true)
                .setBoundingBox(solidsBox)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        template.placeInWorld(level, templateOrigin, templateOrigin, solids, random, 2);

        if (templateIndex == 1) {
            uncoverSecondVariantBasin(level, template, templateIndex, templateOrigin, rotation, groundY, chunkBox);
        }

        if (chunkBox.isInside(center)) {
            spawnWallowFamily(level, random, center, groundY);
        }
        return true;
    }

    /**
     * The second template was authored without saved air blocks. Its mud and water floor is one
     * block below the aligned surface, so terrain grading otherwise caps the whole basin with grass.
     * This clears only that grading cap, and only in columns where the template itself has a floor
     * block but nothing at ground level. Every block the template actually places stays exactly as
     * authored: the old version tested the blocks in the world, so it also deleted the template's
     * own grass, dirt and path rim wherever it sat above mud or podzol.
     */
    private static void uncoverSecondVariantBasin(WorldGenLevel level, StructureTemplate template,
                                                   int templateIndex, BlockPos templateOrigin,
                                                   Rotation rotation, int groundY, BoundingBox chunkBox) {
        boolean[][] surface = columnsWithBlocks(template, templateIndex, SURFACE_LAYERS[templateIndex]);
        int[][] top = highestBlockBelowSurface(template, templateIndex, SURFACE_LAYERS[templateIndex]);
        BlockPos pivot = new BlockPos(HALF_SIZE, 0, HALF_SIZE);
        for (int lx = 0; lx < 31; lx++) {
            for (int lz = 0; lz < 31; lz++) {
                if (surface[lx][lz] || top[lx][lz] < 0) {
                    continue;
                }
                // Near the spire the template has no grass or dirt at all, so the graded terrain must
                // not be left standing there. Outside the basin, columns without a floor stay natural.
                boolean inBasin = Math.abs(lx - HALF_SIZE) <= BASIN_CARVE_RADIUS
                        && Math.abs(lz - HALF_SIZE) <= BASIN_CARVE_RADIUS;
                if (!inBasin && top[lx][lz] < SURFACE_LAYERS[templateIndex] - 1) {
                    continue;
                }
                BlockPos world = StructureTemplate.transform(new BlockPos(lx, 0, lz), Mirror.NONE, rotation, pivot)
                        .offset(templateOrigin.getX(), 0, templateOrigin.getZ());
                if (world.getX() < chunkBox.minX() || world.getX() > chunkBox.maxX()
                        || world.getZ() < chunkBox.minZ() || world.getZ() > chunkBox.maxZ()) {
                    continue;
                }
                // Clear the grading cap, and any graded dirt between it and the template's own top block.
                int lowest = templateOrigin.getY() + top[lx][lz] + 1;
                for (int y = groundY; y >= lowest; y--) {
                    BlockPos capPos = new BlockPos(world.getX(), y, world.getZ());
                    BlockState cap = level.getBlockState(capPos);
                    if (cap.is(Blocks.GRASS_BLOCK) || cap.is(Blocks.DIRT) || cap.is(Blocks.DIRT_PATH)
                            || cap.is(Blocks.COARSE_DIRT) || cap.is(Blocks.PODZOL)) {
                        level.setBlock(capPos, Blocks.AIR.defaultBlockState(), 2);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    /** Highest layer below the surface layer holding a real block in each column, or -1. Cached. */
    private static int[][] highestBlockBelowSurface(StructureTemplate template, int templateIndex, int surfaceLayer) {
        int[][] result = new int[31][31];
        for (int[] row : result) {
            java.util.Arrays.fill(row, -1);
        }
        for (int layer = 0; layer < surfaceLayer; layer++) {
            boolean[][] columns = columnsWithBlocks(template, templateIndex, layer);
            for (int x = 0; x < 31; x++) {
                for (int z = 0; z < 31; z++) {
                    if (columns[x][z]) {
                        result[x][z] = layer;
                    }
                }
            }
        }
        return result;
    }

    /** Which template columns (x, z) hold a real, non-air block on the given layer. Cached per layer. */
    private static boolean[][] columnsWithBlocks(StructureTemplate template, int templateIndex, int layer) {
        return COLUMN_CACHE.computeIfAbsent(templateIndex * 100 + layer, key -> {
            boolean[][] columns = new boolean[31][31];
            CompoundTag tag = template.save(new CompoundTag());
            ListTag palette = tag.getList("palette", Tag.TAG_COMPOUND);
            ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < blocks.size(); i++) {
                CompoundTag block = blocks.getCompound(i);
                ListTag pos = block.getList("pos", Tag.TAG_INT);
                if (pos.getInt(1) != layer) {
                    continue;
                }
                String name = palette.getCompound(block.getInt("state")).getString("Name");
                if (name.equals("minecraft:air") || name.equals("minecraft:cave_air")
                        || name.equals("minecraft:void_air") || name.equals("minecraft:structure_void")) {
                    continue;
                }
                int x = pos.getInt(0);
                int z = pos.getInt(2);
                if (x >= 0 && x < 31 && z >= 0 && z < 31) {
                    columns[x][z] = true;
                }
            }
            return columns;
        });
    }

    private static void blendTerrain(WorldGenLevel level, BlockPos center, int groundY, BoundingBox chunkBox) {
        int reach = HALF_SIZE + BLEND_WIDTH;
        int minX = Math.max(center.getX() - reach, chunkBox.minX());
        int maxX = Math.min(center.getX() + reach, chunkBox.maxX());
        int minZ = Math.max(center.getZ() - reach, chunkBox.minZ());
        int maxZ = Math.min(center.getZ() + reach, chunkBox.maxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int dx = x - center.getX();
                int dz = z - center.getZ();
                // Distance from the edge of the square template, rather than from its centre. The old
                // circular mask did not reach the 31x31 template's corners and left square trenches.
                double outsideX = Math.max(0, Math.abs(dx) - HALF_SIZE);
                double outsideZ = Math.max(0, Math.abs(dz) - HALF_SIZE);
                double distance = Math.hypot(outsideX, outsideZ);
                if (distance > BLEND_WIDTH) {
                    continue;
                }
                BlockPos natural = groundAt(level, x, z);
                double blend = 1.0;
                if (distance > 0.0) {
                    double progress = 1.0 - distance / BLEND_WIDTH;
                    // Smoothstep removes the visible angle where the flattened wallow meets the plains.
                    blend = progress * progress * (3.0 - 2.0 * progress);
                }
                int targetY = Mth.floor(Mth.lerp(blend, natural.getY(), groundY) + 0.5);
                settleColumn(level, natural, targetY);
            }
        }
    }

    private static void settleColumn(WorldGenLevel level, BlockPos natural, int targetY) {
        int x = natural.getX();
        int z = natural.getZ();
        // A tree standing here keeps its ground: lowering or raising the soil under a trunk would
        // leave the whole tree hanging in the air or buried.
        if (level.getBlockState(natural.above()).is(BlockTags.LOGS)) {
            return;
        }
        if (natural.getY() < targetY) {
            for (int y = natural.getY() + 1; y <= targetY; y++) {
                level.setBlock(new BlockPos(x, y, z), y == targetY
                        ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
            }
        } else if (natural.getY() > targetY) {
            for (int y = natural.getY(); y > targetY; y--) {
                level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
            level.setBlock(new BlockPos(x, targetY, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        }
        // Clear the plants above the new surface, up to above where the old one was. Lowering the ground
        // used to clear only the first few blocks over the new surface, so grass, flowers and the top
        // halves of tall plants that grew on the old, higher ground were left hanging in the air.
        int clearTop = Math.max(natural.getY(), targetY) + 3;
        for (int y = targetY + 1; y <= clearTop; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.REPLACEABLE) || state.getBlock() instanceof net.minecraft.world.level.block.BushBlock) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static void spawnWallowFamily(WorldGenLevel level, RandomSource random, BlockPos center, int groundY) {
        int adults = 3;
        int children = 1 + random.nextInt(2);
        for (int i = 0; i < adults + children; i++) {
            boolean baby = i >= adults;
            BlockPos spawn = findSpawn(level, random, center, groundY);
            if (spawn == null) {
                continue;
            }
            MudsnoutBoar boar = ModEntities.MUDSNOUT_BOAR.get().create(level.getLevel());
            if (boar == null) {
                return;
            }
            boar.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    random.nextFloat() * 360.0F, 0.0F);
            boar.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), MobSpawnType.CHUNK_GENERATION, null);
            boar.setBaby(baby);
            if (!level.noCollision(boar, boar.getBoundingBox())) {
                boar.discard();
                continue;
            }
            level.addFreshEntityWithPassengers(boar);
        }
    }

    private static BlockPos findSpawn(WorldGenLevel level, RandomSource random, BlockPos center, int groundY) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = random.nextInt(21) - 10;
            int dz = random.nextInt(21) - 10;
            BlockPos ground = groundAt(level, center.getX() + dx, center.getZ() + dz);
            if (Math.abs(ground.getY() - groundY) > 2 || !level.getFluidState(ground.above()).isEmpty()) {
                continue;
            }
            BlockPos pos = ground.above();
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.above(2)).isAir()) {
                return pos;
            }
        }
        return null;
    }

    private static BlockPos groundAt(WorldGenLevel level, int x, int z) {
        // The worldgen heightmap is set by the terrain pass for every chunk; the in-game ones are only
        // filled in once a chunk has been decorated, so neighbours not yet finished would read as void.
        return new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1, z);
    }
}
