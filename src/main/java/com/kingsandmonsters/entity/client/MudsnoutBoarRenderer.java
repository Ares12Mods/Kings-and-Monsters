package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.MudsnoutBoar;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class MudsnoutBoarRenderer extends GeoEntityRenderer<MudsnoutBoar> {
    public MudsnoutBoarRenderer(EntityRendererProvider.Context context) {
        super(context, new MudsnoutBoarModel());
        shadowRadius = 1.0F;
    }

    @Override
    public void render(MudsnoutBoar boar, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        // A snoutlet's shadow is a fraction of an adult's.
        shadowRadius = boar.isBaby() ? 0.4F : 1.0F;
        super.render(boar, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
