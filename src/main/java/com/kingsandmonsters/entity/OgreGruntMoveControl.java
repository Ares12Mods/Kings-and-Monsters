package com.kingsandmonsters.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;

final class OgreGruntMoveControl extends MoveControl {
    private static final float WALK_TURN_LERP = 0.12F;
    private static final float RUN_TURN_LERP = 0.24F;

    private final OgreGrunt grunt;

    OgreGruntMoveControl(OgreGrunt grunt) {
        super(grunt);
        this.grunt = grunt;
    }

    @Override
    protected float rotlerp(float currentAngle, float targetAngle, float maxSpeed) {
        float angle = Mth.wrapDegrees(targetAngle - currentAngle);
        // Chasing used to close 35% of the remaining angle every tick, which made each new path
        // node read as a small heading snap. A gentler proportional turn keeps pursuit responsive
        // while carrying the body through one clean arc. Attack startup still performs its own
        // exact facing correction before a hit is committed.
        float lerp = grunt.usesRunningTurnSmoothing() ? RUN_TURN_LERP : WALK_TURN_LERP;
        return currentAngle + angle * lerp;
    }
}
