package com.kingsandmonsters.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Turns the events Forge 1.20.1 fires into the finer grained ones the rest of the code listens for.
 * Register this class on the Forge event bus once.
 */
public final class EventBridge {
    private EventBridge() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftForge.EVENT_BUS.post(new ServerTickEvent.Post(event.getServer()));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftForge.EVENT_BUS.post(new PlayerTickEvent.Post(event.player));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            MinecraftForge.EVENT_BUS.post(new ClientTickEvent.Pre());
        } else {
            MinecraftForge.EVENT_BUS.post(new ClientTickEvent.Post());
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        MinecraftForge.EVENT_BUS.post(new EntityTickEvent.Post(event.getEntity()));
    }

    /** Before armor: the incoming-damage hook. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        LivingIncomingDamageEvent incoming = new LivingIncomingDamageEvent(entity, event.getSource(), event.getAmount());
        if (MinecraftForge.EVENT_BUS.post(incoming)) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(incoming.getAmount());
    }

    /** After armor: first the adjustable pre hook, then the report of what was dealt. */
    @SubscribeEvent
    public static void onLivingDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        LivingDamageEvent.Pre pre = new LivingDamageEvent.Pre(entity, event.getSource(), event.getAmount());
        MinecraftForge.EVENT_BUS.post(pre);
        event.setAmount(pre.getNewDamage());
        MinecraftForge.EVENT_BUS.post(new LivingDamageEvent.Post(entity, event.getSource(), pre.getNewDamage()));
    }
}
