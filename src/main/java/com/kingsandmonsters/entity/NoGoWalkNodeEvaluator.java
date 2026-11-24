package com.kingsandmonsters.entity;

import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/** Walk evaluator that treats an ogre's no-go zone (the quarry pit) as solid. */
final class NoGoWalkNodeEvaluator extends WalkNodeEvaluator {
    @Override
    public BlockPathTypes getBlockPathType(BlockGetter context, int x, int y, int z) {
        if (mob instanceof OgreGrunt grunt && grunt.isNoGoColumn(x, z)) {
            return BlockPathTypes.BLOCKED;
        }
        return super.getBlockPathType(context, x, y, z);
    }
}
