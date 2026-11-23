package com.kingsandmonsters.world;

import com.kingsandmonsters.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class BogToadEggFeature extends Feature<NoneFeatureConfiguration> {
    public BogToadEggFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int placed = 0;
        int targetCount = 3 + random.nextInt(3);
        for (int attempt = 0; attempt < 14 && placed < targetCount; attempt++) {
            BlockPos pos = origin.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
            pos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
            if (!canPlaceEgg(level, pos)) continue;
            level.setBlock(pos, ModBlocks.BOG_TOAD_EGG.get().defaultBlockState(), 2);
            placed++;
        }
        return placed > 0;
    }

    public static boolean canPlaceEgg(net.minecraft.world.level.LevelReader level, BlockPos pos) {
        BlockState ground = level.getBlockState(pos.below());
        return level.getBlockState(pos).canBeReplaced()
                && level.getFluidState(pos).isEmpty()
                && ground.isFaceSturdy(level, pos.below(), Direction.UP)
                && (ground.is(BlockTags.DIRT) || ground.is(Blocks.MUD)
                    || ground.is(Blocks.CLAY) || ground.is(Blocks.MOSS_BLOCK));
    }
}
