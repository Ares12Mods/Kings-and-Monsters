package com.kingsandmonsters.compat.jei;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

final class EffectIngredientHelper implements IIngredientHelper<EffectIngredient> {
    @Override
    public IIngredientType<EffectIngredient> getIngredientType() {
        return EffectIngredient.TYPE;
    }

    @Override
    public String getDisplayName(EffectIngredient ingredient) {
        return ingredient.effect().value().getDisplayName().getString();
    }

    @Override
    @SuppressWarnings("removal")
    public String getUniqueId(EffectIngredient ingredient, UidContext context) {
        return ingredient.id().toString();
    }

    @Override
    public Object getUid(EffectIngredient ingredient, UidContext context) {
        return ingredient.id().toString();
    }

    @Override
    public ResourceLocation getResourceLocation(EffectIngredient ingredient) {
        return ingredient.id();
    }

    @Override
    public EffectIngredient copyIngredient(EffectIngredient ingredient) {
        return ingredient;
    }

    @Override
    public String getErrorInfo(@Nullable EffectIngredient ingredient) {
        return ingredient == null ? "null effect" : ingredient.id().toString();
    }
}
