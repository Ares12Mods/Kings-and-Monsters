package com.kingsandmonsters.mixin;

import com.kingsandmonsters.item.FreshFood;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** A shapeless food made with a fresh ingredient comes out fresh. */
@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin {
    @Inject(method = "assemble(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void kingsandmonsters$carryFreshness(CraftingContainer input, RegistryAccess registries,
                                                 CallbackInfoReturnable<ItemStack> result) {
        List<ItemStack> items = new ArrayList<>(input.getContainerSize());
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            items.add(input.getItem(slot));
        }
        FreshFood.carry(items, result.getReturnValue());
    }
}
