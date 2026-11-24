package com.kingsandmonsters.item;

import com.kingsandmonsters.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class ToadStewItem extends Item {
    public ToadStewItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        ItemStack result = super.finishUsingItem(stack, level, consumer);
        if (!level.isClientSide()) {
            consumer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 15 * 20));
            consumer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60 * 20));
            consumer.addEffect(new MobEffectInstance(ModMobEffects.OGRES_BANE.get(), 5 * 60 * 20));
        }
        return com.kingsandmonsters.item.ItemUtil.hasInfiniteMaterials(consumer) ? result : new ItemStack(Items.BOWL);
    }

}
