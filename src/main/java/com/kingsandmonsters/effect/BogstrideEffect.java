package com.kingsandmonsters.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Visible marker for the water utility supplied by Boghide Boots. */
public final class BogstrideEffect extends MobEffect {
    public BogstrideEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x6F7F45);
    }
}
