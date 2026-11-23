package com.kingsandmonsters.network;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.MudsnoutBoar;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent when a rider presses attack, so only a real attack click headbutts and a right-click never does. */
public record BoarHeadbuttPayload() implements CustomPacketPayload {
    public static final Type<BoarHeadbuttPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "boar_headbutt"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BoarHeadbuttPayload> STREAM_CODEC =
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
