package com.kingsandmonsters.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Clearly labelled development utility; deliberately has no survival acquisition path. */
public final class CreativeDestroyerItem extends SwordItem {
    public CreativeDestroyerItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Development testing tool").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("Creative attacks instantly kill living targets")
                .withStyle(ChatFormatting.GRAY));
    }
}
