package com.kingsandmonsters.client;

import com.kingsandmonsters.menu.MudsnoutBoarMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Vanilla's horse screen, minus the armor slot and the chest area. */
public class MudsnoutBoarScreen extends AbstractContainerScreen<MudsnoutBoarMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/horse.png");

    private static final ResourceLocation SADDLE_SLOT_SPRITE =
            ResourceLocation.withDefaultNamespace("container/horse/saddle_slot");

    private float mouseX;
    private float mouseY;

    public MudsnoutBoarScreen(MudsnoutBoarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        // The base texture draws the horse's chest area; a boar has none, so paint the panel back over it.
        graphics.fill(x + 78, y + 16, x + 170, y + 72, 0xFFC6C6C6);
        graphics.blitSprite(SADDLE_SLOT_SPRITE, x + 7, y + 17, 18, 18);
        if (menu.getBoar() != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 26, y + 18, x + 78, y + 70, 17,
                    0.25F, this.mouseX, this.mouseY, menu.getBoar());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
