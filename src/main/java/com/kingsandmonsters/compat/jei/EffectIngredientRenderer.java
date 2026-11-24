package com.kingsandmonsters.compat.jei;

import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Draws an effect's own inventory icon, and shows its name and a short description on hover. */
final class EffectIngredientRenderer implements IIngredientRenderer<EffectIngredient> {
    @Override
    public void render(GuiGraphics guiGraphics, EffectIngredient ingredient) {
        TextureAtlasSprite icon = Minecraft.getInstance().getMobEffectTextures().get(ingredient.effect());
        guiGraphics.blit(0, 0, 0, 16, 16, icon);
    }

    @Override
    public List<Component> getTooltip(EffectIngredient ingredient, TooltipFlag tooltipFlag) {
        MobEffect effect = ingredient.effect();
        Component name = effect.getDisplayName().copy().withStyle(effect.getCategory().getTooltipFormatting());
        Component description = KingsAndMonstersJeiPlugin.effectDescription(ingredient)
                .copy().withStyle(ChatFormatting.GRAY);
        return List.of(name, description);
    }
}
