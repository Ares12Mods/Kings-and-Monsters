package com.kingsandmonsters;

import com.kingsandmonsters.entity.OgreGrunt;
import com.kingsandmonsters.entity.PlagueOgre;
import com.kingsandmonsters.entity.OgreGruntCaptain;
import com.kingsandmonsters.entity.OgreArcher;
import com.kingsandmonsters.entity.OgreBrute;
import com.kingsandmonsters.entity.OgreTrapper;
import com.kingsandmonsters.entity.BogjawTrap;
import com.kingsandmonsters.entity.OgreLord;
import com.kingsandmonsters.entity.OgreMage;
import com.kingsandmonsters.entity.OgreMerchant;
import com.kingsandmonsters.entity.OgreGuard;
import com.kingsandmonsters.entity.OgreSpear;
import com.kingsandmonsters.entity.BogfumeBolt;
import com.kingsandmonsters.entity.BogFangs;
import com.kingsandmonsters.entity.PoisonFogCloud;
import com.kingsandmonsters.entity.BogToad;
import com.kingsandmonsters.entity.BogPole;
import com.kingsandmonsters.entity.MudsnoutBoar;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, KingsAndMonsters.MODID);

    // ------------------------------------------------------------------ //
    //  Entities                                                            //
    // ------------------------------------------------------------------ //

    public static final RegistryObject<EntityType<OgreGrunt>> OGRE_GRUNT =
            ENTITY_TYPES.register("ogre_grunt", () -> EntityType.Builder
                    .<OgreGrunt>of(OgreGrunt::new, MobCategory.MONSTER)
                    .sized(1.2f, 2.8f)
                    .build("ogre_grunt"));

    public static final RegistryObject<EntityType<PlagueOgre>> PLAGUE_OGRE =
            ENTITY_TYPES.register("plague_ogre", () -> EntityType.Builder
                    .<PlagueOgre>of(PlagueOgre::new, MobCategory.MONSTER)
                    .sized(1.2f, 2.8f)
                    .build("plague_ogre"));

    public static final RegistryObject<EntityType<BogToad>> BOG_TOAD =
            ENTITY_TYPES.register("bog_toad", () -> EntityType.Builder
                    .<BogToad>of(BogToad::new, MobCategory.CREATURE)
                    // Authored model bounds are ~2 blocks across the legs, 1.5 blocks tall, and
                    // ~2.75 blocks nose-to-back. A centered 2.5-wide box covers the visible legs
                    // and nearly all of the forward-projecting face without excessive rear reach.
                    .sized(2.5F, 1.5F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("bog_toad"));

    public static final RegistryObject<EntityType<MudsnoutBoar>> MUDSNOUT_BOAR =
            ENTITY_TYPES.register("mudsnout_boar", () -> EntityType.Builder
                    .<MudsnoutBoar>of(MudsnoutBoar::new, MobCategory.CREATURE)
                    .sized(1.6F, 1.9F)
                    .clientTrackingRange(48)
                    .build("mudsnout_boar"));

    public static final RegistryObject<EntityType<BogPole>> BOG_POLE =
            ENTITY_TYPES.register("bog_pole", () -> EntityType.Builder
                    .<BogPole>of(BogPole::new, MobCategory.WATER_CREATURE)
                    .sized(0.75F, 0.8F)
                    .clientTrackingRange(32)
                    .updateInterval(2)
                    .build("bog_pole"));

    public static final RegistryObject<EntityType<OgreGruntCaptain>> OGRE_GRUNT_CAPTAIN =
            ENTITY_TYPES.register("ogre_grunt_captain", () -> EntityType.Builder
                    .<OgreGruntCaptain>of(OgreGruntCaptain::new, MobCategory.MONSTER)
                    .sized(1.2f, 2.8f)
                    .build("ogre_grunt_captain"));

    public static final RegistryObject<EntityType<OgreMage>> OGRE_MAGE =
            ENTITY_TYPES.register("ogre_mage", () -> EntityType.Builder
                    .<OgreMage>of(OgreMage::new, MobCategory.MONSTER)
                    // The physical torso/head reaches roughly 29 model pixels (1.82 blocks).
                    // Ears, staff, and other lateral decoration do not require head clearance.
                    .sized(1.25f, 1.9f)
                    .build("ogre_mage"));

    public static final RegistryObject<EntityType<OgreBrute>> OGRE_BRUTE =
            ENTITY_TYPES.register("ogre_brute", () -> EntityType.Builder
                    .<OgreBrute>of(OgreBrute::new, MobCategory.MONSTER)
                    .sized(1.7f, 3.4f)
                    .build("ogre_brute"));

    public static final RegistryObject<EntityType<OgreTrapper>> OGRE_TRAPPER =
            ENTITY_TYPES.register("ogre_trapper", () -> EntityType.Builder
                    .<OgreTrapper>of(OgreTrapper::new, MobCategory.MONSTER)
                    // Body 22 px wide and 15 deep, head top at 39 px; the arms and the trap on his back are not collision.
                    .sized(1.4f, 2.45f)
                    .build("ogre_trapper"));

    public static final RegistryObject<EntityType<BogjawTrap>> BOGJAW_TRAP =
            ENTITY_TYPES.register("bogjaw_trap", () -> EntityType.Builder
                    .<BogjawTrap>of(BogjawTrap::new, MobCategory.MISC)
                    .sized(1.0f, 0.3f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("bogjaw_trap"));

    public static final RegistryObject<EntityType<OgreArcher>> OGRE_ARCHER =
            ENTITY_TYPES.register("ogre_archer", () -> EntityType.Builder
                    .<OgreArcher>of(OgreArcher::new, MobCategory.MONSTER)
                    .sized(0.95f, 2.45f)
                    .build("ogre_archer"));

    public static final RegistryObject<EntityType<OgreGuard>> OGRE_GUARD =
            ENTITY_TYPES.register("ogre_guard", () -> EntityType.Builder
                    .<OgreGuard>of(OgreGuard::new, MobCategory.MONSTER)
                    // The physical legs, torso, and head span 28 model pixels (1.75 blocks).
                    // The held spear is decorative equipment and is not part of collision.
                    .sized(1.05f, 1.85f)
                    .build("ogre_guard"));

    public static final RegistryObject<EntityType<OgreMerchant>> OGRE_MERCHANT =
            ENTITY_TYPES.register("ogre_merchant", () -> EntityType.Builder
                    .<OgreMerchant>of(OgreMerchant::new, MobCategory.CREATURE)
                    .sized(1.4F, 2.65F)
                    .build("ogre_merchant"));

    public static final RegistryObject<EntityType<OgreSpear>> OGRE_SPEAR =
            ENTITY_TYPES.register("ogre_spear", () -> EntityType.Builder
                    .<OgreSpear>of(OgreSpear::new, MobCategory.MISC)
                    .sized(0.35f, 0.35f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ogre_spear"));

    public static final RegistryObject<EntityType<OgreLord>> OGRE_LORD =
            ENTITY_TYPES.register("ogre_lord", () -> EntityType.Builder
                    .<OgreLord>of(OgreLord::new, MobCategory.MONSTER)
                    // Measured off the model bounds (~5.4 wide arm to arm, ~2.9 deep, ~6.1 tall) and
                    // sized like OgreBrute: width follows front-to-back depth, since the full arm span
                    // would make him too easy to hit from the side, and height is ~92% of the model.
                    // The old 2.3/4.6 left his head and crown above the hittable box. Previewed at 5/6
                    // scale with OgreLordRenderer; go back to 3.3f, 5.6f if the scale returns to 1.0F.
                    .sized(2.75f, 4.67f)
                    .build("ogre_lord"));

    public static final RegistryObject<EntityType<PoisonFogCloud>> POISON_FOG_CLOUD =
            ENTITY_TYPES.register("poison_fog_cloud", () -> EntityType.Builder
                    .<PoisonFogCloud>of(PoisonFogCloud::new, MobCategory.MISC)
                    .sized(PoisonFogCloud.RADIUS * 2.0F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .build("poison_fog_cloud"));

    public static final RegistryObject<EntityType<BogfumeBolt>> BOGFUME_BOLT =
            ENTITY_TYPES.register("bogfume_bolt", () -> EntityType.Builder
                    .<BogfumeBolt>of(BogfumeBolt::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("bogfume_bolt"));

    public static final RegistryObject<EntityType<BogFangs>> BOG_FANGS =
            ENTITY_TYPES.register("bog_fangs", () -> EntityType.Builder
                    .<BogFangs>of(BogFangs::new, MobCategory.MISC)
                    .sized(0.5F, 0.8F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("bog_fangs"));

    // ------------------------------------------------------------------ //
    //  Attribute registration (called from KingsAndMonsters constructor)  //
    // ------------------------------------------------------------------ //

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(BOG_TOAD.get(), BogToad.createAttributes().build());
        event.put(BOG_POLE.get(), BogPole.createAttributes().build());
        event.put(MUDSNOUT_BOAR.get(), MudsnoutBoar.createAttributes().build());
        event.put(OGRE_GRUNT.get(), OgreGrunt.createAttributes().build());
        event.put(PLAGUE_OGRE.get(), PlagueOgre.createAttributes().build());
        event.put(OGRE_GRUNT_CAPTAIN.get(), OgreGruntCaptain.createAttributes().build());
        event.put(OGRE_MAGE.get(), OgreMage.createAttributes().build());
        event.put(OGRE_BRUTE.get(), OgreBrute.createAttributes().build());
        event.put(OGRE_TRAPPER.get(), OgreTrapper.createAttributes().build());
        event.put(OGRE_ARCHER.get(), OgreArcher.createAttributes().build());
        event.put(OGRE_GUARD.get(), OgreGuard.createAttributes().build());
        event.put(OGRE_MERCHANT.get(), OgreMerchant.createAttributes().build());
        event.put(OGRE_LORD.get(), OgreLord.createAttributes().build());
    }

    public static void registerSpawnPlacements() {
        SpawnPlacements.register(PLAGUE_OGRE.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, PlagueOgre::checkPlagueOgreSpawnRules);
        SpawnPlacements.register(BOG_POLE.get(), SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.OCEAN_FLOOR, BogPole::checkBogPoleSpawnRules);
        SpawnPlacements.register(MUDSNOUT_BOAR.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MudsnoutBoar::checkMudsnoutSpawnRules);
        SpawnPlacements.register(BOG_TOAD.get(), SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BogToad::checkBogToadSpawnRules);
    }
}
