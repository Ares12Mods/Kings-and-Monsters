package com.kingsandmonsters.effect;

import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.network.BogrotSyncPayload;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server side of Bogrot synchronization; see {@link BogrotSyncPayload}. */
public final class BogrotSyncEvents {
    private static final int RESYNC_INTERVAL_TICKS = 20;
    /** Entities whose Bogrot was just applied or refreshed and should sync on their next tick. */
    private static final IntOpenHashSet DIRTY = new IntOpenHashSet();

    private BogrotSyncEvents() {}

    public static void onEffectAdded(MobEffectEvent.Added event) {
        // Posted before the instance is stored, so defer the actual send to the entity tick.
        if (!event.getEntity().level().isClientSide() && event.getEffectInstance().is(ModMobEffects.BOGROT)) {
            DIRTY.add(event.getEntity().getId());
        }
    }

    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().is(ModMobEffects.BOGROT)) {
            send(event.getEntity(), 0, 0);
        }
    }

    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().is(ModMobEffects.BOGROT)) {
            send(event.getEntity(), 0, 0);
        }
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) return;
        var bogrot = living.getEffect(ModMobEffects.BOGROT);
        if (bogrot == null) return;
        boolean dirty = !DIRTY.isEmpty() && DIRTY.remove(living.getId());
        if (dirty || living.tickCount % RESYNC_INTERVAL_TICKS == 0) {
            send(living, bogrot.getAmplifier(), bogrot.getDuration());
        }
    }

    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity living && event.getEntity() instanceof ServerPlayer viewer) {
            var bogrot = living.getEffect(ModMobEffects.BOGROT);
            if (bogrot != null) {
                PacketDistributor.sendToPlayer(viewer,
                        new BogrotSyncPayload(living.getId(), bogrot.getAmplifier(), bogrot.getDuration()));
            }
        }
    }

    private static void send(LivingEntity living, int amplifier, int duration) {
        if (living.level().isClientSide()) return;
        // Players already receive their own effects from vanilla; this reaches everyone else tracking.
        PacketDistributor.sendToPlayersTrackingEntity(living,
                new BogrotSyncPayload(living.getId(), amplifier, duration));
    }
}
