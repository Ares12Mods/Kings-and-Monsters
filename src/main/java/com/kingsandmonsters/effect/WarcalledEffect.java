package com.kingsandmonsters.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Icon-only marker shown while the Warcaller Bell's passive bonus is active. It grants nothing
 * itself: the +1 Max Heart / +4 Armor is applied and removed by {@code WarcallerBellEvents} in
 * step with this effect, so the icon and the real bonus never disagree.
 */
public final class WarcalledEffect extends MobEffect {
    public WarcalledEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xC9A227);
    }
}
