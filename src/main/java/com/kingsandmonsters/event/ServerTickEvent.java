package com.kingsandmonsters.event;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/** Fired once at the end of every server tick. */
public abstract class ServerTickEvent extends Event {
    private final MinecraftServer server;

    protected ServerTickEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public static class Post extends ServerTickEvent {
        public Post(MinecraftServer server) {
            super(server);
        }
    }
}
