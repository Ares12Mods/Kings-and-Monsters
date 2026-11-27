package com.kingsandmonsters.effect;

import com.kingsandmonsters.item.ItemAttributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** A heavy-impact debuff. Attribute modifiers are synchronized by vanilla. */
public final class DazedEffect extends MobEffect {
    public static final double MOVEMENT_SPEED_REDUCTION = 0.20;
    public static final double ATTACK_SPEED_REDUCTION = 0.15;

    public DazedEffect() {
        super(MobEffectCategory.HARMFUL, 0xD6B46A);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, id("movement_speed"), -MOVEMENT_SPEED_REDUCTION,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_SPEED, id("attack_speed"), -ATTACK_SPEED_REDUCTION,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static String id(String name) {
        return ItemAttributes.uuid("dazed_" + name).toString();
    }
}
