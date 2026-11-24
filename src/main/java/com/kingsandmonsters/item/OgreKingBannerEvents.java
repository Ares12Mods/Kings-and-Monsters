package com.kingsandmonsters.item;

import com.kingsandmonsters.ModBannerPatterns;
import com.kingsandmonsters.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraftforge.event.level.BlockEvent;

/** Breaking a tribe banner gives back the fixed banner item, not a plain banner with a pattern on it. */
public final class OgreKingBannerEvents {
    private OgreKingBannerEvents() {
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer().isCreative() || !(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (!(level.getBlockEntity(event.getPos()) instanceof BannerBlockEntity banner)) {
            return;
        }

        ItemStack replacement = ItemStack.EMPTY;
        ListTag layers = banner.saveWithoutMetadata().getList("Patterns", 10);
        for (int i = 0; i < layers.size() && replacement.isEmpty(); i++) {
            CompoundTag layer = layers.getCompound(i);
            replacement = replacementFor(layer.getString("Pattern"));
        }
        if (replacement.isEmpty()) {
            return;
        }

        // Remove the banner ourselves so only the tribe banner drops, then stop the normal break.
        level.destroyBlock(event.getPos(), false);
        level.addFreshEntity(new ItemEntity(level,
                event.getPos().getX() + 0.5,
                event.getPos().getY() + 0.5,
                event.getPos().getZ() + 0.5,
                replacement));
        event.setCanceled(true);
    }

    private static ItemStack replacementFor(String hashName) {
        if (hashName.equals(ModBannerPatterns.OGRE_BRUTE.get().getHashname())) {
            return new ItemStack(ModItems.OGRE_BRUTE_BANNER.get());
        }
        if (hashName.equals(ModBannerPatterns.GRUNT_CAPTAIN.get().getHashname())) {
            return new ItemStack(ModItems.GRUNT_CAPTAIN_BANNER.get());
        }
        if (hashName.equals(ModBannerPatterns.OGRE_KING.get().getHashname())) {
            return new ItemStack(ModItems.OGRE_KINGS_BANNER.get());
        }
        if (hashName.equals(ModBannerPatterns.OGRE_MAGE.get().getHashname())) {
            return new ItemStack(ModItems.OGRE_MAGE_BANNER.get());
        }
        return ItemStack.EMPTY;
    }
}
