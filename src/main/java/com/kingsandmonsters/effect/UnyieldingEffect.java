package com.kingsandmonsters.effect;

import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Temporary full-set surge granted by Bog Iron armor. */
public final class UnyieldingEffect extends MobEffect {
    public UnyieldingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x596A45);
        addAttributeModifier(Attributes.ARMOR, id("armor"), 4.0,
                AttributeModifier.Operation.ADDITION);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS, id("toughness"), 2.0,
                AttributeModifier.Operation.ADDITION);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, id("knockback_resistance"), 0.30,
                AttributeModifier.Operation.ADDITION);
        // Matches vanilla Strength I's three-point melee damage increase.
        addAttributeModifier(Attributes.ATTACK_DAMAGE, id("attack_damage"), 3.0,
                AttributeModifier.Operation.ADDITION);
    }

    private static String id(String name) {
        return com.kingsandmonsters.item.ItemAttributes.uuid("unyielding_" + name).toString();
    }
}
