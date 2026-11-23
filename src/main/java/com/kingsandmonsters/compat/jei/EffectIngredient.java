package com.kingsandmonsters.compat.jei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

/** One of this mod's mob effects, listed in JEI so players can look up what each one does. */
record EffectIngredient(Holder<MobEffect> effect) {
    static final IIngredientType<EffectIngredient> TYPE = () -> EffectIngredient.class;

    static final Codec<EffectIngredient> CODEC = ResourceLocation.CODEC.comapFlatMap(
            id -> BuiltInRegistries.MOB_EFFECT.getHolder(id)
                    .map(holder -> DataResult.success(new EffectIngredient(holder)))
                    .orElseGet(() -> DataResult.error(() -> "Unknown mob effect " + id)),
            EffectIngredient::id);

    ResourceLocation id() {
        return effect.unwrapKey().map(ResourceKey::location).orElseThrow();
    }
}
