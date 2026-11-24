package com.kingsandmonsters.network;

import net.minecraft.network.FriendlyByteBuf;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.OgreCrownArmy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Sent when the wearer of the Ogre King's Crown presses the summon key; the server decides if it works. */
public record SummonArmyPayload() implements CustomPacketPayload {
    public static final Type<SummonArmyPayload> TYPE = new Type<>(
            new ResourceLocation(KingsAndMonsters.MODID, "summon_army"));
    public static final StreamCodec<FriendlyByteBuf, SummonArmyPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {}, buffer -> new SummonArmyPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SummonArmyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                OgreCrownArmy.summon(player);
            }
        });
    }
}
