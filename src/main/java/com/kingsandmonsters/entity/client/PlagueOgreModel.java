package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreGrunt;
import com.kingsandmonsters.entity.PlagueOgre;
import software.bernie.geckolib.core.animation.AnimationState;

/** Uses its own geo, texture and animations: geo/plague_ogre, textures/entity/plague_ogre and animations/plague_ogre. */
public class PlagueOgreModel extends OgreModel<PlagueOgre> {
    // The bones his swings, thrown over the combat walk, animate.
    private final AttackOverlayBlender overlayBlender = new AttackOverlayBlender(
            OgreGrunt.ATTACK_OVERLAY_FADE_OUT_TICKS,
            "body", "head", "jaw", "nose", "left_eyebrow", "right_eyebrow",
            "left_ear", "right_ear", "left_arm", "right_arm");

    public PlagueOgreModel() {
        super("plague_ogre", true);
    }

    @Override
    public void setCustomAnimations(PlagueOgre plague, long instanceId, AnimationState<PlagueOgre> animationState) {
        overlayBlender.update(this, instanceId, plague.tickCount + animationState.getPartialTick(),
                plague.isAttackOverlayVisible(), false);
        super.setCustomAnimations(plague, instanceId, animationState);
    }
}
