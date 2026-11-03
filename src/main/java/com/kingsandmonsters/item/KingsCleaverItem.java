package com.kingsandmonsters.item;

import com.kingsandmonsters.ClientConfig;
import com.kingsandmonsters.enchantment.ModEnchantments;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class KingsCleaverItem extends SwordItem {
    public KingsCleaverItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.UNBREAKING)
                || enchantment.is(Enchantments.MENDING)
                || enchantment.is(Enchantments.SWEEPING_EDGE)
                || enchantment.is(Enchantments.LOOTING)
                || enchantment.is(Enchantments.KNOCKBACK)
                || enchantment.is(Enchantments.SHARPNESS)
                || enchantment.is(Enchantments.SMITE)
                || enchantment.is(Enchantments.BANE_OF_ARTHROPODS)
                || enchantment.is(ModEnchantments.TYRANT);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!ClientConfig.showCustomTooltips()) return;
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Maw's Feast").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Kills restore 2 hearts").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("3s cooldown").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Maw's Desperation").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("Below half health: Strength I and Speed I")
                .withStyle(ChatFormatting.RED));
    }
}
