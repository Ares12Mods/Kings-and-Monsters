package com.kingsandmonsters.item;

import com.kingsandmonsters.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class IronbellyStewItem extends Item {
    private static final int ONE_MINUTE = 20 * 60;
    private static final int TWO_MINUTES = 20 * 60 * 2;

    public IronbellyStewItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        ItemStack result = super.finishUsingItem(stack, level, consumer);
        if (!level.isClientSide()) {
            consumer.removeEffect(MobEffects.POISON);
            consumer.addEffect(new MobEffectInstance(ModMobEffects.POISON_RESISTANCE.get(), TWO_MINUTES));
            consumer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ONE_MINUTE));
            consumer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, TWO_MINUTES));
        }
        return com.kingsandmonsters.item.ItemUtil.hasInfiniteMaterials(consumer) ? result : new ItemStack(Items.BOWL);
    }

}
