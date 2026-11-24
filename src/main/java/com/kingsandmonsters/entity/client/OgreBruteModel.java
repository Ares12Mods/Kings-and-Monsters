package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreBrute;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;

public class OgreBruteModel extends OgreModel<OgreBrute> {
    // The bones his swings thrown over the combat walk animate.
    private final AttackOverlayBlender overlayBlender = new AttackOverlayBlender(
            OgreBrute.ATTACK_OVERLAY_FADE_OUT_TICKS,
            "body", "belly", "head", "jaw", "bone", "helmet", "left_ear", "right_ear",
            "left_eyebrow", "right_eyebrow", "left_arm", "right_arm");

    public OgreBruteModel() {
        super("ogre_brute", true);
    }

    @Override
    public void setCustomAnimations(OgreBrute brute, long instanceId, AnimationState<OgreBrute> animationState) {
        overlayBlender.update(this, instanceId, brute.tickCount + animationState.getPartialTick(),
                brute.isAttackOverlayVisible(), brute.isProceduralHeadTrackingSuppressed());
        super.setCustomAnimations(brute, instanceId, animationState);
    }

    @Override
    protected boolean suppressProceduralHeadTracking(OgreBrute brute) {
        return brute.isProceduralHeadTrackingSuppressed();
    }

    @Override
    protected float getMaximumDownwardHeadPitch() {
        // The Brute's short neck needs the same tight downward limit as the King.
        return 6.0F * Mth.DEG_TO_RAD;
    }

}
