package com.kingsandmonsters.network;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.client.QuestDialogueScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Opens (or refreshes) the errand dialogue with the King or a captain. It also carries the player's favor
 * and the line the tribe has to be won past, for the favor bar along the top of the screen.
 */
public record QuestDialoguePayload(int giverId, boolean king, int state, int quest, int progress,
                                  java.util.List<Integer> choices, int favor, int appeasedFavor,
                                  boolean appeased)
        implements CustomPacketPayload {
    public static final Type<QuestDialoguePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "quest_dialogue"));
    private static final StreamCodec<io.netty.buffer.ByteBuf, java.util.List<Integer>> CHOICES =
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list());
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestDialoguePayload> STREAM_CODEC =
            StreamCodec.of(QuestDialoguePayload::write, QuestDialoguePayload::read);

    private static void write(RegistryFriendlyByteBuf buf, QuestDialoguePayload payload) {
        buf.writeVarInt(payload.giverId);
        buf.writeBoolean(payload.king);
        buf.writeVarInt(payload.state);
        buf.writeVarInt(payload.quest);
        buf.writeVarInt(payload.progress);
        CHOICES.encode(buf, payload.choices);
        buf.writeVarInt(payload.favor);
        buf.writeVarInt(payload.appeasedFavor);
        buf.writeBoolean(payload.appeased);
    }

    private static QuestDialoguePayload read(RegistryFriendlyByteBuf buf) {
        int giverId = buf.readVarInt();
        boolean king = buf.readBoolean();
        int state = buf.readVarInt();
        int quest = buf.readVarInt();
        int progress = buf.readVarInt();
        java.util.List<Integer> choices = CHOICES.decode(buf);
        int favor = buf.readVarInt();
        int appeasedFavor = buf.readVarInt();
        boolean appeased = buf.readBoolean();
        return new QuestDialoguePayload(giverId, king, state, quest, progress, choices, favor, appeasedFavor, appeased);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestDialoguePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                QuestDialogueScreen.open(payload);
            }
        });
    }
}
