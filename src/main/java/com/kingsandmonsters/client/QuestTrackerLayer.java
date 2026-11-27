package com.kingsandmonsters.client;

import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import com.kingsandmonsters.tribute.OgreQuest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** A small panel with the current errand and how far along it is. */
public final class QuestTrackerLayer implements IGuiOverlay {
    private static final int LEFT = 10;
    private static final int TOP = 10;
    /** Clears the anger HUD, which sits in the same corner. */
    private static final int BELOW_ANGER_HUD = 62;
    private static final int PADDING = 5;
    private static final int GOLD = 0xFFE6C24A;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        float opacity = QuestTrackerState.advanceFade();
        OgreQuest quest = OgreQuest.byOrdinal(QuestTrackerState.quest());
        if (opacity <= 0.01F || quest == null || minecraft.options.hideGui || minecraft.player == null) {
            return;
        }

        boolean done = QuestTrackerState.progress() >= quest.target();
        Component title = Component.translatable("hud.kingsandmonsters.errand");
        Component goal = Component.translatable("quest.kingsandmonsters." + quest.id() + ".goal");
        Component status = done
                ? Component.translatable(QuestTrackerState.king()
                        ? "hud.kingsandmonsters.errand.report_king" : "hud.kingsandmonsters.errand.report_captain")
                : Component.literal(QuestTrackerState.progress() + " / " + quest.target());

        var font = minecraft.font;
        int width = Math.max(font.width(title), Math.max(font.width(goal), font.width(status))) + PADDING * 2;
        int height = font.lineHeight * 3 + PADDING * 2 + 4;
        int y = AngerHudState.isVisible() ? BELOW_ANGER_HUD : TOP;

        graphics.fill(LEFT, y, LEFT + width, y + height, withAlpha(0xAA000000, opacity));
        graphics.fill(LEFT, y, LEFT + 2, y + height, withAlpha(GOLD, opacity));
        int textX = LEFT + PADDING + 1;
        int textY = y + PADDING;
        graphics.drawString(font, title, textX, textY, withAlpha(GOLD, opacity), true);
        graphics.drawString(font, goal, textX, textY + font.lineHeight + 2, withAlpha(0xFFFFFFFF, opacity), true);
        graphics.drawString(font, status, textX, textY + font.lineHeight * 2 + 4,
                withAlpha(done ? GOLD : 0xFFBBBBBB, opacity), true);
    }

    private static int withAlpha(int color, float opacity) {
        int alpha = (int) (((color >>> 24) & 0xFF) * opacity);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
