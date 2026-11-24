package com.kingsandmonsters.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

/** Fired at the end of every player tick, on both sides. */
public abstract class PlayerTickEvent extends Event {
    private final Player player;

    protected PlayerTickEvent(Player player) {
        this.player = player;
    }

    public Player getEntity() {
        return player;
    }

    public static class Post extends PlayerTickEvent {
        public Post(Player player) {
            super(player);
        }
    }
}
