package com.kingsandmonsters.item;

import com.kingsandmonsters.enchantment.ModEnchantmentEffects;
import com.kingsandmonsters.enchantment.ModEnchantments;
import com.kingsandmonsters.entity.OgreSpear;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A trident-style charge-and-release weapon backed by the custom OgreSpear projectile. */
public final class HunterSpearItem extends Item {
    private static final int THROW_CHARGE_TICKS = 10;
    private static final int USE_DURATION_TICKS = 72_000;
    private static final float BASE_THROW_DAMAGE = 8.0F;
    private static final float BASE_THROW_SPEED = 2.5F;

    public HunterSpearItem(Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 7.0,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.9,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player) || getUseDuration(stack, user) - timeLeft < THROW_CHARGE_TICKS) {
            return;
        }

        ItemStack thrownStack = stack.copyWithCount(1);
        thrownStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        if (!level.isClientSide) {
            int heavyThrowLevel = ModEnchantmentEffects.level(level, thrownStack, ModEnchantments.HEAVY_THROW);
            float speed = ModEnchantmentEffects.heavyThrowSpeed(BASE_THROW_SPEED, heavyThrowLevel);
            float damage = ModEnchantmentEffects.heavyThrowDamage(BASE_THROW_DAMAGE, heavyThrowLevel);
            Vec3 origin = player.getEyePosition().add(player.getLookAngle().scale(0.35));
            OgreSpear.Pickup pickup = player.hasInfiniteMaterials()
                    ? OgreSpear.Pickup.CREATIVE_ONLY : OgreSpear.Pickup.ALLOWED;
            OgreSpear spear = new OgreSpear(level, player, thrownStack, origin,
                    player.getLookAngle().scale(speed), damage, pickup);
            level.addFreshEntity(spear);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW,
                player.getSoundSource(), 1.0F, 1.0F);
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION_TICKS;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.UNBREAKING)
                || enchantment.is(Enchantments.MENDING)
                || enchantment.is(Enchantments.LOYALTY)
                || enchantment.is(ModEnchantments.BARBED)
                || enchantment.is(ModEnchantments.HEAVY_THROW);
    }

}

