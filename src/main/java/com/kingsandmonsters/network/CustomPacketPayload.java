package com.kingsandmonsters.network;

import net.minecraft.resources.ResourceLocation;

/** Marks a class as a packet body. */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {
    }
}
