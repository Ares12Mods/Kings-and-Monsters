package com.kingsandmonsters.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

public final class AngerHudLayer implements LayeredDraw.Layer {
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 7;
    private static final int LEFT_OFFSET = 10;
    private static final int TOP_OFFSET = 11;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!AngerHudState.isVisible()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) {
            return;
        }

        float opacity = AngerHudState.advanceFade();
        if (opacity <= 0.01F) {
            return;
        }

        int anger = AngerHudState.getAngerLevel();
        int max = AngerHudState.getMaxAngerLevel();
        float ratio = Math.min(1.0F, Math.max(0.0F, anger / (float) max));

        int barX = LEFT_OFFSET;
        int barY = TOP_OFFSET + 20;

        int fillColor = withAlpha(angerColor(anger), opacity);

        guiGraphics.fill(barX - 2, barY - 2, barX + BAR_WIDTH + 2, barY + BAR_HEIGHT + 2,
                withAlpha(0xD0000000, opacity));
        guiGraphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT,
                withAlpha(0xFF2B2B2B, opacity));
        int filledWidth = (int) (BAR_WIDTH * ratio);
        if (filledWidth > 0) {
            guiGraphics.fill(barX, barY, barX + filledWidth, barY + BAR_HEIGHT, fillColor);
        }

        Component title = Component.translatable("hud.kingsandmonsters.ogre_faction");
        Component level = Component.translatable(
                "hud.kingsandmonsters.ogre_anger_level",
                Component.translatable(AngerHudState.getAngerTierTranslationKey()),
                anger,
                max);
        int textColor = withAlpha(0xFFFFFFFF, opacity);
        guiGraphics.drawString(minecraft.font, title, LEFT_OFFSET, TOP_OFFSET, textColor);
        guiGraphics.drawString(minecraft.font, level, LEFT_OFFSET, TOP_OFFSET + 10, textColor);
    }

    private static int angerColor(int anger) {
        if (anger <= 0) {
            return 0xFF46B34A;
        }
        return switch (com.kingsandmonsters.api.patrol.PatrolTier.fromAnger(anger)) {
            case SMALL -> 0xFFD1B83F;
            case MEDIUM -> 0xFFF08A24;
            case LARGE -> 0xFFD83A32;
        };
    }

    private static int withAlpha(int color, float opacity) {
        int alpha = (int) (((color >>> 24) & 0xFF) * opacity);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
