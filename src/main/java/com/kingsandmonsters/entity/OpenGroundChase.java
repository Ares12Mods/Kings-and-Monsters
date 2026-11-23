package com.kingsandmonsters.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Lets a chasing mob walk straight at its target over open ground. Node paths run in 8-way segments, so
 * a big body shows the diagonal jog as an angled, zigzagging approach. Wherever the way is flat and clear
 * a mob steers directly at the target instead, and anything in the way falls back to normal pathing.
 */
final class OpenGroundChase {
    private static final double MAX_DISTANCE = 28.0;
    private static final double MAX_HEIGHT_DIFFERENCE = 1.5;

    private OpenGroundChase() {
    }

    /**
     * Whether the way to the target is flat and clear for the mob's whole width: no blocks in the way,
     * solid ground under every step, no water, and the target within the mob's home area.
     */
    static boolean isClear(Mob mob, LivingEntity target) {
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1.0 || distance > MAX_DISTANCE || Math.abs(target.getY() - mob.getY()) > MAX_HEIGHT_DIFFERENCE
                || !mob.getSensing().hasLineOfSight(target) || !mob.isWithinRestriction(target.blockPosition())) {
            return false;
        }
        int steps = (int) Math.ceil(distance);
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            double px = mob.getX() + dx * t;
            double pz = mob.getZ() + dz * t;
            if (!mob.level().noCollision(mob, mob.getBoundingBox().move(px - mob.getX(), 0.0, pz - mob.getZ()))) {
                return false;
            }
            BlockPos below = BlockPos.containing(px, mob.getY() - 0.5, pz);
            if (!mob.level().getBlockState(below).isFaceSturdy(mob.level(), below, Direction.UP)
                    || !mob.level().getFluidState(BlockPos.containing(px, mob.getY(), pz)).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
