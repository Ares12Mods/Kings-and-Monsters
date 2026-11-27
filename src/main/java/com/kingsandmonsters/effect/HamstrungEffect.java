package com.kingsandmonsters.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** A cut tendon: the legs can no longer push off the ground. */
public final class HamstrungEffect extends MobEffect {
    public HamstrungEffect() {
        super(MobEffectCategory.HARMFUL, 0x6B4A3A);
    }
}
