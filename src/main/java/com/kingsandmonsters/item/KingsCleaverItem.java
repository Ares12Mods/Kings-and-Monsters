package com.kingsandmonsters.item;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.kingsandmonsters.ClientConfig;
import com.kingsandmonsters.enchantment.ModEnchantments;

import net.minecraft.ChatFormatting;
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
        super(tier, 3, -2.4F, properties);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == Enchantments.UNBREAKING
                || enchantment == Enchantments.MENDING
                || enchantment == Enchantments.SWEEPING_EDGE
                || enchantment == Enchantments.MOB_LOOTING
                || enchantment == Enchantments.KNOCKBACK
                || enchantment == Enchantments.SHARPNESS
                || enchantment == Enchantments.SMITE
                || enchantment == Enchantments.BANE_OF_ARTHROPODS
                || enchantment == ModEnchantments.TYRANT.get();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
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
