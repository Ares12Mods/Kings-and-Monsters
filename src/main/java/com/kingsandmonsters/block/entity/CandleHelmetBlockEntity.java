package com.kingsandmonsters.block.entity;

import com.kingsandmonsters.ModBlockEntities;
import com.kingsandmonsters.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
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
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Helmet", stack.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Helmet")) {
            ItemStack loaded = ItemStack.of(tag.getCompound("Helmet"));
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
