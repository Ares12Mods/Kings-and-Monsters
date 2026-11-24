package com.kingsandmonsters.event;

import net.minecraftforge.eventbus.api.Event;

/** Fired at the end of every client tick. */
public abstract class ClientTickEvent extends Event {
    public static class Post extends ClientTickEvent {
    }
}
