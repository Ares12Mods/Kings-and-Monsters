package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.BogjawTrap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BogjawTrapRenderer extends GeoEntityRenderer<BogjawTrap> {
    // Degrees per tick the closed trap tumbles end over end while it flies.
    private static final float TUMBLE_SPEED = 32.0F;
    private static final float CENTER_HEIGHT = 0.15F;

    public BogjawTrapRenderer(EntityRendererProvider.Context context) {
        super(context, new BogjawTrapModel());
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(BogjawTrap trap, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        if (trap.getPhase() == BogjawTrap.PHASE_FLYING) {
            poseStack.translate(0.0F, CENTER_HEIGHT, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees((trap.tickCount + partialTick) * TUMBLE_SPEED));
            poseStack.translate(0.0F, -CENTER_HEIGHT, 0.0F);
        }
        super.render(trap, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
