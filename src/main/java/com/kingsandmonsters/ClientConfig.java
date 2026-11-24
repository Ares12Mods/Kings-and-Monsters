package com.kingsandmonsters;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only presentation settings. */
public final class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue SHOW_CUSTOM_TOOLTIPS = BUILDER
            .comment("Show Kings & Monsters custom tooltip text, fonts, colors, and borders. Vanilla item attributes and enchantments remain visible when disabled.")
            .define("showCustomTooltips", true);

    public static final ForgeConfigSpec.BooleanValue BOSS_MUSIC_ENABLED = BUILDER
            .comment("Play the Ogre King's boss fight music. Turn off to hear only the game's normal music and sounds.")
            .define("bossMusicEnabled", true);

    public static final ForgeConfigSpec.BooleanValue USE_CUSTOM_EFFECT_PARTICLES = BUILDER
            .comment("Use Kings & Monsters animated particles for Bleeding, Dazed, and Bogrot. Turn off to use simpler vanilla particles instead.")
            .define("useCustomEffectParticles", true);

    public static final ForgeConfigSpec.BooleanValue SHOW_JADE_FOR_OGRE_KING = BUILDER
            .comment("Show the Jade overlay when looking at the Ogre King. Off by default because the King has its own boss bar.")
            .define("showJadeForOgreKing", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }

    public static boolean bossMusicEnabled() {
        return !SPEC.isLoaded() || BOSS_MUSIC_ENABLED.get();
    }

    public static boolean showCustomTooltips() {
        return !SPEC.isLoaded() || SHOW_CUSTOM_TOOLTIPS.get();
    }

    public static boolean showJadeForOgreKing() {
        return SPEC.isLoaded() && SHOW_JADE_FOR_OGRE_KING.get();
    }

    public static boolean useCustomEffectParticles() {
        return !SPEC.isLoaded() || USE_CUSTOM_EFFECT_PARTICLES.get();
    }
}
