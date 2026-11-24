package com.kingsandmonsters.network;

import net.minecraft.network.FriendlyByteBuf;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.OgreMerchantBackpackItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record OpenBackpackPayload() implements CustomPacketPayload {
    public static final Type<OpenBackpackPayload> TYPE = new Type<>(
            new ResourceLocation(KingsAndMonsters.MODID, "open_backpack"));
    public static final StreamCodec<FriendlyByteBuf, OpenBackpackPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {}, buffer -> new OpenBackpackPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(OpenBackpackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                OgreMerchantBackpackItem.openEquipped(player);
            }
        });
    }
}
