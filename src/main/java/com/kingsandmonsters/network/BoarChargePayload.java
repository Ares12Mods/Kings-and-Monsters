package com.kingsandmonsters.network;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.MudsnoutBoar;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The jump key (space) while riding a boar: pressed starts the charge's wind-up, and released launches
 * it, the longer it was held the farther it goes.
 */
public record BoarChargePayload(boolean pressed) implements CustomPacketPayload {
    public static final Type<BoarChargePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "boar_charge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BoarChargePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, BoarChargePayload::pressed, BoarChargePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BoarChargePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.getControlledVehicle() instanceof MudsnoutBoar boar) {
                if (payload.pressed()) {
                    boar.requestCharge();
                } else {
                    boar.releaseCharge();
                }
            }
        });
    }
}
