package com.kingsandmonsters.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A full head of steam from the Blackhide set. The speed comes from the momentum built up to reach
 * it; this marks that the next impact, a ram or a weapon hit, lands like a boar's.
 */
public final class ChargeEffect extends MobEffect {
    public ChargeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3A2E2B);
    }
}
