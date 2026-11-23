package com.kingsandmonsters.effect;

import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.network.CombatEffectSyncPayload;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server side of Dazed and bleeding synchronization; see {@link CombatEffectSyncPayload}. */
public final class CombatEffectSyncEvents {
    /** Position in this list is the effect's id on the wire. */
    public static final List<Holder<MobEffect>> SYNCED = List.of(
            ModMobEffects.DAZED, ModMobEffects.LESSER_BLEEDING, ModMobEffects.BLEEDING, ModMobEffects.GREATER_BLEEDING);
    private static final int RESYNC_INTERVAL_TICKS = 20;
    /** Entities that gained one of these effects and should sync on their next tick. */
    private static final IntOpenHashSet DIRTY = new IntOpenHashSet();

    private CombatEffectSyncEvents() {}

    private static int indexOf(MobEffectInstance instance) {
        for (int i = 0; i < SYNCED.size(); i++) {
            if (instance.is(SYNCED.get(i))) return i;
        }
        return -1;
    }

    public static void onEffectAdded(MobEffectEvent.Added event) {
        // Posted before the instance is stored, so the send waits for the entity tick.
        if (!event.getEntity().level().isClientSide() && indexOf(event.getEffectInstance()) >= 0) {
            DIRTY.add(event.getEntity().getId());
        }
    }

    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() != null) end(event.getEntity(), event.getEffectInstance());
    }

    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null) end(event.getEntity(), event.getEffectInstance());
    }

    private static void end(LivingEntity living, MobEffectInstance instance) {
        int index = indexOf(instance);
        if (index >= 0 && !living.level().isClientSide()) send(living, index, 0);
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) return;
        boolean dirty = !DIRTY.isEmpty() && DIRTY.remove(living.getId());
        if (!dirty && living.tickCount % RESYNC_INTERVAL_TICKS != 0) return;
        for (int i = 0; i < SYNCED.size(); i++) {
            MobEffectInstance instance = living.getEffect(SYNCED.get(i));
            if (instance != null) send(living, i, instance.getDuration());
        }
    }

    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity living && event.getEntity() instanceof ServerPlayer viewer) {
            for (int i = 0; i < SYNCED.size(); i++) {
                MobEffectInstance instance = living.getEffect(SYNCED.get(i));
                if (instance != null) {
                    PacketDistributor.sendToPlayer(viewer,
                            new CombatEffectSyncPayload(living.getId(), i, instance.getDuration()));
                }
            }
        }
    }

    private static void send(LivingEntity living, int index, int duration) {
        // Players keep their own effects from vanilla; this reaches everyone else tracking the entity.
        PacketDistributor.sendToPlayersTrackingEntity(living,
                new CombatEffectSyncPayload(living.getId(), index, duration));
    }
}
