package com.kingsandmonsters.entity.animation;

import com.kingsandmonsters.KingsAndMonsters;

/**
 * Opt-in tracing for the one-shot sync system. Off unless the game is started with
 * {@code -Dkingsandmonsters.animdebug=true}; it only logs when something happens, never per frame.
 */
public final class AnimationDebug {
    private static final boolean ENABLED = Boolean.getBoolean("kingsandmonsters.animdebug");

    private AnimationDebug() {}

    public static void log(String message, Object... args) {
        if (ENABLED) {
            KingsAndMonsters.LOGGER.info("[anim] " + message, args);
        }
    }
}
