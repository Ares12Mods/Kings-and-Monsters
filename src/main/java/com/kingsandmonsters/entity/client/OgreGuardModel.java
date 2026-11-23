package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreGuard;
import software.bernie.geckolib.animation.AnimationState;

public class OgreGuardModel extends OgreModel<OgreGuard> {
    // The bones his stab and slam animate over the combat walk.
    private final AttackOverlayBlender overlayBlender = new AttackOverlayBlender(
            OgreGuard.ATTACK_OVERLAY_FADE_OUT_TICKS,
            "body", "head", "nose", "eyebrow", "left_ear", "right_ear", "left_arm", "right_arm");

    public OgreGuardModel() {
        super("ogre_guard", true);
    }

    @Override
    public void setCustomAnimations(OgreGuard guard, long instanceId, AnimationState<OgreGuard> animationState) {
        overlayBlender.update(this, instanceId, guard.tickCount + animationState.getPartialTick(),
                guard.isAttackOverlayVisible(), false);
        super.setCustomAnimations(guard, instanceId, animationState);
    }
}
