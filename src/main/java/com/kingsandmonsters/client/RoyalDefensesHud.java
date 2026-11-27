package com.kingsandmonsters.client;

import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;

/** Custom art for the Ogre King's boss bar. Other boss bars, including Royal Trials, stay vanilla. */
public final class RoyalDefensesHud {
    private static final String OGRE_KING_TITLE = "Ogre King";
    private static final ResourceLocation OGRE_KING_EMPTY = new ResourceLocation(
            KingsAndMonsters.MODID, "textures/gui/ogre_king_boss_bar_empty.png");
    private static final ResourceLocation OGRE_KING_FULL = new ResourceLocation(
            KingsAndMonsters.MODID, "textures/gui/ogre_king_boss_bar_full.png");
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
        // The Royal Trials bar is the plain boss bar; only the Ogre King has custom art.
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
