package com.kingsandmonsters.client;

import com.kingsandmonsters.api.patrol.PatrolTier;
import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

public final class AngerHudLayer implements LayeredDraw.Layer {
    private static final int LEFT_OFFSET = 10;
    private static final int TOP_OFFSET = 10;
    private static final int HUD_WIDTH = 39;
    private static final int HUD_HEIGHT = 38;
    private static final int ATLAS_HEIGHT = HUD_HEIGHT * 31;
    private static final int FACE_WIDTH = 22;
    private static final int FACE_HEIGHT = 19;
    private static final int FACE_X = LEFT_OFFSET + 5;
    private static final int FACE_Y = TOP_OFFSET + 6;
    private static final ResourceLocation ANGER_ATLAS = texture("anger_atlas_0_to_30");
    private static final ResourceLocation GRUNT_HAPPY = texture("grunt_happy");
    private static final ResourceLocation GRUNT_CAPTAIN = texture("grunt_captain");
    private static final ResourceLocation MAGE = texture("mage");
    private static final ResourceLocation BRUTE = texture("brute");

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
        int frame = Math.clamp(Math.round(anger * 30.0F / max), 0, 30);
        ResourceLocation face = faceForAnger(anger);
        int faceY = FACE_Y - ((face.equals(GRUNT_CAPTAIN) || face.equals(BRUTE)) ? 2 : 0);

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, opacity);
        guiGraphics.blit(ANGER_ATLAS, LEFT_OFFSET, TOP_OFFSET,
                0, frame * HUD_HEIGHT, HUD_WIDTH, HUD_HEIGHT, HUD_WIDTH, ATLAS_HEIGHT);
        guiGraphics.blit(face, FACE_X, faceY, 0, 0, FACE_WIDTH, FACE_HEIGHT, FACE_WIDTH, FACE_HEIGHT);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        String amount = anger + " / " + max;
        int textColor = withAlpha(0xFFFFFFFF, opacity);
        guiGraphics.drawString(minecraft.font, amount,
                LEFT_OFFSET + (HUD_WIDTH - minecraft.font.width(amount)) / 2,
                TOP_OFFSET + HUD_HEIGHT, textColor, true);
    }

    private static ResourceLocation faceForAnger(int anger) {
        if (anger <= 0) return GRUNT_HAPPY;
        return switch (PatrolTier.fromAnger(anger)) {
            case SMALL -> GRUNT_CAPTAIN;
            case MEDIUM -> MAGE;
            case LARGE -> BRUTE;
        };
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                KingsAndMonsters.MODID, "textures/gui/anger_hud/" + name + ".png");
    }

    private static int withAlpha(int color, float opacity) {
        int alpha = (int) (((color >>> 24) & 0xFF) * opacity);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
