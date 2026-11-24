package com.kingsandmonsters.network;

import net.minecraft.network.FriendlyByteBuf;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.client.CombatEffectClientSync;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Vanilla never tells other clients which effects a mob has, so Dazed and the bleeding tiers are
 * mirrored to everyone tracking the entity. A duration of zero or less means the effect ended.
 */
public record CombatEffectSyncPayload(int entityId, int effectIndex, int duration) implements CustomPacketPayload {
    public static final Type<CombatEffectSyncPayload> TYPE = new Type<>(
            new ResourceLocation(KingsAndMonsters.MODID, "combat_effect_sync"));
    public static final StreamCodec<FriendlyByteBuf, CombatEffectSyncPayload> STREAM_CODEC =
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
