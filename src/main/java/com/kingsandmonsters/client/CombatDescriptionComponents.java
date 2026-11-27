package com.kingsandmonsters.client;

import com.kingsandmonsters.Config;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.effect.BleedingEffect;
import com.kingsandmonsters.effect.BogfumeRageEvents;
import com.kingsandmonsters.effect.CrippledEffect;
import com.kingsandmonsters.effect.DazedEffect;
import com.kingsandmonsters.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Builds balance-aware descriptions shared by inventory tooltips and JEI reference pages. */
public final class CombatDescriptionComponents {
    private CombatDescriptionComponents() {}

    public static Component huntersSpear() {
        return Component.translatable("tooltip.kingsandmonsters.hunters_spear");
    }

    public static Component huntersSpearBleed() {
        return Component.translatable("tooltip.kingsandmonsters.hunters_spear.bleed");
    }

    public static Component huntersSpearCripple() {
        double chance = Config.isLoaded() ? Config.HUNTERS_SPEAR_CRIPPLE_CHANCE.get()
                : Config.HUNTERS_SPEAR_CRIPPLE_CHANCE.getDefault();
        return Component.translatable("tooltip.kingsandmonsters.hunters_spear.cripple", number(chance * 100.0));
    }

    @Nullable
    public static Component enchantment(ResourceKey<Enchantment> key, int level) {
        if (key.equals(ModEnchantments.HEAVY_THROW)) {
            return Component.translatable("tooltip.kingsandmonsters.enchantment.heavy_throw",
                    number(Config.HEAVY_THROW_DAMAGE_PER_LEVEL.get()),
                    number(Config.HEAVY_THROW_KNOCKBACK_PER_LEVEL.get()),
                    percent(Config.HEAVY_THROW_SPEED_PENALTY_PER_LEVEL.get()));
        }
        if (key.equals(ModEnchantments.BARBED)) {
            return Component.translatable(level >= 2
                    ? "tooltip.kingsandmonsters.enchantment.barbed.2"
                    : "tooltip.kingsandmonsters.enchantment.barbed.1");
        }
        if (key.equals(ModEnchantments.TYRANT)) {
            return Component.translatable("tooltip.kingsandmonsters.enchantment.tyrant",
                    number(Config.TYRANT_SEARCH_RADIUS.get()));
        }
        if (key.equals(ModEnchantments.BOGBURST)) {
            return Component.translatable(level >= 2
                    ? "tooltip.kingsandmonsters.enchantment.bogburst.2"
                    : "tooltip.kingsandmonsters.enchantment.bogburst.1");
        }
        return null;
    }

    @Nullable
    public static Component enchantmentDetail(ResourceKey<Enchantment> key) {
        if (key.equals(ModEnchantments.TYRANT)) {
            return Component.translatable("tooltip.kingsandmonsters.enchantment.tyrant.detail");
        }
        return null;
    }

    @Nullable
    public static Component effect(Holder<MobEffect> effect) {
        if (effect.equals(ModMobEffects.DAZED)) {
            return Component.translatable("tooltip.kingsandmonsters.effect.dazed",
                    percent(DazedEffect.MOVEMENT_SPEED_REDUCTION),
                    percent(DazedEffect.ATTACK_SPEED_REDUCTION));
        }
        if (effect.equals(ModMobEffects.HAMSTRUNG)) {
            return Component.translatable("tooltip.kingsandmonsters.effect.hamstrung");
        }
        if (effect.equals(ModMobEffects.CRIPPLED)) {
            return Component.translatable("tooltip.kingsandmonsters.effect.crippled",
                    percent(CrippledEffect.MOVEMENT_SPEED_REDUCTION));
        }
        if (effect.equals(ModMobEffects.BOGFUME_RAGE)) {
            return Component.translatable("tooltip.kingsandmonsters.effect.bogfume_rage",
                    percent(BogfumeRageEvents.DAMAGE_DEALT_MULTIPLIER - 1.0F),
                    percent(1.0F - BogfumeRageEvents.DAMAGE_TAKEN_MULTIPLIER));
        }
        if (effect.equals(ModMobEffects.LESSER_BLEEDING)) {
            return bleeding("lesser_bleeding", BleedingEffect.Severity.LESSER);
        }
        if (effect.equals(ModMobEffects.BLEEDING)) {
            return bleeding("bleeding", BleedingEffect.Severity.NORMAL);
        }
        if (effect.equals(ModMobEffects.GREATER_BLEEDING)) {
            return bleeding("greater_bleeding", BleedingEffect.Severity.GREATER);
        }
        return null;
    }

    private static Component bleeding(String id, BleedingEffect.Severity severity) {
        return Component.translatable("tooltip.kingsandmonsters.effect." + id,
                number(severity.damagePerTick),
                seconds(BleedingEffect.DAMAGE_INTERVAL_TICKS),
                seconds(severity.durationTicks),
                percent(severity.healingReduction));
    }

    private static String seconds(int ticks) {
        return number(ticks / 20.0) + "s";
    }

    private static String percent(double fraction) {
        return number(fraction * 100.0) + "%";
    }

    private static String number(double value) {
        // Gameplay constants are often floats, so promoting them to double exposes values such as
        // 1.2000000476837158. Tooltips only use balance values precise to three decimal places.
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }
}
