package com.kingsandmonsters.client;

import com.kingsandmonsters.effect.CombatEffectSyncEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Puts server-mirrored Dazed and bleeding state on client entities so the particle emitters can read it. */
public final class CombatEffectClientSync {
    private CombatEffectClientSync() {}

    public static void apply(int entityId, int effectIndex, int duration) {
        var level = Minecraft.getInstance().level;
        if (level == null || effectIndex < 0 || effectIndex >= CombatEffectSyncEvents.SYNCED.size()
                || !(level.getEntity(entityId) instanceof LivingEntity living)) return;
        var effect = CombatEffectSyncEvents.SYNCED.get(effectIndex);
        if (duration <= 0) {
            living.removeEffect(effect);
            return;
        }
        MobEffectInstance current = living.getEffect(effect);
        // The client ticks the duration down itself; only replace when the server state has drifted.
        if (current != null && Math.abs(current.getDuration() - duration) <= 3) return;
        living.forceAddEffect(new MobEffectInstance(effect, duration, 0, false, false, true), null);
    }
}
