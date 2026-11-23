package com.kingsandmonsters.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.registries.Registries;
import com.kingsandmonsters.Config;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.tribute.OutpostCaptainType;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Rigid, single-level outpost placement. Candidate selection intentionally
 * follows vanilla jigsaw placement; terrain integration happens in afterPlace
 * and never participates in /locate.
 */
public class OgreOutpostStructure extends Structure {

    private static final ResourceKey<LootTable> SWAMP_TRIBUTE_WITH_MAP = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "chests/ogre_outpost_tribute_swamp_map"));
    private static final ResourceKey<LootTable> SWAMP_TRIBUTE_WITHOUT_MAP = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "chests/ogre_outpost_tribute_swamp"));

    private final StructureSettings structureSettings;
    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final List<PoolAliasBinding> poolAliases;
    private final DimensionPadding dimensionPadding;
    private final LiquidSettings liquidSettings;
    private final boolean swamp;

    public static final MapCodec<OgreOutpostStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                    Codec.intRange(0, 7).fieldOf("size").forGetter(s -> s.maxDepth),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                    Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
                    PoolAliasBinding.CODEC.listOf().optionalFieldOf("pool_aliases", List.of()).forGetter(s -> s.poolAliases),
                    DimensionPadding.CODEC.optionalFieldOf("dimension_padding", DimensionPadding.ZERO).forGetter(s -> s.dimensionPadding),
                    LiquidSettings.CODEC.optionalFieldOf("liquid_settings", LiquidSettings.APPLY_WATERLOGGING).forGetter(s -> s.liquidSettings),
                    Codec.BOOL.optionalFieldOf("swamp", false).forGetter(s -> s.swamp)
            ).apply(instance, OgreOutpostStructure::new)
    );

    public OgreOutpostStructure(
            StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            Optional<ResourceLocation> startJigsawName,
            int maxDepth,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            List<PoolAliasBinding> poolAliases,
            DimensionPadding dimensionPadding,
            LiquidSettings liquidSettings,
            boolean swamp) {
        super(settings);
        this.structureSettings = settings;
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.poolAliases = poolAliases;
        this.dimensionPadding = dimensionPadding;
        this.liquidSettings = liquidSettings;
        this.swamp = swamp;
    }

    @Override
    public Optional<GenerationStub> findValidGenerationPoint(GenerationContext context) {
        /*
         * Structure.findValidGenerationPoint normally performs its biome
         * check only after findGenerationPoint. That made /locate pay for
         * jigsaw assembly for candidates that could never generate. This
         * single climate-sampler
         * lookup uses the jigsaw's exact start X/Z and rejects those first.
         * Vanilla's final biome check still runs through super for candidates
         * that pass, so this cannot admit an invalid start.
         */
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMinBlockX();
        int z = chunk.getMinBlockZ();
        int y = context.chunkGenerator().getSeaLevel();
        Holder<net.minecraft.world.level.biome.Biome> biome =
                context.biomeSource().getNoiseBiome(
                        QuartPos.fromBlock(x),
                        QuartPos.fromBlock(y),
                        QuartPos.fromBlock(z),
                        context.randomState().sampler());
        if (!context.validBiome().test(biome)) {
            return Optional.empty();
        }
        if (!swamp && !isSuitableOutpostSite(context, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ())) {
            return Optional.empty();
        }
        return super.findValidGenerationPoint(context);
    }

    // The outpost is about 40 blocks across; the check covers a little more than that.
    private static final int SITE_RADIUS = 24;
    private static final int SITE_SPACING = 8;
    private static final int SITE_MAX_VARIATION = 6;
    // Shore and riverbank sit right at sea level, so anything this close to it is wet ground.
    private static final int SITE_MIN_ABOVE_SEA_LEVEL = 2;

    /**
     * Rejects candidates on steep ground or near water. A grid across the whole footprint and a margin
     * around it must be dry, clear of the shoreline and within a few blocks of the same height, so an
     * outpost never ends up clinging to a hillside or hanging over a river.
     */
    static boolean isSuitableOutpostSite(GenerationContext context, int centerX, int centerZ) {
        return isSuitableOutpostSite(context, centerX, centerZ, SITE_MAX_VARIATION);
    }

    /** Full-footprint outpost check with a caller-selected slope tolerance. */
    static boolean isSuitableOutpostSite(
            GenerationContext context, int centerX, int centerZ, int maxVariation) {
        int seaLevel = context.chunkGenerator().getSeaLevel();
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (int dx = -SITE_RADIUS; dx <= SITE_RADIUS; dx += SITE_SPACING) {
            for (int dz = -SITE_RADIUS; dz <= SITE_RADIUS; dz += SITE_SPACING) {
                int x = centerX + dx;
                int z = centerZ + dz;
                int ground = context.chunkGenerator().getFirstFreeHeight(
                        x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                int surface = context.chunkGenerator().getFirstFreeHeight(
                        x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                // A surface above the ground is water, and low ground is shoreline.
                if (surface > ground || ground < seaLevel + SITE_MIN_ABOVE_SEA_LEVEL) {
                    return false;
                }
                // Only the footprint itself has to be level; the margin only has to be dry.
                if (Math.abs(dx) <= SITE_RADIUS - SITE_SPACING && Math.abs(dz) <= SITE_RADIUS - SITE_SPACING) {
                    minimum = Math.min(minimum, ground);
                    maximum = Math.max(maximum, ground);
                    if (maximum - minimum > maxVariation) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Preserves the existing permissive rolling-plains candidate check. */
    static boolean isSuitablePlainsSite(GenerationContext context, int centerX, int centerZ) {
        return isSuitablePlainsSite(context, centerX, centerZ, 4);
    }

    /** {@code maxVariation} is how far the sampled ground may vary, in blocks, across the site. */
    static boolean isSuitablePlainsSite(GenerationContext context, int centerX, int centerZ, int maxVariation) {
        int[][] offsets = {
                {0, 0}, {-10, 0}, {10, 0}, {0, -10}, {0, 10},
                {-10, -10}, {-10, 10}, {10, -10}, {10, 10}
        };
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (int[] offset : offsets) {
            int height = context.chunkGenerator().getFirstFreeHeight(
                    centerX + offset[0], centerZ + offset[1],
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    context.heightAccessor(), context.randomState());
            minimum = Math.min(minimum, height);
            maximum = Math.max(maximum, height);
            if (maximum - minimum > maxVariation) return false;
        }
        return true;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        JigsawStructure delegate = new JigsawStructure(
                structureSettings, startPool, startJigsawName, maxDepth,
                startHeight, useExpansionHack, projectStartToHeightmap, maxDistanceFromCenter,
                poolAliases, dimensionPadding, liquidSettings
        );
        Optional<GenerationStub> stubOpt = delegate.findGenerationPoint(context);
        if (swamp || stubOpt.isEmpty()) {
            return stubOpt;
        }

        GenerationStub stub = stubOpt.get();
        StructurePiecesBuilder piecesBuilder = stub.getPiecesBuilder();
        alignPlainsToFootprintMedian(context, piecesBuilder);
        return Optional.of(new GenerationStub(stub.position(), Either.right(piecesBuilder)));
    }

    /**
     * Vanilla projects this rigid pool from one start-column height. Once the
     * candidate and template variant already exist, replace only that vertical
     * anchor with the median of a sparse 5x5 sample across the actual rotated
     * footprint. This never rejects or relocates a candidate horizontally.
     */
    private static void alignPlainsToFootprintMedian(
            GenerationContext context, StructurePiecesBuilder piecesBuilder) {
        alignToFootprintMedian(context, piecesBuilder, 0, 0);
    }

    /**
     * Same median anchor, for templates whose ground layer isn't at template y=0. {@code groundY} is
     * the template-local layer that should land on the sampled ground, {@code lift} raises it further.
     */
    static void alignToFootprintMedian(
            GenerationContext context, StructurePiecesBuilder piecesBuilder, int groundY, int lift) {
        PiecesContainer pieces = piecesBuilder.build();
        BoundingBox box = pieces.calculateBoundingBox();
        if (pieces.pieces().isEmpty()) {
            return;
        }

        int[] heights = new int[25];
        int index = 0;
        for (int sampleX = 0; sampleX < 5; sampleX++) {
            int x = sampleCoordinate(box.minX(), box.maxX(), sampleX);
            for (int sampleZ = 0; sampleZ < 5; sampleZ++) {
                int z = sampleCoordinate(box.minZ(), box.maxZ(), sampleZ);
                heights[index++] = context.chunkGenerator().getFirstFreeHeight(
                        x, z,
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        context.heightAccessor(), context.randomState());
            }
        }
        Arrays.sort(heights);
        int representativeGroundY = heights[heights.length / 2];
        int currentAnchorY = pieces.pieces().stream()
                .filter(PoolElementStructurePiece.class::isInstance)
                .map(PoolElementStructurePiece.class::cast)
                .mapToInt(piece -> piece.getPosition().getY())
                .findFirst()
                .orElse(box.minY());
        int verticalOffset = representativeGroundY + lift - groundY - currentAnchorY;
        if (verticalOffset != 0) {
            for (var piece : pieces.pieces()) {
                piece.move(0, verticalOffset, 0);
            }
        }
    }

    private static int sampleCoordinate(int minimum, int maximum, int sample) {
        if (minimum >= maximum) {
            return minimum;
        }
        // Keep the outer samples one block inside where possible so a single
        // wall overhang does not make the height representative of terrain
        // outside the actual build.
        int insetMin = maximum - minimum >= 4 ? minimum + 1 : minimum;
        int insetMax = maximum - minimum >= 4 ? maximum - 1 : maximum;
        return insetMin + Math.round((insetMax - insetMin) * (sample / 4.0F));
    }

    @Override
    public void afterPlace(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            PiecesContainer pieces) {
        BoundingBox structureBox = pieces.calculateBoundingBox();
        BlockPos campOrigin = structureBox.getCenter();
        OutpostCaptainType captainType = resolveCaptainType(pieces);
        BlockPos fortTarget = FortTargetAuthority.findNearestGuaranteedFort(level.getLevel(), campOrigin).orElse(null);
        WorldgenSavedDataQueue.registerOutpost(
                level.getLevel(), campOrigin, captainType, structureBox, fortTarget);
        if (!swamp) {
            // Every Plains variant's tribute chest carries a guaranteed map pool (see
            // chests/ogre_outpost_tribute.json) — capture its position once so
            // FortMapDeliveryService can repair legacy chests that unpacked without a valid map.
            findFirstTrappedChest(level, chunkBox, structureBox).ifPresent(chestPos -> {
                WorldgenSavedDataQueue.registerMapChest(level.getLevel(), campOrigin, chestPos);
            });
        }
        configureSwampTributeChest(level, chunkBox, structureBox, campOrigin);
        Map<Long, int[]> columnExtents = FortFootprintProcessor.takeCapturedColumns();
        Set<Long> authoredBlocks = FortFootprintProcessor.takeCapturedBlocks();
        OutpostTerrainCleaner.integrate(
                level, chunkBox, structureBox, columnExtents, authoredBlocks, swamp);
        OutpostVegetationCleaner.captureStructureBlocks(
                level, chunkBox, chunkPos, pieces, authoredBlocks, swamp);
    }

    /**
     * Each of the four Outpost templates runs through its own {@link OutpostMobLootProcessor}
     * instance (see {@code worldgen/processor_list/ogre_outpost_*.json}), so the processor knows
     * exactly which template is placed. That capture is authoritative; {@link #captainTypeFor}'s
     * string parse stays only as a fallback for older development-world templates.
     */
    private static OutpostCaptainType resolveCaptainType(PiecesContainer pieces) {
        String capturedVariant = OutpostMobLootProcessor.takeCapturedVariant();
        OutpostCaptainType resolved = switch (capturedVariant == null ? "" : capturedVariant) {
            case "grunt" -> OutpostCaptainType.GRUNT;
            case "mage", "mage_2" -> OutpostCaptainType.MAGE;
            case "brute" -> OutpostCaptainType.BRUTE;
            default -> null;
        };

        return resolved != null ? resolved : captainTypeFor(pieces);
    }

    private static OutpostCaptainType captainTypeFor(PiecesContainer pieces) {
        String templateDescription = pieces.pieces().stream()
                .filter(PoolElementStructurePiece.class::isInstance)
                .map(PoolElementStructurePiece.class::cast)
                .map(piece -> piece.getElement().toString().toLowerCase(java.util.Locale.ROOT))
                .findFirst()
                .orElse("");

        if (templateDescription.contains("mage") || templateDescription.contains("variant_2")) {
            return OutpostCaptainType.MAGE;
        }
        if (templateDescription.contains("grunt") || templateDescription.contains("variant_3")) {
            return OutpostCaptainType.GRUNT;
        }
        // The current first and fourth variants are Brute settlements. Future named templates
        // fall through here only when they do not explicitly identify another captain role.
        return OutpostCaptainType.BRUTE;
    }

    private void configureSwampTributeChest(
            WorldGenLevel level,
            BoundingBox chunkBox,
            BoundingBox structureBox,
            BlockPos campOrigin) {
        if (!swamp) {
            return;
        }

        long hash = level.getSeed()
                ^ ((long) structureBox.minX() * 341873128712L)
                ^ ((long) structureBox.minZ() * 132897987541L);
        RandomSource mapRoll = RandomSource.create(hash);
        boolean includeMap = mapRoll.nextDouble() < Config.SWAMP_OUTPOST_FORT_MAP_CHANCE.get();
        ResourceKey<LootTable> lootTable = includeMap
                ? SWAMP_TRIBUTE_WITH_MAP
                : SWAMP_TRIBUTE_WITHOUT_MAP;

        int minX = Math.max(chunkBox.minX(), structureBox.minX());
        int maxX = Math.min(chunkBox.maxX(), structureBox.maxX());
        int minZ = Math.max(chunkBox.minZ(), structureBox.minZ());
        int maxZ = Math.min(chunkBox.maxZ(), structureBox.maxZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = structureBox.minY(); y <= structureBox.maxY(); y++) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.TRAPPED_CHEST)
                            && level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest) {
                        chest.setLootTable(lootTable);
                        chest.setLootTableSeed(hash ^ pos.asLong());
                        chest.setChanged();
                        if (includeMap) {
                            // This roll carries the guaranteed map pool, so track it the way Plains does
                            // and FortMapDeliveryService can help if the chest opens before the Fort
                            // target resolves. A Swamp Outpost whose roll left the map out is never
                            // registered: for Swamp the map is chance-gated, not guaranteed (see Config).
                            WorldgenSavedDataQueue.registerMapChest(level.getLevel(), campOrigin, pos.immutable());
                        }
                    }
                }
            }
        }
    }

    /** First TRAPPED_CHEST found within this chunk pass's intersection, in scan order. */
    private static Optional<BlockPos> findFirstTrappedChest(
            WorldGenLevel level, BoundingBox chunkBox, BoundingBox structureBox) {
        int minX = Math.max(chunkBox.minX(), structureBox.minX());
        int maxX = Math.min(chunkBox.maxX(), structureBox.maxX());
        int minZ = Math.max(chunkBox.minZ(), structureBox.minZ());
        int maxZ = Math.min(chunkBox.maxZ(), structureBox.maxZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = structureBox.minY(); y <= structureBox.maxY(); y++) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.TRAPPED_CHEST)) {
                        return Optional.of(pos.immutable());
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.OGRE_OUTPOST.get();
    }

}
