package com.kingsandmonsters.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A small read/write pair for packet bodies. Each packet describes its fields once and the same
 * description serves the sending and the receiving side.
 */
public interface StreamCodec<B extends FriendlyByteBuf, T> {
    void encode(B buf, T value);

    T decode(B buf);

    /** Turns this codec into another one, for example {@code VAR_INT.apply(ByteBufCodecs.list())}. */
    default <R> StreamCodec<B, R> apply(Function<StreamCodec<B, T>, StreamCodec<B, R>> wrapper) {
        return wrapper.apply(this);
    }

    static <B extends FriendlyByteBuf, T> StreamCodec<B, T> of(BiConsumer<B, T> encoder, Function<B, T> decoder) {
        return new StreamCodec<>() {
            @Override
            public void encode(B buf, T value) {
                encoder.accept(buf, value);
            }

            @Override
            public T decode(B buf) {
                return decoder.apply(buf);
            }
        };
    }

    interface F3<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    interface F4<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }

    interface F5<A, B, C, D, E, R> {
        R apply(A a, B b, C c, D d, E e);
    }

    interface F6<A, B, C, D, E, F, R> {
        R apply(A a, B b, C c, D d, E e, F f);
    }

    static <B extends FriendlyByteBuf, C, T1> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1, Function<T1, C> make) {
        return of((buf, v) -> c1.encode(buf, g1.apply(v)), buf -> make.apply(c1.decode(buf)));
    }

    static <B extends FriendlyByteBuf, C, T1, T2> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            BiFunction<T1, T2, C> make) {
        return of((buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
        }, buf -> {
            T1 a = c1.decode(buf);
            T2 b = c2.decode(buf);
            return make.apply(a, b);
        });
    }

    static <B extends FriendlyByteBuf, C, T1, T2, T3> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3,
            F3<T1, T2, T3, C> make) {
        return of((buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
        }, buf -> {
            T1 a = c1.decode(buf);
            T2 b = c2.decode(buf);
            T3 c = c3.decode(buf);
            return make.apply(a, b, c);
        });
    }

    static <B extends FriendlyByteBuf, C, T1, T2, T3, T4> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3,
            StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            F4<T1, T2, T3, T4, C> make) {
        return of((buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
            c4.encode(buf, g4.apply(v));
        }, buf -> {
            T1 a = c1.decode(buf);
            T2 b = c2.decode(buf);
            T3 c = c3.decode(buf);
            T4 d = c4.decode(buf);
            return make.apply(a, b, c, d);
        });
    }

    static <B extends FriendlyByteBuf, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3,
            StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            StreamCodec<? super B, T5> c5, Function<C, T5> g5,
            F5<T1, T2, T3, T4, T5, C> make) {
        return of((buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
            c4.encode(buf, g4.apply(v));
            c5.encode(buf, g5.apply(v));
        }, buf -> {
            T1 a = c1.decode(buf);
            T2 b = c2.decode(buf);
            T3 c = c3.decode(buf);
            T4 d = c4.decode(buf);
            T5 e = c5.decode(buf);
            return make.apply(a, b, c, d, e);
        });
    }

    static <B extends FriendlyByteBuf, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3,
            StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            StreamCodec<? super B, T5> c5, Function<C, T5> g5,
            StreamCodec<? super B, T6> c6, Function<C, T6> g6,
            F6<T1, T2, T3, T4, T5, T6, C> make) {
        return of((buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
            c4.encode(buf, g4.apply(v));
            c5.encode(buf, g5.apply(v));
            c6.encode(buf, g6.apply(v));
        }, buf -> {
            T1 a = c1.decode(buf);
            T2 b = c2.decode(buf);
            T3 c = c3.decode(buf);
            T4 d = c4.decode(buf);
            T5 e = c5.decode(buf);
            T6 f = c6.decode(buf);
            return make.apply(a, b, c, d, e, f);
        });
    }
}
