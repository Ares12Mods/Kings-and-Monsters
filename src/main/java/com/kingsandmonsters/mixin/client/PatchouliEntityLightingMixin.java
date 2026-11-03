package com.kingsandmonsters.mixin.client;

import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.book.page.PageEntity;

@Mixin(value = PageEntity.class, remap = false)
public abstract class PatchouliEntityLightingMixin {
    @Inject(method = "renderEntity", at = @At("HEAD"), remap = false)
    private static void kingsandmonsters$useInventoryEntityLighting(
            GuiGraphics graphics,
            Entity entity,
            float x,
            float y,
            float rotation,
            float renderScale,
            float offset,
            CallbackInfo callbackInfo) {
        Lighting.setupForEntityInInventory();
    }

    @Inject(method = "renderEntity", at = @At("RETURN"), remap = false)
    private static void kingsandmonsters$restoreItemLighting(
            GuiGraphics graphics,
            Entity entity,
            float x,
            float y,
            float rotation,
            float renderScale,
            float offset,
            CallbackInfo callbackInfo) {
        Lighting.setupFor3DItems();
    }
}
