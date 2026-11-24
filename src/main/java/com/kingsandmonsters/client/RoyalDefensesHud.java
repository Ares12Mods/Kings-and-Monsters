package com.kingsandmonsters.client;

import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/**
 * Existing rendering for the Royal Trials world-event boss bar.
 *
 * Texture geometry (actual pixels):
 * - Both textures: 192 x 44
 * - Fillable rectangle: x=30, y=31, width=132, height=5
 * - Render offset: centered horizontally at the vanilla boss-bar y position
 * - Event text: centered 10 pixels above the texture
 */
public final class RoyalDefensesHud {
    private static final String ROYAL_DEFENCE_TITLE = "Royal Trials";
    private static final String OGRE_KING_TITLE = "Ogre King";
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            KingsAndMonsters.MODID, "textures/gui/royal_defenses_background.png");
    private static final ResourceLocation PROGRESS = ResourceLocation.fromNamespaceAndPath(
            KingsAndMonsters.MODID, "textures/gui/royal_defenses_progress.png");
    private static final ResourceLocation OGRE_KING_EMPTY = ResourceLocation.fromNamespaceAndPath(
            KingsAndMonsters.MODID, "textures/gui/ogre_king_boss_bar_empty.png");
    private static final ResourceLocation OGRE_KING_FULL = ResourceLocation.fromNamespaceAndPath(
            KingsAndMonsters.MODID, "textures/gui/ogre_king_boss_bar_full.png");
    private static final int TEXTURE_WIDTH = 192;
    private static final int TEXTURE_HEIGHT = 44;
    private static final int FILL_X = 30;
    private static final int FILL_Y = 31;
    private static final int FILL_WIDTH = 132;
    private static final int FILL_HEIGHT = 5;
    private static final int KING_BAR_WIDTH = 256;
    private static final int KING_BAR_HEIGHT = 32;
    private static final int KING_FILL_X = 18;
    private static final int KING_FILL_WIDTH = 220;

    private RoyalDefensesHud() {
    }

    public static void onBossBarRender(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Component name = event.getBossEvent().getName();
        if (name.getString().startsWith(OGRE_KING_TITLE)) {
            renderOgreKingBar(event, name);
            return;
        }
        if (!name.getString().startsWith(ROYAL_DEFENCE_TITLE)) {
            return;
        }

        event.setCanceled(true);
        event.setIncrement(TEXTURE_HEIGHT + 13);

        GuiGraphics graphics = event.getGuiGraphics();
        Minecraft minecraft = Minecraft.getInstance();
        int textureX = (event.getWindow().getGuiScaledWidth() - TEXTURE_WIDTH) / 2;
        int textureY = event.getY();
        int textX = (event.getWindow().getGuiScaledWidth() - minecraft.font.width(name)) / 2;
        graphics.drawString(minecraft.font, name, textX, textureY - 10, 0xFFFFFFFF);
        graphics.blit(BACKGROUND, textureX, textureY, 0, 0,
                TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        int filledWidth = Mth.clamp(
                Math.round(event.getBossEvent().getProgress() * FILL_WIDTH), 0, FILL_WIDTH);
        if (filledWidth > 0) {
            graphics.blit(PROGRESS, textureX + FILL_X, textureY + FILL_Y,
                    FILL_X, FILL_Y, filledWidth, FILL_HEIGHT,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }

    private static void renderOgreKingBar(CustomizeGuiOverlayEvent.BossEventProgress event, Component name) {
        event.setCanceled(true);
        event.setIncrement(KING_BAR_HEIGHT + 2);

        GuiGraphics graphics = event.getGuiGraphics();
        int textureX = (event.getWindow().getGuiScaledWidth() - KING_BAR_WIDTH) / 2;
        int textureY = event.getY() - 8;
        graphics.blit(OGRE_KING_EMPTY, textureX, textureY, 0, 0,
                KING_BAR_WIDTH, KING_BAR_HEIGHT, KING_BAR_WIDTH, KING_BAR_HEIGHT);

        int filledWidth = Mth.clamp(
                Math.round(event.getBossEvent().getProgress() * KING_FILL_WIDTH), 0, KING_FILL_WIDTH);
        if (filledWidth > 0) {
            graphics.blit(OGRE_KING_FULL, textureX + KING_FILL_X, textureY, KING_FILL_X, 0,
                    filledWidth, KING_BAR_HEIGHT, KING_BAR_WIDTH, KING_BAR_HEIGHT);
        }
    }
}
