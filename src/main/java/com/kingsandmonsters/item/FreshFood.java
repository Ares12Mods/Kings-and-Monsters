package com.kingsandmonsters.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
        return stack.hasTag() && stack.getTag().getBoolean(FRESH_TAG);
    }

    /** Makes a food fresh: it fills more when eaten, and says so. Anything that is not food is left alone. */
    public static void freshen(ItemStack stack) {
        if (!stack.getItem().isEdible() || isFresh(stack)) {
            return;
        }
        stack.getOrCreateTag().putBoolean(FRESH_TAG, true);
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.translatable("tooltip.kingsandmonsters.fresh_meat")
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC))));
        display.put("Lore", lore);
    }

    /** Fresh food tops up the hunger bar when it is finished, on top of what the item gives. */
    @SubscribeEvent
    public static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide() && isFresh(event.getItem())) {
            // Saturation added by eat() is nutrition * modifier * 2.
            player.getFoodData().eat(FRESH_NUTRITION, FRESH_SATURATION / (2.0F * FRESH_NUTRITION));
        }
    }

    /** What a recipe makes is fresh when any food that went into it was. */
    public static ItemStack carry(Iterable<ItemStack> ingredients, ItemStack result) {
        if (!result.getItem().isEdible() || isFresh(result)) {
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
