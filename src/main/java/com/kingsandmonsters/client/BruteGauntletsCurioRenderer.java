package com.kingsandmonsters.client;

import com.kingsandmonsters.client.armor.BruteGauntletsArmorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Renders both gauntlet bones through GeckoLib while Curios owns the equipment slot. */
public final class BruteGauntletsCurioRenderer implements ICurioRenderer {
    private final BruteGauntletsArmorRenderer renderer = new BruteGauntletsArmorRenderer();

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffers,
            int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoid)) {
            return;
        }

        renderer.prepForRender(slotContext.entity(), stack, EquipmentSlot.CHEST, humanoid, buffers,
                partialTicks, limbSwing, limbSwingAmount, netHeadYaw, headPitch);
        renderer.renderToBuffer(poseStack, null, light, OverlayTexture.NO_OVERLAY, -1);
        renderer.doPostRenderCleanup();
    }
}
