package com.kingsandmonsters.item;

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
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!ClientConfig.showCustomTooltips()) return;
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("When held in either hand:").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Saves you at 2 hearts and unleashes an Ogre shockwave")
                .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.literal("Resistance II (0:05), Strength I (0:08), Regeneration II (0:05)")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal("Consumed on use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
