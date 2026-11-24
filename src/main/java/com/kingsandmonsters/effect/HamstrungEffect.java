package com.kingsandmonsters.effect;

import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** A cut tendon: the legs can no longer push off the ground. */
public final class HamstrungEffect extends MobEffect {
    public HamstrungEffect() {
        super(MobEffectCategory.HARMFUL, 0x6B4A3A);
    }
}
