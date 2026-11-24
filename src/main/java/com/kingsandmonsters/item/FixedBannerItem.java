package com.kingsandmonsters.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraftforge.registries.RegistryObject;

/** A banner that always comes with one tribe pattern already on it. */
public class FixedBannerItem extends BannerItem {
    private final RegistryObject<BannerPattern> pattern;

    public FixedBannerItem(Block standing, Block wall, Properties properties, RegistryObject<BannerPattern> pattern) {
        super(standing, wall, properties);
        this.pattern = pattern;
    }

    /** A banner that arrives some other way (crafting, loot, commands) still gets its pattern. */
    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level,
                              net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!stack.hasTag() || stack.getTagElement("BlockEntityTag") == null) {
            stack.setTag(getDefaultInstance().getTag());
        }
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        CompoundTag layer = new CompoundTag();
        layer.putString("Pattern", pattern.get().getHashname());
        layer.putInt("Color", DyeColor.WHITE.getId());
        ListTag patterns = new ListTag();
        patterns.add(layer);
        stack.getOrCreateTagElement("BlockEntityTag").put("Patterns", patterns);
        return stack;
    }
}
