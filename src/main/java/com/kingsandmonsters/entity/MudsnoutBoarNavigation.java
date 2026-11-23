package com.kingsandmonsters.entity;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.phys.Vec3;

/**
 * Ground navigation that starts turning toward the next waypoint before the boar has reached the
 * current one, so a chase curves around corners instead of running each node out and snapping to
 * the next. Same idea as the ogres' navigation, with a longer look-ahead for a longer animal.
 */
final class MudsnoutBoarNavigation extends GroundPathNavigation {
    private static final double NODE_LOOK_AHEAD_DISTANCE_SQR = 1.9 * 1.9;

    MudsnoutBoarNavigation(MudsnoutBoar boar, Level level) {
        super(boar, level);
    }

    @Override
    protected void followThePath() {
        if (path != null && path.getNextNodeIndex() + 1 < path.getNodeCount()) {
            Vec3 currentNode = path.getNextEntityPos(mob);
            Node nextNode = path.getNode(path.getNextNodeIndex() + 1);
            double dx = mob.getX() - currentNode.x;
            double dz = mob.getZ() - currentNode.z;
            if (dx * dx + dz * dz <= NODE_LOOK_AHEAD_DISTANCE_SQR
                    && Math.abs(nextNode.y - currentNode.y) <= 1.0
                    && canCutCorner(nextNode.type)) {
                path.advance();
            }
        }
        super.followThePath();
    }
}
