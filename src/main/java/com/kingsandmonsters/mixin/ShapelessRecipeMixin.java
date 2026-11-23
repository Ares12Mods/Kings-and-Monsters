package com.kingsandmonsters.mixin;

import com.kingsandmonsters.item.FreshFood;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A shapeless food made with a fresh ingredient, like Toad Stew, comes out fresh. */
@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin {
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void kingsandmonsters$carryFreshness(CraftingInput input, HolderLookup.Provider registries,
                                                 CallbackInfoReturnable<ItemStack> result) {
        FreshFood.carry(input.items(), result.getReturnValue());
    }
}
