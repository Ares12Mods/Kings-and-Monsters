package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreGruntCaptain;
import net.minecraft.util.Mth;

public class OgreGruntCaptainModel extends OgreModel<OgreGruntCaptain> {
    public OgreGruntCaptainModel() {
        super("ogre_grunt_captain", true);
    }

    @Override
    protected float getMaximumDownwardHeadPitch() {
        return 12.0F * Mth.DEG_TO_RAD;
    }
}
