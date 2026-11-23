package com.kingsandmonsters.network;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.OgreGrunt;
import com.kingsandmonsters.tribute.QuestManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A button pressed on the errand dialogue: accept, turn in or abandon (see QuestManager). */
public record QuestActionPayload(int giverId, int action, int quest) implements CustomPacketPayload {
    public static final Type<QuestActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "quest_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, QuestActionPayload::giverId,
                    ByteBufCodecs.VAR_INT, QuestActionPayload::action,
                    ByteBufCodecs.VAR_INT, QuestActionPayload::quest,
                    QuestActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.level().getEntity(payload.giverId()) instanceof OgreGrunt giver
                    && payload.action() == QuestManager.END_TALK) {
                giver.endErrandTalk(player);
            } else if (context.player() instanceof ServerPlayer player
                    && player.level().getEntity(payload.giverId()) instanceof OgreGrunt giver
                    && giver.canGiveErrandsTo(player)) {
                if (QuestManager.act(player, giver, payload.action(), payload.quest())) {
                    QuestManager.open(player, giver);
                }
            }
        });
    }
}
