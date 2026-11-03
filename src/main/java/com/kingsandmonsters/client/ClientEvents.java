package com.kingsandmonsters.client;

import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.ModBlockEntities;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.client.block.OgreKingsCrownBlockRenderer;
import com.kingsandmonsters.entity.client.OgreArcherRenderer;
import com.kingsandmonsters.entity.client.OgreBruteRenderer;
import com.kingsandmonsters.entity.client.OgreGruntRenderer;
import com.kingsandmonsters.entity.client.OgreGruntCaptainRenderer;
import com.kingsandmonsters.entity.client.OgreLordRenderer;
import com.kingsandmonsters.entity.client.OgreMageRenderer;
import com.kingsandmonsters.entity.client.OgreGuardRenderer;
import com.kingsandmonsters.entity.client.OgreMerchantRenderer;
import com.kingsandmonsters.entity.client.OgreSpearRenderer;
import com.kingsandmonsters.entity.client.BogfumeBoltRenderer;
import com.kingsandmonsters.entity.client.PoisonFogCloudRenderer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.item.ItemProperties;
import com.kingsandmonsters.KingsAndMonsters;
import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.network.PacketDistributor;
import com.kingsandmonsters.network.OpenBackpackPayload;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

public class ClientEvents {
    private static final KeyMapping VIEW_ANGER_HUD = new KeyMapping(
            "key.kingsandmonsters.view_anger_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.kingsandmonsters");
    private static final KeyMapping OPEN_BACKPACK = new KeyMapping(
            "key.kingsandmonsters.open_backpack",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.kingsandmonsters");

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(VIEW_ANGER_HUD);
        event.register(OPEN_BACKPACK);
    }

    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(com.kingsandmonsters.ModMenus.OGRE_MERCHANT_BACKPACK.get(), OgreMerchantBackpackScreen::new);
    }


    public static void registerItemProperties() {
        CuriosRendererRegistry.register(ModItems.OGRE_MERCHANT_BACKPACK.get(), OgreMerchantBackpackRenderer::new);
        ItemProperties.register(
                ModItems.HUNTERS_SPEAR.get(),
                ResourceLocation.withDefaultNamespace("throwing"),
                (stack, level, entity, seed) -> entity != null
                        && entity.isUsingItem()
                        && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.OGRE_KINGS_CROWN.get(), context -> new OgreKingsCrownBlockRenderer());
        event.registerEntityRenderer(ModEntities.OGRE_GRUNT.get(), OgreGruntRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_GRUNT_CAPTAIN.get(), OgreGruntCaptainRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_MAGE.get(), OgreMageRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_BRUTE.get(), OgreBruteRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_ARCHER.get(), OgreArcherRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_GUARD.get(), OgreGuardRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_MERCHANT.get(), OgreMerchantRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_SPEAR.get(), OgreSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_LORD.get(), OgreLordRenderer::new);
        event.registerEntityRenderer(ModEntities.POISON_FOG_CLOUD.get(), PoisonFogCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.BOGFUME_BOLT.get(), BogfumeBoltRenderer::new);
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(OgreMerchantBackpackRenderer.LAYER, OgreMerchantBackpackRenderer::createLayer);
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "anger_hud"),
                new AngerHudLayer());
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "ogre_event_overlay"),
                OgreOverlayState::render);
    }

    public static void registerClientRuntimeEvents() {
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ScreenShakeHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(ScreenShakeHandler::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(GroundRippleRenderer::render);
        NeoForge.EVENT_BUS.addListener(BogEyeGuidance::onClientTick);
        NeoForge.EVENT_BUS.addListener(BogEyeGuidance::render);
        NeoForge.EVENT_BUS.addListener(BossMusicController::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientTooltipEvents::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(ClientTooltipEvents::onEffectScreenTooltip);
        NeoForge.EVENT_BUS.addListener(ClientTooltipEvents::onTooltipColor);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        AngerHudState.setEnabled(VIEW_ANGER_HUD.isDown());
        OgreOverlayState.tick();
        while (OPEN_BACKPACK.consumeClick()) {
            PacketDistributor.sendToServer(new OpenBackpackPayload());
        }
    }
}
