package com.kingsandmonsters.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/** A plain tool tier. {@code level} follows vanilla: 2 mines what iron does, 3 what diamond does. */
public record ModTier(int level, int uses, float speed, float attackDamageBonus, int enchantmentValue,
                      Supplier<Ingredient> repair) implements Tier {

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return attackDamageBonus;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }
}
