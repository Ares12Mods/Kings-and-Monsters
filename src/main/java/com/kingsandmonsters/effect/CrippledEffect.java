package com.kingsandmonsters.effect;

import com.kingsandmonsters.item.ItemAttributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Movement debuff; sprint prevention is handled server-side by CombatEffectEvents. */
public final class CrippledEffect extends MobEffect {
    public static final double MOVEMENT_SPEED_REDUCTION = 0.15;

    public CrippledEffect() {
        super(MobEffectCategory.HARMFUL, 0x7A4B32);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ItemAttributes.uuid("crippled_movement_speed").toString(),
                -MOVEMENT_SPEED_REDUCTION, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
