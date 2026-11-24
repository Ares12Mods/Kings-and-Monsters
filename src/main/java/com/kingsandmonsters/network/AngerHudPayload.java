package com.kingsandmonsters.network;

import net.minecraft.network.FriendlyByteBuf;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.client.AngerHudState;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Sent server->client with the anger level of the nearest tribute camp to the player.
 * angerLevel of -1 means no camp is currently in range — the client should hide the HUD.
 */
public record AngerHudPayload(int angerLevel, int maxAngerLevel, boolean chiefAlive) implements CustomPacketPayload {
    public static final Type<AngerHudPayload> TYPE = new Type<>(
            new ResourceLocation(KingsAndMonsters.MODID, "anger_hud"));
    public static final StreamCodec<FriendlyByteBuf, AngerHudPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            AngerHudPayload::angerLevel,
            ByteBufCodecs.INT,
            AngerHudPayload::maxAngerLevel,
            ByteBufCodecs.BOOL,
            AngerHudPayload::chiefAlive,
            AngerHudPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AngerHudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                AngerHudState.update(payload.angerLevel(), payload.maxAngerLevel(), payload.chiefAlive());
            }
        });
    }
}
