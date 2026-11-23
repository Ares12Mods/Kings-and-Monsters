package com.kingsandmonsters.network;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.client.BogrotClientSync;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Vanilla only sends mob-effect packets to the afflicted player (and riders), so other clients never
 * learn that a mob has Bogrot. This mirrors the effect to every client tracking the entity.
 * A duration of zero or less means the effect was removed.
 */
public record BogrotSyncPayload(int entityId, int amplifier, int duration) implements CustomPacketPayload {
    public static final Type<BogrotSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "bogrot_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BogrotSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BogrotSyncPayload::entityId,
            ByteBufCodecs.VAR_INT, BogrotSyncPayload::amplifier,
            ByteBufCodecs.VAR_INT, BogrotSyncPayload::duration,
            BogrotSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BogrotSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                BogrotClientSync.apply(payload.entityId(), payload.amplifier(), payload.duration());
            }
        });
    }
}
