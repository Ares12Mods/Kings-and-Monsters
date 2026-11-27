package com.kingsandmonsters.item;

import com.kingsandmonsters.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A bone-in Mudsnout roast. Eating the meat leaves its oversized bone behind. */
public final class RoastedMudsnoutHaunchItem extends Item {
    public RoastedMudsnoutHaunchItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        ItemStack result = super.finishUsingItem(stack, level, consumer);
        if (ItemUtil.hasInfiniteMaterials(consumer)) return result;

        ItemStack bone = new ItemStack(ModItems.MUDSNOUT_BONE.get());
        if (result.isEmpty()) return bone;

        if (!level.isClientSide()) {
            if (consumer instanceof Player player) {
                if (!player.getInventory().add(bone)) player.drop(bone, false);
            } else {
                consumer.spawnAtLocation(bone);
            }
        }
        return result;
    }
}
