package com.kingsandmonsters.world;

import com.kingsandmonsters.tribute.OutpostCaptainType;
import com.kingsandmonsters.tribute.OutpostProfile;
import com.kingsandmonsters.tribute.PitZone;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.biome.Biome;
import javax.annotation.Nullable;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The tribe's smaller encounters (supply camps, the moving camp, the abandoned camp) and the
 * King's Quarry. They place like the outposts and the Fort do: the jigsaw delegate finds the start,
 * then the piece is lifted onto the ground and the terrain is blended after the fact.
 *
 * <ul>
 *   <li>Camps follow the outpost path: median-height anchor, shallow foundations, tapered banks.</li>
 *   <li>The quarry follows the Fort path: per-column grading, cliff guard, tree cleanup, plus a
 *       foundation fill through water because it sits in swamp.</li>
 * </ul>
 */
public final class OgreEncounterStructure extends Structure {
    /** Worldgen height difference a Trapper site may span; the grading piece levels it afterwards. */
    private static final int TRAPPER_HUT_MAX_RELIEF = 4;
    private static final int TRAPPER_TRAP_MAX_RELIEF = 5;
    /** How far out the ground has to stay dry: the hut is a wide disc, the trap a smaller rectangle. */
    private static final int TRAPPER_HUT_DRY_RADIUS = 22;
    private static final int TRAPPER_TRAP_DRY_RADIUS = 15;
    /** Directions probed around the site when it has to be near a swamp. */
    private static final int SWAMP_PROBE_STEPS = 12;

    private final StructureSettings structureSettings;
    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final String encounter;
    private final int groundY;
    private final int groundLift;
    private final int swampRadius;
    private final Optional<Pit> pit;

    /** The quarry's hole, in template-local coordinates; see {@link PitZone}. */
    public record Pit(int x, int z, int avoidRadius, int keepRadius) {
        static final Codec<Pit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(Pit::x),
                Codec.INT.fieldOf("z").forGetter(Pit::z),
                Codec.INT.fieldOf("avoid_radius").forGetter(Pit::avoidRadius),
                Codec.INT.fieldOf("keep_radius").forGetter(Pit::keepRadius)
        ).apply(instance, Pit::new));
    }

    public static final Codec<OgreEncounterStructure> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                    Codec.intRange(0, 7).fieldOf("size").forGetter(s -> s.maxDepth),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                    Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),

                    Codec.STRING.fieldOf("encounter").forGetter(s -> s.encounter),
                    Codec.INT.optionalFieldOf("ground_y", 0).forGetter(s -> s.groundY),
                    Codec.INT.optionalFieldOf("ground_lift", 0).forGetter(s -> s.groundLift),
                    Codec.INT.optionalFieldOf("swamp_radius", 0).forGetter(s -> s.swampRadius),
                    Pit.CODEC.optionalFieldOf("pit").forGetter(s -> s.pit)
            ).apply(instance, OgreEncounterStructure::new)
    );

    public OgreEncounterStructure(
            StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            Optional<ResourceLocation> startJigsawName,
            int maxDepth,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            String encounter,
            int groundY,
            int groundLift,
            int swampRadius,
            Optional<Pit> pit) {
        super(settings);
        this.structureSettings = settings;
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.encounter = encounter;
        this.groundY = groundY;
        this.groundLift = groundLift;
        this.swampRadius = swampRadius;
        this.pit = pit;
    }

    private OutpostProfile profile() {
        return OutpostProfile.byEncounter(encounter);
    }

    private boolean isQuarry() {
        return profile() == OutpostProfile.QUARRY;
    }

    @Override
    public Optional<GenerationStub> findValidGenerationPoint(GenerationContext context) {
        // Same early rejection the outposts use: one climate lookup at the start column before
        // paying for jigsaw assembly. The vanilla biome check still runs again through super.
        ChunkPos chunk = context.chunkPos();
        if (!context.validBiome().test(biomeAt(context, chunk.getMinBlockX(), chunk.getMinBlockZ()))) {
            return Optional.empty();
        }
        if (swampRadius > 0 && !hasSwampNearby(context, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ())) {
            return Optional.empty();
        }
        if (isTrapperSite()) {
            return trapperSiteIsViable(context) ? super.findValidGenerationPoint(context) : Optional.empty();
        }
        boolean suitableSite = switch (profile()) {
            case SUPPLY_CAMP -> OgreOutpostStructure.isSuitableOutpostSite(
                    context, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), 4);
            // This camp has an outpost-sized footprint. Checking only its central 20x20 area let
            // the outer walls cross hillsides and forced the terrain cleaner to build a dirt box.
            // Require the complete footprint and its margin to be dry and nearly level instead.
            case ABANDONED_CAMP -> OgreOutpostStructure.isSuitableOutpostSite(
                    context, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), 3);
            default -> OgreOutpostStructure.isSuitablePlainsSite(
                    context, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), 4);
        };
        if (!isQuarry() && !suitableSite) {
            return Optional.empty();
        }
        if (keepsAwayFromWater() && !isClearOfWater(context, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ())) {
            return Optional.empty();
        }
        return super.findValidGenerationPoint(context);
    }

    /** Supply camps and the Trapper's grounds keep off the shoreline; the other camps don't mind it. */
    private boolean keepsAwayFromWater() {
        return switch (profile()) {
            case SUPPLY_CAMP, TRAPPER_HUT, TRAPPER_TRAP -> true;
            default -> false;
        };
    }

    /** Samples two rings around the site; any column whose floor is at or under sea level counts as wet. */
    private static boolean isClearOfWater(GenerationContext context, int centerX, int centerZ) {
        int seaLevel = context.chunkGenerator().getSeaLevel();
        for (int radius : new int[]{0, 10, 18}) {
            int points = radius == 0 ? 1 : 8;
            for (int step = 0; step < points; step++) {
                double angle = step * (Math.PI * 2.0 / points);
                int floor = context.chunkGenerator().getBaseHeight(
                        centerX + (int) Math.round(Math.cos(angle) * radius),
                        centerZ + (int) Math.round(Math.sin(angle) * radius),
                        Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                if (floor <= seaLevel) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Holder<Biome> biomeAt(GenerationContext context, int x, int z) {
        return context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(x),
                QuartPos.fromBlock(context.chunkGenerator().getSeaLevel()),
                QuartPos.fromBlock(z),
                context.randomState().sampler());
    }

    /**
     * A structure can require swamp nearby: some swamp has to be within
     * {@code swamp_radius} blocks, sampled around the site in two rings.
     */
    private boolean hasSwampNearby(GenerationContext context, int centerX, int centerZ) {
        for (int step = 1; step <= SWAMP_PROBE_STEPS; step++) {
            double angle = step * (Math.PI * 2.0 / SWAMP_PROBE_STEPS);
            for (int ring = 1; ring <= 2; ring++) {
                int distance = swampRadius * ring / 2;
                Holder<Biome> biome = biomeAt(context,
                        centerX + (int) Math.round(Math.cos(angle) * distance),
                        centerZ + (int) Math.round(Math.sin(angle) * distance));
                if (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTrapperSite() {
        return profile() == OutpostProfile.TRAPPER_HUT || profile() == OutpostProfile.TRAPPER_TRAP;
    }

    /**
     * Hut and trap sites are accepted on terrain alone, and cheaply, because {@code /locate} asks
     * for every candidate chunk. A trap is only accepted when the hut it is paired with is viable
     * too, so the pair can never be split by one half failing its terrain test.
     */
    private boolean trapperSiteIsViable(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        if (profile() == OutpostProfile.TRAPPER_HUT) {
            return isViableTrapperSite(context, chunk, TRAPPER_HUT_MAX_RELIEF, TRAPPER_HUT_DRY_RADIUS);
        }
        ChunkPos hut = TrapperSites.hutChunk(context.seed(), chunk.x, chunk.z);
        return isViableTrapperSite(context, hut, TRAPPER_HUT_MAX_RELIEF, TRAPPER_HUT_DRY_RADIUS)
                && isViableTrapperSite(context, chunk, TRAPPER_TRAP_MAX_RELIEF, TRAPPER_TRAP_DRY_RADIUS);
    }

    private boolean isViableTrapperSite(GenerationContext context, ChunkPos chunk, int maxRelief, int dryRadius) {
        int centerX = chunk.getMiddleBlockX();
        int centerZ = chunk.getMiddleBlockZ();
        // The corner is the biome probe the hut's own early rejection uses, so a trap never counts a
        // hut that would have been turned away there.
        return context.validBiome().test(biomeAt(context, chunk.getMinBlockX(), chunk.getMinBlockZ()))
                && context.validBiome().test(biomeAt(context, centerX, centerZ))
                && TrapperSites.isViableSite(context, centerX, centerZ, maxRelief, dryRadius);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        JigsawStructure delegate = new JigsawStructure(
                structureSettings, startPool, startJigsawName, maxDepth,
                startHeight, useExpansionHack, projectStartToHeightmap, maxDistanceFromCenter);
        Optional<GenerationStub> stubOpt = delegate.findGenerationPoint(context);
        if (stubOpt.isEmpty()) {
            return stubOpt;
        }

        GenerationStub stub = stubOpt.get();
        StructurePiecesBuilder piecesBuilder = stub.getPiecesBuilder();
        if (isTrapperSite()) {
            return trapperGenerationPoint(context, piecesBuilder);
        }
        PiecesContainer selectedPieces = piecesBuilder.build();
        OgreOutpostStructure.alignToFootprintMedian(
                context, piecesBuilder, selectedGroundY(selectedPieces), groundLift);
        return Optional.of(new GenerationStub(stub.position(), Either.right(piecesBuilder)));
    }

    /**
     * A hut or trap is always centred on its own chunk, whatever rotation was rolled, so the
     * terrain test that accepted the chunk is the terrain the template really covers. The piece
     * then goes onto the median ground height and a {@link TrapperGradingPiece} is placed ahead of
     * it to level the footprint and ease the surrounding ground back to the natural terrain.
     */
    private Optional<GenerationStub> trapperGenerationPoint(
            GenerationContext context, StructurePiecesBuilder delegatePieces) {
        PiecesContainer chosen = delegatePieces.build();
        BoundingBox box = chosen.calculateBoundingBox();
        ChunkPos chunk = context.chunkPos();
        int shiftX = chunk.getMiddleBlockX() - (box.minX() + box.maxX()) / 2;
        int shiftZ = chunk.getMiddleBlockZ() - (box.minZ() + box.maxZ()) / 2;
        for (StructurePiece piece : chosen.pieces()) {
            piece.move(shiftX, 0, shiftZ);
        }
        OgreOutpostStructure.alignToFootprintMedian(context, delegatePieces, selectedGroundY(chosen), groundLift);
        chosen = delegatePieces.build();

        PoolElementStructurePiece template = chosen.pieces().stream()
                .filter(PoolElementStructurePiece.class::isInstance)
                .map(PoolElementStructurePiece.class::cast)
                .findFirst()
                .orElse(null);
        if (template == null) {
            return Optional.empty();
        }
        int planeY = template.getPosition().getY() + selectedGroundY(chosen);
        // The hut's authored ground is a disc; the trap's is its whole template rectangle.
        int shape = profile() == OutpostProfile.TRAPPER_HUT
                ? TrapperGradingPiece.SHAPE_ELLIPSE : TrapperGradingPiece.SHAPE_RECTANGLE;

        StructurePiecesBuilder ordered = new StructurePiecesBuilder();
        ordered.addPiece(new TrapperGradingPiece(template.getBoundingBox(), planeY, shape));
        for (StructurePiece piece : chosen.pieces()) {
            ordered.addPiece(piece);
        }
        BlockPos start = new BlockPos(chunk.getMiddleBlockX(), planeY, chunk.getMiddleBlockZ());
        return Optional.of(new GenerationStub(start, Either.right(ordered)));
    }

    /** The pieces that are actual template placements, without the terrain grading piece. */
    private static PiecesContainer templatePieces(PiecesContainer pieces) {
        return new PiecesContainer(pieces.pieces().stream()
                .filter(PoolElementStructurePiece.class::isInstance)
                .toList());
    }

    @Override
    public void afterPlace(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            PiecesContainer allPieces) {
        // Everything below is about the authored template, not the wider terrain grading piece.
        PiecesContainer pieces = isTrapperSite() ? templatePieces(allPieces) : allPieces;
        BoundingBox structureBox = pieces.calculateBoundingBox();
        int groundPlaneY = groundPlaneY(pieces, structureBox);
        PitZone sitePit = worldPit(pieces);
        BlockPos center = structureBox.getCenter();
        // The camp origin is where residents walk back to, so it sits on the ground, not mid-box.
        BlockPos campOrigin = new BlockPos(center.getX(), groundPlaneY, center.getZ());
        // The grading piece reaches chunks the template never touches. Residents and the boar pen are
        // registered from a chunk that holds template blocks, so the pen is built by the time they spawn.
        boolean holdsTemplate = structureBox.intersects(chunkBox);
        if (holdsTemplate && (profile().hasCaptain() || !profile().slots().isEmpty())) {
            WorldgenSavedDataQueue.registerEncounter(
                    level.getLevel(), campOrigin, profile(), captainFor(level, structureBox), structureBox, groundPlaneY,
                    sitePit);
        }
        if (holdsTemplate && profile() == OutpostProfile.TRAPPER_HUT) {
            WorldgenSavedDataQueue.spawnTrapperBoarFamily(
                    level.getLevel(), campOrigin, transformedPositions(pieces, List.of(
                            new BlockPos(13, 3, 22),
                            new BlockPos(17, 3, 22),
                            new BlockPos(14, 3, 25),
                            new BlockPos(18, 3, 25))));
        }

        Map<Long, int[]> columnExtents = FortFootprintProcessor.takeCapturedColumns();
        Set<Long> authoredBlocks = FortFootprintProcessor.takeCapturedBlocks();
        if (isQuarry()) {
            FortTerrainCleaner.grade(level, chunkBox, columnExtents, authoredBlocks, true);
            FortCliffGuard.guard(level, chunkBox, columnExtents, true);
            FortVegetationCleaner.captureStructureBlocks(level, chunkBox, chunkPos, pieces, authoredBlocks);
        } else {
            if (isTrapperSite()) {
                // The grading piece has already levelled the footprint and eased its edge, so there
                // is no per-chunk terrain pass here. Only the trap's pit is restored: its template
                // was saved without the cavity, so natural ground fills it until it is carved out.
                if (profile() == OutpostProfile.TRAPPER_TRAP) {
                    carveSavedTrapperPit(level, chunkBox, pieces, authoredBlocks);
                }
            } else if (profile() == OutpostProfile.SUPPLY_CAMP) {
                OutpostTerrainCleaner.integrateCampSite(
                        level, chunkBox, columnExtents, authoredBlocks);
            } else {
                OutpostTerrainCleaner.integrate(
                        level, chunkBox, structureBox, columnExtents, authoredBlocks, false, false);
            }
            if (profile() == OutpostProfile.TRAPPER_TRAP) {
                OutpostVegetationCleaner.captureTrapperTrap(level, chunkBox, chunkPos, pieces, authoredBlocks);
            } else if (profile() == OutpostProfile.TRAPPER_HUT) {
                OutpostVegetationCleaner.captureTrapperHut(level, chunkBox, chunkPos, pieces, authoredBlocks);
            } else {
                OutpostVegetationCleaner.captureStructureBlocks(
                        level, chunkBox, chunkPos, pieces, authoredBlocks, false);
            }
        }
    }

    /** The pit centre turned into world coordinates, following the piece's rotation. */
    @Nullable
    private PitZone worldPit(PiecesContainer pieces) {
        if (pit.isEmpty()) {
            return null;
        }
        for (StructurePiece piece : pieces.pieces()) {
            if (piece instanceof PoolElementStructurePiece poolPiece) {
                Pit selectedPit = pit.get();
                BlockPos turned = StructureTemplate.transform(
                        new BlockPos(selectedPit.x(), 0, selectedPit.z()),
                        Mirror.NONE, poolPiece.getRotation(), BlockPos.ZERO);
                return new PitZone(poolPiece.getPosition().getX() + turned.getX(),
                        poolPiece.getPosition().getZ() + turned.getZ(),
                        selectedPit.avoidRadius(), selectedPit.keepRadius());
            }
        }
        return null;
    }

    /** World height of the template's ground layer once the piece has been lifted into place. */
    private int groundPlaneY(PiecesContainer pieces, BoundingBox fallbackBox) {
        int selectedGroundY = selectedGroundY(pieces);
        return pieces.pieces().stream()
                .filter(PoolElementStructurePiece.class::isInstance)
                .map(PoolElementStructurePiece.class::cast)
                .mapToInt(piece -> piece.getPosition().getY() + selectedGroundY)
                .findFirst()
                .orElse(fallbackBox.minY() + selectedGroundY);
    }

    /** The two trap templates have different local ground planes. */
    private int selectedGroundY(PiecesContainer pieces) {
        if (profile() != OutpostProfile.TRAPPER_TRAP) {
            return groundY;
        }
        return pieces.pieces().stream()
                .filter(PoolElementStructurePiece.class::isInstance)
                .map(PoolElementStructurePiece.class::cast)
                .findFirst()
                .map(piece -> isTallTrap(piece) ? 5 : 2)
                .orElse(groundY);
    }

    private static boolean isTallTrap(PoolElementStructurePiece piece) {
        BoundingBox box = piece.getBoundingBox();
        return box.maxY() - box.minY() + 1 > 10;
    }

    /**
     * Trap 1 was saved without air entries. Recreate only its authored four-block-wide opening;
     * every solid block from the template remains authoritative, including the irregular wall,
     * hay supports and dripstone. Trap 2 is a redstone snare and must not be excavated.
     */
    private static void carveSavedTrapperPit(
            WorldGenLevel level,
            BoundingBox chunkBox,
            PiecesContainer pieces,
            Set<Long> authoredBlocks) {
        PoolElementStructurePiece trapPiece = pieces.pieces().stream()
                .filter(PoolElementStructurePiece.class::isInstance)
                .map(PoolElementStructurePiece.class::cast)
                .findFirst()
                .orElse(null);
        if (trapPiece == null || !isTallTrap(trapPiece)) {
            return;
        }

        for (int localZ = 5; localZ <= 8; localZ++) {
            int firstX = localZ == 8 ? 15 : 14;
            for (int localX = firstX; localX <= 17; localX++) {
                for (int localY = 0; localY <= 8; localY++) {
                    BlockPos local = new BlockPos(localX, localY, localZ);
                    BlockPos turned = StructureTemplate.transform(
                            local, Mirror.NONE, trapPiece.getRotation(), BlockPos.ZERO);
                    BlockPos world = trapPiece.getPosition().offset(turned);
                    if (!chunkBox.isInside(world) || authoredBlocks.contains(world.asLong())) {
                        continue;
                    }
                    level.setBlock(world, Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
    }

    /** Template-local positions turned with the selected hut rotation. */
    private static List<BlockPos> transformedPositions(PiecesContainer pieces, List<BlockPos> localPositions) {
        for (StructurePiece piece : pieces.pieces()) {
            if (piece instanceof PoolElementStructurePiece poolPiece) {
                return localPositions.stream().map(local -> {
                    BlockPos turned = StructureTemplate.transform(
                            local, Mirror.NONE, poolPiece.getRotation(), BlockPos.ZERO);
                    return poolPiece.getPosition().offset(turned);
                }).toList();
            }
        }
        return List.of();
    }

    /**
     * Only the moving camp rolls its captain: it isn't established yet, so which captain turns up
     * is a matter of who got there first. Seeded from the position so it never changes on reload.
     */
    private OutpostCaptainType captainFor(WorldGenLevel level, BoundingBox structureBox) {
        return switch (profile()) {
            case QUARRY -> OutpostCaptainType.BRUTE;
            case MOVING_CAMP -> {
                long hash = level.getSeed()
                        ^ ((long) structureBox.minX() * 341873128712L)
                        ^ ((long) structureBox.minZ() * 132897987541L);
                OutpostCaptainType[] captains = OutpostCaptainType.values();
                yield captains[RandomSource.create(hash).nextInt(captains.length)];
            }
            default -> OutpostCaptainType.GRUNT;
        };
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.OGRE_ENCOUNTER.get();
    }
}
