package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreBrute;
import net.minecraft.util.Mth;

public class OgreBruteModel extends OgreModel<OgreBrute> {
    public OgreBruteModel() {
        super("ogre_brute", true);
    }

    @Override
    protected boolean suppressProceduralHeadTracking(OgreBrute brute) {
        return brute.isProceduralHeadTrackingSuppressed();
    }

    @Override
    protected float getMaximumDownwardHeadPitch() {
        // His thick neck still visibly tucked into the torso at 10 degrees. Matched to the King's
        // 6-degree limit, which the screenshot showed is roughly what his proportions need too.
        return 6.0F * Mth.DEG_TO_RAD;
    }

}
