package com.kingsandmonsters.client;

import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;

/** What the errand tracker shows, and whether it is showing: held open by the key, or briefly after a change. */
public final class QuestTrackerState {
    private static final long SHOW_TICKS_AFTER_CHANGE = 160L;

    private static int quest = -1;
    private static int progress;
    private static boolean king;
    private static boolean held;
    private static long showUntil;
    private static float opacity;

    private QuestTrackerState() {}

    public static void update(int newQuest, int newProgress, boolean newKing) {
        boolean changed = newQuest != quest || newProgress != progress;
        quest = newQuest;
        progress = newProgress;
        king = newKing;
        if (changed && newQuest >= 0) {
            showUntil = now() + SHOW_TICKS_AFTER_CHANGE;
        }
    }

    public static void setHeld(boolean isHeld) {
        held = isHeld;
    }

    public static int quest() { return quest; }
    public static int progress() { return progress; }
    public static boolean king() { return king; }

    /** Moves the fade one step and returns the opacity to draw with. */
    public static float advanceFade() {
        boolean show = quest >= 0 && (held || now() < showUntil);
        opacity = Mth.clamp(opacity + (show ? 0.08F : -0.04F), 0.0F, 1.0F);
        return opacity;
    }

    private static long now() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }
}
