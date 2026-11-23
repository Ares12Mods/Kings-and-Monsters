package com.kingsandmonsters.mixin;

import com.kingsandmonsters.item.FreshFood;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Furnaces, smokers, blast furnaces and campfires all cook through here, so fresh food stays fresh. */
@Mixin(AbstractCookingRecipe.class)
public abstract class AbstractCookingRecipeMixin {
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void kingsandmonsters$carryFreshness(SingleRecipeInput input, HolderLookup.Provider registries,
                                                 CallbackInfoReturnable<ItemStack> result) {
        FreshFood.carry(List.of(input.item()), result.getReturnValue());
    }
}
