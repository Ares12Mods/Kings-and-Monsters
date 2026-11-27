package com.kingsandmonsters;

import net.minecraft.world.effect.MobEffects;
import com.kingsandmonsters.effect.BogrotMechanics;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, KingsAndMonsters.MODID);
    public static final Holder<Potion> BOGROT = POTIONS.register("bogrot",
            () -> new Potion("bogrot", BogrotMechanics.standard(45 * 20)));
    public static final Holder<Potion> BOG_SIGHT = POTIONS.register("bog_sight",
            () -> new Potion("bog_sight",
                    new MobEffectInstance(ModMobEffects.BOG_SIGHT, 2 * 60 * 20),
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 2 * 60 * 20)));
    public static final Holder<Potion> LONG_BOG_SIGHT = POTIONS.register("long_bog_sight",
            () -> new Potion("long_bog_sight",
                    new MobEffectInstance(ModMobEffects.BOG_SIGHT, 6 * 60 * 20),
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 6 * 60 * 20)));
    public static final Holder<Potion> STRONG_BOG_SIGHT = POTIONS.register("strong_bog_sight",
            () -> new Potion("strong_bog_sight",
                    new MobEffectInstance(ModMobEffects.BOG_SIGHT, 60 * 20, 1),
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 60 * 20)));
    public static final Holder<Potion> AMPHIBIOUSNESS = POTIONS.register("amphibiousness",
            () -> new Potion("amphibiousness",
                    new MobEffectInstance(ModMobEffects.AMPHIBIOUSNESS, 2 * 60 * 20),
                    new MobEffectInstance(MobEffects.WATER_BREATHING, 2 * 60 * 20)));
    public static final Holder<Potion> LONG_AMPHIBIOUSNESS = POTIONS.register("long_amphibiousness",
            () -> new Potion("long_amphibiousness",
                    new MobEffectInstance(ModMobEffects.AMPHIBIOUSNESS, 6 * 60 * 20),
                    new MobEffectInstance(MobEffects.WATER_BREATHING, 6 * 60 * 20)));
    public static final Holder<Potion> STRONG_AMPHIBIOUSNESS = POTIONS.register("strong_amphibiousness",
            () -> new Potion("strong_amphibiousness",
                    new MobEffectInstance(ModMobEffects.AMPHIBIOUSNESS, 60 * 20, 1),
                    new MobEffectInstance(MobEffects.WATER_BREATHING, 60 * 20)));

    private ModPotions() {}

    public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addMix(Potions.AWKWARD, ModItems.BOG_TOAD_VENOM.get(), BOGROT);
        event.getBuilder().addMix(Potions.AWKWARD, ModItems.TOAD_EYE.get(), BOG_SIGHT);
        event.getBuilder().addMix(BOG_SIGHT, Items.REDSTONE, LONG_BOG_SIGHT);
        event.getBuilder().addMix(BOG_SIGHT, Items.GLOWSTONE_DUST, STRONG_BOG_SIGHT);
        event.getBuilder().addMix(BOG_SIGHT, Items.PUFFERFISH, AMPHIBIOUSNESS);
        event.getBuilder().addMix(AMPHIBIOUSNESS, Items.REDSTONE, LONG_AMPHIBIOUSNESS);
        event.getBuilder().addMix(AMPHIBIOUSNESS, Items.GLOWSTONE_DUST, STRONG_AMPHIBIOUSNESS);
    }
}
