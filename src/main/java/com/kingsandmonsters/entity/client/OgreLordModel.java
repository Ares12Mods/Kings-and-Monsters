package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreLord;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;

public class OgreLordModel extends OgreModel<OgreLord> {
    public OgreLordModel() {
        super("ogre_lord", true);
    }

    @Override
    public void setCustomAnimations(OgreLord lord, long instanceId, AnimationState<OgreLord> animationState) {
        super.setCustomAnimations(lord, instanceId, animationState);

        boolean clubInHand = lord.isClubInHand();
        getBone("club_hand").ifPresent(bone -> bone.setHidden(!clubInHand));
        getBone("club_back").ifPresent(bone -> bone.setHidden(clubInHand));
    }

    @Override
    protected boolean suppressProceduralHeadTracking(OgreLord lord) {
        return lord.isProceduralHeadTrackingSuppressed();
    }

    @Override
    protected float getMaximumDownwardHeadPitch() {
        // His height makes even a small downward tilt read as an exaggerated tuck, so this stays
        // tighter than the shared base limit, similar to the Brute.
        return 6.0F * Mth.DEG_TO_RAD;
    }
}
