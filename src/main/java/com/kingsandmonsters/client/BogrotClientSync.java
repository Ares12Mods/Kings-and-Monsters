package com.kingsandmonsters.client;

import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.effect.BogrotMechanics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Applies server-mirrored Bogrot state to client entities so particles and Jade read real effect data. */
public final class BogrotClientSync {
    private BogrotClientSync() {}

    public static void apply(int entityId, int amplifier, int duration) {
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(entityId) instanceof LivingEntity living)) return;
        if (duration <= 0) {
            living.removeEffect(ModMobEffects.BOGROT);
            return;
        }
        MobEffectInstance current = living.getEffect(ModMobEffects.BOGROT);
        // The client ticks the duration down itself; only replace when the server state has drifted.
        if (current != null && current.getAmplifier() == amplifier
                && Math.abs(current.getDuration() - duration) <= 3) return;
        living.forceAddEffect(BogrotMechanics.instance(duration, amplifier), null);
    }
}
