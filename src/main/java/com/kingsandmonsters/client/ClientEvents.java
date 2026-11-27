package com.kingsandmonsters.client;

import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.ModBlockEntities;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.client.block.OgreKingsCrownBlockRenderer;
import com.kingsandmonsters.entity.client.OgreArcherRenderer;
import com.kingsandmonsters.entity.client.OgreBruteRenderer;
import com.kingsandmonsters.entity.client.OgreTrapperRenderer;
import com.kingsandmonsters.entity.client.BogjawTrapRenderer;
import com.kingsandmonsters.entity.client.OgreGruntRenderer;
import com.kingsandmonsters.entity.client.OgreGruntCaptainRenderer;
import com.kingsandmonsters.entity.client.OgreLordRenderer;
import com.kingsandmonsters.entity.client.OgreMageRenderer;
import com.kingsandmonsters.entity.client.OgreGuardRenderer;
import com.kingsandmonsters.entity.client.OgreMerchantRenderer;
import com.kingsandmonsters.entity.client.OgreSpearRenderer;
import com.kingsandmonsters.entity.client.BogfumeBoltRenderer;
import com.kingsandmonsters.entity.client.BogFangsRenderer;
import com.kingsandmonsters.entity.client.PoisonFogCloudRenderer;
import com.kingsandmonsters.entity.client.BogToadRenderer;
import com.kingsandmonsters.entity.client.BogPoleRenderer;
import com.kingsandmonsters.entity.client.MudsnoutBoarRenderer;
import com.kingsandmonsters.entity.client.PlagueOgreRenderer;
import com.kingsandmonsters.client.particle.BloodDropletParticle;
import com.kingsandmonsters.client.particle.BloodStainParticle;
import com.kingsandmonsters.client.particle.BogrotBodyParticle;
import com.kingsandmonsters.client.particle.BogrotBubbleParticle;
import com.kingsandmonsters.client.particle.BogrotFlyParticle;
import com.kingsandmonsters.client.particle.CandleHelmetFlameParticle;
import com.kingsandmonsters.client.particle.CandleHelmetSmokeParticle;
import com.kingsandmonsters.client.particle.DazedParticle;
import com.kingsandmonsters.client.particle.SleepZParticle;
import com.kingsandmonsters.client.particle.DialogueExclamationParticle;
import com.kingsandmonsters.client.particle.GrazingCrumbParticle;
import com.kingsandmonsters.ModParticles;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import com.kingsandmonsters.event.ClientTickEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.item.ItemProperties;
import com.kingsandmonsters.KingsAndMonsters;
import org.lwjgl.glfw.GLFW;
import com.kingsandmonsters.network.ModNetwork;
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

    private static final KeyMapping SUMMON_ARMY = new KeyMapping(
            "key.kingsandmonsters.summon_army",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.kingsandmonsters");

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(VIEW_ANGER_HUD);
        event.register(OPEN_BACKPACK);
        event.register(SUMMON_ARMY);
    }

    public static void registerMenuScreens() {
        MenuScreens.register(com.kingsandmonsters.ModMenus.OGRE_MERCHANT_BACKPACK.get(), OgreMerchantBackpackScreen::new);
        MenuScreens.register(com.kingsandmonsters.ModMenus.MUDSNOUT_BOAR.get(), MudsnoutBoarScreen::new);
    }

    public static void registerItemProperties() {
        CuriosRendererRegistry.register(ModItems.OGRE_MERCHANT_BACKPACK.get(), OgreMerchantBackpackRenderer::new);
        CuriosRendererRegistry.register(ModItems.BRUTE_GAUNTLETS.get(), BruteGauntletsCurioRenderer::new);
        ItemProperties.register(
                ModItems.HUNTERS_SPEAR.get(),
                new ResourceLocation("throwing"),
                (stack, level, entity, seed) -> entity != null
                        && entity.isUsingItem()
                        && entity.getUseItem() == stack ? 1.0F : 0.0F);
        ItemProperties.register(
                ModItems.BOAR_CALLING_HORN.get(),
                new ResourceLocation("tooting"),
                (stack, level, entity, seed) -> entity != null
                        && entity.isUsingItem()
                        && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.OGRE_KINGS_CROWN.get(), context -> new OgreKingsCrownBlockRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.HAUNCH_PLATTER.get(), context -> new com.kingsandmonsters.client.block.HaunchPlatterBlockRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.CANDLE_HELMET.get(),
                context -> new com.kingsandmonsters.client.block.CandleHelmetBlockRenderer());
        event.registerEntityRenderer(ModEntities.OGRE_GRUNT.get(), OgreGruntRenderer::new);
        event.registerEntityRenderer(ModEntities.PLAGUE_OGRE.get(), PlagueOgreRenderer::new);
        event.registerEntityRenderer(ModEntities.BOG_TOAD.get(), BogToadRenderer::new);
        event.registerEntityRenderer(ModEntities.BOG_POLE.get(), BogPoleRenderer::new);
        event.registerEntityRenderer(ModEntities.MUDSNOUT_BOAR.get(), MudsnoutBoarRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_GRUNT_CAPTAIN.get(), OgreGruntCaptainRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_MAGE.get(), OgreMageRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_BRUTE.get(), OgreBruteRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_TRAPPER.get(), OgreTrapperRenderer::new);
        event.registerEntityRenderer(ModEntities.BOGJAW_TRAP.get(), BogjawTrapRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_ARCHER.get(), OgreArcherRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_GUARD.get(), OgreGuardRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_MERCHANT.get(), OgreMerchantRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_SPEAR.get(), OgreSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.OGRE_LORD.get(), OgreLordRenderer::new);
        event.registerEntityRenderer(ModEntities.POISON_FOG_CLOUD.get(), PoisonFogCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.BOGFUME_BOLT.get(), BogfumeBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.BOG_FANGS.get(), BogFangsRenderer::new);
    }

    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BOGROT_MOTE.get(),
                sprites -> new BogrotBodyParticle.Provider(sprites, BogrotBodyParticle.Kind.MOTE));
        event.registerSpriteSet(ModParticles.BOGROT_FLY.get(), BogrotFlyParticle.Provider::new);
        event.registerSpriteSet(ModParticles.BOGROT_FLIES.get(),
                sprites -> new BogrotBodyParticle.Provider(sprites, BogrotBodyParticle.Kind.FLIES));
        event.registerSpriteSet(ModParticles.BOGROT_BUBBLE.get(), BogrotBubbleParticle.Provider::new);
        event.registerSpriteSet(ModParticles.BLOOD_DROPLET.get(), BloodDropletParticle.Provider::new);
        event.registerSpriteSet(ModParticles.BLOOD_STAIN.get(), BloodStainParticle.Provider::new);
        event.registerSpriteSet(ModParticles.CANDLE_HELMET_FLAME.get(), CandleHelmetFlameParticle.Provider::new);
        event.registerSpriteSet(ModParticles.CANDLE_HELMET_SMOKE.get(), CandleHelmetSmokeParticle.Provider::new);
        event.registerSpriteSet(ModParticles.DAZED.get(), DazedParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SLEEP_Z.get(), SleepZParticle.Provider::new);
        event.registerSpriteSet(ModParticles.DIALOGUE_EXCLAMATION.get(), DialogueExclamationParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GRAZING_CRUMB.get(), GrazingCrumbParticle.Provider::new);
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(OgreMerchantBackpackRenderer.LAYER, OgreMerchantBackpackRenderer::createLayer);
        event.registerLayerDefinition(BogFangsRenderer.LAYER, BogFangsRenderer::createLayer);
    }

    public static void registerGuiLayers(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("anger_hud", new AngerHudLayer());
        event.registerAboveAll("quest_tracker", new QuestTrackerLayer());
        event.registerAboveAll("ogre_event_overlay", OgreOverlayState::render);
    }

    public static void registerClientRuntimeEvents() {
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(CinematicDialogueScreen::onRenderHand);
        MinecraftForge.EVENT_BUS.addListener(CinematicDialogueScreen::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ScreenShakeHandler::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ScreenShakeHandler::onCameraAngles);
        MinecraftForge.EVENT_BUS.addListener(GroundRippleRenderer::render);
        MinecraftForge.EVENT_BUS.addListener(BogEyeGuidance::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(BogEyeGuidance::render);
        MinecraftForge.EVENT_BUS.addListener(BossMusicController::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(RoyalDefensesHud::onBossBarRender);
        MinecraftForge.EVENT_BUS.addListener(BleedingParticleEmitter::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(BleedingParticleEmitter::onLoggingOut);
        MinecraftForge.EVENT_BUS.addListener(BogrotParticleEmitter::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(BogrotParticleEmitter::onLoggingOut);
        MinecraftForge.EVENT_BUS.addListener(DazedParticleEmitter::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(CandleHelmetFlameParticles::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientTooltipEvents::onItemTooltip);
        MinecraftForge.EVENT_BUS.addListener(MudsnoutBoarClient::onRenderLivingPre);
        MinecraftForge.EVENT_BUS.addListener(MudsnoutBoarClient::onInteractionKey);
        MinecraftForge.EVENT_BUS.addListener(MudsnoutBoarClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(MudsnoutBoarClient::onClientTickPre);
        MinecraftForge.EVENT_BUS.addListener(ClientTooltipEvents::onTooltipColor);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        AngerHudState.setEnabled(VIEW_ANGER_HUD.isDown());
        QuestTrackerState.setHeld(VIEW_ANGER_HUD.isDown());
        OgreOverlayState.tick();
        while (OPEN_BACKPACK.consumeClick()) {
            ModNetwork.sendToServer(new OpenBackpackPayload());
        }
        while (SUMMON_ARMY.consumeClick()) {
            // Only worth a packet while the crown is worn; the server checks it again.
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)
                    .is(com.kingsandmonsters.ModItems.OGRE_KINGS_CROWN.get())) {
                ModNetwork.sendToServer(new com.kingsandmonsters.network.SummonArmyPayload());
            }
        }
    }
}
