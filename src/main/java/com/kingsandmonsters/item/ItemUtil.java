package com.kingsandmonsters.item;

import java.util.List;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Small helpers for item handling that 1.21 had built in. */
public final class ItemUtil {
    private ItemUtil() {}

    /** True for a player in creative mode, who does not use up items. */
    public static boolean hasInfiniteMaterials(LivingEntity entity) {
        return entity instanceof Player player && player.getAbilities().instabuild;
    }

    /** Uses up items from the stack unless the user is in creative mode. */
    public static void consume(ItemStack stack, int amount, LivingEntity user) {
        if (!hasInfiniteMaterials(user)) {
            stack.shrink(amount);
        }
    }

    /** Damages the item, breaking it with the usual animation for the given slot. */
    public static void hurt(ItemStack stack, int amount, LivingEntity user, EquipmentSlot slot) {
        stack.hurtAndBreak(amount, user, broken -> broken.broadcastBreakEvent(slot));
    }

    /** Gives the item lore lines, shown under its name. */
    public static void setLore(ItemStack stack, List<Component> lines) {
        ListTag lore = new ListTag();
        for (Component line : lines) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        stack.getOrCreateTagElement("display").put("Lore", lore);
    }

    public static EquipmentSlot slotForHand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }
}
