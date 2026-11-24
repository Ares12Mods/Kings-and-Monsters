package com.kingsandmonsters.network;

import net.minecraft.network.FriendlyByteBuf;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.client.QuestTrackerState;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** The player's current errand, for the on-screen tracker. quest is -1 when there is none. */
public record QuestTrackerPayload(int quest, int progress, boolean king) implements CustomPacketPayload {
    public static final QuestTrackerPayload NONE = new QuestTrackerPayload(-1, 0, false);

    public static final Type<QuestTrackerPayload> TYPE = new Type<>(
            new ResourceLocation(KingsAndMonsters.MODID, "quest_tracker"));
    public static final StreamCodec<FriendlyByteBuf, QuestTrackerPayload> STREAM_CODEC =
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
