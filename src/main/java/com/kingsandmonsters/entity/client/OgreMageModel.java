package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreMage;
import net.minecraft.util.Mth;

public class OgreMageModel extends OgreModel<OgreMage> {
    public OgreMageModel() {
        super("ogre_mage", true);
    }

    @Override
    protected boolean suppressProceduralHeadTracking(OgreMage mage) {
        return mage.isProceduralHeadTrackingSuppressed();
    }

    @Override
    protected float getMaximumDownwardHeadPitch() {
        return 9.0F * Mth.DEG_TO_RAD;
    }
}
