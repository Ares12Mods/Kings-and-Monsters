package com.kingsandmonsters.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect; {@link SwampPotionEvents} supplies swimming and mining bonuses. */
public final class AmphibiousnessEffect extends MobEffect {
    public AmphibiousnessEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3A9E88);
    }
}
