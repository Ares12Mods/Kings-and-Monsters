package com.kingsandmonsters.world;

import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, KingsAndMonsters.MODID);

    public static final RegistryObject<BogToadEggFeature> BOG_TOAD_EGGS =
            FEATURES.register("bog_toad_eggs", () -> new BogToadEggFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<AcornFeature> ACORNS =
            FEATURES.register("acorns", () -> new AcornFeature(NoneFeatureConfiguration.CODEC));

}
