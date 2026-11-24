package com.kingsandmonsters.network;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

/** What a packet handler can do with the connection it arrived on. */
public final class IPayloadContext {
    private final NetworkEvent.Context context;

    IPayloadContext(NetworkEvent.Context context) {
        this.context = context;
    }

    /** Runs the work on the game thread. */
    public void enqueueWork(Runnable work) {
        context.enqueueWork(work);
    }

    /** The sending player when this arrived on the server; on the client, the local player. */
    public Player player() {
        if (context.getSender() != null) {
            return context.getSender();
        }
        return FMLEnvironment.dist.isClient() ? ClientPlayerAccess.get() : null;
    }
}
