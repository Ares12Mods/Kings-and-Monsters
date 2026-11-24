package com.kingsandmonsters.item;

import org.jetbrains.annotations.Nullable;
import com.kingsandmonsters.ClientConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class GoldenOgreToothItem extends Item {
    public GoldenOgreToothItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!ClientConfig.showCustomTooltips()) return;
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.golden_ogre_tooth.flavor.1")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.golden_ogre_tooth.flavor.2")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }
}
