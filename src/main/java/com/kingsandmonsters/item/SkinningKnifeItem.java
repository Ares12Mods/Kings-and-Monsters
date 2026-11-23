package com.kingsandmonsters.item;

import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.effect.BleedingEffect;
import com.kingsandmonsters.effect.CombatEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public final class SkinningKnifeItem extends SwordItem {
    private static final String HAMSTRING_COOLDOWN_TAG = "KingsAndMonstersSkinningKnifeCooldown";
    private static final int HAMSTRING_TICKS = 3 * 20;

    public SkinningKnifeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        applyKnifeCut(target, attacker);
        return super.hurtEnemy(stack, target, attacker);
    }

    /**
     * The trapper's cut: it slows the target, keeps it from jumping away and opens a bleeding wound,
     * then needs a few seconds before it cuts the same target that deep again. The Trapper's own
     * knife uses this too.
     */
    public static void applyKnifeCut(LivingEntity target, LivingEntity attacker) {
        long gameTime = target.level().getGameTime();
        if (gameTime >= target.getPersistentData().getLong(HAMSTRING_COOLDOWN_TAG)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HAMSTRING_TICKS, 1), attacker);
            target.addEffect(new MobEffectInstance(ModMobEffects.HAMSTRUNG, HAMSTRING_TICKS, 0, false, false, true), attacker);
            CombatEffects.applyBleeding(target, BleedingEffect.Severity.NORMAL, attacker);
            target.getPersistentData().putLong(HAMSTRING_COOLDOWN_TAG, gameTime + 4 * 20);
        }
    }

}
