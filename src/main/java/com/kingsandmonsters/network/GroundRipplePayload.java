package com.kingsandmonsters.network;

import net.minecraft.network.FriendlyByteBuf;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.client.GroundRippleRenderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Starts one render-only, terrain-conforming ground wave on nearby clients. */
public record GroundRipplePayload(double x, double y, double z, float radius, float propagationSpeed,
                                  int blockDurationTicks, float maxLift)
        implements CustomPacketPayload {
    public static final Type<GroundRipplePayload> TYPE = new Type<>(
            new ResourceLocation(KingsAndMonsters.MODID, "ground_ripple"));
    public static final StreamCodec<FriendlyByteBuf, GroundRipplePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeDouble(payload.x());
                buffer.writeDouble(payload.y());
                buffer.writeDouble(payload.z());
                buffer.writeFloat(payload.radius());
                buffer.writeFloat(payload.propagationSpeed());
                buffer.writeVarInt(payload.blockDurationTicks());
                buffer.writeFloat(payload.maxLift());
            },
            buffer -> new GroundRipplePayload(
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readVarInt(), buffer.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GroundRipplePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                GroundRippleRenderer.start(payload);
            }
        });
    }
}
