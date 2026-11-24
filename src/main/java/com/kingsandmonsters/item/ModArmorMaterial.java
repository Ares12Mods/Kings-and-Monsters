package com.kingsandmonsters.item;

import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/** Armor stats for one set. Durability is set per item at registration, so only defense lives here. */
public record ModArmorMaterial(String name, int helmet, int chestplate, int leggings, int boots,
                               int enchantmentValue, SoundEvent sound, Supplier<Ingredient> repair,
                               float toughness, float knockbackResistance) implements ArmorMaterial {

    private static final int[] BASE_DURABILITY = {13, 15, 16, 11};

    /** The same defense on every piece. */
    public static ModArmorMaterial uniform(String name, int defense, int enchantmentValue, SoundEvent sound,
                                           Supplier<Ingredient> repair, float toughness, float knockbackResistance) {
        return new ModArmorMaterial(name, defense, defense, defense, defense, enchantmentValue, sound, repair,
                toughness, knockbackResistance);
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY[type.ordinal()] * 15;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> helmet;
            case CHESTPLATE -> chestplate;
            case LEGGINGS -> leggings;
            case BOOTS -> boots;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return sound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }

    @Override
    public String getName() {
        return KingsAndMonsters.MODID + ":" + name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }

    /** Vanilla's per-type base durability times a multiplier, for items that set their own. */
    public static int durability(ArmorItem.Type type, int multiplier) {
        return BASE_DURABILITY[type.ordinal()] * multiplier;
    }
}
