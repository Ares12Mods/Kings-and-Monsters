package com.kingsandmonsters.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** One plated portion of the Haunch Platter. The bowl it was served in comes back empty. */
public final class HaunchPlatterServingItem extends Item {
    public HaunchPlatterServingItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        ItemStack result = super.finishUsingItem(stack, level, consumer);
        return com.kingsandmonsters.item.ItemUtil.hasInfiniteMaterials(consumer) ? result : new ItemStack(Items.BOWL);
    }
}
