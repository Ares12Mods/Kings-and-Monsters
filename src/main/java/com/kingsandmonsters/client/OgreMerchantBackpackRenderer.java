package com.kingsandmonsters.client;

import com.kingsandmonsters.KingsAndMonsters;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class OgreMerchantBackpackRenderer implements ICurioRenderer {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "ogre_merchant_backpack"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KingsAndMonsters.MODID, "textures/item/backpack_texture.png");
    private final ModelPart backpack;
    private final ModelPart bagTop;

    public OgreMerchantBackpackRenderer() {
        backpack = Minecraft.getInstance().getEntityModels().bakeLayer(LAYER);
        bagTop = backpack.getChild("bag_top");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bag", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-11F, -13F, 0F, 22F, 19F, 10F, CubeDeformation.NONE),
                PartPose.ZERO);
        root.addOrReplaceChild("bag_top", CubeListBuilder.create()
                        .texOffs(0, 29).addBox(-11F, -7F, 0F, 22F, 7F, 10F, CubeDeformation.NONE)
                        .texOffs(0, 46).addBox(-11F, -2F, 0F, 22F, 2F, 10F, new CubeDeformation(0.1F))
                        .texOffs(0, 58).addBox(-2F, -2F, 10F, 4F, 5F, 2F, new CubeDeformation(0.1F)),
                PartPose.offset(0F, -13F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
            PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffers,
            int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoid)) return;
        poseStack.pushPose();
        ICurioRenderer.translateIfSneaking(poseStack, slotContext.entity());
        humanoid.body.translateAndRotate(poseStack);
        poseStack.translate(0F, 0.90F, 0.15F);
        poseStack.scale(0.75F, 0.75F, 0.75F);
        boolean localWearer = slotContext.entity() == Minecraft.getInstance().player;
        bagTop.xRot = localWearer ? (float) Math.toRadians(BackpackAnimationState.angleDegrees()) : 0F;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        backpack.render(poseStack, consumer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
