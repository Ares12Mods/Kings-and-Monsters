package com.kingsandmonsters.network;

import net.minecraft.network.FriendlyByteBuf;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.client.KingDialogueScreen;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Opens the Ogre King's dialogue screen; sent once he has sat down for a visitor. {@code beaten} is true
 * for a player who has won a duel against him, and {@code duelRestSeconds} is how long before they can
 * duel him again (0 when they can).
 */
public record KingDialoguePayload(int kingId, int favor, int appeasedFavor, int tradeFavor, int duelFavor,
                                 boolean appeased, boolean beaten, int duelRestSeconds)
        implements CustomPacketPayload {
    public static final Type<KingDialoguePayload> TYPE = new Type<>(
            new ResourceLocation(KingsAndMonsters.MODID, "king_dialogue"));
    public static final StreamCodec<FriendlyByteBuf, KingDialoguePayload> STREAM_CODEC =
            StreamCodec.of(KingDialoguePayload::write, KingDialoguePayload::read);

    private static void write(FriendlyByteBuf buf, KingDialoguePayload payload) {
        buf.writeVarInt(payload.kingId);
        buf.writeVarInt(payload.favor);
        buf.writeVarInt(payload.appeasedFavor);
        buf.writeVarInt(payload.tradeFavor);
        buf.writeVarInt(payload.duelFavor);
        buf.writeBoolean(payload.appeased);
        buf.writeBoolean(payload.beaten);
        buf.writeVarInt(payload.duelRestSeconds);
    }

    private static KingDialoguePayload read(FriendlyByteBuf buf) {
        return new KingDialoguePayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KingDialoguePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                KingDialogueScreen.open(payload.kingId(), payload.favor(), payload.appeasedFavor(),
                        payload.tradeFavor(), payload.duelFavor(), payload.appeased(), payload.beaten(),
                        payload.duelRestSeconds());
            }
        });
    }
}
