package com.kingsandmonsters.network;

import net.minecraft.network.FriendlyByteBuf;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.client.OgrebloodTotemActivation;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

public record OgrebloodTotemActivationPayload() implements CustomPacketPayload {
    public static final Type<OgrebloodTotemActivationPayload> TYPE = new Type<>(
            new ResourceLocation(KingsAndMonsters.MODID, "ogreblood_totem_activation"));
    public static final StreamCodec<FriendlyByteBuf, OgrebloodTotemActivationPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> { }, buffer -> new OgrebloodTotemActivationPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OgrebloodTotemActivationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                OgrebloodTotemActivation.show();
            }
        });
    }
}
