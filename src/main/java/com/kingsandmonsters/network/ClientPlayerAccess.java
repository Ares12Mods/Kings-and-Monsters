package com.kingsandmonsters.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Keeps the client-only class reference out of the shared packet code. */
final class ClientPlayerAccess {
    private ClientPlayerAccess() {
    }

    static Player get() {
        return Minecraft.getInstance().player;
    }
}
