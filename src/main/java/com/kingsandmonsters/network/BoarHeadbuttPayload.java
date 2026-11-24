package com.kingsandmonsters.network;

import net.minecraft.network.FriendlyByteBuf;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.MudsnoutBoar;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Sent when a rider presses attack, so only a real attack click headbutts and a right-click never does. */
public record BoarHeadbuttPayload() implements CustomPacketPayload {
    public static final Type<BoarHeadbuttPayload> TYPE = new Type<>(
            new ResourceLocation(KingsAndMonsters.MODID, "boar_headbutt"));
    public static final StreamCodec<FriendlyByteBuf, BoarHeadbuttPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {}, buffer -> new BoarHeadbuttPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BoarHeadbuttPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.getControlledVehicle() instanceof MudsnoutBoar boar) {
                boar.requestHeadbutt();
            }
        });
    }
}
