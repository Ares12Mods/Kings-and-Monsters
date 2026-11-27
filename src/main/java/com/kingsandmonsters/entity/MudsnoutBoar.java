package com.kingsandmonsters.entity;

import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.entity.animation.CanonicalOneShotState;
import com.kingsandmonsters.entity.animation.SynchronizedAnimationController;
import net.minecraft.core.BlockPos;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.client.MudsnoutBoarClient;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import com.kingsandmonsters.network.ScreenShakePayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.SimpleMenuProvider;
import com.kingsandmonsters.menu.MudsnoutBoarMenu;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * The Mudsnout Boar: a huge, short-tempered plains boar. It roams in small packs, headbutts anything
 * it takes for a threat (players included) and, from a few blocks off, paws the ground and charges.
 * It is afraid of one thing only: ogres. The Tribe came out of the swamp onto the plains and found
 * the boars already there, and the boars had never met anything that did not give way to a charge.
 */
public class MudsnoutBoar extends PathfinderMob implements GeoEntity, PlayerRideableJumping, HasCustomInventoryScreen, ContainerListener {
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(MudsnoutBoar.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OVERLAY_ID =
            SynchedEntityData.defineId(MudsnoutBoar.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_OVERLAY_START =
            SynchedEntityData.defineId(MudsnoutBoar.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_FLEEING =
            SynchedEntityData.defineId(MudsnoutBoar.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TAMED =
            SynchedEntityData.defineId(MudsnoutBoar.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SADDLED =
            SynchedEntityData.defineId(MudsnoutBoar.class, EntityDataSerializers.BOOLEAN);
    /** Winded after a ridden ram: the rider's client slows the boar to a crawl. The timers tick on the server. */
    private static final EntityDataAccessor<Boolean> DATA_WINDED =
            SynchedEntityData.defineId(MudsnoutBoar.class, EntityDataSerializers.BOOLEAN);
    /** A ram that landed leaves the boar unable to sprint for a while. */
    private static final EntityDataAccessor<Boolean> DATA_SPRINT_LOCKED =
            SynchedEntityData.defineId(MudsnoutBoar.class, EntityDataSerializers.BOOLEAN);
    /** How sturdy this boar is compared to an average one (0 until rolled). Bred alongside speed. */
    private static final EntityDataAccessor<Float> DATA_HEALTH_TRAIT =
            SynchedEntityData.defineId(MudsnoutBoar.class, EntityDataSerializers.FLOAT);
    /** How fast this boar runs compared to an average one (0 until rolled). Like a horse, some are just quicker. */
    private static final EntityDataAccessor<Float> DATA_SPEED_TRAIT =
            SynchedEntityData.defineId(MudsnoutBoar.class, EntityDataSerializers.FLOAT);
    /** A snoutlet: the baby, drawn with its own model, too small to fight, saddle or tame. */
    private static final EntityDataAccessor<Boolean> DATA_BABY =
            SynchedEntityData.defineId(MudsnoutBoar.class, EntityDataSerializers.BOOLEAN);
    /** The one-shot body pose the boar is in: lying down in a wallow, or sitting on command. */
    private static final EntityDataAccessor<Integer> DATA_POSE =
            SynchedEntityData.defineId(MudsnoutBoar.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("IDLE");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("WALK");
    private static final RawAnimation COMBAT_WALK = RawAnimation.begin().thenLoop("COMBAT_WALK");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("RUN");
    private static final RawAnimation CHARGE_INDICATOR = RawAnimation.begin().thenLoop("CHARGE_INDICATOR");
    private static final RawAnimation HEADBUTT = RawAnimation.begin().thenPlay("HEADBUTT_BLEND");
    private static final RawAnimation RAM = RawAnimation.begin().thenPlay("RAM_BLEND");
    private static final RawAnimation EATING = RawAnimation.begin().thenPlay("EATING_BLEND");
    private static final RawAnimation GRAZING = RawAnimation.begin().thenPlay("GRAZING_BLEND");
    // The snoutlet's own eating clip has no "_BLEND" in its name.
    private static final RawAnimation BABY_EATING = RawAnimation.begin().thenPlay("EATING");
    private static final RawAnimation LAYING_DOWN = RawAnimation.begin().thenPlayAndHold("LAYING_DOWN");
    private static final RawAnimation LAY = RawAnimation.begin().thenLoop("LAY");
    private static final RawAnimation BACKUP = RawAnimation.begin().thenPlayAndHold("BACKUP");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlayAndHold("SIT_DOWN");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("SIT");
    private static final RawAnimation STAND = RawAnimation.begin().thenPlayAndHold("STAND");

    private static final int POSE_NONE = 0;
    private static final int POSE_LAYING_DOWN = 1;
    private static final int POSE_LAYING = 2;
    private static final int POSE_BACKUP = 3;
    private static final int POSE_SIT_DOWN = 4;
    private static final int POSE_SITTING = 5;
    private static final int POSE_STAND = 6;
    // Lying down and getting up are 0.75s clips, sitting down and standing are 1s; the extra 2 ticks
    // cover the synced start reaching clients, so the belly meets the mud as the splash plays.
    private static final int LAY_CLIP_TICKS = 17;
    private static final int SIT_CLIP_TICKS = 22;
    /** Twenty minutes to grow up; feeding a snoutlet moves that along. */
    private static final int GROW_UP_TICKS = 24000;
    private static final int FEEDING_GROWTH_TICKS = 2400;
    private static final float BABY_HEALTH = 14.0F;

    private static final int PHASE_NONE = 0;
    private static final int PHASE_WINDUP = 1;
    private static final int PHASE_CHARGE = 2;
    private static final int OVERLAY_HEADBUTT = 1;
    private static final int OVERLAY_RAM = 2;
    private static final int OVERLAY_EAT = 3;
    private static final int OVERLAY_GRAZE = 4;
    /** The grazing clip is two seconds long. */
    private static final int GRAZE_OVERLAY_TICKS = 40;
    /** Both blend clips are one second long. Read by the model for the blend out. */
    private static final int OVERLAY_TICKS = 20;
    /** The eating clip runs two seconds. */
    private static final int EAT_OVERLAY_TICKS = 40;
    private static final double ADULT_HEALTH = 48.0;
    public static final float OVERLAY_FADE_OUT_TICKS = 4.0F;

    // Every clip plays at the pace it was animated at; none is sped up or slowed down. The walk clip's
    // feet cover about 0.04 blocks a tick, so wandering goes at that speed. The combat walk (stalking)
    // and the run (fleeing and the charge) are faster than their clips' own pace, which is a gameplay
    // choice: see STALK_BLOCKS_PER_TICK.
    //
    // A mob's ground speed is not its speed setting: its forward input is scaled by the setting too, so
    // on normal ground it covers about 2.86 x setting^2 blocks a tick. groundSpeedModifier turns a real
    // blocks-a-tick figure into the navigation multiplier that produces it.
    private static final double SPEED_ATTRIBUTE = 0.28;
    private static final double WANDER_SPEED = groundSpeedModifier(0.04);
    private static final double PATROL_SPEED = groundSpeedModifier(0.07);
    private static final double PACK_REJOIN_SPEED = groundSpeedModifier(0.045);
    // The combat walk is faster than the King (about 0.145 blocks a tick) and a little over a walking
    // player (0.216) but still under a sprinting one (0.28): 0.24 blocks a tick, or 4.8 blocks a second. The clip plays at its
    // normal pace regardless.
    // 10% faster than before, matching the combat walk clip that was sped up from 2.0 s to 1.8182 s.
    private static final double STALK_BLOCKS_PER_TICK = 0.264;
    private static final double STALK_SPEED = groundSpeedModifier(STALK_BLOCKS_PER_TICK);
    private static final double FOLLOW_SPEED = groundSpeedModifier(0.12);
    private static final double OGRE_FEAR_DISTANCE = 16.0;
    private static final double PACK_COHESION_RANGE = 8.0;
    private static final double PACK_SEARCH_RANGE = 36.0;
    // Natural spawning refuses a fourth wild boar within this many blocks of three others.
    private static final int MAX_PACK_SIZE = 3;
    private static final double PACK_SPAWN_CAP_RANGE = 32.0;

    // The headbutt lands 0.54s into its clip (tick 11) and the ram 0.46s into its (tick 9). The extra
    // ticks are the clip's trip to the client and its blend-in (see CLIP_LAG_TICKS on the ogres).
    private static final int CLIP_LAG_TICKS = 4;
    private static final int HEADBUTT_IMPACT_TICKS = 11 + CLIP_LAG_TICKS;
    private static final int HEADBUTT_COOLDOWN_TICKS = 40;
    private static final double HEADBUTT_REACH = 3.2;
    // The headbutt is an uppercut: the target is driven back and launched upward.
    private static final double HEADBUTT_PUSH = 1.0;
    private static final double HEADBUTT_LAUNCH = 0.75;
    // The charge: a two-second paw-the-ground telegraph, then a straight run at the spot it chose.
    private static final int WINDUP_TICKS = 40;
    private static final int FIRST_CHARGE_SNORT_TICK = 27;
    private static final int SECOND_CHARGE_SNORT_TICK = 7;
    private static final int CHARGE_MAX_TICKS = 32;
    private static final double CHARGE_SPEED = 0.52;
    private static final double CHARGE_MIN_RANGE = 6.0;
    private static final double CHARGE_MAX_RANGE = 16.0;
    // The ram connects when the boxes touch: half the boar, the 1.2 ram reach and half the target.
    private static final double RAM_CONTACT_REACH = 0.8 + 1.2 + 0.3;
    // The ram clip starts this far from the target, so its impact tick (plus the lag) lands as the hit does.
    private static final double RAM_ANIMATION_RANGE = (9 + CLIP_LAG_TICKS) * CHARGE_SPEED + RAM_CONTACT_REACH;
    private static final double RAM_HIT_RANGE = 1.9;
    private static final double RAM_KNOCKBACK = 2.6;
    private static final double RAM_LIFT = 0.95;
    /** How much harder a charge hits than a plain heavy blow, for wild, tamed and jockey boars alike. */
    private static final float RAM_DAMAGE_SCALE = 1.4F;
    /** How long a charge's ram leaves its victim Dazed: 3 seconds, under the Brute's and King's 5. */
    private static final int RAM_DAZED_TICKS = 60;
    /** What a charge does to something other than its target that is caught in its path. */
    private static final double SHOVE_SIDEWAYS = 1.0;
    private static final double SHOVE_FORWARD = 0.35;
    private static final double SHOVE_LIFT = 0.4;
    private static final int CHARGE_COOLDOWN_TICKS = 300;
    // A jockey boar charges only now and then; between charges it skirmishes at the archer's distance.
    private static final int JOCKEY_CHARGE_COOLDOWN_TICKS = 500;
    private static final int JOCKEY_CHARGE_COOLDOWN_RANGE = 300;
    private static final int JOCKEY_MELEE_TICKS = 80;
    private static final int JOCKEY_MELEE_RANGE = 120;
    private static final double SKIRMISH_MIN = 8.0;
    private static final double SKIRMISH_MAX = 14.0;
    private static final double SKIRMISH_RING = 11.0;
    private static final int CHARGE_RECOVERY_TICKS = 24;

    // Each wild boar has its own temperament. This range makes acorns take between two and five feeds,
    // while richer foods still build trust faster.
    private static final int MIN_TRUST_TO_TAME = 6;
    private static final int TRUST_TO_TAME_VARIATION = 10;
    // Riding: forward at about 0.2 blocks a tick (4 blocks a second). A ridden mob's speed is linear in
    // its speed setting, about 2.92 blocks a tick for each point of it.
    private static final double RIDDEN_BLOCKS_PER_TICK = 0.20;
    // Holding the sprint key while riding runs it at 0.8 to 0.9 blocks a tick: 16 to 18 blocks a second.
    private static final double RIDDEN_SPRINT_MIN_BLOCKS_PER_TICK = 0.8;
    private static final double RIDDEN_SPRINT_MAX_BLOCKS_PER_TICK = 0.9;
    // Where the attachment point sits. A seated rider's hips are about 0.75 above their feet and their feet
    // hang 0.6 below the point, so this puts the hips at the top of the saddle (hump top 2.44 blocks).
    private static final double RUN_CLIP_SPEED_THRESHOLD = 0.26;
    private static final double SEAT_HEIGHT = 2.55;
    private static final double JOCKEY_SEAT_HEIGHT = 1.55;
    /** The archer sits this far along the boar's spine from its centre (positive is toward the head). */
    private static final double JOCKEY_SEAT_OFFSET = -0.15;
    /** Ridden speed builds slowly (about 3 seconds from standing to a full sprint) and bleeds off a bit quicker. */
    private static final double RIDDEN_ACCEL = 0.01;
    private static final double RIDDEN_DECEL = 0.025;
    /** A ridden charge is a short, hard burst, well past a sprint; wild boars keep the slower CHARGE_SPEED. */
    private static final double RIDDEN_CHARGE_SPEED = 1.1;
    private static final double RIDDEN_CHARGE_ACCEL = 0.08;
    private static final int RIDDEN_RAM_TICKS = 28;
    /** Left-click only rams at (nearly) a full sprint; slower than this it is the usual headbutt. */
    private static final double RIDDEN_RAM_MIN_SPEED = 0.7;
    /** Winded after any ram, then no sprinting for five seconds if it connected. */
    private static final int RIDDEN_WINDED_TICKS = 24;
    private static final double RIDDEN_WINDED_SPEED = 0.07;
    private static final int RIDDEN_SPRINT_LOCK_TICKS = 100;
    /** A charge never launches slower than this, so even one from a standstill covers real ground. */
    private static final double RIDDEN_CHARGE_MIN_START = 0.25;
    private static final int RIDDEN_CHARGE_COOLDOWN_TICKS = 120;
    private static final float RIDDEN_TURN = 0.25F;
    // Breeding: a healthy tame boar fed a cooked leg or an acorn goes into love; two in love make a snoutlet.
    private static final int LOVE_TICKS = 600;
    private static final int BREED_COOLDOWN_TICKS = 12000;
    private static final double BREED_SEEK_RANGE = 16.0;
    private static final double BREED_TOUCH_RANGE = 4.0;
    // Wild boars roll speed 0.8 to 1.2 and health 0.85 to 1.15 (41 to 55 HP); breeding can carry both further, health to 68.
    private static final float SPEED_TRAIT_MIN = 0.8F;
    private static final float SPEED_TRAIT_BRED_MAX = 1.6F;
    private static final float HEALTH_TRAIT_MIN = 0.85F;
    private static final float HEALTH_TRAIT_BRED_MAX = 1.416F;
    // How far a foal can stray from the average of its parents.
    private static final float TRAIT_DRIFT = 0.03F;
    /** Thick hide: this share of the arrows that hit a boar glance off, but tridents and spears pierce. */
    private static final float MISSILE_DEFLECT_CHANCE = 0.2F;
    @Nullable
    private AbstractArrow reflectedArrow;
    private Vec3 reflectedVelocity = Vec3.ZERO;
    private static final double OWNER_FOLLOW_START = 7.0;
    private static final double OWNER_FOLLOW_STOP = 4.0;
    private static final double OWNER_FOLLOW_FAR = 16.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final CanonicalOneShotState visualOverlayOneShot = new CanonicalOneShotState();
    private boolean overlayVisible;
    /** Ticks until this wild boar may next wander off from its wallow to graze. */
    private int grazeCooldownTicks = 600;
    private int headbuttCooldownTicks;
    private int chargeCooldownTicks = 60;
    /** A jockey boar that has just charged stays on the target, headbutting, for this many more ticks. */
    private int meleeTicks;
    private int phaseTicks;
    private int recoveryTicks;
    private int headbuttImpactDelay = -1;
    private boolean ramAnimationPlayed;
    /** Ridden momentum, in blocks per tick. Simulated by the rider's client, which is where the movement happens. */
    private double riddenSpeedNow;
    private double riddenChargeSpeed;
    private boolean headbuttRequested;
    private int sprintLockTicks;
    /** Blocks moved per tick, smoothed over a few ticks; the server cannot read the rider-simulated speed directly. */
    private double recentSpeed;

    /** Called when the rider's client reports an attack click. */
    public void requestHeadbutt() {
        headbuttRequested = true;
    }
    private boolean riddenCharging;
    private boolean ridden;
    private float riderBob;
    private float riderPitch;
    private float riderRoll;
    private Vec3 chargeDirection = Vec3.ZERO;
    // Where the target last stood: a windup that loses its target still charges there.
    private Vec3 chargeAim = Vec3.ZERO;
    @Nullable
    private UUID ownerId;
    @Nullable
    private UUID trustedId;
    private int trust;
    private int trustToTame;
    private long saddledAtTick = -100;
    private int loveTicks;
    private int breedCooldownTicks;
    /** One slot, for the saddle. The boar's own hide does the work of armor, so there is nothing else to wear. */
    private final SimpleContainer saddleSlot = new SimpleContainer(1) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.is(Items.SADDLE);
        }
    };

    public MudsnoutBoar(EntityType<? extends MudsnoutBoar> type, Level level) {
        super(type, level);
        moveControl = new MudsnoutBoarMoveControl(this);
        saddleSlot.addListener(this);
    }

    public Container getSaddleSlot() {
        return saddleSlot;
    }

    // ------------------------------------------------------------------ //
    //  Wallows                                                            //
    // ------------------------------------------------------------------ //

    /** Blocks a boar will wallow in; a tag so packs or other mud can be added without code. */
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> WALLOW_BLOCKS =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kingsandmonsters", "boar_wallows"));
    private static final int WALLOW_SEARCH_RADIUS = 20;
    /** Wild boars drift back toward a wallow they know of when it is within this many blocks. */
    private static final double WALLOW_HOME_RANGE = 40.0;

    @Nullable
    private BlockPos wallow;
    private long nextWallowScan;

    private boolean isWallowBlock(BlockPos pos) {
        return level().hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && isWallowBlockLoaded(pos);
    }

    private boolean isWallowBlockLoaded(BlockPos pos) {
        if (!level().getBlockState(pos).is(WALLOW_BLOCKS)) {
            return false;
        }
        for (int y = 1; y <= 3; y++) {
            BlockPos above = pos.above(y);
            if (!level().getBlockState(above).getCollisionShape(level(), above).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The nearest open mud this boar knows of, or null. The answer is remembered, and the search for a
     * new one only runs every ten seconds or so.
     */
    @Nullable
    BlockPos findWallow() {
        if (wallow != null && isWallowBlock(wallow)) {
            return wallow;
        }
        wallow = null;
        long now = level().getGameTime();
        if (now < nextWallowScan) {
            return null;
        }
        nextWallowScan = now + 600 + random.nextInt(600);
        BlockPos center = blockPosition();
        double best = Double.MAX_VALUE;
        for (int dx = -WALLOW_SEARCH_RADIUS; dx <= WALLOW_SEARCH_RADIUS; dx++) {
            for (int dz = -WALLOW_SEARCH_RADIUS; dz <= WALLOW_SEARCH_RADIUS; dz++) {
                int horizontalDistance = dx * dx + dz * dz;
                if (horizontalDistance > WALLOW_SEARCH_RADIUS * WALLOW_SEARCH_RADIUS
                        || horizontalDistance >= best) {
                    continue;
                }
                BlockPos column = center.offset(dx, 0, dz);
                if (!level().hasChunk(column.getX() >> 4, column.getZ() >> 4)) {
                    continue;
                }
                for (int dy = -4; dy <= 3; dy++) {
                    double distance = horizontalDistance + dy * dy;
                    if (distance >= best) {
                        continue;
                    }
                    BlockPos pos = column.offset(0, dy, 0);
                    if (isWallowBlockLoaded(pos)) {
                        best = distance;
                        wallow = pos.immutable();
                    }
                }
            }
        }
        return wallow;
    }

    @Override
    public void containerChanged(Container container) {
        boolean saddled = saddleSlot.getItem(0).is(Items.SADDLE);
        if (!level().isClientSide && saddled != isSaddled()) {
            entityData.set(DATA_SADDLED, saddled);
        }
    }

    public void playSaddleEquipSound() {
        if (!level().isClientSide) {
            playSound(SoundEvents.HORSE_SADDLE, 0.8F, 0.8F);
        }
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (!level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && isTamed() && isOwner(player)) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider((id, inventory, p) -> new MudsnoutBoarMenu(id, inventory, this),
                            getDisplayName()),
                    buffer -> buffer.writeVarInt(getId()));
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new MudsnoutBoarNavigation(this, level);
    }

    /** Whether the boar is running (fleeing), so it can turn a little tighter than when it stalks. */
    boolean isRunningForTurning() {
        return entityData.get(DATA_FLEEING);
    }

    private long lastMovingAnimationTick = Long.MIN_VALUE / 2;

    @Override
    public double getBoneResetTime() {
        // Bones a clip stops keying ease back to rest over a few ticks instead of snapping there.
        return 6.0;
    }

    private static double groundSpeedModifier(double blocksPerTick) {
        return Math.sqrt(blocksPerTick / 2.86) / SPEED_ATTRIBUTE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, ADULT_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, SPEED_ATTRIBUTE)
                .add(Attributes.FOLLOW_RANGE, 22.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.ARMOR, 18.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, PHASE_NONE);
        builder.define(DATA_OVERLAY_ID, 0);
        builder.define(DATA_OVERLAY_START, 0L);
        builder.define(DATA_FLEEING, false);
        builder.define(DATA_TAMED, false);
        builder.define(DATA_SADDLED, false);
        builder.define(DATA_SPEED_TRAIT, 0.0F);
        builder.define(DATA_HEALTH_TRAIT, 0.0F);
        builder.define(DATA_WINDED, false);
        builder.define(DATA_SPRINT_LOCKED, false);
        builder.define(DATA_BABY, false);
        builder.define(DATA_POSE, POSE_NONE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Ogres are the one thing a boar runs from, and it wins out over fighting.
        goalSelector.addGoal(1, new FearOgresGoal(this));
        // A snoutlet that is hurt bolts, with the run clip, instead of standing to fight like an adult.
        goalSelector.addGoal(1, new BabyPanicGoal(this));
        goalSelector.addGoal(2, new BoarCombatGoal(this));
        // A wild boar does not pick fights on sight: it shies from players, and one that gets close is
        // warned off with a snort before it attacks.
        goalSelector.addGoal(2, new WarnPlayerGoal(this));
        goalSelector.addGoal(3, new ShyFromPlayersGoal(this));
        goalSelector.addGoal(3, new StayWithPackGoal(this));
        goalSelector.addGoal(2, new BreedGoal(this));
        goalSelector.addGoal(3, new FollowOwnerGoal(this));
        goalSelector.addGoal(4, new JockeyPatrolGoal(this));
        goalSelector.addGoal(4, new WallowGoal(this));
        goalSelector.addGoal(4, new GrazeGoal(this));
        goalSelector.addGoal(5, new WallowStrollGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new DefendOwnerGoal(this));
        // A boar with an ogre jockey hunts what the Tribe hunts (zombies, skeletons, spiders, witches,
        // slimes, Bog Toads), the same list its rider goes by; a wild one only fights players.
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> hasOgreRider() && OgreGrunt.isTerritorialInvaderType(target)));
        // A wild boar only hunts a player on sight if it is a Tribe jockey's mount, or if that player has
        // already hurt it. Anyone else is warned first (see WarnPlayerGoal); a tame one never starts a fight.
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> !(target instanceof Player player) || !isTamed() && !player.isCreative()
                        && !player.isSpectator() && !isFriendlyTo(player)
                        && (hasOgreRider() || isAngryAt(player))));
    }

    // The rider sets the swimming pace directly, in blocks a tick, because water's own drag would hold the
    // boar to a crawl. It eases toward that pace so it still gathers and loses speed like a swimmer.
    private static final double SWIM_BLOCKS_PER_TICK = 0.09;
    private static final double SWIM_SPRINT_BLOCKS_PER_TICK = 0.18;
    private static final double SWIM_EASE = 0.2;
    private Vec3 swimVelocity = Vec3.ZERO;
    // Climbing out: how fast it rises up a bank and pushes onward, how tall a bank it manages, and how
    // long the push lasts once it has cleared the edge.
    private static final double CLIMB_OUT_SPEED = 0.3;
    private static final double CLIMB_OUT_PUSH = 0.15;
    private static final double CLIMB_OUT_MAX_RISE = 2.5;
    private static final int CLIMB_OUT_TICKS = 6;
    private int climbOutTicks;
    // A swimming boar treads water: it sits three quarters under, with its back and head clear, and rises
    // and falls around that line, slowly at rest and twice as often under way.
    private static final double TREAD_DEPTH = 0.75;
    private static final double TREAD_IDLE_RISE = 0.08;
    private static final double TREAD_MOVING_RISE = 0.1;
    private static final int TREAD_IDLE_PERIOD_TICKS = 40;
    private static final int TREAD_MOVING_PERIOD_TICKS = 20;
    private static final double TREAD_PULL = 0.3;
    private static final double TREAD_MAX_SPEED = 0.08;
    private double treadPhase;

    /** A boar floats low and treads water instead of sinking, and climbs out onto banks. */
    private void tickWaterBuoyancy() {
        if (tickClimbOut() || !isInWater()) {
            return;
        }
        double submerged = getFluidHeight(net.minecraft.tags.FluidTags.WATER);
        Vec3 motion = getDeltaMovement();
        double y = motion.y;
        // Height is left alone while it pushes against a bank, so the climb out of the water is not flattened.
        if (!horizontalCollision && !onGround() && isAfloat()) {
            y = treadWaterSpeed(submerged, motion.horizontalDistanceSqr() > 0.001);
        }
        if (getControllingPassenger() instanceof Player rider && entityData.get(DATA_PHASE) == PHASE_NONE
                && isAfloat()) {
            swimVelocity = easedSwimVelocity(rider);
        } else {
            swimVelocity = new Vec3(motion.x, 0.0, motion.z);
        }
        setDeltaMovement(swimVelocity.x, y, swimVelocity.z);
    }

    /** The next step from the current swimming velocity toward the one the rider is asking for. */
    private Vec3 easedSwimVelocity(Player rider) {
        double forward = rider.zza > 0.0F ? rider.zza : rider.zza * 0.25;
        Vec3 input = new Vec3(rider.xxa * 0.4, 0.0, forward);
        if (input.lengthSqr() > 1.0) {
            input = input.normalize();
        }
        double pace = isRiderSprinting(rider) ? SWIM_SPRINT_BLOCKS_PER_TICK : SWIM_BLOCKS_PER_TICK;
        Vec3 wanted = input.scale(pace).yRot(-getYRot() * net.minecraft.util.Mth.DEG_TO_RAD);
        return swimVelocity.add(wanted.subtract(swimVelocity).scale(SWIM_EASE));
    }

    private boolean isRiderSprinting(Player rider) {
        return (rider.isSprinting() || level().isClientSide && MudsnoutBoarClient.isSprintHeld())
                && !entityData.get(DATA_SPRINT_LOCKED);
    }

    /** Floats low, so the float goal only kicks it upward once it is nearly under. */
    @Override
    public double getFluidJumpThreshold() {
        return getBbHeight() * 0.9;
    }

    /**
     * It floats too low for the usual hop to reach the top of a bank, so one pushing into a bank, ridden
     * or following a path, is lifted up the face and carried over the edge.
     */
    private boolean tickClimbOut() {
        boolean pushing = getControllingPassenger() instanceof Player rider ? rider.zza > 0.0F
                : !getNavigation().isDone();
        if (horizontalCollision && pushing && (climbOutTicks > 0 || isInWater() && hasBankAhead())) {
            climbOutTicks = CLIMB_OUT_TICKS;
        }
        if (climbOutTicks <= 0) {
            return false;
        }
        climbOutTicks--;
        Vec3 ahead = Vec3.directionFromRotation(0.0F, getYRot()).scale(CLIMB_OUT_PUSH);
        double rise = horizontalCollision ? CLIMB_OUT_SPEED : Math.max(getDeltaMovement().y, 0.1);
        setDeltaMovement(ahead.x, rise, ahead.z);
        return true;
    }

    /** Whether there is room just ahead at some height the climb can reach. */
    private boolean hasBankAhead() {
        Vec3 ahead = Vec3.directionFromRotation(0.0F, getYRot()).scale(0.8);
        AABB moved = getBoundingBox().move(ahead.x, 0.0, ahead.z);
        for (double rise = 0.5; rise <= CLIMB_OUT_MAX_RISE; rise += 0.5) {
            if (level().noCollision(this, moved.move(0.0, rise, 0.0))) {
                return true;
            }
        }
        return false;
    }

    /**
     * In or just over water deeper than one block. It is judged from the blocks themselves, so no moment
     * of the bob at the surface counts as dry footing.
     */
    private boolean isAfloat() {
        return isWaterAt(1.05) || isWaterAt(-0.4);
    }

    private boolean isWaterAt(double rise) {
        return level().getFluidState(BlockPos.containing(getX(), getY() + rise, getZ()))
                .is(net.minecraft.tags.FluidTags.WATER);
    }

    /** The vertical speed that carries a ridden boar toward its bobbing waterline this tick. */
    private double treadWaterSpeed(double submerged, boolean moving) {
        treadPhase += Math.PI * 2.0 / (moving ? TREAD_MOVING_PERIOD_TICKS : TREAD_IDLE_PERIOD_TICKS);
        double line = getBbHeight() * TREAD_DEPTH
                + Math.sin(treadPhase) * (moving ? TREAD_MOVING_RISE : TREAD_IDLE_RISE);
        return net.minecraft.util.Mth.clamp((submerged - line) * TREAD_PULL, -TREAD_MAX_SPEED, TREAD_MAX_SPEED);
    }

    /**
     * Pack alerts set a target directly, without asking canAttack: a wild boar hurt by a player calls
     * its neighbours in, tame ones included. A tame boar never turns on its own owner.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (isBaby() && target != null) {
            return;
        }
        if (target instanceof Player player && isTamed() && isOwner(player)) {
            return;
        }
        super.setTarget(target);
    }

    /**
     * Boars run from ogres, but one that an ogre has struck, or that is already set on one, fights that ogre
     * rather than being beaten up while it flees.
     */
    private boolean isFightingBackAgainst(LivingEntity ogre) {
        return ogre == getLastHurtByMob() && tickCount - getLastHurtByMobTimestamp() < 200 || ogre == getTarget();
    }

    private boolean isFightingOgre() {
        LivingEntity hurtBy = getLastHurtByMob();
        return getTarget() instanceof OgreGrunt
                || hurtBy instanceof OgreGrunt && tickCount - getLastHurtByMobTimestamp() < 200;
    }

    /**
     * A boar will charge nearly anything, but never an ogre. Only a tamed or summoned boar, which the
     * Tribe may be fighting, answers an ogre that has struck it; a wild one, or one with a jockey, never does.
     */
    @Override
    public boolean canAttack(LivingEntity target) {
        return (!(target instanceof OgreGrunt) || (armyBoar || isTamed()) && isFightingBackAgainst(target))
                && !(target instanceof OgreGrunt summon && summon.isArmyAlly() && ownerId != null
                        && ownerId.equals(summon.getArmyOwnerId()))
                && !(armyBoar && OgreGrunt.spareByArmy(target))
                && !(target instanceof MudsnoutBoar)
                && !(target instanceof Player player && isFriendlyTo(player))
                // A jockey's boar honours whatever peace its rider has with the player.
                && !(target instanceof Player && getFirstPassenger() instanceof OgreGrunt rider
                        && !rider.canAttack(target))
                && super.canAttack(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        tickWaterBuoyancy();
        if (level().isClientSide) {
            return;
        }
        if (isDeadOrDying()) {
            // A boar that dies mid-attack lands nothing: the pending headbutt and charge die with it.
            headbuttImpactDelay = -1;
            if (entityData.get(DATA_PHASE) != PHASE_NONE) {
                setPhase(PHASE_NONE);
            }
            return;
        }
        if (armyBoar && --armyTicksLeft <= 0) {
            dismissArmyBoar();
            return;
        }
        if (isTamed() && !armyBoar && tickCount % 20 == 0
                && getOwner() instanceof net.minecraft.server.level.ServerPlayer owner) {
            com.kingsandmonsters.item.MudsnoutRecallTracker.remember(this, owner);
        }
        if (entityData.get(DATA_SPEED_TRAIT) == 0.0F) {
            // Averaging two rolls makes the extremes rare: most boars are near 10 blocks/s, a few are 8 or 12.
            entityData.set(DATA_SPEED_TRAIT, 0.8F + (random.nextFloat() + random.nextFloat()) * 0.2F);
        }
        if (entityData.get(DATA_HEALTH_TRAIT) == 0.0F) {
            entityData.set(DATA_HEALTH_TRAIT, 0.85F + (random.nextFloat() + random.nextFloat()) * 0.15F);
            applyHealthTrait();
        }
        sendReflectedArrowBack();
        chargeDustTrail();
        tickBreeding();
        headbuttCooldownTicks = Math.max(headbuttCooldownTicks - 1, 0);
        chargeCooldownTicks = Math.max(chargeCooldownTicks - 1, 0);
        recoveryTicks = Math.max(recoveryTicks - 1, 0);
        sprintLockTicks = Math.max(sprintLockTicks - 1, 0);
        if ((recoveryTicks > 0) != entityData.get(DATA_WINDED)) {
            entityData.set(DATA_WINDED, recoveryTicks > 0);
        }
        if ((sprintLockTicks > 0) != entityData.get(DATA_SPRINT_LOCKED)) {
            entityData.set(DATA_SPRINT_LOCKED, sprintLockTicks > 0);
        }
        recentSpeed = Math.max(Math.hypot(getX() - xo, getZ() - zo), recentSpeed * 0.8);
        syncRunningState();
        tickPose();
        shareTargetWithRider();
        tickProximityPursuit();
        dropPlayerTargetTheRiderSpares();
        if (recoveryTicks > 0 && getControllingPassenger() == null) {
            // Winded after a charge: stands where it stopped.
            getNavigation().stop();
            setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        }
        if (grazeCooldownTicks > 0) {
            grazeCooldownTicks--;
        }
        tickHeadbuttImpact();
        tickRidden();
    }

    // ------------------------------------------------------------------ //
    //  Attacks                                                            //
    // ------------------------------------------------------------------ //

    private void startHeadbutt() {
        headbuttCooldownTicks = HEADBUTT_COOLDOWN_TICKS;
        headbuttImpactDelay = HEADBUTT_IMPACT_TICKS;
        playOverlay(OVERLAY_HEADBUTT);
    }

    /**
     * Who a headbutt or ram can land on. A boar left to itself never hits an ogre, but a ridden one is
     * its rider's weapon: it hits whatever the rider could, ogres included, and spares the rider, the
     * rider's pets, other tame boars and players the server does not let them harm.
     */
    private boolean canStrike(LivingEntity target) {
        if (armyBoar && OgreGrunt.spareByArmy(target)) {
            return false;
        }
        if (!(getControllingPassenger() instanceof Player rider)) {
            // On its own a boar hurts what it is fighting, not whatever happens to be standing in the
            // swing or the charge's path: livestock, villagers, armor stands and other players' pets are
            // left alone. Wild ones still go for any player that is not friendly to them.
            return canAttack(target)
                    && (target == getTarget() || target == getLastHurtByMob()
                    || target instanceof Player && !isTamed()
                    || hasOgreRider() && OgreGrunt.isTerritorialInvaderType(target));
        }
        if (target == rider || target instanceof MudsnoutBoar boar && boar.isTamed()) {
            return false;
        }
        if (target instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isOwnedBy(rider)) {
            return false;
        }
        if (target instanceof Player other && (rider.isAlliedTo(other) || !rider.canHarmPlayer(other))) {
            return false;
        }
        return true;
    }

    private void tickHeadbuttImpact() {
        if (headbuttImpactDelay < 0 || headbuttImpactDelay-- > 0) {
            return;
        }
        headbuttImpactDelay = -1;
        // Everything in front within reach takes it, so it can't be dodged by a hop or a tree.
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        float damage = OgreDamage.raw(OgreDamage.Tier.BOAR, OgreDamage.Blow.STANDARD);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(HEADBUTT_REACH, 1.0, HEADBUTT_REACH),
                candidate -> candidate != this && candidate.isAlive() && canStrike(candidate))) {
            Vec3 relative = target.position().subtract(position());
            if (relative.dot(forward) < -0.2) {
                continue;
            }
            // Not through a wall: whoever is just on the other side of one is out of reach.
            if (!getSensing().hasLineOfSight(target)) {
                continue;
            }
            if (target.hurt(damageSources().mobAttack(this), damage)) {
                Vec3 away = new Vec3(target.getX() - getX(), 0.0, target.getZ() - getZ());
                away = away.lengthSqr() < 1.0E-4 ? forward : away.normalize();
                double carried = 1.0 - Math.min(1.0, Math.max(0.0,
                        target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
                target.setDeltaMovement(away.x * HEADBUTT_PUSH * carried,
                        HEADBUTT_LAUNCH * Math.max(0.3, carried), away.z * HEADBUTT_PUSH * carried);
                target.hasImpulse = true;
            }
        }
        playSound(SoundEvents.HOGLIN_ATTACK, 1.0F, 0.7F);
    }

    private void beginWindup(LivingEntity target) {
        chargeAim = target.position();
        setPhase(PHASE_WINDUP);
        phaseTicks = WINDUP_TICKS;
        getNavigation().stop();
    }

    private void beginCharge() {
        shovedThisCharge.clear();
        Vec3 toTarget = new Vec3(chargeAim.x - getX(), 0.0, chargeAim.z - getZ());
        chargeDirection = toTarget.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, getYRot())
                : toTarget.normalize();
        setPhase(PHASE_CHARGE);
        phaseTicks = CHARGE_MAX_TICKS;
        ramAnimationPlayed = false;
        playSound(ModSoundEvents.MUDSNOUT_CHARGE_LAUNCH.get(), 0.9F, 0.76F);
    }

    /** One tick of the charge; returns false when it has ended. */
    private boolean tickCharge(LivingEntity target) {
        setYRot(yawOf(chargeDirection));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        setDeltaMovement(chargeDirection.x * CHARGE_SPEED, getDeltaMovement().y, chargeDirection.z * CHARGE_SPEED);
        hasImpulse = true;

        if (!ramAnimationPlayed && target != null && distanceToSqr(target) <= RAM_ANIMATION_RANGE * RAM_ANIMATION_RANGE) {
            ramAnimationPlayed = true;
            playOverlay(OVERLAY_RAM);
        }

        // Only what the boar is charging at stops it. Anything else caught in the line is shoved aside
        // and hurt, and the charge carries on toward its target.
        shoveBystanders(chargeDirection, target);
        if (target != null && !ramAround(chargeDirection, null, target)) {
            return false;
        }
        // A wall stops it with a crash, and it has run out of breath.
        if (horizontalCollision || wallAhead(chargeDirection)) {
            wallImpact(chargeDirection);
            return false;
        }
        return --phaseTicks > 0;
    }

    /** Who has already been shoved aside by the charge in progress, so each one is only hit once. */
    private final java.util.Set<UUID> shovedThisCharge = new java.util.HashSet<>();

    /**
     * Anything other than the charge's target that is in the boar's path takes a headbutt's worth of
     * damage and is thrown to the side, out of the line, without stopping the charge. Ogres, other boars
     * and anyone friendly to this boar are left alone.
     */
    private void shoveBystanders(Vec3 direction, @Nullable LivingEntity target) {
        shoveBystanders(direction, target, null);
    }

    /** A rider's charge shoves whatever the rider could hit; a charge on its own spares ogres and boars too. */
    private void shoveBystanders(Vec3 direction, @Nullable LivingEntity target, @Nullable LivingEntity rider) {
        float damage = OgreDamage.raw(OgreDamage.Tier.BOAR, OgreDamage.Blow.STANDARD);
        for (LivingEntity bystander : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(1.2, 0.5, 1.2),
                candidate -> candidate != this && candidate != target && candidate != rider && candidate.isAlive()
                        && !(armyBoar && OgreGrunt.spareByArmy(candidate))
                        && (rider != null ? canStrike(candidate)
                                : !(candidate instanceof OgreGrunt) && !(candidate instanceof MudsnoutBoar)
                                        && !(candidate instanceof Player player
                                                && (isFriendlyTo(player) || player.isCreative()
                                                || player.isSpectator())))
                        && !shovedThisCharge.contains(candidate.getUUID()))) {
            shovedThisCharge.add(bystander.getUUID());
            bystander.hurt(damageSources().mobAttack(this), damage);
            // To whichever side of the line it is already on.
            Vec3 offset = bystander.position().subtract(position());
            double side = offset.x * -direction.z + offset.z * direction.x;
            Vec3 aside = new Vec3(-direction.z, 0.0, direction.x).scale(side >= 0.0 ? 1.0 : -1.0);
            double carried = 1.0 - Math.min(1.0, Math.max(0.0,
                    bystander.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
            bystander.setDeltaMovement(
                    (aside.x * SHOVE_SIDEWAYS + direction.x * SHOVE_FORWARD) * carried,
                    SHOVE_LIFT * Math.max(0.3, carried),
                    (aside.z * SHOVE_SIDEWAYS + direction.z * SHOVE_FORWARD) * carried);
            bystander.hasImpulse = true;
            level().playSound(null, bystander.getX(), bystander.getY(), bystander.getZ(),
                    SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 0.9F, 0.8F);
        }
    }

    /**
     * Rams everything in the boar's path with the charge's hit; returns false if it connected, which
     * ends the charge. The rider and anything friendly to them are never hit. When {@code only} is set,
     * that is the one thing it rams (a charge on its own has a single target).
     */
    private boolean ramAround(Vec3 direction, @Nullable LivingEntity rider, @Nullable LivingEntity only) {
        float damage = OgreDamage.raw(OgreDamage.Tier.BOAR, OgreDamage.Blow.HEAVY) * RAM_DAMAGE_SCALE;
        boolean rammed = false;
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(1.2, 0.5, 1.2),
                candidate -> candidate != this && candidate != rider && candidate.isAlive()
                        && (only == null || candidate == only) && canStrike(candidate))) {
            boolean hurt = victim.hurt(damageSources().mobAttack(this), damage);
            // A target still in its hit-immunity, or behind a raised shield, takes no damage but is still
            // run into: the charge stops there instead of passing straight through them.
            if (hurt || victim.invulnerableTime > 0 || victim.isBlocking()) {
                double carried = 1.0 - Math.min(1.0, Math.max(0.0,
                        victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
                victim.setDeltaMovement(direction.x * RAM_KNOCKBACK * carried,
                        RAM_LIFT * Math.max(0.3, carried), direction.z * RAM_KNOCKBACK * carried);
                victim.hasImpulse = true;
                if (hurt) {
                    // The full force of a charge leaves its victim reeling.
                    com.kingsandmonsters.effect.CombatEffects.applyDazed(victim, RAM_DAZED_TICKS, this);
                }
                rammed = true;
                ramImpactParticles(victim);
            }
        }
        if (rammed) {
            if (rider instanceof net.minecraft.server.level.ServerPlayer charger) {
                com.kingsandmonsters.advancement.RoyalAdvancementEvents.awardPath(charger, "boars/ramming_speed");
            }
            if (!ramAnimationPlayed) {
                ramAnimationPlayed = true;
                playOverlay(OVERLAY_RAM);
            }
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                    SoundSource.HOSTILE, 1.2F, 0.6F);
            playSound(SoundEvents.HOGLIN_ATTACK, 1.2F, 0.6F);
        }
        return !rammed;
    }

    /**
     * True when something a ridden charge would hit is far enough ahead that the ram clip, started now,
     * lands its impact as the boar reaches it. Without this the clip only began after the hit.
     */
    private boolean ramAhead(Vec3 direction, LivingEntity rider) {
        double speed = net.minecraft.util.Mth.clamp(recentSpeed, CHARGE_SPEED, RIDDEN_CHARGE_SPEED);
        double reach = (9 + CLIP_LAG_TICKS) * speed + RAM_CONTACT_REACH;
        AABB path = getBoundingBox().expandTowards(direction.scale(reach)).inflate(1.2, 0.5, 1.2);
        return !level().getEntitiesOfClass(LivingEntity.class, path,
                candidate -> candidate != this && candidate != rider && candidate.isAlive() && canStrike(candidate))
                .isEmpty();
    }

    /**
     * A wall within a stride of the boar's snout along {@code direction}: something at least two blocks
     * tall, such as a tree trunk or a built wall. A single block is a step the boar climbs (its step height
     * is 1.0), so the check starts just above that and a charge carries on over it.
     */
    private boolean wallAhead(Vec3 direction) {
        AABB box = getBoundingBox().move(direction.x * 0.6, 0.0, direction.z * 0.6);
        box = new AABB(box.minX, box.minY + 1.1, box.minZ, box.maxX, box.maxY - 0.2, box.maxZ);
        return level().getBlockCollisions(this, box).iterator().hasNext();
    }

    /**
     * The head slams into a wall at charge speed: the block it hit breaks if it is soft enough, and
     * either way dust, debris and a shake mark the blow. Returns after the caller has ended the charge.
     */
    private void wallImpact(Vec3 direction) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 head = position().add(direction.x * (getBbWidth() * 0.5 + 0.7), getBbHeight() * 0.6,
                direction.z * (getBbWidth() * 0.5 + 0.7));
        BlockPos hit = BlockPos.containing(head);
        if (serverLevel.getBlockState(hit).getCollisionShape(serverLevel, hit).isEmpty()) {
            hit = BlockPos.containing(head.add(direction.x * 0.6, 0.0, direction.z * 0.6));
        }
        BlockState state = serverLevel.getBlockState(hit);
        boolean broke = OgreGroundBreaker.breakBlock(serverLevel, this, hit, 0.9F);
        // A couple of neighbours crack loose too, at the head's height.
        OgreGroundBreaker.breakAround(serverLevel, this, head, 1.2, 2, 0.35F);
        if (!state.isAir()) {
            if (!broke) {
                serverLevel.levelEvent(2001, hit, Block.getId(state));
            }
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    head.x, head.y, head.z, 30, 0.5, 0.5, 0.5, 0.15);
        }
        // Clouds of dust thrown back along the way it came, and out to the sides.
        serverLevel.sendParticles(ParticleTypes.POOF, head.x, head.y, head.z, 18, 0.7, 0.5, 0.7, 0.05);
        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, head.x - direction.x * 0.4, head.y - 0.3,
                head.z - direction.z * 0.4, 6, 0.6, 0.2, 0.6, 0.02);
        serverLevel.playSound(null, head.x, head.y, head.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 0.7F, 1.5F);
        playSound(SoundEvents.HOGLIN_ATTACK, 1.2F, 0.5F);
        PacketDistributor.sendToPlayersNear(serverLevel, null, head.x, head.y, head.z, 24.0,
                new ScreenShakePayload(head.x, head.y, head.z, 0.8F));
        if (!ramAnimationPlayed) {
            ramAnimationPlayed = true;
            playOverlay(OVERLAY_RAM);
        }
    }

    private void endCharge() {
        shovedThisCharge.clear();
        setPhase(PHASE_NONE);
        chargeCooldownTicks = hasOgreRider()
                ? JOCKEY_CHARGE_COOLDOWN_TICKS + random.nextInt(JOCKEY_CHARGE_COOLDOWN_RANGE)
                : CHARGE_COOLDOWN_TICKS;
        if (hasOgreRider()) {
            meleeTicks = JOCKEY_MELEE_TICKS + random.nextInt(JOCKEY_MELEE_RANGE);
        }
        recoveryTicks = CHARGE_RECOVERY_TICKS;
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
    }

    private static float yawOf(Vec3 direction) {
        return (float) (net.minecraft.util.Mth.atan2(direction.z, direction.x) * (180.0 / Math.PI)) - 90.0F;
    }

    private void setPhase(int phase) {
        entityData.set(DATA_PHASE, phase);
    }

    private void playOverlay(int overlayId) {
        if (!level().isClientSide) {
            entityData.set(DATA_OVERLAY_ID, overlayId);
            entityData.set(DATA_OVERLAY_START, level().getGameTime());
        }
    }

    /**
     * Re-derives the synced "running" state from the goals that are actually running each tick, so
     * the run clip can't be left on by a goal that ended without clearing it: fleeing only while the
     * ogre-fear goal runs, and no windup or charge phase once the combat goal has let go.
     */
    private void syncRunningState() {
        boolean fleeing = false;
        boolean fighting = false;
        for (var wrapped : goalSelector.getAvailableGoals()) {
            if (!wrapped.isRunning()) {
                continue;
            }
            fleeing |= wrapped.getGoal() instanceof FearOgresGoal || wrapped.getGoal() instanceof BabyPanicGoal;
            fighting |= wrapped.getGoal() instanceof BoarCombatGoal;
        }
        if (entityData.get(DATA_FLEEING) != fleeing) {
            entityData.set(DATA_FLEEING, fleeing);
        }
        // A boar a player is riding has its own goals stood down, so no combat goal is ever running; its
        // wind-up and charge belong to the rider and are ended by tickRidden, not cleared here.
        boolean riderCharge = getControllingPassenger() instanceof Player && ridden;
        if (!fighting && !riderCharge && entityData.get(DATA_PHASE) != PHASE_NONE) {
            setPhase(PHASE_NONE);
        }
    }

    void setFleeing(boolean fleeing) {
        entityData.set(DATA_FLEEING, fleeing);
    }

    // ------------------------------------------------------------------ //
    //  Taming                                                             //
    // ------------------------------------------------------------------ //

    public boolean isTamed() {
        return entityData.get(DATA_TAMED);
    }

    // --- Summon Army: a boar called up by the Ogre King's Crown to carry an Archer into battle ----------

    private boolean armyBoar;
    private int armyTicksLeft;

    /** The player whose army this boar is in, or null for any other boar. */
    @Nullable
    public UUID getArmyOwnerId() {
        return armyBoar ? ownerId : null;
    }

    /**
     * Puts this boar in the player's army for {@code ticks}. It is tamed to them for that time, so it
     * follows them and defends them without ever counting as a tamed boar of theirs: no recall, no
     * advancement, no loot, and it goes when the time is up.
     */
    public void joinArmy(Player owner, int ticks) {
        armyBoar = true;
        armyTicksLeft = ticks;
        ownerId = owner.getUUID();
        entityData.set(DATA_TAMED, true);
        setPersistenceRequired();
    }

    private void dismissArmyBoar() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.5, getZ(),
                    18, getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 0.03);
            serverLevel.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.6F, 0.7F);
        }
        ejectPassengers();
        discard();
    }

    @Override
    protected boolean shouldDropLoot() {
        return !armyBoar && super.shouldDropLoot();
    }

    @Override
    protected int getBaseExperienceReward() {
        return armyBoar ? 0 : super.getBaseExperienceReward();
    }

    // ------------------------------------------------------------------ //
    //  Poses and growing up                                               //
    // ------------------------------------------------------------------ //

    private int poseTicks;
    private int babyTicks;
    private boolean sitting;

    private int getBodyPose() {
        return entityData.get(DATA_POSE);
    }

    private void setBodyPose(int pose, int ticks) {
        entityData.set(DATA_POSE, pose);
        poseTicks = ticks;
    }

    /** True while lying, sitting or changing between the two: the boar holds its ground. */
    boolean isPoseLocked() {
        return getBodyPose() != POSE_NONE;
    }

    /** Lies down in the mud: the splash comes as he lands, and he stays down until told to rise. */
    void lieDown() {
        getNavigation().stop();
        setBodyPose(POSE_LAYING_DOWN, LAY_CLIP_TICKS);
    }

    /** True once the boar has landed and is resting in the mud. */
    boolean isLyingInMud() {
        return getBodyPose() == POSE_LAYING;
    }

    boolean isGettingUp() {
        return getBodyPose() == POSE_BACKUP;
    }

    void getUp() {
        setBodyPose(POSE_BACKUP, LAY_CLIP_TICKS);
    }

    /** Drops any pose at once, for a boar that is hurt, scared or being mounted. */
    void snapUp() {
        sitting = false;
        setBodyPose(POSE_NONE, 0);
    }

    /** The owner's sit and stand command, like a dog's. */
    void toggleSit() {
        int pose = getBodyPose();
        if (isBaby() || isVehicle() || pose == POSE_SIT_DOWN || pose == POSE_STAND) {
            return;
        }
        if (sitting) {
            sitting = false;
            setBodyPose(POSE_STAND, SIT_CLIP_TICKS);
        } else if (pose == POSE_NONE && onGround()) {
            sitting = true;
            setTarget(null);
            getNavigation().stop();
            setBodyPose(POSE_SIT_DOWN, SIT_CLIP_TICKS);
        }
    }

    private void tickPose() {
        if (isBaby() && ++babyTicks >= GROW_UP_TICKS) {
            setBaby(false);
        }
        int pose = getBodyPose();
        if (pose == POSE_NONE) {
            return;
        }
        // Every pose keeps the boar where it is.
        getNavigation().stop();
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(0.0, movement.y, 0.0);
        if (poseTicks > 0 && --poseTicks == 0) {
            switch (pose) {
                case POSE_LAYING_DOWN -> {
                    splash();
                    entityData.set(DATA_POSE, POSE_LAYING);
                }
                case POSE_SIT_DOWN -> entityData.set(DATA_POSE, POSE_SITTING);
                case POSE_BACKUP, POSE_STAND -> entityData.set(DATA_POSE, POSE_NONE);
                default -> { }
            }
        }
    }

    /** The one burst of mud as he drops into the wallow. */
    private void splash() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState mud = serverLevel.getBlockState(getOnPos());
        if (!mud.is(WALLOW_BLOCKS)) {
            mud = net.minecraft.world.level.block.Blocks.MUD.defaultBlockState();
        }
        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, mud),
                getX(), getY() + 0.3, getZ(), 40, 0.9, 0.25, 0.9, 0.15);
        playSound(SoundEvents.MUD_PLACE, 1.2F, 0.7F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (armyBoar && source.getEntity() instanceof Player attacker && attacker.getUUID().equals(ownerId)) {
            return false;
        }
        // The Tribe's own blows (a swing, a ripple or a stray arrow) never hurt a wild boar or an ogre's
        // mount, so one of them never starts a fight between a guard and a boar.
        if (!armyBoar && !isTamed() && source.getEntity() instanceof OgreGrunt striker
                && !striker.isArmyAlly() && !(striker instanceof PlagueOgre)) {
            return false;
        }
        if (!level().isClientSide && source.getDirectEntity() instanceof AbstractArrow arrow
                && com.kingsandmonsters.item.BlackhideSet.isDeflectable(arrow)
                && random.nextFloat() < MISSILE_DEFLECT_CHANCE) {
            // Refusing the hit makes the arrow glance off. It is then sent back the way it came, at
            // the speed it arrived with, in the next tick (the arrow slows its own bounce this one).
            reflectedArrow = arrow;
            reflectedVelocity = arrow.getDeltaMovement().scale(-1.0);
            playSound(SoundEvents.SHIELD_BLOCK, 0.8F, 1.4F);
            // A glanced-off arrow still counts as an attack: the shooter has picked a fight.
            if (source.getEntity() instanceof Player shooter) {
                angerAt(shooter);
            }
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && amount > 0.0F && !level().isClientSide && isPoseLocked()) {
            snapUp();
        }
        if (hurt && amount > 0.0F && !level().isClientSide && source.getEntity() instanceof Player attacker) {
            angerAt(attacker);
        }
        return hurt;
    }

    /**
     * Hitting a boar, or shooting at it, breaks whatever trust it had in you, and a wild one will not
     * forgive you for a while even if you are holding food: a fed, calmed boar used to stay friendly
     * however hard it was hit.
     */
    private void angerAt(Player attacker) {
        if (isTamed()) {
            return;
        }
        if (attacker.getUUID().equals(trustedId)) {
            trustedId = null;
            trust = 0;
        }
        angeredBy = attacker.getUUID();
        angryUntil = level().getGameTime() + ANGER_MEMORY_TICKS;
    }

    /** How long a wild boar stays set against a player who hurt it, whatever they are holding. */
    private static final int ANGER_MEMORY_TICKS = 20 * 60 * 3;
    @Nullable
    private UUID angeredBy;
    private long angryUntil;

    /**
     * The boar goes after whatever its jockey has picked, and whatever has just hurt its jockey, as well as
     * its own targets. A boar does not pick fights with most mobs on its own, so without this a rider
     * under attack from one it ignores was left to be hit while the boar stood passive.
     */
    private void shareTargetWithRider() {
        if (tickCount % 10 != 0 || !(getFirstPassenger() instanceof OgreArcher archer)) {
            return;
        }
        LivingEntity current = getTarget();
        if (current != null && current.isAlive()) {
            return;
        }
        LivingEntity riderTarget = archer.getTarget();
        if (riderTarget != null && riderTarget.isAlive() && canAttack(riderTarget)) {
            setTarget(riderTarget);
            return;
        }
        LivingEntity attacker = archer.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && attacker != this && !(attacker instanceof OgreGrunt)
                && archer.tickCount - archer.getLastHurtByMobTimestamp() < JOCKEY_REVENGE_TICKS
                && canAttack(attacker)) {
            setTarget(attacker);
        }
    }

    /** How long after its jockey is hurt a boar will still go after whatever did it. */
    private static final int JOCKEY_REVENGE_TICKS = 200;

    /** A rare boar comes with an Ogre Archer on its back; the spawner adds him along with the boar. */
    public void addJockey(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty) {
        if (!hasJockeyRoom(level)) {
            // Under a roof, ledge or overhang the rider would sit inside the blocks and suffocate.
            return;
        }
        OgreArcher archer = com.kingsandmonsters.ModEntities.OGRE_ARCHER.get().create(level.getLevel());
        if (archer == null) {
            return;
        }
        archer.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        archer.finalizeSpawn(level, difficulty, MobSpawnType.JOCKEY, null);
        archer.setPersistenceRequired();
        // Its first charge comes a few seconds into the fight, not the instant it sees a target.
        chargeCooldownTicks = 200 + random.nextInt(JOCKEY_CHARGE_COOLDOWN_RANGE);
        // A boar still being spawned is added with its passengers by the spawner, like a spider jockey;
        // adding the rider first would tell clients about a rider on a boar they have never heard of.
        if (!isAddedToLevel()) {
            archer.startRiding(this, true);
            return;
        }
        level.addFreshEntity(archer);
        if (!archer.startRiding(this, true)) {
            archer.discard();
        }
    }

    /** Whether the space above the boar is clear for a seated Archer, seat height plus his full height. */
    private boolean hasJockeyRoom(ServerLevelAccessor level) {
        double archerHeight = com.kingsandmonsters.ModEntities.OGRE_ARCHER.get().getDimensions().height();
        net.minecraft.world.phys.AABB column = new net.minecraft.world.phys.AABB(
                getX() - 0.5, getY() + getBbHeight(), getZ() - 0.5,
                getX() + 0.5, getY() + JOCKEY_SEAT_HEIGHT + archerHeight + 0.2, getZ() + 0.5);
        return level.getLevel().noCollision(this, column);
    }

    @Override
    public boolean isBaby() {
        return entityData.get(DATA_BABY);
    }

    public void setBaby(boolean baby) {
        entityData.set(DATA_BABY, baby);
        babyTicks = 0;
        var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue((baby ? BABY_HEALTH : ADULT_HEALTH) * healthTraitOrAverage());
            setHealth(getMaxHealth());
        }
    }

    private double healthTraitOrAverage() {
        float trait = entityData.get(DATA_HEALTH_TRAIT);
        return trait == 0.0F ? 1.0 : trait;
    }

    /** Sets the max health from the trait, keeping the same share of it. */
    private void applyHealthTrait() {
        var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            float share = getHealth() / getMaxHealth();
            health.setBaseValue((isBaby() ? BABY_HEALTH : ADULT_HEALTH) * healthTraitOrAverage());
            setHealth(getMaxHealth() * share);
        }
    }

    /** The arrow a deflection glanced off flies back at its shooter, now the boar's own, and cannot be picked up. */
    private void sendReflectedArrowBack() {
        AbstractArrow arrow = reflectedArrow;
        if (arrow == null) {
            return;
        }
        reflectedArrow = null;
        if (arrow.isRemoved()) {
            return;
        }
        arrow.setOwner(this);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setDeltaMovement(reflectedVelocity);
        arrow.setYRot((float) (net.minecraft.util.Mth.atan2(reflectedVelocity.x, reflectedVelocity.z) * 57.3F));
        arrow.setXRot((float) (net.minecraft.util.Mth.atan2(reflectedVelocity.y, reflectedVelocity.horizontalDistance()) * 57.3F));
        arrow.hurtMarked = true;
    }

    public float getSpeedTrait() {
        return entityData.get(DATA_SPEED_TRAIT);
    }

    public float getHealthTrait() {
        return entityData.get(DATA_HEALTH_TRAIT);
    }

    /** Lang key of the speed tier this boar falls in. */
    public String getSpeedTierKey() {
        float trait = getSpeedTrait();
        String tier = trait < 0.9F ? "sluggish" : trait < 1.1F ? "steady" : trait < 1.25F ? "swift"
                : trait < 1.45F ? "fleet" : "blazing";
        return "boar.kingsandmonsters.speed." + tier;
    }

    /** Lang key of the health tier this boar falls in. */
    public String getHealthTierKey() {
        float trait = getHealthTrait();
        String tier = trait < 0.92F ? "frail" : trait < 1.08F ? "hardy" : trait < 1.2F ? "sturdy"
                : trait < 1.32F ? "mighty" : "titanic";
        return "boar.kingsandmonsters.health." + tier;
    }

    // ------------------------------------------------------------------ //
    //  Breeding                                                           //
    // ------------------------------------------------------------------ //

    boolean isInLove() {
        return loveTicks > 0;
    }

    private void tickBreeding() {
        breedCooldownTicks = Math.max(breedCooldownTicks - 1, 0);
        if (loveTicks > 0) {
            loveTicks--;
        }
    }

    private static float inheritTrait(float a, float b, float min, float max, RandomSource random) {
        float average = (a == 0.0F ? 1.0F : a) / 2.0F + (b == 0.0F ? 1.0F : b) / 2.0F;
        return net.minecraft.util.Mth.clamp(average + (float) random.nextGaussian() * TRAIT_DRIFT, min, max);
    }

    /** Two boars in love meet: a snoutlet is born with traits near the average of its parents. */
    private void breedWith(MudsnoutBoar partner) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        MudsnoutBoar baby = com.kingsandmonsters.ModEntities.MUDSNOUT_BOAR.get().create(serverLevel);
        if (baby != null) {
            baby.entityData.set(DATA_SPEED_TRAIT, inheritTrait(getSpeedTrait(), partner.getSpeedTrait(),
                    SPEED_TRAIT_MIN, SPEED_TRAIT_BRED_MAX, random));
            baby.entityData.set(DATA_HEALTH_TRAIT, inheritTrait(getHealthTrait(), partner.getHealthTrait(),
                    HEALTH_TRAIT_MIN, HEALTH_TRAIT_BRED_MAX, random));
            baby.moveTo((getX() + partner.getX()) / 2.0, getY(), (getZ() + partner.getZ()) / 2.0,
                    random.nextFloat() * 360.0F, 0.0F);
            baby.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                    MobSpawnType.BREEDING, null);
            baby.setBaby(true);
            baby.ownerId = ownerId;
            baby.entityData.set(DATA_TAMED, true);
            baby.setPersistenceRequired();
            serverLevel.addFreshEntityWithPassengers(baby);
            serverLevel.sendParticles(ParticleTypes.HEART, baby.getX(), baby.getY() + 0.6, baby.getZ(),
                    7, 0.4, 0.3, 0.4, 0.02);
            serverLevel.addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(
                    serverLevel, getX(), getY(), getZ(), random.nextInt(7) + 1));
        }
        loveTicks = 0;
        partner.loveTicks = 0;
        breedCooldownTicks = BREED_COOLDOWN_TICKS;
        partner.breedCooldownTicks = BREED_COOLDOWN_TICKS;
    }

    /** A boar in love walks to the nearest other boar in love and, once they meet, they breed. */
    private static final class BreedGoal extends Goal {
        private final MudsnoutBoar boar;
        private MudsnoutBoar partner;
        private int repathTicks;

        private BreedGoal(MudsnoutBoar boar) {
            this.boar = boar;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean eligible(MudsnoutBoar other) {
            return other.isAlive() && other.isInLove() && other.isTamed() && !other.isBaby()
                    && !other.isPoseLocked() && !other.isVehicle();
        }

        @Override
        public boolean canUse() {
            if (!boar.isInLove() || boar.isBaby() || boar.isPoseLocked() || boar.isVehicle()
                    || boar.entityData.get(DATA_PHASE) != PHASE_NONE) {
                return false;
            }
            partner = boar.level().getEntitiesOfClass(MudsnoutBoar.class,
                            boar.getBoundingBox().inflate(BREED_SEEK_RANGE),
                            other -> other != boar && eligible(other)).stream()
                    .min(java.util.Comparator.comparingDouble(boar::distanceToSqr)).orElse(null);
            return partner != null;
        }

        @Override
        public boolean canContinueToUse() {
            return boar.isInLove() && partner != null && eligible(partner) && boar.getTarget() == null;
        }

        @Override
        public void start() {
            repathTicks = 0;
        }

        @Override
        public void stop() {
            partner = null;
            boar.getNavigation().stop();
        }

        @Override
        public void tick() {
            boar.getLookControl().setLookAt(partner, 30.0F, 30.0F);
            if (boar.distanceToSqr(partner) <= BREED_TOUCH_RANGE * BREED_TOUCH_RANGE) {
                // Only one of the two starts it, so a pair makes a single foal.
                if (boar.getId() < partner.getId() && boar.breedCooldownTicks <= 0
                        && partner.breedCooldownTicks <= 0) {
                    boar.breedWith(partner);
                }
                return;
            }
            if (--repathTicks <= 0) {
                boar.getNavigation().moveTo(partner, FOLLOW_SPEED);
                repathTicks = 10;
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_BABY.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        return isBaby() ? EntityDimensions.scalable(0.9F, 0.8F).withEyeHeight(0.6F) : super.getDefaultDimensions(pose);
    }

    /** Rare roaming groups are two adults with one snoutlet; wallow families are spawned explicitly. */
    @Override
    @Nullable
    @SuppressWarnings("deprecation")
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable net.minecraft.world.entity.SpawnGroupData group) {
        net.minecraft.world.entity.SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, group);
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            MudsnoutPackData pack = group instanceof MudsnoutPackData existing
                    ? existing : new MudsnoutPackData();
            setBaby(pack.spawned >= 2);
            pack.spawned++;
            return pack;
        }
        return data != null ? data : new net.minecraft.world.entity.SpawnGroupData() { };
    }

    private static final class MudsnoutPackData implements net.minecraft.world.entity.SpawnGroupData {
        private int spawned;
    }

    public boolean isSaddled() {
        return entityData.get(DATA_SADDLED);
    }

    @Nullable
    public Player getOwner() {
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    public boolean isOwner(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    /** One line saying why this boar would or would not go for this player; for the debug command. */
    public String describeAggroFor(Player player) {
        return String.format(java.util.Locale.ROOT,
                "%s dist=%.1f baby=%s tamed=%s pose=%d phase=%d target=%s friendly=%s canAttack=%s los=%s"
                        + " fleeing=%s ogreRider=%s hurtTime=%d navDone=%s",
                blockPosition().toShortString(), Math.sqrt(distanceToSqr(player)), isBaby(), isTamed(),
                getBodyPose(), entityData.get(DATA_PHASE),
                getTarget() == null ? "none" : getTarget().getName().getString(), isFriendlyTo(player),
                canAttack(player), getSensing().hasLineOfSight(player), entityData.get(DATA_FLEEING),
                hasOgreRider(), hurtTime, getNavigation().isDone());
    }

    /** Toad meat in hand, a trusted feeder or the owner: someone a boar does not charge. */
    private boolean isFriendlyTo(Player player) {
        if (isTamed()) {
            return isOwner(player);
        }
        if (angeredBy != null && player.getUUID().equals(angeredBy) && level().getGameTime() < angryUntil) {
            return false;
        }
        return trustValue(player.getMainHandItem()) > 0 || trustValue(player.getOffhandItem()) > 0
                || trust > 0 && player.getUUID().equals(trustedId);
    }

    /** How much trust one of these foods earns; 0 for anything a boar won't take. */
    private static int trustValue(ItemStack stack) {
        if (stack.is(ModItems.RAW_TOAD_LEG.get()) || stack.is(Items.APPLE)) {
            return 1;
        }
        if (stack.is(ModItems.COOKED_TOAD_LEG.get())) {
            return 2;
        }
        if (stack.is(ModItems.ACORN.get())) {
            return 3;
        }
        return 0;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hasOgreRider()) {
            // A boar with a jockey belongs to the Tribe: no taming, no saddle.
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        int value = trustValue(stack);
        if (isBaby()) {
            // A snoutlet takes food to grow up faster, and nothing else: no taming, saddle or sitting.
            if (value == 0) {
                return super.mobInteract(player, hand);
            }
            if (!level().isClientSide) {
                eat(player, stack);
                babyTicks += FEEDING_GROWTH_TICKS;
                playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.3F);
                playOverlay(OVERLAY_EAT);
                sendParticles(ParticleTypes.HAPPY_VILLAGER, 4);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isTamed() && isOwner(player)) {
            if (!isSaddled() && stack.is(Items.SADDLE)) {
                // Like a horse: the saddle goes on, and the boar's look changes with it.
                if (!level().isClientSide) {
                    saddledAtTick = level().getGameTime();
                    saddleSlot.setItem(0, stack.copyWithCount(1));
                    playSaddleEquipSound();
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (player.isSecondaryUseActive() && stack.isEmpty() && !isVehicle()) {
                // Sneak and right-click with a free hand: sit, or stand back up, like a dog.
                if (!level().isClientSide) {
                    toggleSit();
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (player.isSecondaryUseActive() && !isVehicle()) {
                openCustomInventoryScreen(player);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (isSaddled() && value == 0 && !player.isSecondaryUseActive() && !isVehicle()) {
                // A held right-click repeats; the click that saddled the boar must not also mount it.
                if (!level().isClientSide && level().getGameTime() - saddledAtTick > 20) {
                    snapUp();
                    player.startRiding(this);
                    if (player instanceof net.minecraft.server.level.ServerPlayer mounter) {
                        com.kingsandmonsters.advancement.RoyalAdvancementEvents.awardPath(mounter, "boars/hog_wild");
                    }
                    player.displayClientMessage(Component.translatable("boar.kingsandmonsters.traits",
                            Component.translatable(getSpeedTierKey()), Component.translatable(getHealthTierKey())), true);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        if (value == 0 || isTamed() && !isOwner(player)) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (isTamed() && getHealth() >= getMaxHealth()
                && !(value >= 2 && breedCooldownTicks <= 0 && loveTicks <= 0)) {
            // Nothing for the food to do: it is neither hurt nor ready to breed.
            return InteractionResult.CONSUME;
        }

        eat(player, stack);
        // A boar lying in its wallow gets up for food, rising the way it does when it is done resting.
        // (A tame boar told to sit stays sitting: that is the owner's command.)
        if (getBodyPose() == POSE_LAYING_DOWN || getBodyPose() == POSE_LAYING) {
            getUp();
        }
        playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.7F);
        playOverlay(OVERLAY_EAT);
        if (isTamed()) {
            if (getHealth() < getMaxHealth()) {
                heal(value * 4.0F);
                sendParticles(ParticleTypes.HAPPY_VILLAGER, 5);
            } else {
                loveTicks = LOVE_TICKS;
                sendParticles(ParticleTypes.HEART, 7);
            }
            return InteractionResult.CONSUME;
        }

        if (!player.getUUID().equals(trustedId)) {
            trustedId = player.getUUID();
            trust = 0;
        }
        trust += value;
        // Eating calms it: it drops whatever it was after and stops where it stands.
        setTarget(null);
        getNavigation().stop();
        if (trustToTame <= 0) {
            trustToTame = MIN_TRUST_TO_TAME + getRandom().nextInt(TRUST_TO_TAME_VARIATION);
        }
        if (trust >= trustToTame) {
            tame(player);
        } else {
            sendParticles(ParticleTypes.SMOKE, 4);
        }
        return InteractionResult.CONSUME;
    }

    private void eat(Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            return;
        }
        stack.shrink(1);
    }

    private void tame(Player player) {
        ownerId = player.getUUID();
        trustedId = null;
        trust = 0;
        entityData.set(DATA_TAMED, true);
        if (player instanceof net.minecraft.server.level.ServerPlayer tamer) {
            com.kingsandmonsters.item.MudsnoutRecallTracker.remember(this, tamer);
            com.kingsandmonsters.advancement.RoyalAdvancementEvents.awardPath(tamer, "boars/swine_friend");
        }
        setPersistenceRequired();
        setTarget(null);
        sendParticles(ParticleTypes.HEART, 7);
    }

    /** The burst where a charge lands: the same puff, dust and sparks the Blackhide set's impacts throw. */
    private void ramImpactParticles(LivingEntity victim) {
        if (level() instanceof ServerLevel serverLevel) {
            double y = victim.getY() + victim.getBbHeight() * 0.5;
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), y, victim.getZ(), 1, 0.3, 0.3, 0.3, 0.0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, victim.getX(), y, victim.getZ(), 14, 0.35, 0.3, 0.35, 0.18);
            serverLevel.sendParticles(ParticleTypes.CRIT, victim.getX(), y, victim.getZ(), 16, 0.4, 0.4, 0.4, 0.5);
        }
    }

    /** A charging boar kicks up a dust trail behind it, like a Charged Blackhide wearer. */
    private void chargeDustTrail() {
        if (tickCount % 2 == 0 && entityData.get(DATA_PHASE) == PHASE_CHARGE
                && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 1, 0.3, 0.0, 0.3, 0.01);
        }
    }

    private void sendParticles(net.minecraft.core.particles.ParticleOptions particle, int count) {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, getX(), getY() + 1.4, getZ(), count, 0.6, 0.5, 0.6, 0.02);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        if (trustedId != null) {
            tag.putUUID("TrustedPlayer", trustedId);
        }
        tag.putInt("Trust", trust);
        if (armyBoar) {
            tag.putBoolean("ArmyBoar", true);
            tag.putInt("ArmyTicks", armyTicksLeft);
        }
        tag.putInt("TrustToTame", trustToTame);
        tag.putFloat("SpeedTrait", entityData.get(DATA_SPEED_TRAIT));
        tag.putFloat("HealthTrait", entityData.get(DATA_HEALTH_TRAIT));
        tag.putInt("BreedCooldown", breedCooldownTicks);
        tag.putBoolean("Baby", isBaby());
        tag.putInt("BabyTicks", babyTicks);
        tag.putBoolean("Sitting", sitting);
        if (!saddleSlot.getItem(0).isEmpty()) {
            tag.put("SaddleItem", saddleSlot.getItem(0).save(registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        trustedId = tag.hasUUID("TrustedPlayer") ? tag.getUUID("TrustedPlayer") : null;
        trust = tag.getInt("Trust");
        armyBoar = tag.getBoolean("ArmyBoar");
        armyTicksLeft = tag.getInt("ArmyTicks");
        trustToTame = tag.getInt("TrustToTame");
        entityData.set(DATA_SPEED_TRAIT, tag.getFloat("SpeedTrait"));
        entityData.set(DATA_HEALTH_TRAIT, tag.getFloat("HealthTrait"));
        breedCooldownTicks = tag.getInt("BreedCooldown");
        entityData.set(DATA_TAMED, ownerId != null);
        // The max health a snoutlet has is saved with its attributes, so only the flag is restored here.
        entityData.set(DATA_BABY, tag.getBoolean("Baby"));
        babyTicks = tag.getInt("BabyTicks");
        sitting = tag.getBoolean("Sitting");
        entityData.set(DATA_POSE, sitting ? POSE_SITTING : POSE_NONE);
        ItemStack saddle = tag.contains("SaddleItem")
                ? ItemStack.parseOptional(registryAccess(), tag.getCompound("SaddleItem"))
                : tag.getBoolean("Saddled") ? new ItemStack(Items.SADDLE) : ItemStack.EMPTY;
        saddleSlot.setItem(0, saddle);
        entityData.set(DATA_SADDLED, saddle.is(Items.SADDLE));
    }

    // ------------------------------------------------------------------ //
    //  Riding                                                             //
    // ------------------------------------------------------------------ //

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return isSaddled() && getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(net.minecraft.world.entity.Entity passenger, EntityDimensions dimensions,
                                               float scale) {
        boolean jockey = passenger instanceof OgreArcher;
        Vec3 seat = new Vec3(0.0, jockey ? JOCKEY_SEAT_HEIGHT : SEAT_HEIGHT, jockey ? JOCKEY_SEAT_OFFSET : 0.0);
        // The offset follows the way the body faces, which is what the model is drawn along. It is not
        // rotated for us, and the body lags the look direction at rest, so a fixed offset drifted around
        // the saddle and settled only once the boar started to walk.
        return seat.yRot(-yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD);
    }

    /**
     * Falls hurt it the way they hurt a horse: about half a point for each block past six, so a drop that
     * would cost a player or an ordinary mob seven hearts costs a boar and its rider two. Vanilla's rule
     * for everything else is a point for each block past three, which is far too harsh on a rider.
     */
    @Override
    protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
        return net.minecraft.util.Mth.ceil((fallDistance * 0.5F - 3.0F) * damageMultiplier);
    }

    /** As on a horse, whoever is riding takes the same fall damage as the boar, instead of working out their own. */
    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        int damage = calculateFallDamage(fallDistance, damageMultiplier);
        if (damage <= 0) {
            return false;
        }
        hurt(source, damage);
        if (isVehicle()) {
            for (net.minecraft.world.entity.Entity passenger : getIndirectPassengers()) {
                passenger.hurt(source, damage);
            }
        }
        playBlockFallSound();
        return true;
    }

    /** The rider takes the seated riding pose, as on a horse. */
    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    /** A rider can sprint it with the sprint key; vanilla only allows that on mounts that say so. */
    @Override
    public boolean canSprint() {
        return true;
    }

    /** While a player rides it the boar's own goals stand down: the rider is in charge. */
    @Override
    public boolean isEffectiveAi() {
        return getControllingPassenger() == null && super.isEffectiveAi();
    }

    /** The rider steers with the camera: the boar turns toward where they look, along a smooth arc. */
    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        if (entityData.get(DATA_PHASE) == PHASE_CHARGE) {
            if (!riddenCharging) {
                // The burst launches from whatever speed the boar already has: slow from a stop, a surge at a sprint.
                riddenCharging = true;
                riddenChargeSpeed = Math.max(riddenSpeedNow, RIDDEN_CHARGE_MIN_START);
            }
            riddenChargeSpeed = Math.min(riddenChargeSpeed + RIDDEN_CHARGE_ACCEL, RIDDEN_CHARGE_SPEED);
            // The charge runs straight along the way the boar already faces.
            Vec3 dir = Vec3.directionFromRotation(0.0F, getYRot());
            setDeltaMovement(dir.x * riddenChargeSpeed, getDeltaMovement().y, dir.z * riddenChargeSpeed);
            hasImpulse = true;
            return;
        }
        if (riddenCharging) {
            // Winded after the charge: the momentum is spent.
            riddenCharging = false;
            riddenSpeedNow = 0.0;
        }
        float target = player.getYRot();
        float yaw = getYRot() + net.minecraft.util.Mth.wrapDegrees(target - getYRot()) * RIDDEN_TURN;
        setRot(yaw, player.getXRot() * 0.3F);
        yRotO = yBodyRot = yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        // Planted during the wind-up, and carried by the charge's own speed during the charge.
        if (entityData.get(DATA_PHASE) != PHASE_NONE) {
            return Vec3.ZERO;
        }
        float strafe = player.xxa * 0.4F;
        float forward = player.zza;
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }
        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        if (entityData.get(DATA_PHASE) == PHASE_WINDUP) {
            riddenSpeedNow = 0.0;
            return 0.0F;
        }
        if (entityData.get(DATA_PHASE) == PHASE_CHARGE) {
            return 0.0F;
        }
        boolean sprint = isRiderSprinting(player);
        float trait = entityData.get(DATA_SPEED_TRAIT);
        // The speed trait runs 0.8 to 1.2 in the wild, spreading the sprint across 16 to 18 blocks a second;\n        // bred boars carry it on to 1.6, which is 20.
        double traitShare = trait == 0.0F ? 0.5 : net.minecraft.util.Mth.clamp((trait - 0.8) / 0.4, 0.0, 2.0);
        double sprintSpeed = net.minecraft.util.Mth.lerp(traitShare, RIDDEN_SPRINT_MIN_BLOCKS_PER_TICK,
                RIDDEN_SPRINT_MAX_BLOCKS_PER_TICK);
        // The trait mostly shows at full tilt; the walking pace barely changes.
        double walkSpeed = RIDDEN_BLOCKS_PER_TICK * (trait == 0.0F ? 1.0 : 1.0 + (trait - 1.0) * 0.3);
        boolean moving = player.zza != 0.0F || player.xxa != 0.0F;
        double target = !moving ? 0.0 : sprint ? sprintSpeed : walkSpeed;
        if (entityData.get(DATA_WINDED)) {
            // Winded after the ram: a crawl for a moment.
            target = Math.min(target, RIDDEN_WINDED_SPEED);
            riddenSpeedNow = Math.min(riddenSpeedNow, RIDDEN_WINDED_SPEED);
        }
        double change = target - riddenSpeedNow;
        riddenSpeedNow += net.minecraft.util.Mth.clamp(change, -RIDDEN_DECEL, RIDDEN_ACCEL);
        return (float) (riddenSpeedNow / 2.92);
    }

    // No jump bar: the charge is a tap of the jump key, sent by the client as its own packet.
    @Override
    public void onPlayerJump(int jumpPower) {
    }

    @Override
    public boolean canJump() {
        return false;
    }

    @Override
    public void handleStartJump(int jumpPower) {
    }

    @Override
    public void handleStopJump() {
    }

    /** Set by the rider's jump-key press; read and cleared by tickRidden. */
    private boolean chargeRequested;
    /** Set by the release of the jump key; the charge launches once the minimum wind-up has passed. */
    private boolean chargeReleased;
    /** How many ticks the jump key has been held since the wind-up began. */
    private int riddenWindupHeld;

    /** Held at least this long before a release launches, so even a tap has its wind-up. */
    private static final int RIDDEN_WINDUP_MIN_HOLD = 8;
    /** Held this long and it launches by itself, at full distance: the same 2 seconds a wild boar winds up. */
    private static final int RIDDEN_WINDUP_MAX_HOLD = WINDUP_TICKS;
    /** The shortest charge, about five blocks; the longest is {@link #RIDDEN_RAM_TICKS}, about twenty-six. */
    private static final int RIDDEN_CHARGE_MIN_TICKS = 8;

    public void requestCharge() {
        chargeRequested = true;
        // A tap can bring its press and its release in the same tick; the release must outlive the start.
        chargeReleased = false;
    }

    public void releaseCharge() {
        chargeReleased = true;
    }

    /** How many ticks the launched charge runs for a given hold, from the shortest to the longest. */
    private static int riddenChargeTicksFor(int heldTicks) {
        double share = net.minecraft.util.Mth.clamp(
                (heldTicks - RIDDEN_WINDUP_MIN_HOLD) / (double) (RIDDEN_WINDUP_MAX_HOLD - RIDDEN_WINDUP_MIN_HOLD),
                0.0, 1.0);
        return (int) Math.round(net.minecraft.util.Mth.lerp(share, RIDDEN_CHARGE_MIN_TICKS, RIDDEN_RAM_TICKS));
    }

    /** Blocks a charge of this many ticks covers: it starts slow and speeds up to its top speed. */
    private static double riddenChargeDistance(int ticks) {
        double speed = RIDDEN_CHARGE_MIN_START;
        double distance = 0.0;
        for (int tick = 0; tick < ticks; tick++) {
            speed = Math.min(speed + RIDDEN_CHARGE_ACCEL, RIDDEN_CHARGE_SPEED);
            distance += speed;
        }
        return distance;
    }

    /**
     * Server side: the rider's jump key begins a full charge, the same one a wild boar throws. It stops
     * where it stands and paws the ground for the wind-up, with the same two snorts, turning with the
     * rider's aim. The rider holds the key for as long as they want the charge to run, then lets go: it
     * launches in a straight line along the way the boar faces, up to twenty blocks.
     */
    private void startRiddenWindup() {
        phaseTicks = WINDUP_TICKS;
        riddenWindupHeld = 0;
        setPhase(PHASE_WINDUP);
        ramAnimationPlayed = false;
        ridden = true;
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        riddenSpeedNow = 0.0;
    }

    /** The rider's action bar while the charge winds up: how far it will go if they let go now. */
    private void showChargeMeter(LivingEntity rider) {
        if (!(rider instanceof net.minecraft.server.level.ServerPlayer player) || riddenWindupHeld % 2 != 0) {
            return;
        }
        int segments = 10;
        int filled = net.minecraft.util.Mth.clamp(
                Math.round(riddenWindupHeld * segments / (float) RIDDEN_WINDUP_MAX_HOLD), 0, segments);
        int blocks = (int) Math.round(riddenChargeDistance(riddenChargeTicksFor(riddenWindupHeld)));
        player.displayClientMessage(Component.literal("Charge  ").withStyle(net.minecraft.ChatFormatting.GRAY)
                .append(Component.literal("|".repeat(filled)).withStyle(net.minecraft.ChatFormatting.GOLD))
                .append(Component.literal("|".repeat(segments - filled)).withStyle(net.minecraft.ChatFormatting.DARK_GRAY))
                .append(Component.literal("  " + blocks + " blocks").withStyle(net.minecraft.ChatFormatting.GRAY)), true);
    }

    /** Who the rider aimed the charge at when it launched, if anyone was in the line. */
    @Nullable
    private UUID riddenChargeTarget;

    /** The nearest thing the rider could hit that is roughly dead ahead, within the charge's reach. */
    @Nullable
    private LivingEntity pickRiddenChargeTarget(Vec3 direction, int chargeTicks) {
        LivingEntity rider = getControllingPassenger();
        double reach = riddenChargeDistance(chargeTicks);
        AABB path = getBoundingBox().expandTowards(direction.scale(reach)).inflate(2.0, 1.0, 2.0);
        LivingEntity best = null;
        double bestAhead = Double.MAX_VALUE;
        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, path,
                other -> other != this && other != rider && other.isAlive() && canStrike(other))) {
            Vec3 offset = candidate.position().subtract(position());
            double ahead = offset.x * direction.x + offset.z * direction.z;
            double across = Math.abs(offset.x * -direction.z + offset.z * direction.x);
            if (ahead > 0.5 && across <= 2.5 && ahead < bestAhead) {
                best = candidate;
                bestAhead = ahead;
            }
        }
        return best;
    }

    /** The wind-up is over: the burst launches. */
    private void beginRiddenCharge(int chargeTicks) {
        LivingEntity aimedAt = pickRiddenChargeTarget(Vec3.directionFromRotation(0.0F, getYRot()), chargeTicks);
        riddenChargeTarget = aimedAt == null ? null : aimedAt.getUUID();
        shovedThisCharge.clear();
        phaseTicks = chargeTicks;
        setPhase(PHASE_CHARGE);
        ramAnimationPlayed = false;
        playSound(ModSoundEvents.MUDSNOUT_CHARGE_LAUNCH.get(), 0.9F, 0.76F);
    }

    @Override
    public int getJumpCooldown() {
        return 0;
    }

    /**
     * Server side, every tick while ridden: a swing of the rider's arm is a headbutt, and a charge
     * rams whatever is in its path. The charge's movement itself is simulated on the rider's client.
     */
    private void tickRidden() {
        LivingEntity rider = getControllingPassenger();
        if (rider == null) {
            if (ridden && entityData.get(DATA_PHASE) == PHASE_CHARGE) {
                endCharge();
            } else if (ridden && entityData.get(DATA_PHASE) == PHASE_WINDUP) {
                // The rider got off before it launched: no charge, and no cooldown for one that never ran.
                setPhase(PHASE_NONE);
            }
            ridden = false;
            chargeRequested = false;
            chargeReleased = false;
            return;
        }
        int phase = entityData.get(DATA_PHASE);
        if (phase == PHASE_NONE && recoveryTicks <= 0) {
            if (chargeRequested && chargeCooldownTicks <= 0 && !isAfloat()) {
                startRiddenWindup();
            } else if (headbuttRequested && headbuttCooldownTicks <= 0) {
                startHeadbutt();
            }
        }
        headbuttRequested = false;
        chargeRequested = false;
        // No charging while swimming: a wind-up is dropped, and a charge that reaches deep water ends there.
        if (ridden && isAfloat()) {
            if (entityData.get(DATA_PHASE) == PHASE_WINDUP) {
                setPhase(PHASE_NONE);
                ridden = false;
            } else if (entityData.get(DATA_PHASE) == PHASE_CHARGE) {
                endCharge();
                chargeCooldownTicks = RIDDEN_CHARGE_COOLDOWN_TICKS;
                ridden = false;
            }
        }
        if (ridden && entityData.get(DATA_PHASE) == PHASE_WINDUP) {
            setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
            getNavigation().stop();
            riddenWindupHeld++;
            // The snorts come at the same moments in the wind-up as a wild boar's.
            if (riddenWindupHeld == WINDUP_TICKS - FIRST_CHARGE_SNORT_TICK) {
                playSound(ModSoundEvents.MUDSNOUT_CHARGE_SNORT_FIRST.get(), 1.0F, 1.0F);
            } else if (riddenWindupHeld == WINDUP_TICKS - SECOND_CHARGE_SNORT_TICK) {
                playSound(ModSoundEvents.MUDSNOUT_CHARGE_SNORT_SECOND.get(), 0.95F, 0.84F);
            }
            showChargeMeter(rider);
            if (chargeReleased && riddenWindupHeld >= RIDDEN_WINDUP_MIN_HOLD
                    || riddenWindupHeld >= RIDDEN_WINDUP_MAX_HOLD) {
                beginRiddenCharge(riddenChargeTicksFor(riddenWindupHeld));
                chargeReleased = false;
            }
        }
        if (ridden && entityData.get(DATA_PHASE) == PHASE_CHARGE) {
            Vec3 direction = Vec3.directionFromRotation(0.0F, getYRot());
            boolean wall = wallAhead(direction);
            if (wall) {
                wallImpact(direction);
            }
            if (!ramAnimationPlayed && !wall && ramAhead(direction, rider)) {
                ramAnimationPlayed = true;
                playOverlay(OVERLAY_RAM);
            }
            // With a target picked at launch, only it stops the charge and everything else in the line is
            // shoved aside; with none (or once it is gone), the first thing hit stops it.
            LivingEntity chargeTarget = riddenChargeTarget == null ? null
                    : level() instanceof ServerLevel serverLevel
                    && serverLevel.getEntity(riddenChargeTarget) instanceof LivingEntity found && found.isAlive()
                    ? found : null;
            if (!wall && chargeTarget != null) {
                shoveBystanders(direction, chargeTarget, rider);
            }
            boolean rammed = !wall && !ramAround(direction, rider, chargeTarget);
            if (wall || rammed || --phaseTicks <= 0) {
                // Every charge finishes with the ram, whether or not it connected.
                if (!ramAnimationPlayed) {
                    ramAnimationPlayed = true;
                    playOverlay(OVERLAY_RAM);
                }
                endCharge();
                chargeCooldownTicks = RIDDEN_CHARGE_COOLDOWN_TICKS;
                recoveryTicks = RIDDEN_WINDED_TICKS;
                if (rammed) {
                    sprintLockTicks = RIDDEN_SPRINT_LOCK_TICKS;
                }
                ridden = false;
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  Spawning                                                           //
    // ------------------------------------------------------------------ //

    /** Plains grass in decent light, like the other farm animals. */
    public static boolean checkMudsnoutSpawnRules(EntityType<MudsnoutBoar> type, ServerLevelAccessor level,
                                                  MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockState ground = level.getBlockState(pos.below());
        if (!ground.is(BlockTags.ANIMALS_SPAWNABLE_ON) || level.getRawBrightness(pos, 0) <= 8) {
            return false;
        }
        // Wild boars live at wallows. Each wallow spawns its own family when it generates, so chunk
        // generation adds nothing, and later spawns only happen on the wallow itself.
        if (spawnType == MobSpawnType.CHUNK_GENERATION) {
            return false;
        }
        if (spawnType == MobSpawnType.NATURAL) {
            if (!isAtWallow(level, pos)) {
                return false;
            }
            // Never more than a pack of three wild boars at one wallow, however many spawn attempts
            // land there. Tamed boars don't count against the wild ones.
            int nearby = level.getLevel().getEntitiesOfClass(MudsnoutBoar.class, new AABB(pos).inflate(PACK_SPAWN_CAP_RANGE),
                    boar -> !boar.isTamed()).size();
            return nearby < MAX_PACK_SIZE;
        }
        return true;
    }

    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure>
            WALLOW_STRUCTURE = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.STRUCTURE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kingsandmonsters", "mud_wallow"));

    /** True inside the footprint of a generated wallow. */
    private static boolean isAtWallow(ServerLevelAccessor level, BlockPos pos) {
        var structure = level.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(WALLOW_STRUCTURE);
        return structure != null && level.getLevel().structureManager().getStructureAt(pos, structure).isValid();
    }

    /** Packs of two or three, never a herd. */
    @Override
    public int getMaxSpawnClusterSize() {
        return 3;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (!saddleSlot.getItem(0).isEmpty()) {
            spawnAtLocation(saddleSlot.getItem(0));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ------------------------------------------------------------------ //
    //  Sounds                                                             //
    // ------------------------------------------------------------------ //

    @Override
    public float getVoicePitch() {
        return 1.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isBaby() ? ModSoundEvents.MUDSNOUT_BABY_AMBIENT.get()
                : ModSoundEvents.MUDSNOUT_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        // Mudsnouts are large, restrained animals; keep their vocals noticeable without
        // having a nearby pack constantly talking over itself. Vanilla's default is 80 ticks.
        return 260; // about 15 seconds between idle sounds on average
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isBaby() ? ModSoundEvents.MUDSNOUT_BABY_HURT.get()
                : ModSoundEvents.MUDSNOUT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isBaby() ? ModSoundEvents.MUDSNOUT_BABY_DEATH.get()
                : ModSoundEvents.MUDSNOUT_DEATH.get();
    }

    // Each hoof lands when its leg swings furthest forward (animation ticks). The walks are a four-beat
    // gait over 2s, the run a pair of hind then a pair of fore hooves every half second.
    private static final java.util.Map<String, double[]> FOOT_CONTACTS = java.util.Map.of(
            "WALK", new double[]{6.67, 13.33, 26.67, 33.33},
            "COMBAT_WALK", new double[]{3.33, 6.67, 13.33, 16.67, 23.33, 26.67, 33.33, 36.67},
            "RUN", new double[]{0.8, 5.8, 10.8, 15.8, 20.8, 25.8, 30.8, 35.8});

    /** Adults step on the animation (see {@link #playBoarFootfall}); a snoutlet patters along quietly. */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (isBaby()) {
            // Small light feet: a quiet, high patter on the ground, none of the adult's heavy hoglin thud.
            playSound(state.getSoundType(level(), pos, this).getStepSound(), 0.12F, 1.5F + random.nextFloat() * 0.2F);
        }
    }

    /** Client-side: a heavy thud on whatever it is standing on, with a grunt of hoglin step under it. */
    private void playBoarFootfall() {
        if (isBaby() || !onGround() || isInWater() || isSilent() || getBodyPose() != POSE_NONE
                || MudsnoutBoarClient.isInventoryPreviewOpen(this)) {
            return;
        }
        float variation = getRandom().nextFloat() * 0.1F;
        BlockPos ground = getOnPos();
        SoundType surface = level().getBlockState(ground).getSoundType(level(), ground, this);
        level().playLocalSound(getX(), getY(), getZ(), surface.getStepSound(), SoundSource.NEUTRAL,
                0.55F, 0.7F + variation, false);
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.HOGLIN_STEP, SoundSource.NEUTRAL,
                0.35F, 0.75F + variation, false);
    }

    // ------------------------------------------------------------------ //
    //  Animation                                                          //
    // ------------------------------------------------------------------ //

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new SynchronizedAnimationController<>(this, "Boar Movement", 5, this::animateMovement)
                .withFootfalls(FOOT_CONTACTS, this::playBoarFootfall));
        // Registered second so it is applied over the movement clip: the headbutt and the ram only
        // key the head, jaw and body. See MudsnoutBoarModel for the blend out.
        controllers.add(new SynchronizedAnimationController<>(this, "Boar Overlay", 3, this::animateOverlay));
    }

    private PlayState animateMovement(AnimationState<MudsnoutBoar> state) {
        if (isDeadOrDying()) {
            // No charge or windup clip carries on while it falls.
            return state.setAndContinue(IDLE);
        }
        switch (getBodyPose()) {
            case POSE_LAYING_DOWN -> {
                return state.setAndContinue(LAYING_DOWN);
            }
            case POSE_LAYING -> {
                return state.setAndContinue(LAY);
            }
            case POSE_BACKUP -> {
                return state.setAndContinue(BACKUP);
            }
            case POSE_SIT_DOWN -> {
                return state.setAndContinue(SIT_DOWN);
            }
            case POSE_SITTING -> {
                return state.setAndContinue(SIT);
            }
            case POSE_STAND -> {
                return state.setAndContinue(STAND);
            }
            default -> { }
        }
        int phase = entityData.get(DATA_PHASE);
        if (phase == PHASE_WINDUP) {
            return state.setAndContinue(CHARGE_INDICATOR);
        }
        if (phase == PHASE_CHARGE) {
            return state.setAndContinue(RUN);
        }
        // A short grace, counted in game ticks (this runs once per rendered frame), so a pause of an
        // instant while wandering does not flash the idle pose between two walk steps.
        long now = level().getGameTime();
        if (state.isMoving() || getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
            lastMovingAnimationTick = now;
        }
        if (now - lastMovingAnimationTick > 3) {
            return state.setAndContinue(IDLE);
        }
        if (getControllingPassenger() != null) {
            // Ridden: a combat walk, and the run clip once it is covering ground at a sprint's pace. Read
            // from how far it moved this tick, so every viewer sees the same thing.
            double moved = Math.hypot(getX() - xo, getZ() - zo);
            return state.setAndContinue(moved > RUN_CLIP_SPEED_THRESHOLD ? RUN : COMBAT_WALK);
        }
        if (entityData.get(DATA_FLEEING)) {
            return state.setAndContinue(RUN);
        }
        // Both walks play at the pace they were animated at.
        return state.setAndContinue(isAggressive() ? COMBAT_WALK : WALK);
    }

    private PlayState animateOverlay(AnimationState<MudsnoutBoar> state) {
        if (isDeadOrDying()) {
            // A headbutt, ram or meal in progress is cancelled the moment it dies.
            visualOverlayOneShot.clear();
            overlayVisible = false;
            return PlayState.STOP;
        }
        int id = entityData.get(DATA_OVERLAY_ID);
        int visualId = visualOverlayOneShot.update(state.getController(), id,
                id > 0 ? entityData.get(DATA_OVERLAY_START) : 0L,
                type -> type == OVERLAY_EAT ? EAT_OVERLAY_TICKS
                        : type == OVERLAY_GRAZE ? GRAZE_OVERLAY_TICKS : OVERLAY_TICKS, level().getGameTime());
        overlayVisible = visualId > 0;
        if (!overlayVisible) {
            return PlayState.STOP;
        }
        if (visualId == OVERLAY_EAT) {
            return state.setAndContinue(isBaby() ? BABY_EATING : EATING);
        }
        if (visualId == OVERLAY_GRAZE) {
            return state.setAndContinue(GRAZING);
        }
        return state.setAndContinue(visualId == OVERLAY_RAM ? RAM : HEADBUTT);
    }

    /** Synced, render-only: no head tracking while the boar winds up or charges. */
    public boolean isHeadTrackingSuppressed() {
        return entityData.get(DATA_PHASE) != PHASE_NONE || isPoseLocked();
    }

    /** Client-side: how far the back is currently raised by the animation, in blocks. */
    public float getRiderBob() {
        return riderBob;
    }

    /** Height of the seat above the body bone's pivot, which is what swings the rider as the body pitches. */
    private static final double LEAN_ARM = 1.3;

    /** Client-side: blocks the seat swings forward (or back) as the body pitches. */
    public double getRiderForward() {
        return -LEAN_ARM * Math.sin(riderPitch);
    }

    /** Client-side: blocks the seat drops as the body pitches either way. */
    public double getRiderLeanDrop() {
        return LEAN_ARM * (Math.cos(riderPitch) - 1.0);
    }

    /** Client-side: the back's height, pitch and roll from the body bone, which a rider follows. */
    public void setRiderPose(float bob, float pitch, float roll) {
        riderBob = bob;
        riderPitch = pitch;
        riderRoll = roll;
    }

    public float getRiderPitch() {
        return riderPitch;
    }

    public float getRiderRoll() {
        return riderRoll;
    }

    /** True while an Ogre Archer is riding this boar. */
    public boolean hasOgreRider() {
        return getFirstPassenger() instanceof OgreGrunt;
    }

    /** True on the client while a headbutt or ram clip is playing over the movement clip. */
    public boolean isOverlayVisible() {
        return overlayVisible;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ------------------------------------------------------------------ //
    //  Goals                                                              //
    // ------------------------------------------------------------------ //

    // ------------------------------------------------------------------ //
    //  Wild boar manners: shy away, warn, then attack only if ignored     //
    // ------------------------------------------------------------------ //

    /** A player this close, with the boar able to see them, gets warned. */
    private static final double WARN_RANGE = 4.5;
    /** The boar calms down once the player has backed off this far. */
    private static final double WARN_CALM_RANGE = 6.5;
    /** How long the warning lasts before a player who stayed close is attacked (3 seconds). */
    private static final int WARN_TICKS = 60;
    /** A player this close, with no way for the boar to back off, is fought without a warning. */
    private static final double CORNERED_RANGE = 2.5;
    private static final double CORNERED_ESCAPE_DISTANCE = 6.0;
    /** A fight started by proximity gives up after 5 seconds, or if the player gets this far away. */
    private static final int PROXIMITY_PURSUIT_TICKS = 100;
    private static final double PROXIMITY_PURSUIT_MAX_DISTANCE = 12.0;
    /** Pause before another warning after a player backed away, and after a proximity chase ended. */
    private static final int WARN_COOLDOWN_TICKS = 100;
    private static final int PURSUIT_COOLDOWN_TICKS = 400;
    private static final double SHY_DISTANCE = 6.0;
    private long warnCooldownUntil;
    private long proximityPursuitUntil;
    @Nullable
    private UUID proximityTargetId;

    private boolean isAngryAt(Player player) {
        return angeredBy != null && player.getUUID().equals(angeredBy) && level().getGameTime() < angryUntil;
    }

    /** A free, wild adult boar that is not busy with anything else. */
    private boolean isWildAndIdle() {
        return !isTamed() && !isBaby() && !hasOgreRider() && !isVehicle() && getTarget() == null
                && !isPoseLocked() && entityData.get(DATA_PHASE) == PHASE_NONE && !armyBoar;
    }

    private boolean canWarn(Player player) {
        return !player.isCreative() && !player.isSpectator() && player.isAlive() && !isFriendlyTo(player);
    }

    /** A jockey's boar honours the peace its rider makes: once he stops attacking a player, so does it. */
    private void dropPlayerTargetTheRiderSpares() {
        if (tickCount % 10 == 0 && entityData.get(DATA_PHASE) == PHASE_NONE
                && getTarget() instanceof Player player && getFirstPassenger() instanceof OgreGrunt rider
                && !rider.canAttack(player)) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    /** Ends a warning-triggered fight that went on too long or ran too far; a grudge has no limit. */
    private void tickProximityPursuit() {
        if (proximityTargetId == null) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.getUUID().equals(proximityTargetId)
                || target instanceof Player player && isAngryAt(player)) {
            proximityTargetId = null;
            return;
        }
        if (entityData.get(DATA_PHASE) != PHASE_NONE) {
            return;
        }
        if (level().getGameTime() >= proximityPursuitUntil
                || distanceToSqr(target) > PROXIMITY_PURSUIT_MAX_DISTANCE * PROXIMITY_PURSUIT_MAX_DISTANCE
                || !getSensing().hasLineOfSight(target)) {
            proximityTargetId = null;
            setTarget(null);
            getNavigation().stop();
            warnCooldownUntil = level().getGameTime() + PURSUIT_COOLDOWN_TICKS;
        }
    }

    /** Wild boars keep their distance from players instead of wandering up to them. */
    private static final class ShyFromPlayersGoal extends AvoidEntityGoal<Player> {
        private final MudsnoutBoar boar;

        private ShyFromPlayersGoal(MudsnoutBoar boar) {
            super(boar, Player.class, (float) SHY_DISTANCE, 0.9, 1.1);
            this.boar = boar;
        }

        @Override
        public boolean canUse() {
            return boar.isWildAndIdle() && super.canUse() && toAvoid instanceof Player player && boar.canWarn(player);
        }
    }

    /**
     * A player who gets close to a wild boar is warned: it stops, faces them and snorts, and gives them
     * three seconds to back away. If they do, it calms down; if they stay, it attacks. One with nowhere to
     * back off to fights straight away.
     */
    private static final class WarnPlayerGoal extends Goal {
        private final MudsnoutBoar boar;
        @Nullable
        private Player player;
        private int warnTicks;

        private WarnPlayerGoal(MudsnoutBoar boar) {
            this.boar = boar;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!boar.isWildAndIdle() || boar.level().getGameTime() < boar.warnCooldownUntil) {
                return false;
            }
            Player nearest = boar.level().getNearestPlayer(boar, WARN_RANGE);
            if (nearest == null || !boar.canWarn(nearest) || !boar.getSensing().hasLineOfSight(nearest)) {
                return false;
            }
            player = nearest;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return player != null && boar.getTarget() == null && boar.isWildAndIdle() && boar.canWarn(player);
        }

        @Override
        public void start() {
            warnTicks = 0;
            boar.getNavigation().stop();
            if (player != null && boar.distanceToSqr(player) <= CORNERED_RANGE * CORNERED_RANGE && boxedIn(player)) {
                attack(player);
                return;
            }
            boar.playSound(ModSoundEvents.MUDSNOUT_CHARGE_SNORT_FIRST.get(), 1.0F, 1.0F);
        }

        @Override
        public void stop() {
            boar.getNavigation().stop();
            player = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (player == null || boar.getTarget() != null) {
                return;
            }
            boar.getNavigation().stop();
            boar.getLookControl().setLookAt(player, 30.0F, 30.0F);
            boar.turnToward(player);
            double distance = Math.sqrt(boar.distanceToSqr(player));
            if (distance > WARN_CALM_RANGE) {
                // They backed off: no hard feelings, and no snort spam if they wander back right away.
                boar.warnCooldownUntil = boar.level().getGameTime() + WARN_COOLDOWN_TICKS;
                player = null;
                return;
            }
            if (++warnTicks >= WARN_TICKS) {
                if (distance <= WARN_RANGE) {
                    attack(player);
                } else {
                    // Out of the close zone but not far enough to count as retreating: warn afresh.
                    warnTicks = 0;
                }
            }
        }

        private void attack(Player target) {
            boar.proximityTargetId = target.getUUID();
            boar.proximityPursuitUntil = boar.level().getGameTime() + PROXIMITY_PURSUIT_TICKS;
            boar.setTarget(target);
        }

        /** No path 6 blocks directly away from the player: walls, water or other boars hem it in. */
        private boolean boxedIn(Player threat) {
            Vec3 away = boar.position().subtract(threat.position()).multiply(1.0, 0.0, 1.0);
            away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, boar.getYRot()) : away.normalize();
            Vec3 spot = boar.position().add(away.scale(CORNERED_ESCAPE_DISTANCE));
            return boar.getNavigation().createPath(spot.x, spot.y, spot.z, 0) == null;
        }
    }

    /** A hurt snoutlet runs for it; the movement clip reads the same "fleeing" flag as the ogre fear goal. */
    private static final class BabyPanicGoal extends PanicGoal {
        private final MudsnoutBoar boar;

        private BabyPanicGoal(MudsnoutBoar boar) {
            super(boar, groundSpeedModifier(0.18));
            this.boar = boar;
        }

        @Override
        public boolean canUse() {
            return boar.isBaby() && !boar.isPoseLocked() && super.canUse();
        }
    }

    /** Running from an ogre also sets the run animation, so the herd visibly bolts. */
    private static final class FearOgresGoal extends AvoidEntityGoal<OgreGrunt> {
        private final MudsnoutBoar boar;

        private FearOgresGoal(MudsnoutBoar boar) {
            super(boar, OgreGrunt.class, (float) OGRE_FEAR_DISTANCE, 1.1, 1.5);
            this.boar = boar;
        }

        /** A tamed boar has learned the Tribe's smell and no longer bolts. */
        @Override
        public boolean canUse() {
            return !boar.isTamed() && !boar.hasOgreRider() && !boar.isFightingOgre() && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            boar.setFleeing(true);
        }

        @Override
        public void stop() {
            super.stop();
            boar.setFleeing(false);
        }
    }

    /**
     * The boar's whole fight: close in, headbutt at close range, and from a few blocks off paw the
     * ground, then charge the spot where the target stood.
     */
    private static final class BoarCombatGoal extends Goal {
        private final MudsnoutBoar boar;
        private int repathTicks;
        // A jockey boar skirmishes: it holds a band of distance, circles, and plants itself now and then.
        private int pauseTicks;
        private boolean legStarted;
        private int strafeSign = 1;

        private BoarCombatGoal(MudsnoutBoar boar) {
            this.boar = boar;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = boar.getTarget();
            return target != null && target.isAlive() && !boar.isBaby() && !boar.isPoseLocked();
        }

        @Override
        public boolean canContinueToUse() {
            // A started windup or charge plays out even if the target is lost.
            if (boar.entityData.get(DATA_PHASE) != PHASE_NONE) {
                return true;
            }
            LivingEntity target = boar.getTarget();
            return target != null && target.isAlive()
                    && (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator());
        }

        @Override
        public void start() {
            boar.setAggressive(true);
            repathTicks = 0;
        }

        @Override
        public void stop() {
            boar.setAggressive(false);
            boar.getNavigation().stop();
            if (boar.entityData.get(DATA_PHASE) != PHASE_NONE) {
                boar.endCharge();
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = boar.getTarget();
            int phase = boar.entityData.get(DATA_PHASE);

            if (phase == PHASE_CHARGE) {
                if (!boar.tickCharge(target)) {
                    boar.endCharge();
                }
                return;
            }
            if (phase == PHASE_WINDUP) {
                if (target != null && target.isAlive()) {
                    boar.getLookControl().setLookAt(target, 40.0F, 40.0F);
                    boar.snapFace(target);
                    boar.chargeAim = target.position();
                }
                boar.getNavigation().stop();
                boar.setDeltaMovement(0.0, boar.getDeltaMovement().y, 0.0);
                if (boar.phaseTicks == FIRST_CHARGE_SNORT_TICK) {
                    boar.playSound(ModSoundEvents.MUDSNOUT_CHARGE_SNORT_FIRST.get(), 1.0F, 1.0F);
                } else if (boar.phaseTicks == SECOND_CHARGE_SNORT_TICK) {
                    // A lower, longer second warning keeps the buildup from sounding like
                    // the first snort was simply triggered twice.
                    boar.playSound(ModSoundEvents.MUDSNOUT_CHARGE_SNORT_SECOND.get(), 0.95F, 0.84F);
                }
                if (--boar.phaseTicks <= 0) boar.beginCharge();
                return;
            }
            if (target == null || boar.recoveryTicks > 0) {
                return;
            }

            boar.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = Math.sqrt(boar.distanceToSqr(target));
            boolean onGround = boar.onGround();

            if (distance <= HEADBUTT_REACH - 0.4 && boar.headbuttCooldownTicks <= 0 && onGround) {
                // Thrown over the combat walk: it keeps closing in while the head swings.
                boar.snapFace(target);
                boar.startHeadbutt();
            }
            if (distance >= CHARGE_MIN_RANGE && distance <= CHARGE_MAX_RANGE && boar.chargeCooldownTicks <= 0
                    && onGround && boar.getSensing().hasLineOfSight(target) && boar.getRandom().nextInt(15) == 0) {
                boar.beginWindup(target);
                return;
            }

            if (boar.hasOgreRider()) {
                skirmish(target, distance);
                return;
            }
            if (tickDirectChase(target)) {
                return;
            }
            if (--repathTicks <= 0) {
                boar.getNavigation().moveTo(target, STALK_SPEED);
                // A longer commitment to each path keeps the chase on one smooth arc.
                repathTicks = 8 + boar.getRandom().nextInt(4);
            }
        }

        private int directChaseRecheckTicks;
        private boolean directChaseActive;

        /** Straight at the target over open ground instead of along path nodes; see {@link OpenGroundChase}. */
        private boolean tickDirectChase(LivingEntity target) {
            if (--directChaseRecheckTicks <= 0) {
                directChaseRecheckTicks = 4;
                boolean open = boar.onGround() && OpenGroundChase.isClear(boar, target);
                if (directChaseActive && !open) {
                    repathTicks = 0;
                }
                directChaseActive = open;
            }
            if (!directChaseActive) {
                return false;
            }
            boar.getNavigation().stop();
            boar.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), STALK_SPEED);
            return true;
        }

        /**
         * Mounted-archer tactics: back off when the target gets close, close in when it is far, and in
         * between circle it on a ring, stopping to face it so the rider can shoot. The charge stays an
         * occasional move, on its own long cooldown.
         */
        private void skirmish(LivingEntity target, double distance) {
            if (boar.meleeTicks > 0) {
                // Fresh off a charge it presses the attack, headbutting, before it falls back to skirmishing.
                boar.meleeTicks--;
                pauseTicks = 0;
                legStarted = false;
                if (--repathTicks <= 0) {
                    boar.getNavigation().moveTo(target, STALK_SPEED);
                    repathTicks = 8 + boar.getRandom().nextInt(4);
                }
                return;
            }
            if (pauseTicks > 0) {
                pauseTicks--;
                boar.getNavigation().stop();
                boar.turnToward(target);
                return;
            }
            boolean moving = !boar.getNavigation().isDone();
            if (legStarted && !moving) {
                legStarted = false;
                if (distance >= SKIRMISH_MIN && distance <= SKIRMISH_MAX) {
                    pauseTicks = 20 + boar.getRandom().nextInt(30);
                    if (boar.getRandom().nextInt(3) == 0) {
                        strafeSign = -strafeSign;
                    }
                    return;
                }
            }
            if (moving && --repathTicks > 0) {
                return;
            }
            repathTicks = 12 + boar.getRandom().nextInt(6);
            Vec3 targetPos = target.position();
            Vec3 away = new Vec3(boar.getX() - targetPos.x, 0.0, boar.getZ() - targetPos.z);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
            Vec3 destination;
            if (distance < SKIRMISH_MIN) {
                float swerve = (boar.getRandom().nextFloat() - 0.5F) * 0.7F;
                destination = boar.position().add(away.yRot(swerve).scale(9.0));
            } else if (distance > SKIRMISH_MAX) {
                destination = targetPos.add(away.scale(SKIRMISH_RING));
            } else {
                double bearing = Math.atan2(away.z, away.x)
                        + strafeSign * Math.toRadians(40.0 + boar.getRandom().nextInt(30));
                destination = targetPos.add(Math.cos(bearing) * SKIRMISH_RING, 0.0, Math.sin(bearing) * SKIRMISH_RING);
            }
            legStarted = boar.getNavigation().moveTo(destination.x, destination.y, destination.z, STALK_SPEED);
            if (!legStarted) {
                pauseTicks = 10;
            }
        }
    }

    /** Eases the boar round to face a target while it stands still, so the rider can shoot. */
    private void turnToward(LivingEntity target) {
        float desired = (float) (net.minecraft.util.Mth.atan2(target.getZ() - getZ(), target.getX() - getX())
                * (180.0 / Math.PI)) - 90.0F;
        float yaw = getYRot() + net.minecraft.util.Mth.wrapDegrees(desired - getYRot()) * 0.12F;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    /** Faces a target on the spot. */
    private void snapFace(LivingEntity target) {
        float yaw = (float) (net.minecraft.util.Mth.atan2(target.getZ() - getZ(), target.getX() - getX())
                * (180.0 / Math.PI)) - 90.0F;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    /** A tame boar trots after its owner, faster when it has fallen well behind. */
    private static final class FollowOwnerGoal extends Goal {
        private final MudsnoutBoar boar;
        private Player owner;
        private int repathTicks;

        private FollowOwnerGoal(MudsnoutBoar boar) {
            this.boar = boar;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!boar.isTamed() || boar.getTarget() != null || boar.entityData.get(DATA_PHASE) != PHASE_NONE
                    || boar.isPoseLocked()) {
                return false;
            }
            owner = boar.getOwner();
            return owner != null && !owner.isSpectator()
                    && boar.distanceToSqr(owner) > OWNER_FOLLOW_START * OWNER_FOLLOW_START;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && owner.isAlive() && boar.getTarget() == null
                    && boar.distanceToSqr(owner) > OWNER_FOLLOW_STOP * OWNER_FOLLOW_STOP;
        }

        @Override
        public void start() {
            repathTicks = 0;
        }

        @Override
        public void stop() {
            owner = null;
            boar.getNavigation().stop();
        }

        @Override
        public void tick() {
            boar.getLookControl().setLookAt(owner, 10.0F, boar.getMaxHeadXRot());
            if (--repathTicks <= 0) {
                repathTicks = 8 + boar.getRandom().nextInt(4);
                boolean far = boar.distanceToSqr(owner) > OWNER_FOLLOW_FAR * OWNER_FOLLOW_FAR;
                boar.getNavigation().moveTo(owner, far ? STALK_SPEED : FOLLOW_SPEED);
            }
        }
    }

    /** A tame boar takes on whatever hurts its owner, and whatever its owner is fighting. */
    private static final class DefendOwnerGoal extends Goal {
        private final MudsnoutBoar boar;
        private LivingEntity victim;
        private int lastHurtByTimestamp;
        private int lastHurtTimestamp;

        private DefendOwnerGoal(MudsnoutBoar boar) {
            this.boar = boar;
        }

        @Override
        public boolean canUse() {
            Player owner = boar.isTamed() ? boar.getOwner() : null;
            if (owner == null || boar.isPoseLocked() || boar.isBaby()) {
                return false;
            }
            LivingEntity attacker = owner.getLastHurtByMob();
            if (attacker != null && owner.getLastHurtByMobTimestamp() != lastHurtByTimestamp
                    && boar.canAttack(attacker)) {
                lastHurtByTimestamp = owner.getLastHurtByMobTimestamp();
                victim = attacker;
                return true;
            }
            LivingEntity target = owner.getLastHurtMob();
            if (target != null && owner.getLastHurtMobTimestamp() != lastHurtTimestamp && boar.canAttack(target)) {
                lastHurtTimestamp = owner.getLastHurtMobTimestamp();
                victim = target;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            boar.setTarget(victim);
        }
    }

    /** Now and then a wild boar walks to the mud it knows of and lies about in it to keep cool. */
    private static final class WallowGoal extends Goal {
        private final MudsnoutBoar boar;
        private BlockPos spot;
        private static final int STAGE_TRAVEL = 0;
        private static final int STAGE_DROPPING = 1;
        private static final int STAGE_RESTING = 2;
        private static final int STAGE_RISING = 3;
        private int stage;
        private int wallowTicks;
        private int travelTicks;
        private boolean finished;

        private WallowGoal(MudsnoutBoar boar) {
            this.boar = boar;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (boar.isTamed() || boar.isBaby() || boar.isPoseLocked() || boar.isVehicle()
                    || boar.getTarget() != null || boar.hurtTime > 0 || boar.entityData.get(DATA_FLEEING)
                    || boar.getRandom().nextInt(250) != 0) {
                return false;
            }
            BlockPos home = boar.findWallow();
            if (home == null || boar.distanceToSqr(Vec3.atCenterOf(home)) > WALLOW_HOME_RANGE * WALLOW_HOME_RANGE) {
                return false;
            }
            // Somewhere in the patch, not always the same block.
            spot = home;
            for (int attempt = 0; attempt < 6; attempt++) {
                int dx = boar.getRandom().nextInt(7) - 3;
                int dz = boar.getRandom().nextInt(7) - 3;
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos candidate = home.offset(dx, dy, dz);
                    if (boar.isWallowBlock(candidate)) {
                        spot = candidate;
                        return true;
                    }
                }
            }
            return boar.isWallowBlock(spot);
        }

        @Override
        public boolean canContinueToUse() {
            return spot != null && !finished && (stage > 0 || travelTicks < 200) && !boar.isTamed()
                    && !boar.isVehicle() && boar.getTarget() == null && boar.hurtTime == 0
                    && !boar.entityData.get(DATA_FLEEING)
                    && (stage > 0 || !boar.getNavigation().isDone() || arrived())
                    // Stood up by something else (a player feeding it): the wallow is over.
                    && (stage < STAGE_RESTING || boar.isPoseLocked());
        }

        private boolean arrived() {
            double dx = boar.getX() - (spot.getX() + 0.5);
            double dz = boar.getZ() - (spot.getZ() + 0.5);
            return dx * dx + dz * dz < 2.25;
        }

        @Override
        public void start() {
            finished = false;
            stage = STAGE_TRAVEL;
            wallowTicks = 0;
            travelTicks = 0;
            boar.getNavigation().moveTo(spot.getX() + 0.5, spot.getY() + 1, spot.getZ() + 0.5, WANDER_SPEED);
        }

        @Override
        public void tick() {
            travelTicks++;
            switch (stage) {
                case STAGE_TRAVEL -> {
                    if (arrived()) {
                        // Down he goes; the mud splashes once as he lands, then he rests.
                        wallowTicks = 100 + boar.getRandom().nextInt(140);
                        boar.lieDown();
                        stage = STAGE_DROPPING;
                    }
                }
                case STAGE_DROPPING -> {
                    if (boar.isLyingInMud()) {
                        stage = STAGE_RESTING;
                    }
                }
                case STAGE_RESTING -> {
                    if (--wallowTicks <= 0) {
                        boar.getUp();
                        stage = STAGE_RISING;
                    }
                }
                default -> {
                    if (!boar.isPoseLocked()) {
                        finished = true;
                    }
                }
            }
        }

        @Override
        public void stop() {
            // Interrupted mid-wallow (hurt, scared, a fight): up at once rather than lying there.
            if (!finished && boar.isPoseLocked()) {
                boar.snapUp();
            }
            stage = STAGE_TRAVEL;
            wallowTicks = 0;
            travelTicks = 0;
            spot = null;
        }
    }

    /**
     * Now and then a wild boar leaves its wallow, walks out onto the grass, grazes for a little while with
     * its head down, and then walks back to the wallow. Only adults, only wild ones, and never while
     * they are fighting, hurt, afraid of an ogre or carrying a rider.
     */
    private static final class GrazeGoal extends Goal {
        private static final int STAGE_OUT = 0;
        private static final int STAGE_GRAZING = 1;
        private static final int STAGE_BACK = 2;
        private static final int MIN_DISTANCE = 7;
        private static final int MAX_DISTANCE = 16;
        private static final int MAX_TRAVEL_TICKS = 400;

        private final MudsnoutBoar boar;
        private BlockPos home;
        private BlockPos spot;
        private int stage;
        private int travelTicks;
        private int bitesLeft;
        private int cycleTicks = -1;
        private int pauseTicks;
        private boolean finished;

        private GrazeGoal(MudsnoutBoar boar) {
            this.boar = boar;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean disturbed() {
            return boar.isTamed() || boar.isBaby() || boar.isPoseLocked() || boar.isVehicle()
                    || boar.getTarget() != null || boar.hurtTime > 0 || boar.entityData.get(DATA_FLEEING)
                    || boar.entityData.get(DATA_PHASE) != PHASE_NONE;
        }

        @Override
        public boolean canUse() {
            if (boar.grazeCooldownTicks > 0 || disturbed() || boar.getRandom().nextInt(300) != 0) {
                return false;
            }
            home = boar.findWallow();
            if (home == null || boar.distanceToSqr(Vec3.atCenterOf(home)) > 18.0 * 18.0) {
                return false;
            }
            spot = pickSpot();
            return spot != null;
        }

        /** Open grass a good way from the wallow that the boar can actually walk to. */
        private BlockPos pickSpot() {
            for (int attempt = 0; attempt < 10; attempt++) {
                double angle = boar.getRandom().nextDouble() * Math.PI * 2.0;
                double distance = MIN_DISTANCE + boar.getRandom().nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
                int x = home.getX() + (int) Math.round(Math.cos(angle) * distance);
                int z = home.getZ() + (int) Math.round(Math.sin(angle) * distance);
                if (!boar.level().hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                BlockPos top = boar.level().getHeightmapPos(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                if (boar.level().getBlockState(top.below()).is(Blocks.GRASS_BLOCK)
                        && boar.level().getFluidState(top).isEmpty()
                        && boar.getNavigation().createPath(top, 1) != null) {
                    return top;
                }
            }
            return null;
        }

        @Override
        public boolean canContinueToUse() {
            return !finished && !disturbed();
        }

        @Override
        public void start() {
            finished = false;
            stage = STAGE_OUT;
            travelTicks = 0;
            bitesLeft = 3 + boar.getRandom().nextInt(3);
            cycleTicks = -1;
            pauseTicks = 0;
            boar.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, WANDER_SPEED);
        }

        @Override
        public void tick() {
            switch (stage) {
                case STAGE_OUT -> {
                    travelTicks++;
                    if (reached(spot, 2.5) || boar.getNavigation().isDone() && travelTicks > 20 || travelTicks > MAX_TRAVEL_TICKS) {
                        boar.getNavigation().stop();
                        stage = STAGE_GRAZING;
                    }
                }
                case STAGE_GRAZING -> grazeTick();
                default -> {
                    travelTicks++;
                    if (reached(home, 4.0) || boar.getNavigation().isDone() && travelTicks > 20
                            || travelTicks > MAX_TRAVEL_TICKS) {
                        finished = true;
                    }
                }
            }
        }

        private boolean reached(BlockPos pos, double range) {
            double dx = boar.getX() - (pos.getX() + 0.5);
            double dz = boar.getZ() - (pos.getZ() + 0.5);
            return dx * dx + dz * dz < range * range;
        }

        /** Head down for a few bites, each one the grazing clip, with a short pause between them. */
        private void grazeTick() {
            boar.getNavigation().stop();
            if (cycleTicks >= 0) {
                cycleTicks++;
                // The jaw closes on the grass three times in the clip.
                if (cycleTicks == 12 || cycleTicks == 22 || cycleTicks == 32) {
                    boar.biteGrass();
                }
                if (cycleTicks >= GRAZE_OVERLAY_TICKS) {
                    cycleTicks = -1;
                    pauseTicks = 10 + boar.getRandom().nextInt(25);
                    bitesLeft--;
                }
                return;
            }
            if (pauseTicks > 0) {
                pauseTicks--;
                return;
            }
            if (bitesLeft > 0) {
                boar.playOverlay(OVERLAY_GRAZE);
                cycleTicks = 0;
                return;
            }
            // Full: back to the wallow.
            stage = STAGE_BACK;
            travelTicks = 0;
            boar.getNavigation().moveTo(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5, WANDER_SPEED);
        }

        @Override
        public void stop() {
            // Interrupted or done, it does not go straight back out again: several minutes before the next trip.
            boar.grazeCooldownTicks = 3600 + boar.getRandom().nextInt(4800);
            spot = null;
            home = null;
        }
    }

    /** A mouthful of grass: a few crumbs of turf at the snout and a quiet crunch. */
    private void biteGrass() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
        double x = getX() + forward.x * 1.3;
        double z = getZ() + forward.z * 1.3;
        // Vanilla block debris can randomly live well past the two-second grazing clip. These crumbs
        // have a fixed six-tick lifetime, so even the final bite is gone before the head rises.
        serverLevel.sendParticles(com.kingsandmonsters.ModParticles.GRAZING_CRUMB.get(),
                x, getY() + 0.15, z, 6, 0.18, 0.05, 0.18, 0.04);
        playSound(SoundEvents.GENERIC_EAT, 0.35F, 0.8F);
    }

    /** Ordinary wandering, except a wild boar mostly mills about within a few strides of a wallow it knows. */
    private static final class WallowStrollGoal extends WaterAvoidingRandomStrollGoal {
        private final MudsnoutBoar boar;

        private WallowStrollGoal(MudsnoutBoar boar) {
            super(boar, WANDER_SPEED);
            this.boar = boar;
        }

        @Override
        @Nullable
        protected Vec3 getPosition() {
            BlockPos home = boar.isTamed() ? null : boar.findWallow();
            if (home != null && boar.getRandom().nextFloat() < 0.75F
                    && boar.distanceToSqr(Vec3.atCenterOf(home)) < WALLOW_HOME_RANGE * WALLOW_HOME_RANGE) {
                BlockPos around = home.offset(boar.getRandom().nextInt(19) - 9, 0, boar.getRandom().nextInt(19) - 9);
                if (boar.level().hasChunk(around.getX() >> 4, around.getZ() >> 4)) {
                    BlockPos top = boar.level().getHeightmapPos(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, around);
                    return Vec3.atBottomCenterOf(top);
                }
            }
            return super.getPosition();
        }
    }

    /** A jockey boar walks a loop around the structure it guards, pausing between legs. */
    private static final class JockeyPatrolGoal extends Goal {
        private static final int ATTEMPTS = 6;
        private final MudsnoutBoar boar;
        private int pauseTicks;
        @Nullable
        private Vec3 destination;

        private JockeyPatrolGoal(MudsnoutBoar boar) {
            this.boar = boar;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!boar.hasOgreRider() || boar.isTamed() || !boar.hasRestriction() || boar.getTarget() != null) {
                return false;
            }
            if (pauseTicks > 0) {
                pauseTicks--;
                return false;
            }
            destination = pickDestination();
            return destination != null;
        }

        @Override
        public boolean canContinueToUse() {
            return boar.hasOgreRider() && boar.getTarget() == null && !boar.getNavigation().isDone();
        }

        @Override
        public void start() {
            boar.getNavigation().moveTo(destination.x, destination.y, destination.z, PATROL_SPEED);
        }

        @Override
        public void stop() {
            boar.getNavigation().stop();
            pauseTicks = 40 + boar.getRandom().nextInt(100);
            destination = null;
        }

        /** A point on the far side of the guarded ground, never the spot it is already standing near. */
        @Nullable
        private Vec3 pickDestination() {
            BlockPos center = boar.getRestrictCenter();
            float radius = boar.getRestrictRadius();
            for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                double angle = boar.getRandom().nextDouble() * Math.PI * 2.0;
                double distance = radius * (0.45 + boar.getRandom().nextDouble() * 0.5);
                int x = center.getX() + (int) Math.round(Math.cos(angle) * distance);
                int z = center.getZ() + (int) Math.round(Math.sin(angle) * distance);
                if (!boar.level().hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                BlockPos top = boar.level().getHeightmapPos(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        new BlockPos(x, 0, z));
                if (!boar.level().getFluidState(top.below()).isEmpty() || !boar.isWithinRestriction(top)
                        || boar.distanceToSqr(Vec3.atBottomCenterOf(top)) < 36.0) {
                    continue;
                }
                return Vec3.atBottomCenterOf(top);
            }
            return null;
        }
    }

    /**
     * Wanders back toward the pack when it drifts off: if the nearest other boar is more than a few
     * blocks away, walk over to it. Keeps a pack roaming together without a leader.
     */
    private static final class StayWithPackGoal extends Goal {
        private final MudsnoutBoar boar;
        private MudsnoutBoar mate;

        private StayWithPackGoal(MudsnoutBoar boar) {
            this.boar = boar;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (boar.isTamed() || boar.getTarget() != null || boar.getRandom().nextInt(20) != 0) {
                return false;
            }
            List<MudsnoutBoar> others = boar.level().getEntitiesOfClass(MudsnoutBoar.class,
                    boar.getBoundingBox().inflate(PACK_SEARCH_RANGE), other -> other != boar && other.isAlive());
            mate = others.stream().min(Comparator.comparingDouble(boar::distanceToSqr)).orElse(null);
            return mate != null && boar.distanceToSqr(mate) > PACK_COHESION_RANGE * PACK_COHESION_RANGE;
        }

        @Override
        public boolean canContinueToUse() {
            return mate != null && mate.isAlive() && boar.getTarget() == null
                    && !boar.getNavigation().isDone()
                    && boar.distanceToSqr(mate) > 4.0 * 4.0;
        }

        @Override
        public void start() {
            boar.getNavigation().moveTo(mate, PACK_REJOIN_SPEED);
        }

        @Override
        public void stop() {
            mate = null;
        }
    }
}
