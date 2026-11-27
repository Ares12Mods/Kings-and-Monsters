package com.kingsandmonsters.item;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.kingsandmonsters.ClientConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class OgrebloodTotemItem extends Item {
    public OgrebloodTotemItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!ClientConfig.showCustomTooltips()) return;
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.ogreblood_totem.condition").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.ogreblood_totem.revive")
                .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.ogreblood_totem.effects")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.ogreblood_totem.consumed").withStyle(ChatFormatting.DARK_GRAY));
    }
}
