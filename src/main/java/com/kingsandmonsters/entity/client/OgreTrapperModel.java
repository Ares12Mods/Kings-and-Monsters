package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreTrapper;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;

public class OgreTrapperModel extends OgreModel<OgreTrapper> {
    // The bones his swings thrown over the combat walk animate.
    private final AttackOverlayBlender overlayBlender = new AttackOverlayBlender(
            OgreTrapper.ATTACK_OVERLAY_FADE_OUT_TICKS,
            "body", "head", "jaw", "nose", "left_ear", "right_ear",
            "left_eyebrow", "right_eyebrow", "left_arm", "right_arm");

    public OgreTrapperModel() {
        super("ogre_trapper", true);
    }

    @Override
    protected float getMaximumDownwardHeadPitch() {
        // Looser than the Brute's 6 degrees, but far short of the 22.5 every ogre starts with.
        return 10.0F * Mth.DEG_TO_RAD;
    }

    @Override
    public void setCustomAnimations(OgreTrapper trapper, long instanceId, AnimationState<OgreTrapper> animationState) {
        overlayBlender.update(this, instanceId, trapper.tickCount + animationState.getPartialTick(),
                trapper.isAttackOverlayVisible(), false);
        super.setCustomAnimations(trapper, instanceId, animationState);

        // The trap shows on his back, in his left hand, or not at all. While the deploy plays, its
        // clip shrinks one and grows the other, so both are left to it.
        boolean deploying = trapper.getSyncedAttackId() == OgreTrapper.ATTACK_ID_DEPLOY;
        int trap = trapper.getTrapState();
        getBone("backtrap").ifPresent(bone -> bone.setHidden(!deploying && trap != OgreTrapper.TRAP_ON_BACK));
        getBone("handtrap").ifPresent(bone -> bone.setHidden(!deploying && trap != OgreTrapper.TRAP_IN_HAND));
    }
}
