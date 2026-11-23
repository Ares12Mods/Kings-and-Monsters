package com.kingsandmonsters.block.entity;

import com.kingsandmonsters.ModBlockEntities;
import com.kingsandmonsters.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Holds the exact helmet stack so enchantments, damage and lit state survive being placed and picked up. */
public final class CandleHelmetBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ItemStack stack = new ItemStack(ModItems.CANDLE_HELMET.get());

    public CandleHelmetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CANDLE_HELMET.get(), pos, state);
    }

    /** A copy, so callers can edit it freely and write it back with {@link #setStack}. */
    public ItemStack getStack() {
        return stack.copy();
    }

    public void setStack(ItemStack stack) {
        this.stack = stack.copyWithCount(1);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Helmet", stack.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Helmet")) {
            ItemStack loaded = ItemStack.parseOptional(registries, tag.getCompound("Helmet"));
            if (!loaded.isEmpty()) stack = loaded;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
