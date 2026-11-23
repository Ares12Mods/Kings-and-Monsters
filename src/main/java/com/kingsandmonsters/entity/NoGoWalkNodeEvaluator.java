package com.kingsandmonsters.entity;

import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/** Walk evaluator that treats an ogre's no-go zone (the quarry pit) as solid. */
final class NoGoWalkNodeEvaluator extends WalkNodeEvaluator {
    @Override
    public PathType getPathType(PathfindingContext context, int x, int y, int z) {
        if (mob instanceof OgreGrunt grunt && grunt.isNoGoColumn(x, z)) {
            return PathType.BLOCKED;
        }
        return super.getPathType(context, x, y, z);
    }
}
