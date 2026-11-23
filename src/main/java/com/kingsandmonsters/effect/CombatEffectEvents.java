package com.kingsandmonsters.effect;

import com.kingsandmonsters.ModMobEffects;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Lightweight per-tick behavior attached to combat effects. */
public final class CombatEffectEvents {
    private CombatEffectEvents() {}

    /** Also blocks bleeding from potions, commands and other mods, not just this mod's attacks. */
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        var effect = event.getEffectInstance().getEffect();
        if ((effect.is(ModMobEffects.LESSER_BLEEDING) || effect.is(ModMobEffects.BLEEDING)
                || effect.is(ModMobEffects.GREATER_BLEEDING)) && CombatEffects.isBleedImmune(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
        // Poison Resistance (Ironbelly Stew) shrugs off new poison for its whole duration.
        if (effect.is(MobEffects.POISON) && event.getEntity().hasEffect(ModMobEffects.POISON_RESISTANCE)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** A bleeding wound keeps most of a heal from landing. */
    public static void onLivingHeal(LivingHealEvent event) {
        float multiplier = CombatEffects.healingMultiplier(event.getEntity());
        if (multiplier < 1.0F) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.hasEffect(ModMobEffects.CRIPPLED) && player.isSprinting()) {
            player.setSprinting(false);
        }
    }

}
