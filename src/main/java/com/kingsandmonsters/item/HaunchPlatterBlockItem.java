package com.kingsandmonsters.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Advertises the platter's serving food values to HUD mods while keeping the whole platter
 * placeable-only. Its food is taken from the placed block with a bowl, never eaten from the hand.
 */
public final class HaunchPlatterBlockItem extends BlockItem {
    public HaunchPlatterBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
