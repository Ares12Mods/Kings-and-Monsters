package com.kingsandmonsters.network;

import net.minecraft.network.FriendlyByteBuf;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.client.BogEyeGuidance;
import net.minecraft.core.BlockPos;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Sent once to the using player only, when a Bog Eye Charm successfully locates a chest. */
public record BogEyeTargetPayload(BlockPos target) implements CustomPacketPayload {
    public static final Type<BogEyeTargetPayload> TYPE = new Type<>(
            new ResourceLocation(KingsAndMonsters.MODID, "bog_eye_target"));
    public static final StreamCodec<FriendlyByteBuf, BogEyeTargetPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, payload -> payload.target().getX(),
            ByteBufCodecs.VAR_INT, payload -> payload.target().getY(),
            ByteBufCodecs.VAR_INT, payload -> payload.target().getZ(),
            (x, y, z) -> new BogEyeTargetPayload(new BlockPos(x, y, z)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BogEyeTargetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                BogEyeGuidance.start(payload.target());
            }
        });
    }
}
