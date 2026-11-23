package com.kingsandmonsters.network;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.OgreLord;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A button pressed on the Ogre King's dialogue screen. */
public record KingDialogueActionPayload(int kingId, int action) implements CustomPacketPayload {
    public static final int TRADE = 0;
    public static final int CHALLENGE = 1;
    public static final int OFFER_COINS = 2;
    public static final int ERRANDS = 3;
    public static final int DUEL = 4;

    public static final Type<KingDialogueActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "king_dialogue_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KingDialogueActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, KingDialogueActionPayload::kingId,
                    ByteBufCodecs.VAR_INT, KingDialogueActionPayload::action,
                    KingDialogueActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KingDialogueActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.level().getEntity(payload.kingId()) instanceof OgreLord king) {
                king.handleDialogueAction(player, payload.action());
            }
        });
    }
}
