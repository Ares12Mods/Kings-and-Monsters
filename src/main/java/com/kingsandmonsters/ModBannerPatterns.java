package com.kingsandmonsters;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** The four tribe banner patterns. Textures live in {@code textures/entity/banner/<name>.png}. */
public final class ModBannerPatterns {
    public static final DeferredRegister<BannerPattern> BANNER_PATTERNS =
            DeferredRegister.create(Registries.BANNER_PATTERN, KingsAndMonsters.MODID);

    public static final RegistryObject<BannerPattern> OGRE_BRUTE = pattern("ogre_brute", "kmb");
    public static final RegistryObject<BannerPattern> GRUNT_CAPTAIN = pattern("grunt_captain", "kmg");
    public static final RegistryObject<BannerPattern> OGRE_KING = pattern("ogre_king", "kmk");
    public static final RegistryObject<BannerPattern> OGRE_MAGE = pattern("ogre_mage", "kmm");

    private static RegistryObject<BannerPattern> pattern(String name, String hash) {
        return BANNER_PATTERNS.register(name, () -> new BannerPattern(hash));
    }

    private ModBannerPatterns() {
    }
}
