package com.kingsandmonsters.client;

import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.client.Minecraft;
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
    private static final int KING_BAR_WIDTH = 196;
    private static final int KING_BAR_HEIGHT = 19;
    private static final int KING_EMPTY_TEXTURE_HEIGHT = 32;
    private static final int KING_FULL_TEXTURE_HEIGHT = 26;
    private static final int KING_EMPTY_V = 13;
    private static final int KING_FULL_V = 7;

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
        event.setIncrement(KING_BAR_HEIGHT + 13);

        GuiGraphics graphics = event.getGuiGraphics();
        Minecraft minecraft = Minecraft.getInstance();
        int textureX = (event.getWindow().getGuiScaledWidth() - KING_BAR_WIDTH) / 2;
        int textureY = event.getY();
        int textX = (event.getWindow().getGuiScaledWidth() - minecraft.font.width(name)) / 2;
        graphics.drawString(minecraft.font, name, textX, textureY - 10, 0xFFFFFFFF);
        graphics.blit(OGRE_KING_EMPTY, textureX, textureY, 0, KING_EMPTY_V,
                KING_BAR_WIDTH, KING_BAR_HEIGHT, KING_BAR_WIDTH, KING_EMPTY_TEXTURE_HEIGHT);

        int filledWidth = Mth.clamp(
                Math.round(event.getBossEvent().getProgress() * KING_BAR_WIDTH), 0, KING_BAR_WIDTH);
        if (filledWidth > 0) {
            graphics.blit(OGRE_KING_FULL, textureX, textureY, 0, KING_FULL_V,
                    filledWidth, KING_BAR_HEIGHT, KING_BAR_WIDTH, KING_FULL_TEXTURE_HEIGHT);
        }
    }
}
