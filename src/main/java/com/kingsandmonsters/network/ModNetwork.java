package com.kingsandmonsters.network;

import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;

/** The single network channel of the mod and the helpers for sending on it. */
public final class ModNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KingsAndMonsters.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int nextId;

    private ModNetwork() {
    }

    public static void register() {
        toClient(ScreenShakePayload.class, ScreenShakePayload.STREAM_CODEC, ScreenShakePayload::handle);
        toClient(GroundRipplePayload.class, GroundRipplePayload.STREAM_CODEC, GroundRipplePayload::handle);
        toClient(AngerHudPayload.class, AngerHudPayload.STREAM_CODEC, AngerHudPayload::handle);
        toClient(OgreOverlayPayload.class, OgreOverlayPayload.STREAM_CODEC, OgreOverlayPayload::handle);
        toClient(OgrebloodTotemActivationPayload.class,
                OgrebloodTotemActivationPayload.STREAM_CODEC, OgrebloodTotemActivationPayload::handle);
        toClient(BogEyeTargetPayload.class, BogEyeTargetPayload.STREAM_CODEC, BogEyeTargetPayload::handle);
        toClient(BogrotSyncPayload.class, BogrotSyncPayload.STREAM_CODEC, BogrotSyncPayload::handle);
        toClient(CombatEffectSyncPayload.class, CombatEffectSyncPayload.STREAM_CODEC,
                CombatEffectSyncPayload::handle);
        toClient(KingDialoguePayload.class, KingDialoguePayload.STREAM_CODEC, KingDialoguePayload::handle);
        toServer(KingDialogueActionPayload.class, KingDialogueActionPayload.STREAM_CODEC,
                KingDialogueActionPayload::handle);
        toClient(QuestDialoguePayload.class, QuestDialoguePayload.STREAM_CODEC, QuestDialoguePayload::handle);
        toClient(QuestTrackerPayload.class, QuestTrackerPayload.STREAM_CODEC, QuestTrackerPayload::handle);
        toServer(QuestActionPayload.class, QuestActionPayload.STREAM_CODEC, QuestActionPayload::handle);
        toServer(OpenBackpackPayload.class, OpenBackpackPayload.STREAM_CODEC, OpenBackpackPayload::handle);
        toServer(BoarHeadbuttPayload.class, BoarHeadbuttPayload.STREAM_CODEC, BoarHeadbuttPayload::handle);
        toServer(BoarChargePayload.class, BoarChargePayload.STREAM_CODEC, BoarChargePayload::handle);
        toServer(SummonArmyPayload.class, SummonArmyPayload.STREAM_CODEC, SummonArmyPayload::handle);
    }

    private static <T> void toClient(Class<T> type, StreamCodec<FriendlyByteBuf, T> codec,
                                     BiConsumer<T, IPayloadContext> handler) {
        add(type, codec, handler, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static <T> void toServer(Class<T> type, StreamCodec<FriendlyByteBuf, T> codec,
                                     BiConsumer<T, IPayloadContext> handler) {
        add(type, codec, handler, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <T> void add(Class<T> type, StreamCodec<FriendlyByteBuf, T> codec,
                                BiConsumer<T, IPayloadContext> handler, NetworkDirection direction) {
        CHANNEL.registerMessage(nextId++, type, (value, buf) -> codec.encode(buf, value), codec::decode,
                (value, contextSupplier) -> {
                    var context = contextSupplier.get();
                    handler.accept(value, new IPayloadContext(context));
                    context.setPacketHandled(true);
                }, Optional.of(direction));
    }

    public static void sendToPlayer(ServerPlayer player, Object payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public static void sendToServer(Object payload) {
        CHANNEL.sendToServer(payload);
    }

    public static void sendToPlayersNear(ServerLevel level, ServerPlayer excluded, double x, double y, double z,
                                         double radius, Object payload) {
        CHANNEL.send(PacketDistributor.NEAR.with(() ->
                new PacketDistributor.TargetPoint(excluded, x, y, z, radius, level.dimension())), payload);
    }

    public static void sendToPlayersTrackingEntity(Entity entity, Object payload) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), payload);
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, Object payload) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), payload);
    }
}
