package com.kingsandmonsters.item;

import com.kingsandmonsters.ClientConfig;

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

public class OgreHookbladeItem extends SwordItem {
    public OgreHookbladeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.UNBREAKING)
                || enchantment.is(Enchantments.MENDING)
                || enchantment.is(Enchantments.SHARPNESS)
                || enchantment.is(Enchantments.BANE_OF_ARTHROPODS)
                || enchantment.is(Enchantments.SMITE)
                || enchantment.is(Enchantments.FIRE_ASPECT)
                || enchantment.is(Enchantments.SWEEPING_EDGE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!ClientConfig.showCustomTooltips()) return;
        tooltip.add(Component.literal("Successful hits hook enemies toward you")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal("Applies Slowness I for 1.5 seconds")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal("Pull cooldown: 3 seconds")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("When enchanted with Sweeping Edge, sweeps hook every enemy struck")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Bosses and immovable targets resist the hook")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
