package com.kingsandmonsters.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;

/**
 * Decides which of a template's saved air blocks actually get placed.
 *
 * <p>The camps keep air above their ground layer, so grass, bushes and small hills inside the camp
 * are cleared, but drop the air under it so the camp doesn't hollow out the soil below. The quarry
 * is different: it is a stone plug sunk into the ground, so only the air inside the pit cylinder is
 * placed (that carves the pit) and everything outside the cylinder is left to the natural terrain.</p>
 */
public final class EncounterAirProcessor extends StructureProcessor {
    public static final Codec<EncounterAirProcessor> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("ground_y", 0).forGetter(p -> p.groundY),
                    Codec.INT.optionalFieldOf("pit_x", 0).forGetter(p -> p.pitX),
                    Codec.INT.optionalFieldOf("pit_z", 0).forGetter(p -> p.pitZ),
                    Codec.FLOAT.optionalFieldOf("pit_radius", 0.0F).forGetter(p -> p.pitRadius),
                    Codec.BOOL.optionalFieldOf("oval", false).forGetter(p -> p.oval),
                    Codec.INT.listOf().optionalFieldOf("cavity", java.util.List.of()).forGetter(p -> p.cavity)
            ).apply(instance, EncounterAirProcessor::new));

    private final int groundY;
    private final int pitX;
    private final int pitZ;
    private final float pitRadius;
    private final boolean oval;
    /** minX, minZ, maxX, maxZ of the authored underground space, or empty for none. */
    private final java.util.List<Integer> cavity;

    public EncounterAirProcessor(int groundY, int pitX, int pitZ, float pitRadius, boolean oval,
                                 java.util.List<Integer> cavity) {
        this.cavity = java.util.List.copyOf(cavity);
        this.oval = oval;
        this.groundY = groundY;
        this.pitX = pitX;
        this.pitZ = pitZ;
        this.pitRadius = pitRadius;
    }

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
        if (!relativeInfo.state().isAir()) {
            return relativeInfo;
        }
        BlockPos local = rawInfo.pos();
        if (cavity.size() == 4) {
            // Air above the ground plane clears the site. Underground, only the authored space is
            // opened; the plane itself stays ground, so no hole in the surface shows what is below.
            if (local.getY() > groundY) {
                return relativeInfo;
            }
            boolean inside = local.getX() >= cavity.get(0) && local.getZ() >= cavity.get(1)
                    && local.getX() <= cavity.get(2) && local.getZ() <= cavity.get(3);
            return local.getY() < groundY && inside ? relativeInfo : null;
        }
        if (pitRadius > 0.0F) {
            float dx = local.getX() - pitX;
            float dz = local.getZ() - pitZ;
            return dx * dx + dz * dz <= pitRadius * pitRadius ? relativeInfo : null;
        }
        if (local.getY() < groundY) {
            return null;
        }
        if (oval) {
            // Clear only an oval inside the template box, so the corners keep the natural hillside.
            float halfX = template.getSize().getX() / 2.0F;
            float halfZ = template.getSize().getZ() / 2.0F;
            float dx = (local.getX() + 0.5F - halfX) / halfX;
            float dz = (local.getZ() + 0.5F - halfZ) / halfZ;
            return dx * dx + dz * dz <= 1.0F ? relativeInfo : null;
        }
        return relativeInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructures.ENCOUNTER_AIR.get();
    }
}
