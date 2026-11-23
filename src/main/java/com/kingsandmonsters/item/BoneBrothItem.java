package com.kingsandmonsters.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class BoneBrothItem extends Item {
    public BoneBrothItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        ItemStack result = super.finishUsingItem(stack, level, consumer);
        if (!level.isClientSide()) {
            consumer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 8 * 20));
        }
        return consumer.hasInfiniteMaterials() ? result : new ItemStack(Items.BOWL);
    }

}
