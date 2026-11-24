package com.kingsandmonsters.compat.jei;

import com.kingsandmonsters.ContentDisplayOrder;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.client.CombatDescriptionComponents;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public class KingsAndMonstersJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        // JEI has no mob effect ingredient of its own, so each of the mod's effects is listed as one.
        registration.register(EffectIngredient.TYPE, listedEffects(), new EffectIngredientHelper(),
                new EffectIngredientRenderer());
    }

    // Only effects with their own custom icon are listed for now.
    private static List<EffectIngredient> listedEffects() {
        return List.of(
                new EffectIngredient(ModMobEffects.DAZED.get()),
                new EffectIngredient(ModMobEffects.CRIPPLED.get()),
                new EffectIngredient(ModMobEffects.LESSER_BLEEDING.get()),
                new EffectIngredient(ModMobEffects.BLEEDING.get()),
                new EffectIngredient(ModMobEffects.GREATER_BLEEDING.get()),
                new EffectIngredient(ModMobEffects.BOGROT.get()),
                new EffectIngredient(ModMobEffects.BOGSTRIDE.get()),
                new EffectIngredient(ModMobEffects.CHARGE.get()));
    }

    /** The short line shown on hover and on the effect's info page. */
    static Component effectDescription(EffectIngredient effect) {
        Component computed = CombatDescriptionComponents.effect(effect.effect());
        return computed != null ? computed
                : Component.translatable("jei.kingsandmonsters.effect." + effect.id().getPath() + ".description");
    }

    private static Component effectSource(EffectIngredient effect) {
        return Component.translatable("jei.kingsandmonsters.effect." + effect.id().getPath() + ".source");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (EffectIngredient effect : listedEffects()) {
            registration.addIngredientInfo(effect, EffectIngredient.TYPE,
                    effectDescription(effect), effectSource(effect));
        }

        registration.addItemStackInfo(new ItemStack(ModItems.HUNTERS_SPEAR.get()),
                Component.translatable("jei.kingsandmonsters.hunters_spear.description"),
                CombatDescriptionComponents.huntersSpear());
        registration.addItemStackInfo(new ItemStack(ModItems.KINGS_CLEAVER.get()),
                Component.translatable("jei.kingsandmonsters.kings_cleaver.description"));
        registration.addItemStackInfo(new ItemStack(ModItems.OGRE_KINGS_CROWN.get()),
                Component.translatable("jei.kingsandmonsters.ogre_kings_crown.description"));
        registration.addItemStackInfo(new ItemStack(ModItems.OGRE_KINGS_CLUB.get()),
                Component.translatable("jei.kingsandmonsters.ogre_kings_club.description"));
        registration.addItemStackInfo(new ItemStack(ModItems.OGRE_TOOTH_NECKLACE.get()),
                Component.translatable("jei.kingsandmonsters.ogre_tooth_necklace.description"));
        registration.addItemStackInfo(new ItemStack(ModItems.BRUTE_HEART.get()),
                Component.translatable("jei.kingsandmonsters.brute_heart.description"));
        registration.addItemStackInfo(new ItemStack(ModItems.BOG_IRON_BAND.get()),
                Component.translatable("jei.kingsandmonsters.bog_iron_band.description"));
        registration.addItemStackInfo(new ItemStack(ModItems.BOGFUME_CHARM.get()),
                Component.translatable("jei.kingsandmonsters.bogfume_charm.description"));
        registration.addItemStackInfo(new ItemStack(ModItems.BUCKLER.get()),
                Component.translatable("jei.kingsandmonsters.buckler.description"));

    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        var ingredientManager = jeiRuntime.getIngredientManager();
        List<ItemStack> registeredModStacks = ingredientManager.getAllItemStacks().stream()
                .filter(stack -> KingsAndMonsters.MODID.equals(
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()))
                .toList();

        // JEI normally exposes registry order. Reinsert the public 1.5 content in the same curated
        // order as the Creative tabs; this also omits internal, dev-only, and shelved v2 items.
        ingredientManager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, registeredModStacks);
        ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, ContentDisplayOrder.jeiItems());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(KingsAndMonsters.MODID, path);
    }
}
