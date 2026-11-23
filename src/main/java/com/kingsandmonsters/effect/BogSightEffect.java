package com.kingsandmonsters.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect; {@link SwampPotionEvents} performs the hostile detection pulse. */
public final class BogSightEffect extends MobEffect {
    public BogSightEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x78A83A);
    }
}
