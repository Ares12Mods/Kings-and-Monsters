package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.BogFangs;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EvokerFangsModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class BogFangsRenderer extends EntityRenderer<BogFangs> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(KingsAndMonsters.MODID, "bog_fangs"), "main");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            KingsAndMonsters.MODID, "textures/entity/bogvoker_fang.png");

    private final EvokerFangsModel<BogFangs> model;

    public BogFangsRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new EvokerFangsModel<>(context.bakeLayer(LAYER));
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(0F, 0F, 0F, 10F, 12F, 10F)
                        .texOffs(-12, 0).addBox(-3F, 12F, -3F, 16F, 0F, 16F),
                PartPose.offset(-5F, 24F, -5F));
        CubeListBuilder jaw = CubeListBuilder.create()
                .texOffs(0, 38).addBox(0F, 0F, 0F, 4F, 14F, 8F);
        root.addOrReplaceChild("upper_jaw", jaw, PartPose.offset(1.5F, 24F, -4F));
        root.addOrReplaceChild("lower_jaw", jaw,
                PartPose.offsetAndRotation(-1.5F, 24F, 4F, 0F, (float) Math.PI, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(BogFangs fangs, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        float progress = fangs.getAnimationProgress(partialTick);
        if (progress == 0F) return;

        float scale = 2F;
        if (progress > 0.9F) scale *= (1F - progress) / 0.1F;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90F - fangs.getYRot()));
        poseStack.scale(-scale, -scale, scale);
        poseStack.translate(0D, -0.8D, 0D);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        model.setupAnim(fangs, progress, 0F, 0F, fangs.getYRot(), fangs.getXRot());
        VertexConsumer consumer = buffers.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(fangs, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BogFangs entity) {
        return TEXTURE;
    }
}
