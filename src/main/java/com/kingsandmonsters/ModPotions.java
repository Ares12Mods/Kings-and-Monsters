package com.kingsandmonsters;

import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.registries.ForgeRegistries;
import com.kingsandmonsters.effect.BogrotMechanics;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;

public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, KingsAndMonsters.MODID);
    public static final RegistryObject<Potion> BOGROT = POTIONS.register("bogrot",
            () -> new Potion("bogrot", BogrotMechanics.standard(45 * 20)));
    public static final RegistryObject<Potion> BOG_SIGHT = POTIONS.register("bog_sight",
            () -> new Potion("bog_sight",
                    new MobEffectInstance(ModMobEffects.BOG_SIGHT.get(), 2 * 60 * 20),
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 2 * 60 * 20)));
    public static final RegistryObject<Potion> LONG_BOG_SIGHT = POTIONS.register("long_bog_sight",
            () -> new Potion("long_bog_sight",
                    new MobEffectInstance(ModMobEffects.BOG_SIGHT.get(), 6 * 60 * 20),
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 6 * 60 * 20)));
    public static final RegistryObject<Potion> STRONG_BOG_SIGHT = POTIONS.register("strong_bog_sight",
            () -> new Potion("strong_bog_sight",
                    new MobEffectInstance(ModMobEffects.BOG_SIGHT.get(), 60 * 20, 1),
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 60 * 20)));
    public static final RegistryObject<Potion> AMPHIBIOUSNESS = POTIONS.register("amphibiousness",
            () -> new Potion("amphibiousness",
                    new MobEffectInstance(ModMobEffects.AMPHIBIOUSNESS.get(), 2 * 60 * 20),
                    new MobEffectInstance(MobEffects.WATER_BREATHING, 2 * 60 * 20)));
    public static final RegistryObject<Potion> LONG_AMPHIBIOUSNESS = POTIONS.register("long_amphibiousness",
            () -> new Potion("long_amphibiousness",
                    new MobEffectInstance(ModMobEffects.AMPHIBIOUSNESS.get(), 6 * 60 * 20),
                    new MobEffectInstance(MobEffects.WATER_BREATHING, 6 * 60 * 20)));
    public static final RegistryObject<Potion> STRONG_AMPHIBIOUSNESS = POTIONS.register("strong_amphibiousness",
            () -> new Potion("strong_amphibiousness",
                    new MobEffectInstance(ModMobEffects.AMPHIBIOUSNESS.get(), 60 * 20, 1),
                    new MobEffectInstance(MobEffects.WATER_BREATHING, 60 * 20)));

    private ModPotions() {}

    /** Adds the brewing steps. Called once during common setup. */
    public static void registerBrewingRecipes() {
        mix(Potions.AWKWARD, ModItems.BOG_TOAD_VENOM.get(), BOGROT);
        mix(Potions.AWKWARD, ModItems.TOAD_EYE.get(), BOG_SIGHT);
        mix(BOG_SIGHT.get(), Items.REDSTONE, LONG_BOG_SIGHT);
        mix(BOG_SIGHT.get(), Items.GLOWSTONE_DUST, STRONG_BOG_SIGHT);
        mix(BOG_SIGHT.get(), Items.PUFFERFISH, AMPHIBIOUSNESS);
        mix(AMPHIBIOUSNESS.get(), Items.REDSTONE, LONG_AMPHIBIOUSNESS);
        mix(AMPHIBIOUSNESS.get(), Items.GLOWSTONE_DUST, STRONG_AMPHIBIOUSNESS);
    }

    private static void mix(Potion from, Item ingredient, RegistryObject<Potion> to) {
        BrewingRecipeRegistry.addRecipe(new IBrewingRecipe() {
            @Override
            public boolean isInput(ItemStack input) {
                return input.is(Items.POTION) && PotionUtils.getPotion(input) == from;
            }

            @Override
            public boolean isIngredient(ItemStack stack) {
                return stack.is(ingredient);
            }

            @Override
            public ItemStack getOutput(ItemStack input, ItemStack stack) {
                if (!isInput(input) || !isIngredient(stack)) {
                    return ItemStack.EMPTY;
                }
                return PotionUtils.setPotion(new ItemStack(Items.POTION), to.get());
            }
        });
    }
}
