package com.kingsandmonsters.network;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.client.QuestTrackerState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The player's current errand, for the on-screen tracker. quest is -1 when there is none. */
public record QuestTrackerPayload(int quest, int progress, boolean king) implements CustomPacketPayload {
    public static final QuestTrackerPayload NONE = new QuestTrackerPayload(-1, 0, false);

    public static final Type<QuestTrackerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "quest_tracker"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestTrackerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, QuestTrackerPayload::quest,
                    ByteBufCodecs.VAR_INT, QuestTrackerPayload::progress,
                    ByteBufCodecs.BOOL, QuestTrackerPayload::king,
                    QuestTrackerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestTrackerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                QuestTrackerState.update(payload.quest(), payload.progress(), payload.king());
            }
        });
    }
}
