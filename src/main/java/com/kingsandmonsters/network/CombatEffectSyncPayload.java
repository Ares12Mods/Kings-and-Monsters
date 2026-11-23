package com.kingsandmonsters.network;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.client.CombatEffectClientSync;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Vanilla never tells other clients which effects a mob has, so Dazed and the bleeding tiers are
 * mirrored to everyone tracking the entity. A duration of zero or less means the effect ended.
 */
public record CombatEffectSyncPayload(int entityId, int effectIndex, int duration) implements CustomPacketPayload {
    public static final Type<CombatEffectSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "combat_effect_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CombatEffectSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CombatEffectSyncPayload::entityId,
                    ByteBufCodecs.VAR_INT, CombatEffectSyncPayload::effectIndex,
                    ByteBufCodecs.VAR_INT, CombatEffectSyncPayload::duration,
                    CombatEffectSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CombatEffectSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                CombatEffectClientSync.apply(payload.entityId(), payload.effectIndex(), payload.duration());
            }
        });
    }
}
