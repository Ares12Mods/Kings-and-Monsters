package com.kingsandmonsters;

import com.kingsandmonsters.command.TestingCommands;
import com.kingsandmonsters.compat.spell_power.SpellPowerCompat;
import com.kingsandmonsters.effect.BogrotSyncEvents;
import com.kingsandmonsters.effect.CombatEffectSyncEvents;
import com.kingsandmonsters.effect.SwampPotionEvents;
import com.kingsandmonsters.enchantment.ModEnchantments;
import com.kingsandmonsters.entity.OgreLord;
import com.kingsandmonsters.entity.OgreRallyEvents;
import com.kingsandmonsters.entity.OgreVanillaHostilityEvents;
import com.kingsandmonsters.entity.RippleJumpTracker;
import com.kingsandmonsters.event.EventBridge;
import com.kingsandmonsters.item.BlackhideEvents;
import com.kingsandmonsters.item.BogIronEquipmentEvents;
import com.kingsandmonsters.item.BogToadContentEvents;
import com.kingsandmonsters.item.BogsteelChestplateEvents;
import com.kingsandmonsters.item.BoneClubEvents;
import com.kingsandmonsters.item.FreshFood;
import com.kingsandmonsters.item.FreshMeatEvents;
import com.kingsandmonsters.item.ItemAttributes;
import com.kingsandmonsters.item.MudsnoutRecallTracker;
import com.kingsandmonsters.item.OgreCrownArmy;
import com.kingsandmonsters.item.OgreGearEvents;
import com.kingsandmonsters.light.CandleHelmetLight;
import com.kingsandmonsters.tribute.QuestManager;
import com.kingsandmonsters.tribute.StructureDiscoveryManager;
import com.kingsandmonsters.world.WorldgenSavedDataQueue;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraftforge.eventbus.api.EventPriority;
import com.kingsandmonsters.advancement.RoyalAdvancementEvents;
import com.kingsandmonsters.client.ClientEvents;
import com.kingsandmonsters.compat.irons_spellbooks.IronsSpellbooksCompat;
import com.kingsandmonsters.effect.BogfumeRageEvents;
import com.kingsandmonsters.effect.CombatEffectEvents;
import com.kingsandmonsters.enchantment.ModEnchantmentEffects;
import com.kingsandmonsters.item.ArtifactEvents;
import com.kingsandmonsters.item.HunterSpearEvents;
import com.kingsandmonsters.item.KingsCleaverEvents;
import com.kingsandmonsters.item.OgreKingsClubEvents;
import com.kingsandmonsters.item.OgreKingBannerEvents;
import com.kingsandmonsters.item.RattleboneRingEvents;
import com.kingsandmonsters.item.WarcallerBellEvents;
import com.kingsandmonsters.item.OgreHookbladeEvents;
import com.kingsandmonsters.item.OgrebloodTotemEvents;
import com.kingsandmonsters.item.CreativeDestroyerEvents;
import com.kingsandmonsters.entity.BogPoleSwampSpawner;
import com.kingsandmonsters.entity.OgreBrute;
import com.kingsandmonsters.entity.OgreGrunt;
import com.kingsandmonsters.entity.OgreBruteCombatEvents;
import com.kingsandmonsters.entity.OgreDamageCapEvents;
import com.kingsandmonsters.entity.OgreLordCombatEvents;
import com.kingsandmonsters.entity.OgreMage;
import com.kingsandmonsters.network.ModNetwork;
import com.kingsandmonsters.tribute.BogIronArmorEffects;
import com.kingsandmonsters.tribute.BogIronProgression;
import com.kingsandmonsters.tribute.AngerHudSync;
import com.kingsandmonsters.tribute.OutpostEvents;
import com.kingsandmonsters.tribute.OutpostPopulationManager;
import com.kingsandmonsters.tribute.StructureJockeyPopulationManager;
import com.kingsandmonsters.tribute.OutpostPlagueSpawner;
import com.kingsandmonsters.tribute.PatrolManager;
import com.kingsandmonsters.tribute.TributeManager;
import com.kingsandmonsters.world.ModFeatures;
import com.kingsandmonsters.world.ModStructures;
import com.kingsandmonsters.world.FortVegetationCleaner;
import com.kingsandmonsters.world.FortPopulationSpawner;
import com.kingsandmonsters.world.FortMapTrades;
import com.kingsandmonsters.world.OutpostVegetationCleaner;
import com.kingsandmonsters.world.OgreMerchantSpawnManager;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(KingsAndMonsters.MODID)
public class KingsAndMonsters {

    public static final String MODID = "kingsandmonsters";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> KINGS_AND_MONSTERS_TAB =
            CREATIVE_MODE_TABS.register("kings_and_monsters_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.kingsandmonsters"))
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon(() -> ModItems.OGRE_KINGS_CROWN.get().getDefaultInstance())
                            .displayItems((parameters, output) ->
                                    ContentDisplayOrder.mainCreativeItems().forEach(output::accept))
                            .build());

    public static final RegistryObject<CreativeModeTab> KINGS_AND_MONSTERS_ARTIFACTS_TAB =
            CREATIVE_MODE_TABS.register("kings_and_monsters_artifacts_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.kingsandmonsters.artifacts"))
                            .withTabsBefore(KINGS_AND_MONSTERS_TAB.getKey())
                            .icon(() -> ModItems.OGRE_TOOTH_NECKLACE.get().getDefaultInstance())
                            .displayItems((parameters, output) ->
                                    ContentDisplayOrder.artifactCreativeItems().forEach(output::accept))
                            .build());

    public KingsAndMonsters() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener((FMLClientSetupEvent e) -> {
                LOGGER.info("Kings and Monsters client setup");
                e.enqueueWork(ClientEvents::registerItemProperties);
                e.enqueueWork(ClientEvents::registerMenuScreens);
                ClientEvents.registerClientRuntimeEvents();
            });
            modEventBus.addListener(ClientEvents::registerRenderers);
            modEventBus.addListener(ClientEvents::registerParticleProviders);
            modEventBus.addListener(ClientEvents::registerLayerDefinitions);
            modEventBus.addListener(ClientEvents::registerGuiLayers);
            modEventBus.addListener(ClientEvents::registerKeyMappings);
        }
        modEventBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(ModNetwork::register));
        ModBannerPatterns.BANNER_PATTERNS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(ItemAttributes.class);
        MinecraftForge.EVENT_BUS.register(EventBridge.class);
        MinecraftForge.EVENT_BUS.register(FreshFood.class);

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModEnchantments.ENCHANTMENTS.register(modEventBus);
        ModPotions.POTIONS.register(modEventBus);
        ModSoundEvents.SOUND_EVENTS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModLootFunctions.LOOT_FUNCTIONS.register(modEventBus);
        ModLootFunctions.LOOT_MODIFIERS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModStructures.STRUCTURE_TYPES.register(modEventBus);
        ModStructures.STRUCTURE_PIECE_TYPES.register(modEventBus);
        ModStructures.STRUCTURE_PROCESSOR_TYPES.register(modEventBus);
        ModStructures.STRUCTURE_PLACEMENT_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(ModEntities::registerSpawnPlacements));
        CREATIVE_MODE_TABS.register(modEventBus);
        IronsSpellbooksCompat.registerIfPresent();
        SpellPowerCompat.registerIfPresent();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(BogIronArmorEffects::onMobEffectApplicable);
        MinecraftForge.EVENT_BUS.addListener(BogIronArmorEffects::onLivingDamagePost);
        MinecraftForge.EVENT_BUS.addListener(BogIronArmorEffects::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(BogIronEquipmentEvents::onLivingDamagePost);
        MinecraftForge.EVENT_BUS.addListener(BogIronProgression::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(BogIronProgression::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,
                BogIronEquipmentEvents::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(CompendiumGrant::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(BogfumeRageEvents::onIncomingDamage);
        MinecraftForge.EVENT_BUS.addListener(CombatEffectEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(CombatEffectEvents::onLivingHeal);
        MinecraftForge.EVENT_BUS.addListener(CombatEffectEvents::onLivingJump);
        MinecraftForge.EVENT_BUS.addListener(FreshMeatEvents::onLivingDrops);
        MinecraftForge.EVENT_BUS.addListener(CombatEffectEvents::onEffectApplicable);
        MinecraftForge.EVENT_BUS.addListener(BogrotSyncEvents::onEffectAdded);
        MinecraftForge.EVENT_BUS.addListener(BogrotSyncEvents::onEffectRemoved);
        MinecraftForge.EVENT_BUS.addListener(BogrotSyncEvents::onEffectExpired);
        MinecraftForge.EVENT_BUS.addListener(BogrotSyncEvents::onEntityTick);
        MinecraftForge.EVENT_BUS.addListener(BogrotSyncEvents::onStartTracking);
        MinecraftForge.EVENT_BUS.addListener(CombatEffectSyncEvents::onEffectAdded);
        MinecraftForge.EVENT_BUS.addListener(CombatEffectSyncEvents::onEffectRemoved);
        MinecraftForge.EVENT_BUS.addListener(CombatEffectSyncEvents::onEffectExpired);
        MinecraftForge.EVENT_BUS.addListener(CombatEffectSyncEvents::onEntityTick);
        MinecraftForge.EVENT_BUS.addListener(CombatEffectSyncEvents::onStartTracking);
        MinecraftForge.EVENT_BUS.addListener(BogToadContentEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(BogToadContentEvents::onLivingJump);
        MinecraftForge.EVENT_BUS.addListener(BogToadContentEvents::onLivingDamagePre);
        MinecraftForge.EVENT_BUS.addListener(BogToadContentEvents::onLivingDamagePost);
        MinecraftForge.EVENT_BUS.addListener(BogToadContentEvents::onCriticalHit);
        MinecraftForge.EVENT_BUS.addListener(BlackhideEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(BlackhideEvents::onIncomingDamage);
        MinecraftForge.EVENT_BUS.addListener(BlackhideEvents::onDamagePost);
        MinecraftForge.EVENT_BUS.addListener(BlackhideEvents::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(ServerStateReset::onServerStopped);
        modEventBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(ModPotions::registerBrewingRecipes));
        MinecraftForge.EVENT_BUS.addListener(SwampPotionEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(SwampPotionEvents::onBreakSpeed);
        MinecraftForge.EVENT_BUS.addListener(ModEnchantmentEffects::onIncomingDamage);
        MinecraftForge.EVENT_BUS.addListener(OgreGearEvents::onItemAttributes);
        MinecraftForge.EVENT_BUS.addListener(OgreGearEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(OgreGearEvents::onUseStart);
        MinecraftForge.EVENT_BUS.addListener(OgreGearEvents::onRightClickItem);
        MinecraftForge.EVENT_BUS.addListener(OgreGearEvents::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(ArtifactEvents::onKnockBack);
        MinecraftForge.EVENT_BUS.addListener(ArtifactEvents::onEntityJoinLevel);
        MinecraftForge.EVENT_BUS.addListener(ArtifactEvents::onIncomingDamage);
        MinecraftForge.EVENT_BUS.addListener(BogsteelChestplateEvents::onIncomingDamage);
        MinecraftForge.EVENT_BUS.addListener(ArtifactEvents::onLivingDamagePost);
        MinecraftForge.EVENT_BUS.addListener(ArtifactEvents::onMobEffectAdded);
        MinecraftForge.EVENT_BUS.addListener(OgreCrownArmy::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(OgreCrownArmy::onIncomingDamage);
        MinecraftForge.EVENT_BUS.addListener(OgreCrownArmy::onMobEffectAdded);
        MinecraftForge.EVENT_BUS.addListener(KingsCleaverEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(KingsCleaverEvents::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(RoyalAdvancementEvents::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(RoyalAdvancementEvents::onBabySpawn);
        MinecraftForge.EVENT_BUS.addListener(OgreKingsClubEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(OgreKingBannerEvents::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(RattleboneRingEvents::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(RattleboneRingEvents::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(OgrebloodTotemEvents::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(OgrebloodTotemEvents::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(BogPoleSwampSpawner::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(MudsnoutRecallTracker::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(WarcallerBellEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(OgreHookbladeEvents::onLivingDamagePost);
        MinecraftForge.EVENT_BUS.addListener(BoneClubEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(BoneClubEvents::onCriticalHit);
        MinecraftForge.EVENT_BUS.addListener(BoneClubEvents::onIncomingDamage);
        MinecraftForge.EVENT_BUS.addListener(BoneClubEvents::onLivingDamagePost);
        MinecraftForge.EVENT_BUS.addListener(CreativeDestroyerEvents::onAttackEntity);
        MinecraftForge.EVENT_BUS.addListener(PatrolManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(TestingCommands::register);
        MinecraftForge.EVENT_BUS.addListener(OutpostPopulationManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(StructureJockeyPopulationManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(OutpostPlagueSpawner::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(FortVegetationCleaner::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(FortPopulationSpawner::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(OutpostVegetationCleaner::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(OgreMerchantSpawnManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(OutpostEvents::onTributeChestOpened);
        MinecraftForge.EVENT_BUS.addListener(OutpostEvents::onItemFrameTaken);
        MinecraftForge.EVENT_BUS.addListener(OutpostEvents::onOgreKilled);
        MinecraftForge.EVENT_BUS.addListener(QuestManager::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(OutpostEvents::onOgreHurt);
        MinecraftForge.EVENT_BUS.addListener(FortMapTrades::onVillagerTrades);
        MinecraftForge.EVENT_BUS.addListener(OgreBruteCombatEvents::onShieldBlock);
        MinecraftForge.EVENT_BUS.addListener(OgreLordCombatEvents::onShieldBlock);
        MinecraftForge.EVENT_BUS.addListener(OgreLord::onRoyalTrialLeaderDeath);
        MinecraftForge.EVENT_BUS.addListener(OgreLord::onRoyalTrialCaptainSpawnCheck);
        MinecraftForge.EVENT_BUS.addListener(OgreLord::onRoyalTrialSpawnerBreak);
        MinecraftForge.EVENT_BUS.addListener(OgreDamageCapEvents::onLivingDamagePre);
        MinecraftForge.EVENT_BUS.addListener(OgreRallyEvents::onLivingDamagePost);
        MinecraftForge.EVENT_BUS.addListener(OgreVanillaHostilityEvents::onEntityTick);
        MinecraftForge.EVENT_BUS.addListener(RippleJumpTracker::onLivingJump);
        MinecraftForge.EVENT_BUS.addListener(HunterSpearEvents::onLivingDamagePost);
        MinecraftForge.EVENT_BUS.addListener(AngerHudSync::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(QuestManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(QuestManager::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(StructureDiscoveryManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(AngerHudSync::onPlayerLoggedOut);
        CandleHelmetLight.register(MinecraftForge.EVENT_BUS);

        // Some values are needed while server resources and villager trades are
        // assembled, before per-world SERVER configs become available. Load the
        // server-authoritative settings at startup so those hooks can read them.
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "kingsandmonsters-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "kingsandmonsters-client.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Kings and Monsters common setup");
        event.enqueueWork(() -> ComposterBlock.COMPOSTABLES.put(
                ModItems.ACORN.get(), 0.3F));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Kings and Monsters server starting");
        TributeManager.attach(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        TributeManager.detach();
        AngerHudSync.reset();
        QuestManager.resetSync();
        OgreKingsClubEvents.reset();
        OgrebloodTotemEvents.reset();
        FortPopulationSpawner.reset();
        FortVegetationCleaner.reset();
        OutpostVegetationCleaner.reset();
        OgreMerchantSpawnManager.reset();
        WorldgenSavedDataQueue.reset();
        StructureDiscoveryManager.reset();
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof OgreGrunt ogreGrunt) {
            ogreGrunt.applyConfiguredCombatAttributes(!event.loadedFromDisk());
            ogreGrunt.onJoinedLevel();
            if (!event.loadedFromDisk() && !event.getLevel().isClientSide()) {
                playOgreEntranceHuff(ogreGrunt);
            }
        }
    }

    private void playOgreEntranceHuff(OgreGrunt ogre) {
        if (!(ogre instanceof OgreMage) && !(ogre instanceof OgreBrute)) {
            return;
        }

        SoundEvent sound = ModSoundEvents.OGRE_BRUTE_IDLE.get();
        float volume = 0.85F;
        float pitch = 0.9F;

        if (ogre instanceof OgreMage) {
            sound = ModSoundEvents.OGRE_MAGE_HUFF.get();
            volume = 0.8F;
            pitch = 1.0F;
        } else if (ogre instanceof OgreBrute) {
            volume = 1.0F;
            pitch = 0.68F;
        }

        ogre.level().playSound(null, ogre.getX(), ogre.getY(), ogre.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }
}
