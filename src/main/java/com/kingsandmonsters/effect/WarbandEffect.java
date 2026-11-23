package com.kingsandmonsters.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Icon-only timer shown while the Ogre King's Crown army is in the field. It grants nothing itself;
 * its duration matches how long the summoned warband stays.
 */
public final class WarbandEffect extends MobEffect {
    public WarbandEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xD4AF37);
    }
}
