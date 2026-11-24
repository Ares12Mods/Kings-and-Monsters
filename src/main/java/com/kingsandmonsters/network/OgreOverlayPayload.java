package com.kingsandmonsters.network;

import net.minecraft.util.Mth;
import net.minecraft.network.FriendlyByteBuf;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.client.OgreOverlayState;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Server-authored, client-rendered dramatic Ogre notification. */
public record OgreOverlayPayload(
        String title,
        String subtitle,
        int fadeInTicks,
        int holdTicks,
        int fadeOutTicks,
        float titleScale,
        float subtitleScale,
        Emphasis emphasis) implements CustomPacketPayload {

    public enum Emphasis { MAJOR_EVENT, LOCATION_DISCOVERY }

    public static final Type<OgreOverlayPayload> TYPE = new Type<>(
            new ResourceLocation(KingsAndMonsters.MODID, "ogre_overlay"));
    public static final StreamCodec<FriendlyByteBuf, OgreOverlayPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.title, 128);
                buffer.writeUtf(payload.subtitle, 128);
                buffer.writeVarInt(payload.fadeInTicks);
                buffer.writeVarInt(payload.holdTicks);
                buffer.writeVarInt(payload.fadeOutTicks);
                buffer.writeFloat(payload.titleScale);
                buffer.writeFloat(payload.subtitleScale);
                buffer.writeByte(payload.emphasis.ordinal());
            },
            buffer -> new OgreOverlayPayload(
                    buffer.readUtf(128),
                    buffer.readUtf(128),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    Emphasis.values()[Mth.clamp(buffer.readUnsignedByte(), 0, Emphasis.values().length - 1)]));

    public static OgreOverlayPayload major(String title, String subtitle) {
        return new OgreOverlayPayload(title, subtitle, 10, 50, 20, 2.0F, 1.0F, Emphasis.MAJOR_EVENT);
    }

    public static OgreOverlayPayload location(String title, String subtitle, boolean fort) {
        return new OgreOverlayPayload(title, subtitle, 10, fort ? 60 : 50, 20,
                1.0F, 1.0F, Emphasis.LOCATION_DISCOVERY);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OgreOverlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                OgreOverlayState.show(payload);
            }
        });
    }
}
