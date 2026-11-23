package com.kingsandmonsters.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * Turns the boar toward where it is going with a proportional lerp instead of a fixed step, like
 * the ogres' move control: a long, even arc around its target rather than a series of jerks.
 */
final class MudsnoutBoarMoveControl extends MoveControl {
    // Fraction of the remaining angle closed per tick: gentle while it wanders or stalks, tighter
    // when it is running from an ogre.
    private static final float STALK_TURN = 0.10F;
    private static final float RUN_TURN = 0.16F;

    private final MudsnoutBoar boar;

    MudsnoutBoarMoveControl(MudsnoutBoar boar) {
        super(boar);
        this.boar = boar;
    }

    @Override
    protected float rotlerp(float currentAngle, float targetAngle, float maxSpeed) {
        float angle = Mth.wrapDegrees(targetAngle - currentAngle);
        return currentAngle + angle * (boar.isRunningForTurning() ? RUN_TURN : STALK_TURN);
    }
}
