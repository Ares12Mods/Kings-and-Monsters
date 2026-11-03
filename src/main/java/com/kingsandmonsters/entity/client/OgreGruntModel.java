package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreGrunt;
import net.minecraft.util.Mth;

public class OgreGruntModel extends OgreModel<OgreGrunt> {
    public OgreGruntModel() {
        super("ogre_grunt", true);
    }

    @Override
    protected float getMaximumDownwardHeadPitch() {
        return 12.0F * Mth.DEG_TO_RAD;
    }
}
