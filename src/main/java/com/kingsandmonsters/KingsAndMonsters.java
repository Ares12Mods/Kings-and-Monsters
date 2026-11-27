package com.kingsandmonsters;

import com.kingsandmonsters.command.TestingCommands;
import com.kingsandmonsters.compat.spell_power.SpellPowerCompat;
import com.kingsandmonsters.effect.BogrotSyncEvents;
import com.kingsandmonsters.effect.CombatEffectSyncEvents;
import com.kingsandmonsters.effect.SwampPotionEvents;
import com.kingsandmonsters.entity.OgreRallyEvents;
import com.kingsandmonsters.entity.OgreVanillaHostilityEvents;
import com.kingsandmonsters.entity.RippleJumpTracker;
import com.kingsandmonsters.item.BlackhideEvents;
import com.kingsandmonsters.item.BogIronEquipmentEvents;
import com.kingsandmonsters.item.BogToadContentEvents;
import com.kingsandmonsters.item.BogsteelChestplateEvents;
import com.kingsandmonsters.item.BoneClubEvents;
import com.kingsandmonsters.item.FreshMeatEvents;
import com.kingsandmonsters.item.MudsnoutRecallTracker;
import com.kingsandmonsters.item.OgreCrownArmy;
import com.kingsandmonsters.item.OgreGearEvents;
import com.kingsandmonsters.light.CandleHelmetLight;
import com.kingsandmonsters.tribute.QuestManager;
import com.kingsandmonsters.tribute.StructureDiscoveryManager;
import com.kingsandmonsters.world.WorldgenSavedDataQueue;
import net.neoforged.bus.api.EventPriority;
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
import com.kingsandmonsters.world.ArcherTrialSpawnerActivation;
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

import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(KingsAndMonsters.MODID)
public class KingsAndMonsters {

    public static final String MODID = "kingsandmonsters";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KINGS_AND_MONSTERS_TAB =
            CREATIVE_MODE_TABS.register("kings_and_monsters_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.kingsandmonsters"))
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon(() -> ModItems.OGRE_KINGS_CROWN.get().getDefaultInstance())
                            .displayItems((parameters, output) ->
                                    ContentDisplayOrder.mainCreativeItems().forEach(output::accept))
                            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KINGS_AND_MONSTERS_ARTIFACTS_TAB =
            CREATIVE_MODE_TABS.register("kings_and_monsters_artifacts_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.kingsandmonsters.artifacts"))
                            .withTabsBefore(KINGS_AND_MONSTERS_TAB.getKey())
                            .icon(() -> ModItems.OGRE_TOOTH_NECKLACE.get().getDefaultInstance())
                            .displayItems((parameters, output) ->
                                    ContentDisplayOrder.artifactCreativeItems().forEach(output::accept))
                            .build());

    public KingsAndMonsters(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener((FMLClientSetupEvent e) -> {
                LOGGER.info("Kings and Monsters client setup");
                e.enqueueWork(ClientEvents::registerItemProperties);
                ClientEvents.registerClientRuntimeEvents();
            });
            modEventBus.addListener(ClientEvents::registerRenderers);
            modEventBus.addListener(ClientEvents::registerParticleProviders);
            modEventBus.addListener(ClientEvents::registerLayerDefinitions);
            modEventBus.addListener(ClientEvents::registerGuiLayers);
            modEventBus.addListener(ClientEvents::registerKeyMappings);
            modEventBus.addListener(ClientEvents::registerMenuScreens);
        }
        modEventBus.addListener(ModNetwork::register);

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModPotions.POTIONS.register(modEventBus);
        ModSoundEvents.SOUND_EVENTS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModLootFunctions.LOOT_FUNCTIONS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModStructures.STRUCTURE_TYPES.register(modEventBus);
        ModStructures.STRUCTURE_PIECE_TYPES.register(modEventBus);
        ModStructures.STRUCTURE_PROCESSOR_TYPES.register(modEventBus);
        ModStructures.STRUCTURE_PLACEMENT_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModEntities::registerSpawnPlacements);
        CREATIVE_MODE_TABS.register(modEventBus);
        IronsSpellbooksCompat.registerIfPresent();
        SpellPowerCompat.registerIfPresent();

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(BogIronArmorEffects::onMobEffectApplicable);
        NeoForge.EVENT_BUS.addListener(BogIronArmorEffects::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(BogIronArmorEffects::onServerTick);
        NeoForge.EVENT_BUS.addListener(BogIronEquipmentEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(BogIronProgression::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(BogIronProgression::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,
                BogIronEquipmentEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(CompendiumGrant::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(BogfumeRageEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(CombatEffectEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(CombatEffectEvents::onLivingHeal);
        NeoForge.EVENT_BUS.addListener(FreshMeatEvents::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(CombatEffectEvents::onEffectApplicable);
        NeoForge.EVENT_BUS.addListener(BogrotSyncEvents::onEffectAdded);
        NeoForge.EVENT_BUS.addListener(BogrotSyncEvents::onEffectRemoved);
        NeoForge.EVENT_BUS.addListener(BogrotSyncEvents::onEffectExpired);
        NeoForge.EVENT_BUS.addListener(BogrotSyncEvents::onEntityTick);
        NeoForge.EVENT_BUS.addListener(BogrotSyncEvents::onStartTracking);
        NeoForge.EVENT_BUS.addListener(CombatEffectSyncEvents::onEffectAdded);
        NeoForge.EVENT_BUS.addListener(CombatEffectSyncEvents::onEffectRemoved);
        NeoForge.EVENT_BUS.addListener(CombatEffectSyncEvents::onEffectExpired);
        NeoForge.EVENT_BUS.addListener(CombatEffectSyncEvents::onEntityTick);
        NeoForge.EVENT_BUS.addListener(CombatEffectSyncEvents::onStartTracking);
        NeoForge.EVENT_BUS.addListener(BogToadContentEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(BogToadContentEvents::onLivingJump);
        NeoForge.EVENT_BUS.addListener(BogToadContentEvents::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(BogToadContentEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(BogToadContentEvents::onCriticalHit);
        NeoForge.EVENT_BUS.addListener(BlackhideEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(BlackhideEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(BlackhideEvents::onDamagePost);
        NeoForge.EVENT_BUS.addListener(BlackhideEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(ServerStateReset::onServerStopped);
        NeoForge.EVENT_BUS.addListener(ModPotions::registerBrewingRecipes);
        NeoForge.EVENT_BUS.addListener(SwampPotionEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SwampPotionEvents::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(ModEnchantmentEffects::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(OgreGearEvents::onItemAttributes);
        NeoForge.EVENT_BUS.addListener(OgreGearEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(OgreGearEvents::onUseStart);
        NeoForge.EVENT_BUS.addListener(OgreGearEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(OgreGearEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(ArtifactEvents::onKnockBack);
        NeoForge.EVENT_BUS.addListener(ArtifactEvents::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(ArtifactEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(BogsteelChestplateEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(ArtifactEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(ArtifactEvents::onMobEffectAdded);
        NeoForge.EVENT_BUS.addListener(OgreCrownArmy::onServerTick);
        NeoForge.EVENT_BUS.addListener(OgreCrownArmy::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(OgreCrownArmy::onMobEffectAdded);
        NeoForge.EVENT_BUS.addListener(KingsCleaverEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(KingsCleaverEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(RoyalAdvancementEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(RoyalAdvancementEvents::onBabySpawn);
        NeoForge.EVENT_BUS.addListener(OgreKingsClubEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(OgreKingBannerEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(RattleboneRingEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(RattleboneRingEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(OgrebloodTotemEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(OgrebloodTotemEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(BogPoleSwampSpawner::onServerTick);
        NeoForge.EVENT_BUS.addListener(MudsnoutRecallTracker::onServerTick);
        NeoForge.EVENT_BUS.addListener(WarcallerBellEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(OgreHookbladeEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(BoneClubEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(BoneClubEvents::onCriticalHit);
        NeoForge.EVENT_BUS.addListener(BoneClubEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(BoneClubEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(CreativeDestroyerEvents::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(PatrolManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(TestingCommands::register);
        NeoForge.EVENT_BUS.addListener(OutpostPopulationManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(StructureJockeyPopulationManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(OutpostPlagueSpawner::onServerTick);
        NeoForge.EVENT_BUS.addListener(FortVegetationCleaner::onServerTick);
        NeoForge.EVENT_BUS.addListener(FortPopulationSpawner::onServerTick);
        NeoForge.EVENT_BUS.addListener(OutpostVegetationCleaner::onServerTick);
        NeoForge.EVENT_BUS.addListener(OgreMerchantSpawnManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(ArcherTrialSpawnerActivation::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(OutpostEvents::onTributeChestOpened);
        NeoForge.EVENT_BUS.addListener(OutpostEvents::onItemFrameTaken);
        NeoForge.EVENT_BUS.addListener(OutpostEvents::onOgreKilled);
        NeoForge.EVENT_BUS.addListener(QuestManager::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(OutpostEvents::onOgreHurt);
        NeoForge.EVENT_BUS.addListener(FortMapTrades::onVillagerTrades);
        NeoForge.EVENT_BUS.addListener(OgreBruteCombatEvents::onShieldBlock);
        NeoForge.EVENT_BUS.addListener(OgreLordCombatEvents::onShieldBlock);
        NeoForge.EVENT_BUS.addListener(OgreDamageCapEvents::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(OgreRallyEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(OgreVanillaHostilityEvents::onEntityTick);
        NeoForge.EVENT_BUS.addListener(RippleJumpTracker::onLivingJump);
        NeoForge.EVENT_BUS.addListener(HunterSpearEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(AngerHudSync::onServerTick);
        NeoForge.EVENT_BUS.addListener(QuestManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(QuestManager::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(StructureDiscoveryManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(AngerHudSync::onPlayerLoggedOut);
        CandleHelmetLight.register(NeoForge.EVENT_BUS);

        // Some values are needed while server resources and villager trades are
        // assembled, before per-world SERVER configs become available. Load the
        // server-authoritative settings at startup so those hooks can read them.
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "kingsandmonsters-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "kingsandmonsters-client.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Kings and Monsters common setup");
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
