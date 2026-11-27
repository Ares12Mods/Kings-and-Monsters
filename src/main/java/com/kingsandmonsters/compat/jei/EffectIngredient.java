package com.kingsandmonsters.compat.jei;

import net.minecraftforge.registries.ForgeRegistries;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

/** One of this mod's mob effects, listed in JEI so players can look up what each one does. */
record EffectIngredient(MobEffect effect) {
    static final IIngredientType<EffectIngredient> TYPE = () -> EffectIngredient.class;

    ResourceLocation id() {
        return ForgeRegistries.MOB_EFFECTS.getKey(effect);
    }
}
