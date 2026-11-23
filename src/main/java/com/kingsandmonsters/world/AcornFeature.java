package com.kingsandmonsters.world;

import com.kingsandmonsters.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class AcornFeature extends Feature<NoneFeatureConfiguration> {
    public AcornFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        List<BlockPos> candidates = new ArrayList<>();

        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                for (int y = 7; y >= -10; y--) {
                    BlockPos leaf = origin.offset(x, y, z);
                    BlockPos hanging = leaf.below();
                    if (level.getBlockState(leaf).is(Blocks.OAK_LEAVES)
                            && level.getBlockState(hanging).isAir()) {
                        candidates.add(hanging.immutable());
                        break;
                    }
                }
            }
        }

        if (candidates.isEmpty()) return false;
        int count = Math.min(candidates.size(), 1 + random.nextInt(3));
        for (int i = 0; i < count; i++) {
            BlockPos chosen = candidates.remove(random.nextInt(candidates.size()));
            level.setBlock(chosen, ModBlocks.ACORN.get().defaultBlockState(), 2);
        }
        return true;
    }
}
