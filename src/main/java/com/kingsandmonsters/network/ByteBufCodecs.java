package com.kingsandmonsters.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** The handful of field codecs the packets use. */
public final class ByteBufCodecs {
    public static final StreamCodec<FriendlyByteBuf, Boolean> BOOL =
            StreamCodec.of(FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean);
    public static final StreamCodec<FriendlyByteBuf, Integer> INT =
            StreamCodec.of(FriendlyByteBuf::writeInt, FriendlyByteBuf::readInt);
    public static final StreamCodec<FriendlyByteBuf, Integer> VAR_INT =
            StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt);
    public static final StreamCodec<FriendlyByteBuf, Float> FLOAT =
            StreamCodec.of(FriendlyByteBuf::writeFloat, FriendlyByteBuf::readFloat);
    public static final StreamCodec<FriendlyByteBuf, Double> DOUBLE =
            StreamCodec.of(FriendlyByteBuf::writeDouble, FriendlyByteBuf::readDouble);

    private ByteBufCodecs() {
    }

    /** A length-prefixed list of whatever the wrapped codec writes. */
    public static <T> Function<StreamCodec<FriendlyByteBuf, T>, StreamCodec<FriendlyByteBuf, List<T>>> list() {
        return element -> StreamCodec.of(
                (buf, list) -> {
                    buf.writeVarInt(list.size());
                    list.forEach(value -> element.encode(buf, value));
                },
                buf -> {
                    int size = buf.readVarInt();
                    List<T> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        list.add(element.decode(buf));
                    }
                    return list;
                });
    }
}
