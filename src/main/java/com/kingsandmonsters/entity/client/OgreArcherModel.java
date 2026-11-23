package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.MudsnoutBoar;
import com.kingsandmonsters.entity.OgreArcher;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;

public class OgreArcherModel extends OgreModel<OgreArcher> {
    // The bones his swings thrown over the combat walk animate.
    private final AttackOverlayBlender overlayBlender = new AttackOverlayBlender(
            OgreArcher.ATTACK_OVERLAY_FADE_OUT_TICKS,
            "body", "head", "nose", "left_eyebrow", "right_eyebrow", "left_arm", "right_arm");

    public OgreArcherModel() {
        super("ogre_archer", true);
    }

    @Override
    public void setCustomAnimations(OgreArcher archer, long instanceId, AnimationState<OgreArcher> animationState) {
        overlayBlender.update(this, instanceId, archer.tickCount + animationState.getPartialTick(),
                archer.isAttackOverlayVisible(), false);
        super.setCustomAnimations(archer, instanceId, animationState);
        MudsnoutBoar boar = archer.getVehicle() instanceof MudsnoutBoar mount ? mount : null;
        followBoar(boar);
        if (boar != null) {
            aimFromBoar(archer, boar, animationState.getPartialTick());
        }
    }

    /**
     * The whole archer moves with the boar's back as it bobs, lunges and pitches. No clip keys the
     * root bone, so its pose is set from scratch each frame; adding to it would pile up frame after
     * frame. Off the boar it goes back to rest.
     */
    private void followBoar(MudsnoutBoar boar) {
        getBone("main").ifPresent(main -> {
            if (boar == null) {
                main.updatePosition(0.0F, 0.0F, 0.0F);
                main.updateRotation(0.0F, 0.0F, 0.0F);
                return;
            }
            // Blocks to model units; the boar and the archer share the same bone space, with forward at -Z.
            main.updatePosition(0.0F,
                    (boar.getRiderBob() + (float) boar.getRiderLeanDrop()) * 16.0F,
                    -(float) boar.getRiderForward() * 16.0F);
            main.updateRotation(boar.getRiderPitch(), 0.0F, boar.getRiderRoll());
        });
    }

    /**
     * Vanilla passenger positioning keeps the whole archer fixed to the boar. Only his upper body
     * twists within the forward firing cone.
     */
    private void aimFromBoar(OgreArcher archer, MudsnoutBoar boar, float partialTick) {
        float aim = Mth.rotLerp(partialTick, archer.yHeadRotO, archer.yHeadRot);
        float facing = Mth.rotLerp(partialTick, boar.yBodyRotO, boar.yBodyRot);
        float twist = Mth.clamp(Mth.wrapDegrees(aim - facing),
                -OgreArcher.MOUNTED_AIM_CONE_DEGREES, OgreArcher.MOUNTED_AIM_CONE_DEGREES);
        // The same sign the head tracking uses: a bone's yaw is the negative of the entity's.
        getBone("body").ifPresent(body -> body.setRotY(body.getRotY() - twist * Mth.DEG_TO_RAD));
    }

    /** His head follows his twisted body; the usual look-at tracking would double the turn. */
    @Override
    protected boolean suppressProceduralHeadTracking(OgreArcher archer) {
        return archer.isMounted();
    }
}
