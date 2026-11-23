package com.kingsandmonsters.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * Fresh food: it fills a drumstick more and keeps more of it as saturation. Raw meat from a
 * Skinning Knife kill starts fresh, and the freshness carries into whatever it is cooked or
 * crafted into (see the recipe mixins).
 */
public final class FreshFood {
    private static final int FRESH_NUTRITION = 2;
    private static final float FRESH_SATURATION = 2.0F;
    private static final String FRESH_TAG = "KingsAndMonstersFresh";

    private FreshFood() {}

    public static boolean isFresh(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(FRESH_TAG);
    }

    /** Makes a food fresh: it fills more when eaten, and says so. Anything that is not food is left alone. */
    public static void freshen(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null || isFresh(stack)) {
            return;
        }
        stack.set(DataComponents.FOOD, new FoodProperties(food.nutrition() + FRESH_NUTRITION,
                food.saturation() + FRESH_SATURATION, food.canAlwaysEat(), food.eatSeconds(),
                food.usingConvertsTo(), food.effects()));
        stack.set(DataComponents.CUSTOM_DATA, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .update(tag -> tag.putBoolean(FRESH_TAG, true)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("tooltip.kingsandmonsters.fresh_meat")
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC))));
    }

    /** What a recipe makes is fresh when any food that went into it was. */
    public static ItemStack carry(Iterable<ItemStack> ingredients, ItemStack result) {
        if (!result.has(DataComponents.FOOD) || isFresh(result)) {
            return result;
        }
        for (ItemStack ingredient : ingredients) {
            if (isFresh(ingredient)) {
                freshen(result);
                break;
            }
        }
        return result;
    }
}
