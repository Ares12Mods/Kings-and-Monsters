package com.kingsandmonsters.effect;

import com.kingsandmonsters.ModMobEffects;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import com.kingsandmonsters.event.PlayerTickEvent;

/** Lightweight per-tick behavior attached to combat effects. */
public final class CombatEffectEvents {
    private CombatEffectEvents() {}

    /** Also blocks bleeding from potions, commands and other mods, not just this mod's attacks. */
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        var effect = event.getEffectInstance().getEffect();
        if ((effect == ModMobEffects.LESSER_BLEEDING.get() || effect == ModMobEffects.BLEEDING.get()
                || effect == ModMobEffects.GREATER_BLEEDING.get()) && CombatEffects.isBleedImmune(event.getEntity())) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
        // Poison Resistance (Ironbelly Stew) shrugs off new poison for its whole duration.
        if (effect == MobEffects.POISON && event.getEntity().hasEffect(ModMobEffects.POISON_RESISTANCE.get())) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    /** A cut tendon: the legs can no longer push off the ground. */
    public static void onLivingJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        var entity = event.getEntity();
        if (entity.hasEffect(ModMobEffects.HAMSTRUNG.get())) {
            var motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, Math.min(motion.y, 0.0), motion.z);
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
        if (player.hasEffect(ModMobEffects.CRIPPLED.get()) && player.isSprinting()) {
            player.setSprinting(false);
        }
    }

}
