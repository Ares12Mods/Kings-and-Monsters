package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreGrunt;
import com.kingsandmonsters.entity.OgreGruntCaptain;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;

public class OgreGruntCaptainModel extends OgreModel<OgreGruntCaptain> {
    // The bones his swings thrown over the combat walk animate.
    private final AttackOverlayBlender overlayBlender = new AttackOverlayBlender(
            OgreGrunt.ATTACK_OVERLAY_FADE_OUT_TICKS,
            "body", "head", "jaw", "nose", "left_eyebrow", "right_eyebrow", "left_arm", "right_arm");

    public OgreGruntCaptainModel() {
        super("ogre_grunt_captain", true);
    }

    @Override
    public void setCustomAnimations(OgreGruntCaptain captain, long instanceId,
                                    AnimationState<OgreGruntCaptain> animationState) {
        overlayBlender.update(this, instanceId, captain.tickCount + animationState.getPartialTick(),
                captain.isAttackOverlayVisible(), false);
        super.setCustomAnimations(captain, instanceId, animationState);
    }

    @Override
    protected float getMaximumDownwardHeadPitch() {
        return 12.0F * Mth.DEG_TO_RAD;
    }
}
