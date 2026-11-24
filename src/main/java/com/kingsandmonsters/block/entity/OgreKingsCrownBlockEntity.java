package com.kingsandmonsters.block.entity;

import com.kingsandmonsters.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class OgreKingsCrownBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** The crown as it was set down, so breaking the block gives back the same enchanted, worn crown. */
    private net.minecraft.world.item.ItemStack stack = net.minecraft.world.item.ItemStack.EMPTY;

    public OgreKingsCrownBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OGRE_KINGS_CROWN.get(), pos, state);
    }

    public net.minecraft.world.item.ItemStack getStack() {
        return stack.copy();
    }

    public void setStack(net.minecraft.world.item.ItemStack stack) {
        this.stack = stack.copyWithCount(1);
        setChanged();
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        if (!stack.isEmpty()) {
            tag.put("Crown", stack.save(new net.minecraft.nbt.CompoundTag()));
        }
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        stack = tag.contains("Crown") ? net.minecraft.world.item.ItemStack.of(tag.getCompound("Crown"))
                : net.minecraft.world.item.ItemStack.EMPTY;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
