package com.kingsandmonsters.network;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.item.OgreCrownArmy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent when the wearer of the Ogre King's Crown presses the summon key; the server decides if it works. */
public record SummonArmyPayload() implements CustomPacketPayload {
    public static final Type<SummonArmyPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "summon_army"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SummonArmyPayload> STREAM_CODEC =
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
