package com.kingsandmonsters.mixin;

import com.kingsandmonsters.item.FreshFood;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Furnaces, smokers, blast furnaces and campfires all cook through here, so fresh food stays fresh. */
@Mixin(AbstractCookingRecipe.class)
public abstract class AbstractCookingRecipeMixin {
    @Inject(method = "assemble(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void kingsandmonsters$carryFreshness(Container input, RegistryAccess registries,
                                                 CallbackInfoReturnable<ItemStack> result) {
        FreshFood.carry(List.of(input.getItem(0)), result.getReturnValue());
    }
}
