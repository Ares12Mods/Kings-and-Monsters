package com.kingsandmonsters.item;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.kingsandmonsters.enchantment.ModEnchantments;
import com.kingsandmonsters.ClientConfig;

import net.minecraft.ChatFormatting;
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
        super(tier, 3, -2.4F, properties);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == Enchantments.UNBREAKING
                || enchantment == Enchantments.MENDING
                || enchantment == Enchantments.SHARPNESS
                || enchantment == Enchantments.BANE_OF_ARTHROPODS
                || enchantment == Enchantments.SMITE
                || enchantment == Enchantments.FIRE_ASPECT
                || enchantment == Enchantments.SWEEPING_EDGE
                || enchantment == ModEnchantments.TYRANT.get();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!ClientConfig.showCustomTooltips()) return;
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.ogre_hookblade.hook")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.ogre_hookblade.slowness")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.ogre_hookblade.cooldown")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.ogre_hookblade.sweeping")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.ogre_hookblade.resistance")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
