package com.kingsandmonsters.entity;

import com.kingsandmonsters.Config;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.entity.animation.SynchronizedAnimationController;
import com.kingsandmonsters.entity.animation.CanonicalOneShotState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.EnumSet;
import java.util.List;

public class OgreArcher extends OgreGrunt {
    private static final EntityDataAccessor<Long> DATA_ARCHER_SHOT_START_TICK =
            SynchedEntityData.defineId(OgreArcher.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_ARCHER_SWING_START_TICK =
            SynchedEntityData.defineId(OgreArcher.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_ARCHER_SWING_ID =
            SynchedEntityData.defineId(OgreArcher.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ARCHER_AIMING =
            SynchedEntityData.defineId(OgreArcher.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ARCHER_COMBAT_MOVING =
            SynchedEntityData.defineId(OgreArcher.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ARCHER_RETREATING =
            SynchedEntityData.defineId(OgreArcher.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RARE_IDLE = RawAnimation.begin().then("idle2", software.bernie.geckolib.core.animation.Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation COMBAT_WALK = RawAnimation.begin().thenLoop("Combat walk");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("Shoot");
    private static final RawAnimation MOUNTED_IDLE = RawAnimation.begin().thenLoop("Mounted Idle");
    private static final RawAnimation MOUNTED_SHOOT = RawAnimation.begin().thenPlay("Mounted_Shoot");
    // The mounted shot is a 2.6s clip: the bow is drawn from 0.5s, held to 1.8s, and loosed at 2.0s
    // (tick 40, plus 2 ticks for the synced start reaching clients).
    private static final int MOUNTED_SHOOT_ANIMATION_TICKS = 52;
    private static final int MOUNTED_BOW_DRAW_START_TICKS = 10;
    private static final int MOUNTED_ARROW_RELEASE_TICKS = 42;
    /** How far to either side of the boar's facing a jockey can twist to aim, in degrees. */
    public static final float MOUNTED_AIM_CONE_DEGREES = 60.0F;
    private static final float MOUNTED_AIM_TURN_SPEED = 0.2F;
    private static final RawAnimation SWEEP = RawAnimation.begin().thenPlay("Regular attack 1");
    private static final RawAnimation HEAVY_SWING = RawAnimation.begin().thenPlay("regular attack 2");
    // The authored "Shoot" clip is 2.5s (50 ticks) — this used to be set to 40, cutting the
    // animation's last half-second short every time.
    private static final int SHOOT_ANIMATION_TICKS = 50;
    private static final int BOW_DRAW_START_TICKS = 5;
    // 1.88s into the (now correctly full-length) animation.
    private static final int ARROW_RELEASE_TICKS = 38;
    // Idle beat after a shot's animation finishes, before the next draw can start. Without it the
    // short inter-shot cooldown had already run out during the animation, so shots chained with no
    // breather and looked jittery.
    private static final int POST_SHOT_IDLE_TICKS = 16;
    public static final float ATTACK_OVERLAY_FADE_OUT_TICKS = 4.0F;
    private static final int SWEEP_ID = 1;
    private static final int HEAVY_SWING_ID = 2;
    // Both clips run 2.25s and land at 1.0s; the extra 2 ticks cover the synced start reaching clients.
    private static final int SWING_ANIMATION_TICKS = 45;
    private static final int SWING_IMPACT_TICKS = 22;
    // Tick marks of the foot landings in each walk clip, so steps sound with the visible foot.
    private static final java.util.Map<String, double[]> FOOT_CONTACTS = java.util.Map.of(
            "walk", new double[]{0.0, 18.84},
            "Combat walk", new double[]{0.0, 10.64});
    private static final int SWING_SOUND_LEAD_TICKS = 2;
    private static final int SWING_COOLDOWN_TICKS = 70;
    private static final double SWEEP_REACH = 3.4;
    private static final double SWEEP_HALF_WIDTH = 2.2;
    private static final double SWING_START_RANGE = 3.0;
    private static final float HEAVY_SWING_CHANCE = 0.35F;
    private static final int MIN_SHOT_COOLDOWN_TICKS = 6;
    private static final int SHOT_COOLDOWN_RANDOM_TICKS = 5;
    private static final double SHOOT_RANGE = 22.0;
    // He keeps walking in until he is this close, swinging on the way.
    private static final double PUNCH_RANGE = 2.35;
    private static final double HEAVY_HIT_RANGE = 3.5;
    private static final double RETREAT_RANGE = 6.0;
    private static final double RETREAT_DISTANCE = 7.0;
    // Retreat and approach share one pace, played with the combat walk.
    // The Archer's whole pace (chase, strafe and retreat) is this share of the configured speed: about
    // 0.2457 against the default 0.288: 3.45 blocks a second, since a mob's ground speed goes with the
    // square of its speed setting.
    /** The Archer's Buckler is their kit: the same 25% chance to turn an arrow aside that it gives a player. */
    private static final float BUCKLER_ARROW_BLOCK_CHANCE = 0.25F;
    private static final double ARCHER_SPEED_SCALE = 0.853;
    // Absolute wander pace, slowed to match the new combat walk.
    private static final double ARCHER_WANDER_SPEED = 0.125;
    private static final double RETREAT_SPEED_MODIFIER = 1.0;
    private static final int RETREAT_REPATH_TICKS = 28;
    private static final int POST_MELEE_RETREAT_TICKS = 36;
    private static final double POST_MELEE_RETREAT_DISTANCE = 9.0;
    private static final double POST_MELEE_RETREAT_SPEED = 1.0;
    private static final float SWEEP_DAMAGE = OgreDamage.raw(OgreDamage.Tier.ARCHER, OgreDamage.Blow.MINOR);
    private static final float HEAVY_SWING_DAMAGE = OgreDamage.raw(OgreDamage.Tier.ARCHER, OgreDamage.Blow.LIGHT);
    private static final double SWEEP_KNOCKBACK_STRENGTH = 0.45;
    private static final double HEAVY_KNOCKBACK_STRENGTH = 0.9;
    private static final float SWING_SOUND_VOLUME = 0.75F;
    private static final float SWING_SOUND_PITCH = 0.8F;
    private static final int MIN_CLOSE_PRESSURE_BEFORE_MELEE_TICKS = 18;
    private static final int STAND_AND_FIGHT_MIN_PUNCHES = 2;
    private static final int STAND_AND_FIGHT_RANDOM_PUNCHES = 1;
    private static final float ARROW_PROJECTILE_SPEED = 2.3F;
    private static final float ARROW_INACCURACY = 4.5F;
    private static final double ARROW_LOB_COMPENSATION = 0.14;
    private static final double ARROW_SPAWN_FORWARD_OFFSET = 0.45;
    private static final double ARROW_SPAWN_SIDE_OFFSET = 0.20;
    private static final double ARROW_SPAWN_VERTICAL_OFFSET = 0.20;
    // Calibrated against full diamond armor (~1.5 hearts effective per hit) to match the grunt/brute
    // melee tuning pass — arrows hit noticeably harder on lighter armor (~2.4 hearts vs full iron).
    private static final double OGRE_ARROW_BASE_DAMAGE = OgreDamage.raw(OgreDamage.Tier.ARCHER, OgreDamage.Blow.STANDARD);
    private static final int POISON_ARROW_ROLL = 15;
    private static final int SLOWNESS_ARROW_ROLL = 28;
    private static final int POISON_ARROW_DURATION_TICKS = 100;
    private static final int SLOWNESS_ARROW_DURATION_TICKS = 100;
    // Fraction of the remaining turn angle closed per tick (proportional lerp, see
    // rotlerpTurn/turnBodyToward) — not a degrees-per-tick cap. Matches OgreGruntMoveControl's
    // 0.12-0.35 walking/running range for the same gentle, natural-feeling deceleration.
    private static final float AIM_TURN_SPEED = 0.15F;
    private static final float SHOOT_VISUAL_TRACK_TURN_SPEED = 0.15F;
    private static final float MAX_SHOOT_ANGLE = 25.0F;

    private int idleAnimationTicks;
    private int rareIdleTicks;
    private int activeShotTicks;
    private int arrowReleaseTicks;
    private int activeSwingId;
    private int activePunchTicks;
    private int punchImpactTicks;
    private int punchCooldownTicks;
    private int combatMovementAnimationTicks;
    private int retreatAnimationTicks;
    private int lastAnimationTick = -1;
    private long lastMovingAnimationTick = Long.MIN_VALUE / 2;
    private static final int MOVEMENT_ANIMATION_GRACE_TICKS = 4;
    private boolean arrowReleasedThisShot;
    private boolean bowDrawStartedThisShot;
    private boolean punchHitThisPunch;
    private boolean punchImpactSoundPlayed;
    private boolean swingOverlayVisible;
    private LivingEntity pendingShotTarget;
    private LivingEntity pendingPunchTarget;
    private double enchantedBowDamageBonus;
    private boolean enchantedBowHasFlame;
    private double configuredArrowBaseDamage = OGRE_ARROW_BASE_DAMAGE;
    private final CanonicalOneShotState visualOneShot = new CanonicalOneShotState();
    private final CanonicalOneShotState visualSwingOneShot = new CanonicalOneShotState();

    public OgreArcher(EntityType<? extends OgreArcher> type, Level level) {
        super(type, level);
        avoidWaterPathfinding();
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    @Override
    @SuppressWarnings("deprecation") // Required 1.21.1 OverrideOnly spawn hook; no replacement exists in this target.
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData, @org.jetbrains.annotations.Nullable net.minecraft.nbt.CompoundTag spawnTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, spawnTag);
        equipVariantBow(level);
        return data;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putDouble("EnchantedBowDamageBonus", enchantedBowDamageBonus);
        compound.putBoolean("EnchantedBowHasFlame", enchantedBowHasFlame);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        enchantedBowDamageBonus = compound.getDouble("EnchantedBowDamageBonus");
        enchantedBowHasFlame = compound.getBoolean("EnchantedBowHasFlame");
        ensureBowEquipped();
    }

    @Override

    public float maxUpStep() {

        return 1.0F;

    }


    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.288 * ARCHER_SPEED_SCALE)
                .add(Attributes.FOLLOW_RANGE, 34.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.ATTACK_KNOCKBACK, 0.35);
                // See OgreGrunt#createAttributes — default 0 caps water acceleration at a tiny fixed
                // baseline regardless of requested speed, which would make applyShallowWaterSpeedFactor's
                // 50% reduction meaningless since he's already moving slower than that.;
    }

    @Override
    protected double getWanderSpeedModifier() {
        // Wandering keeps its own pace: the multiplier is set against the scaled speed.
        return ARCHER_WANDER_SPEED / (0.288 * ARCHER_SPEED_SCALE);
    }

    @Override
    protected boolean usesAnimatedFootsteps() {
        return true;
    }

    @Override
    public boolean isAttackOverlayVisible() {
        return swingOverlayVisible;
    }

    /** Client-side: a light step each time a foot lands; he is lanky, so lighter than the other ogres. */
    private void playArcherFootfall() {
        if (!onGround() || isInWater() || isSilent()) {
            return;
        }
        float variation = getRandom().nextFloat() * 0.08F;
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.WARDEN_STEP, SoundSource.HOSTILE,
                0.22F, 1.15F + variation, false);
        BlockPos ground = getOnPos();
        SoundType surface = level().getBlockState(ground).getSoundType(level(), ground, this);
        level().playLocalSound(getX(), getY(), getZ(), surface.getStepSound(), SoundSource.HOSTILE,
                0.3F, 1.0F + variation, false);
    }

    @Override
    public void applyConfiguredCombatAttributes(boolean healToMax) {
        if (!Config.isLoaded()) {
            return;
        }

        setAttributeBaseValue(Attributes.MAX_HEALTH, Config.OGRE_ARCHER_MAX_HEALTH.get());
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, Config.OGRE_ARCHER_MELEE_DAMAGE.get());
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, Config.OGRE_ARCHER_MOVEMENT_SPEED.get() * ARCHER_SPEED_SCALE);
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, Config.OGRE_ARCHER_FOLLOW_RANGE.get());
        setAttributeBaseValue(Attributes.ARMOR, Config.OGRE_ARCHER_ARMOR.get());
        configuredArrowBaseDamage = Config.OGRE_ARCHER_ARROW_DAMAGE.get();
        applyConfiguredHealth(healToMax);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.OGRE_ARCHER_HURT.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.OGRE_ARCHER_IDLE.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        // Archers often patrol in pairs or ride together, so keep their chatter restrained.
        return IDLE_SOUND_INTERVAL_TICKS;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.OGRE_ARCHER_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return 0.97F + getRandom().nextFloat() * 0.06F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ARCHER_SHOT_START_TICK, 0L);
        this.entityData.define(DATA_ARCHER_SWING_START_TICK, 0L);
        this.entityData.define(DATA_ARCHER_SWING_ID, 0);
        this.entityData.define(DATA_ARCHER_AIMING, false);
        this.entityData.define(DATA_ARCHER_COMBAT_MOVING, false);
        this.entityData.define(DATA_ARCHER_RETREATING, false);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            shareTargetWithBoar();
            tickMountedFacing();
            if (combatMovementAnimationTicks > 0 && --combatMovementAnimationTicks <= 0) {
                setCombatMoving(false);
            }
            if (retreatAnimationTicks > 0 && --retreatAnimationTicks <= 0) {
                setRetreating(false);
            }
        }
    }

    /** True while he is riding a Mudsnout Boar: a jockey who aims and shoots but doesn't walk. */
    public boolean isMounted() {
        return getVehicle() instanceof MudsnoutBoar;
    }

    @Override
    protected boolean canGuardSuperiors() {
        return !isMounted() && super.canGuardSuperiors();
    }

    /** The jockey and his boar go after the same enemy: whichever picks one first shares it. */
    private void shareTargetWithBoar() {
        if (!(getVehicle() instanceof MudsnoutBoar boar)) {
            return;
        }
        LivingEntity boarTarget = boar.getTarget();
        // The mount controls positioning, so its live target is authoritative for the rider too.
        // Without this, two targets acquired on the same tick could remain split indefinitely.
        if (boarTarget != null && boarTarget.isAlive() && getTarget() != boarTarget
                && canAttack(boarTarget)) {
            setTarget(boarTarget);
        }
    }

    private int shootClipTicks() {
        return isMounted() ? MOUNTED_SHOOT_ANIMATION_TICKS : SHOOT_ANIMATION_TICKS;
    }

    private float boarYaw() {
        return getVehicle() instanceof LivingEntity boar ? boar.yBodyRot : getYRot();
    }

    private float yawToTarget(LivingEntity target) {
        double xOffset = target.getX() - getX();
        double zOffset = target.getZ() - getZ();
        return (float) (Mth.atan2(zOffset, xOffset) * Mth.RAD_TO_DEG) - 90.0F;
    }

    /** Whether the target is within the twist he can manage from the boar's back. */
    private boolean isTargetInMountedCone(LivingEntity target) {
        return Math.abs(Mth.wrapDegrees(yawToTarget(target) - boarYaw())) <= MOUNTED_AIM_CONE_DEGREES;
    }

    /** Turns toward the target as far as the cone allows; the legs and seat stay with the boar. */
    private void turnWithinMountedCone(LivingEntity target) {
        float boarYaw = boarYaw();
        float offset = Mth.clamp(Mth.wrapDegrees(yawToTarget(target) - boarYaw),
                -MOUNTED_AIM_CONE_DEGREES, MOUNTED_AIM_CONE_DEGREES);
        // Keep the rider's root and seated legs locked to the boar. Only the synced head yaw carries
        // the aiming offset; OgreArcherModel applies that offset to the upper body. Rotating both the
        // entity and the body bone made the jockey double-turn and eventually spin around the mount.
        float aimYaw = rotlerpTurn(yHeadRot, boarYaw + offset, MOUNTED_AIM_TURN_SPEED);
        setYRot(boarYaw);
        yBodyRot = boarYaw;
        yHeadRot = aimYaw;
    }

    /** Vanilla-style passenger facing: the seat follows the vehicle while only the upper body aims. */
    private void tickMountedFacing() {
        if (!(getVehicle() instanceof MudsnoutBoar)) {
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            turnWithinMountedCone(target);
            return;
        }
        float yaw = boarYaw();
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        registerBogToadWarinessGoal();
        goalSelector.addGoal(1, new OgreArcherShootGoal(this, 1.0));
        goalSelector.addGoal(4, new GuardSuperiorGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, getWanderSpeedModifier()) {
            @Override
            public boolean canUse() {
                return !isMounted() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        registerTerritorialTargetGoal();
        registerArmyGoals();
    }

    private void beginBowShot(LivingEntity target) {
        ensureBowEquipped();
        setBowAiming(true);
        pendingShotTarget = target;
        activeShotTicks = shootClipTicks();
        arrowReleaseTicks = isMounted() ? MOUNTED_ARROW_RELEASE_TICKS : ARROW_RELEASE_TICKS;
        arrowReleasedThisShot = false;
        bowDrawStartedThisShot = false;
        playShootAnimation();
    }

    private void tickPendingBowShot() {
        if (pendingShotTarget == null) {
            return;
        }

        activeShotTicks--;
        int elapsedShotTicks = shootClipTicks() - activeShotTicks;
        if (!bowDrawStartedThisShot
                && elapsedShotTicks >= (isMounted() ? MOUNTED_BOW_DRAW_START_TICKS : BOW_DRAW_START_TICKS)) {
            bowDrawStartedThisShot = true;
            startUsingItem(InteractionHand.MAIN_HAND);
        }

        if (!arrowReleasedThisShot && --arrowReleaseTicks <= 0) {
            shootPendingArrow();
        }

        if (activeShotTicks <= 0) {
            cancelBowShot();
        }
    }

    private void shootPendingArrow() {
        if (pendingShotTarget == null || arrowReleasedThisShot) {
            return;
        }

        ensureBowEquipped();
        arrowReleasedThisShot = true;
        if (isUsingItem()) {
            stopUsingItem();
        }

        if (level().isClientSide) {
            return;
        }

        LivingEntity target = pendingShotTarget;
        if (!hasClearShotToTarget(target)) {
            return;
        }

        Vec3 arrowSpawn = getArrowSpawnPosition();
        Arrow arrow = new Arrow(level(), this);
        arrow.setPos(arrowSpawn.x, arrowSpawn.y, arrowSpawn.z);

        double xOffset = target.getX() - arrowSpawn.x;
        double zOffset = target.getZ() - arrowSpawn.z;
        double horizontalDistance = Math.sqrt(xOffset * xOffset + zOffset * zOffset);
        double yOffset = target.getY(0.3333333333333333) - arrowSpawn.y;
        arrow.shoot(xOffset, yOffset + horizontalDistance * ARROW_LOB_COMPENSATION, zOffset,
                ARROW_PROJECTILE_SPEED, ARROW_INACCURACY);
        arrow.setBaseDamage(getVelocityAdjustedArrowDamage());
        if (enchantedBowHasFlame) {
            arrow.setSecondsOnFire(5);
        }

        int specialArrowRoll = getRandom().nextInt(100);
        if (specialArrowRoll < POISON_ARROW_ROLL) {
            arrow.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_ARROW_DURATION_TICKS));
        } else if (specialArrowRoll < SLOWNESS_ARROW_ROLL) {
            arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_ARROW_DURATION_TICKS));
        }
        level().addFreshEntity(arrow);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.SKELETON_SHOOT,
                SoundSource.HOSTILE, 2.0F, 0.82F + getRandom().nextFloat() * 0.12F);
    }

    private Vec3 getArrowSpawnPosition() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x).normalize();
        return position()
                .add(0.0, getEyeHeight() - ARROW_SPAWN_VERTICAL_OFFSET, 0.0)
                .add(forward.scale(ARROW_SPAWN_FORWARD_OFFSET))
                .add(right.scale(ARROW_SPAWN_SIDE_OFFSET));
    }

    private double getVelocityAdjustedArrowDamage() {
        return Math.max(0.0, configuredArrowBaseDamage + enchantedBowDamageBonus) / ARROW_PROJECTILE_SPEED;
    }

    private boolean hasClearShotToTarget(LivingEntity target) {
        Vec3 start = getArrowSpawnPosition();
        Vec3 end = target.getEyePosition().subtract(0.0, 0.15, 0.0);
        double targetDistanceSqr = start.distanceToSqr(end);
        AABB shotLane = new AABB(start, end).inflate(0.75);
        List<OgreGrunt> alliesInLane = level().getEntitiesOfClass(
                OgreGrunt.class,
                shotLane,
                ally -> ally != this && ally.isAlive());

        for (OgreGrunt ally : alliesInLane) {
            if (ally == target) {
                continue;
            }

            var allyHit = ally.getBoundingBox().inflate(0.35).clip(start, end);
            if (allyHit.isPresent() && start.distanceToSqr(allyHit.get()) < targetDistanceSqr) {
                return false;
            }
        }

        return true;
    }

    private void equipVariantBow(ServerLevelAccessor level) {
        ItemStack bow = new ItemStack(Items.BOW);
        enchantedBowDamageBonus = 0.0;
        enchantedBowHasFlame = false;

        int roll = getRandom().nextInt(100);
        if (roll < 45) {
            setItemSlot(EquipmentSlot.MAINHAND, bow);
            return;
        }

        if (roll < 70) {
            enchantBow(level, bow, Enchantments.POWER_ARROWS, 1);
            enchantedBowDamageBonus = 0.5;
        } else if (roll < 88) {
            enchantBow(level, bow, Enchantments.POWER_ARROWS, 2);
            enchantedBowDamageBonus = 1.0;
        } else if (roll < 96) {
            enchantBow(level, bow, Enchantments.POWER_ARROWS, 2);
            enchantBow(level, bow, Enchantments.PUNCH_ARROWS, 1);
            enchantedBowDamageBonus = 1.0;
        } else {
            enchantBow(level, bow, Enchantments.POWER_ARROWS, 1);
            enchantBow(level, bow, Enchantments.FLAMING_ARROWS, 1);
            enchantedBowDamageBonus = 0.5;
            enchantedBowHasFlame = true;
        }

        setItemSlot(EquipmentSlot.MAINHAND, bow);
    }

    private void ensureBowEquipped() {
        if (getMainHandItem().is(Items.CROSSBOW)) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
    }

    private void enchantBow(ServerLevelAccessor level, ItemStack bow, Enchantment enchantment, int enchantmentLevel) {
        bow.enchant(enchantment, enchantmentLevel);
    }

    private void cancelBowShot() {
        pendingShotTarget = null;
        activeShotTicks = 0;
        arrowReleaseTicks = 0;
        arrowReleasedThisShot = false;
        bowDrawStartedThisShot = false;
        if (isUsingItem()) {
            stopUsingItem();
        }
        if (!level().isClientSide) {
            // Retain the authoritative event through the authored recovery, not merely until the
            // arrow leaves the bow. This also lets a client resuming tracking after release seek
            // into the correct recovery pose instead of seeing idle while the arrow is in flight.
            entityData.set(DATA_ARCHER_SHOT_START_TICK, 0L);
        }
    }

    private void setBowAiming(boolean aiming) {
        entityData.set(DATA_ARCHER_AIMING, aiming);
    }

    private boolean isPreparingBowShot() {
        return pendingShotTarget != null;
    }

    /** Starts a swing over the combat walk: a wide sweep most of the time, now and then a heavier hit. */
    private void beginPunch(LivingEntity target) {
        cancelBowShot();
        pendingPunchTarget = target;
        activeSwingId = getRandom().nextFloat() < HEAVY_SWING_CHANCE ? HEAVY_SWING_ID : SWEEP_ID;
        activePunchTicks = SWING_ANIMATION_TICKS;
        punchImpactTicks = SWING_IMPACT_TICKS;
        punchHitThisPunch = false;
        punchImpactSoundPlayed = false;
        punchCooldownTicks = SWING_COOLDOWN_TICKS;
        if (!level().isClientSide) {
            entityData.set(DATA_ARCHER_SWING_ID, activeSwingId);
            entityData.set(DATA_ARCHER_SWING_START_TICK, level().getGameTime());
        }
    }

    private void tickPendingPunch() {
        if (pendingPunchTarget == null) {
            return;
        }

        activePunchTicks--;
        punchImpactTicks--;
        if (!punchImpactSoundPlayed && punchImpactTicks <= SWING_SOUND_LEAD_TICKS) {
            playSwingSound();
        }
        if (!punchHitThisPunch && punchImpactTicks <= 0) {
            applySwingImpact();
        }

        if (activePunchTicks <= 0) {
            cancelPunch();
        }
    }

    private void applySwingImpact() {
        punchHitThisPunch = true;
        if (level().isClientSide) {
            return;
        }
        if (activeSwingId == HEAVY_SWING_ID) {
            applyHeavySwing();
        } else {
            applySweep();
        }
    }

    /** The first swing catches everything in the arc in front of him, softer toward the edge. */
    private void applySweep() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double search = SWEEP_REACH + SWEEP_HALF_WIDTH;
        List<LivingEntity> candidates = level().getEntitiesOfClass(
                LivingEntity.class, getBoundingBox().inflate(search, 2.0, search),
                target -> target != this && target.isAlive() && canAttackOgreTarget(target));
        for (LivingEntity target : candidates) {
            Vec3 relative = target.getBoundingBox().getCenter().subtract(position());
            double halfWidth = target.getBbWidth() * 0.5;
            double ahead = relative.dot(forward);
            double across = relative.dot(right);
            boolean inArc = ahead >= -0.3 - halfWidth && ahead <= SWEEP_REACH + halfWidth
                    && Math.abs(across) <= SWEEP_HALF_WIDTH + halfWidth;
            if (inArc) {
                float falloff = areaFalloff(Math.hypot(ahead, across), search, OgreDamage.ARC_EDGE_SHARE);
                target.hurt(damageSources().mobAttack(this), SWEEP_DAMAGE * falloff);
                target.knockback(SWEEP_KNOCKBACK_STRENGTH, -forward.x, -forward.z);
            }
        }
    }

    /** The second swing is one harder blow on whoever he is fighting. */
    private void applyHeavySwing() {
        LivingEntity target = pendingPunchTarget;
        if (target == null || !target.isAlive()
                || distanceToSqr(target) > HEAVY_HIT_RANGE * HEAVY_HIT_RANGE
                || !getSensing().hasLineOfSight(target)) {
            return;
        }
        target.hurt(damageSources().mobAttack(this), HEAVY_SWING_DAMAGE);
        target.knockback(HEAVY_KNOCKBACK_STRENGTH, getX() - target.getX(), getZ() - target.getZ());
    }

    private void playSwingSound() {
        punchImpactSoundPlayed = true;
        if (level().isClientSide) {
            return;
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE,
                SWING_SOUND_VOLUME, SWING_SOUND_PITCH);
        if (activeSwingId == HEAVY_SWING_ID) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE,
                    SWING_SOUND_VOLUME, SWING_SOUND_PITCH);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.MUD_HIT, SoundSource.HOSTILE, 0.35F, 0.75F);
        }
    }

    private void cancelPunch() {
        pendingPunchTarget = null;
        activeSwingId = 0;
        activePunchTicks = 0;
        punchImpactTicks = 0;
        punchHitThisPunch = false;
        punchImpactSoundPlayed = false;
        if (!level().isClientSide) {
            entityData.set(DATA_ARCHER_SWING_ID, 0);
            entityData.set(DATA_ARCHER_SWING_START_TICK, 0L);
        }
    }

    private boolean isPreparingPunch() {
        return pendingPunchTarget != null;
    }

    private boolean canPunch(LivingEntity target) {
        return punchCooldownTicks <= 0
                && target.isAlive()
                && distanceToSqr(target) <= SWING_START_RANGE * SWING_START_RANGE
                && getSensing().hasLineOfSight(target);
    }

    private void tickPunchCooldown() {
        if (punchCooldownTicks > 0) {
            punchCooldownTicks--;
        }
    }

    private boolean turnBodyToward(LivingEntity target) {
        return turnBodyToward(target, AIM_TURN_SPEED);
    }

    private boolean turnBodyToward(LivingEntity target, float turnSpeed) {
        double xOffset = target.getX() - getX();
        double zOffset = target.getZ() - getZ();
        float targetYaw = (float) (Mth.atan2(zOffset, xOffset) * Mth.RAD_TO_DEG) - 90.0F;
        // Proportional lerp instead of a fixed degrees-per-tick step — see the matching comment
        // in OgreMage#turnToward. Gives the same gentle, decelerating arc as pathing turns
        // instead of a mechanical constant-speed correction against a moving target.
        setYRot(rotlerpTurn(getYRot(), targetYaw, turnSpeed));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        float yawDifference = Mth.wrapDegrees(targetYaw - getYRot());
        return Math.abs(yawDifference) <= MAX_SHOOT_ANGLE;
    }

    /**
     * Proportional turn easing shared by every Archer body-turn call. {@code lerpFraction} is
     * the share of the remaining angle closed each tick (0-1), not a degrees-per-tick cap.
     */
    private static float rotlerpTurn(float currentAngle, float targetAngle, float lerpFraction) {
        float remaining = Mth.wrapDegrees(targetAngle - currentAngle);
        return currentAngle + remaining * lerpFraction;
    }

    private void visuallyTrackShotTarget(LivingEntity target) {
        getLookControl().setLookAt(target, 45.0F, 45.0F);
        turnBodyToward(target, SHOOT_VISUAL_TRACK_TURN_SPEED);
    }

    private void playShootAnimation() {
        if (!level().isClientSide) {
            entityData.set(DATA_ARCHER_SHOT_START_TICK, level().getGameTime());
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new SynchronizedAnimationController<>(this, "Archer Movement", 5, this::animateMovement)
                .withFootfalls(FOOT_CONTACTS, this::playArcherFootfall));
        // Registered second so the swings blend in over whatever the walk is doing; the model
        // eases them back out (see OgreArcherModel).
        controllers.add(new SynchronizedAnimationController<>(this, "Archer Swing Overlay", 5,
                this::animateSwingOverlay));
    }

    private PlayState animateSwingOverlay(AnimationState<OgreArcher> state) {
        state.setControllerSpeed(1.0F);
        if (isDeadOrDying()) {
            visualSwingOneShot.clear();
            swingOverlayVisible = false;
            return PlayState.STOP;
        }
        int swingId = entityData.get(DATA_ARCHER_SWING_ID);
        int visualSwing = visualSwingOneShot.update(state.getController(), swingId,
                swingId > 0 ? entityData.get(DATA_ARCHER_SWING_START_TICK) : 0L,
                id -> SWING_ANIMATION_TICKS, level().getGameTime());
        swingOverlayVisible = visualSwing > 0;
        if (!swingOverlayVisible) {
            return PlayState.STOP;
        }
        return state.setAndContinue(visualSwing == HEAVY_SWING_ID ? HEAVY_SWING : SWEEP);
    }

    private PlayState animateMovement(AnimationState<OgreArcher> state) {
        if (isDeadOrDying()) {
            // A shot or punch in progress is cancelled the moment he dies.
            visualOneShot.clear();
            state.setControllerSpeed(1.0F);
            return state.setAndContinue(IDLE);
        }
        long shotStart = entityData.get(DATA_ARCHER_SHOT_START_TICK);
        int visualType = visualOneShot.update(state.getController(), shotStart > 0L ? 1 : 0, shotStart,
                id -> shootClipTicks(), level().getGameTime());
        if (visualType > 0) {
            return state.setAndContinue(isMounted() ? MOUNTED_SHOOT : SHOOT);
        }
        if (isMounted()) {
            // On a boar's back he only sits; the boar carries him.
            idleAnimationTicks = 0;
            rareIdleTicks = 0;
            return state.setAndContinue(MOUNTED_IDLE);
        }

        // Retreat and actual combat movement are synced because getTarget() is server-only.
        // Bow aiming is intentionally not a movement signal: between shots the archer is planted
        // in place and should use his idle animation.
        if (isRetreating()) {
            idleAnimationTicks = 0;
            rareIdleTicks = 0;
            lastAnimationTick = tickCount;
            return state.setAndContinue(COMBAT_WALK);
        }

        if (isCombatMoving()) {
            idleAnimationTicks = 0;
            rareIdleTicks = 0;
            lastAnimationTick = tickCount;
            return state.setAndContinue(COMBAT_WALK);
        }

        if (isRunMovementState() && isMovingForAnimation(state)) {
            idleAnimationTicks = 0;
            rareIdleTicks = 0;
            lastAnimationTick = tickCount;
            return state.setAndContinue(COMBAT_WALK);
        }

        if (isMovingForAnimation(state)) {
            idleAnimationTicks = 0;
            rareIdleTicks = 0;
            lastAnimationTick = tickCount;
            return state.setAndContinue(WALK);
        }

        updateIdleAnimationTimers(state);
        if (rareIdleTicks > 0 || state.isCurrentAnimation(RARE_IDLE) && !state.getController().hasAnimationFinished()) {
            return state.setAndContinue(RARE_IDLE);
        }
        return state.setAndContinue(IDLE);
    }

    private void updateIdleAnimationTimers(AnimationState<OgreArcher> state) {
        if (lastAnimationTick == tickCount) {
            return;
        }

        int tickDelta = lastAnimationTick < 0 ? 1 : Math.max(1, tickCount - lastAnimationTick);
        lastAnimationTick = tickCount;

        if (rareIdleTicks > 0) {
            rareIdleTicks = Math.max(0, rareIdleTicks - tickDelta);
            return;
        }

        idleAnimationTicks += tickDelta;
        if (idleAnimationTicks >= 160 && getRandom().nextInt(240) == 0) {
            idleAnimationTicks = 0;
            rareIdleTicks = 40;
            state.getController().forceAnimationReset();
        }
    }

    private boolean isMovingForAnimation(AnimationState<OgreArcher> state) {
        boolean moving = state.isMoving() || getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
        // Counted in game ticks: this runs once per rendered frame, so a frame counter would let the
        // walk drop to idle for a moment at high frame rates whenever the ogre paused for an instant.
        long now = level().getGameTime();
        if (moving) {
            lastMovingAnimationTick = now;
            return true;
        }
        return now - lastMovingAnimationTick <= MOVEMENT_ANIMATION_GRACE_TICKS;
    }

    private void markCombatMovementForAnimation() {
        combatMovementAnimationTicks = 12;
        setCombatMoving(true);
    }

    private void setCombatMoving(boolean combatMoving) {
        entityData.set(DATA_ARCHER_COMBAT_MOVING, combatMoving);
    }

    private boolean isCombatMoving() {
        return entityData.get(DATA_ARCHER_COMBAT_MOVING);
    }

    private void markRetreating() {
        retreatAnimationTicks = 12;
        setRetreating(true);
    }

    private void setRetreating(boolean retreating) {
        entityData.set(DATA_ARCHER_RETREATING, retreating);
    }

    private boolean isRetreating() {
        return entityData.get(DATA_ARCHER_RETREATING);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Thrown ogre spears are not blocked, as with the player's buckler.
        if (!level().isClientSide && source.getDirectEntity() instanceof AbstractArrow arrow
                && !(arrow instanceof OgreSpear) && random.nextFloat() < BUCKLER_ARROW_BLOCK_CHANCE) {
            playSound(SoundEvents.SHIELD_BLOCK, 0.8F, 1.4F);
            return false;
        }
        return super.hurt(source, amount);
    }

    private static class OgreArcherShootGoal extends Goal {
        private final OgreArcher archer;
        private final double speedModifier;
        private int ticksUntilNextPathRecalculation;
        private int ticksUntilNextMeleeApproachPath;
        private int ticksUntilNextShot;
        private Vec3 retreatTargetPos;
        private int retreatCommitTicks;
        private int postMeleeRetreatTicks;
        private int closePressureTicks;
        private int standAndFightPunches;
        private int standAndFightPunchGoal;
        private boolean meleeExchangeActive;

        private OgreArcherShootGoal(OgreArcher archer, double speedModifier) {
            this.archer = archer;
            this.speedModifier = speedModifier;
            this.ticksUntilNextShot = archer.getRandom().nextInt(MIN_SHOT_COOLDOWN_TICKS);
            rerollStandAndFightPunchGoal();
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = archer.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = archer.getTarget();
            return archer.isPreparingPunch()
                    || archer.isPreparingBowShot()
                    || target != null
                    && target.isAlive()
                    && (!(target instanceof Player player) || !player.isSpectator() && !player.isCreative())
                    && archer.isWithinRestriction(target.blockPosition());
        }

        @Override
        public void stop() {
            archer.getNavigation().stop();
            archer.setBowAiming(false);
            archer.setCombatMoving(false);
            archer.setRetreating(false);
            archer.cancelPunch();
            archer.cancelBowShot();
            ticksUntilNextPathRecalculation = 0;
            ticksUntilNextMeleeApproachPath = 0;
            retreatTargetPos = null;
            retreatCommitTicks = 0;
            postMeleeRetreatTicks = 0;
            closePressureTicks = 0;
            standAndFightPunches = 0;
            meleeExchangeActive = false;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = archer.getTarget();
            archer.tickPunchCooldown();

            if (target != null && archer.isMounted()) {
                tickMounted(target);
                return;
            }

            if (target == null) {
                archer.setBowAiming(false);
                archer.setCombatMoving(false);
                archer.setRetreating(false);
                archer.tickPendingPunch();
                archer.tickPendingBowShot();
                closePressureTicks = 0;
                meleeExchangeActive = false;
                standAndFightPunches = 0;
                return;
            }

            archer.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distanceToTargetSqr = archer.distanceToSqr(target);
            boolean hasLineOfSight = archer.getSensing().hasLineOfSight(target);
            boolean targetTooClose = distanceToTargetSqr < RETREAT_RANGE * RETREAT_RANGE;
            updateClosePressure(targetTooClose && hasLineOfSight);

            if (archer.isPreparingPunch()) {
                archer.setBowAiming(false);
                archer.turnBodyToward(target);
                archer.tickPendingPunch();
                walkIntoMelee(target, distanceToTargetSqr);
                return;
            }

            if (archer.isPreparingBowShot()) {
                archer.visuallyTrackShotTarget(target);
                archer.getNavigation().stop();
                archer.tickPendingBowShot();
                if (!archer.isPreparingBowShot()) {
                    ticksUntilNextShot = Math.max(ticksUntilNextShot, adjustedTickDelay(POST_SHOT_IDLE_TICKS));
                    if (targetTooClose && hasLineOfSight) {
                        startRetreat();
                        tickPostMeleeRetreat(target);
                    }
                }
                return;
            }

            boolean inShootRange = distanceToTargetSqr <= SHOOT_RANGE * SHOOT_RANGE;

            if (postMeleeRetreatTicks > 0) {
                archer.setBowAiming(false);
                postMeleeRetreatTicks--;
                tickPostMeleeRetreat(target);
                if (postMeleeRetreatTicks <= 0 && targetTooClose && hasLineOfSight) {
                    meleeExchangeActive = true;
                    standAndFightPunches = 0;
                    rerollStandAndFightPunchGoal();
                }
                ticksUntilNextShot = Math.max(ticksUntilNextShot, adjustedTickDelay(12));
                return;
            }

            if (targetTooClose) {
                archer.setBowAiming(false);
                if (!meleeExchangeActive) {
                    startRetreat();
                    tickPostMeleeRetreat(target);
                    ticksUntilNextShot = Math.max(ticksUntilNextShot, adjustedTickDelay(12));
                    return;
                }

                if (standAndFightPunches >= standAndFightPunchGoal) {
                    startRetreat();
                    meleeExchangeActive = false;
                    standAndFightPunches = 0;
                    rerollStandAndFightPunchGoal();
                    tickPostMeleeRetreat(target);
                    ticksUntilNextShot = Math.max(ticksUntilNextShot, adjustedTickDelay(12));
                    return;
                }

                if (meleeExchangeActive) {
                    archer.turnBodyToward(target);
                    retreatTargetPos = null;
                    retreatCommitTicks = 0;
                    if (archer.canPunch(target)) {
                        archer.beginPunch(target);
                        standAndFightPunches++;
                    }
                    walkIntoMelee(target, distanceToTargetSqr);

                    ticksUntilNextShot = Math.max(ticksUntilNextShot, adjustedTickDelay(12));
                    return;
                }

                ticksUntilNextMeleeApproachPath = 0;
                retreatCommitTicks = Math.max(retreatCommitTicks - 1, 0);
                boolean needsNewRetreatTarget = retreatTargetPos == null
                        || retreatCommitTicks <= 0
                        || archer.distanceToSqr(retreatTargetPos) < 2.0
                        || archer.getNavigation().isDone();

                if (needsNewRetreatTarget) {
                    Vec3 retreatPos = DefaultRandomPos.getPosAway(archer, (int) Math.ceil(RETREAT_DISTANCE), 4, target.position());
                    if (retreatPos == null) {
                        Vec3 away = archer.position().subtract(target.position());
                        if (away.horizontalDistanceSqr() < 1.0E-4) {
                            away = Vec3.directionFromRotation(0.0F, archer.getYRot());
                        }

                        Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z).normalize();
                        retreatPos = archer.position().add(horizontalAway.scale(RETREAT_DISTANCE));
                    }

                    retreatTargetPos = retreatPos;
                    retreatCommitTicks = adjustedTickDelay(RETREAT_REPATH_TICKS);
                    archer.getNavigation().moveTo(retreatTargetPos.x, retreatTargetPos.y, retreatTargetPos.z, RETREAT_SPEED_MODIFIER);
                    archer.markCombatMovementForAnimation();
                    archer.markRetreating();
                }

                ticksUntilNextShot = Math.max(ticksUntilNextShot, adjustedTickDelay(8));
                return;
            }

            ticksUntilNextMeleeApproachPath = 0;
            retreatTargetPos = null;
            retreatCommitTicks = 0;

            if (!hasLineOfSight || !inShootRange) {
                archer.setBowAiming(false);
                ticksUntilNextPathRecalculation = Math.max(ticksUntilNextPathRecalculation - 1, 0);
                if (ticksUntilNextPathRecalculation <= 0) {
                    boolean pathing = archer.getNavigation().moveTo(target, archer.applyShallowWaterSpeedFactor(speedModifier));
                    if (pathing) {
                        archer.markCombatMovementForAnimation();
                    }
                    ticksUntilNextPathRecalculation = adjustedTickDelay(10);
                }
            } else {
                archer.setBowAiming(true);
                archer.getNavigation().stop();
                archer.setCombatMoving(false);
            }

            // While closing distance, vanilla navigation must own body yaw so turns stay aligned with
            // the current path node (one-block steps, passable plants). Forcing the body toward the
            // target here made navigation and aiming fight every tick and caused patrol zigzags.
            boolean facingTarget = hasLineOfSight
                    && inShootRange
                    && archer.turnBodyToward(target);
            ticksUntilNextShot = Math.max(ticksUntilNextShot - 1, 0);
            if (ticksUntilNextShot <= 0
                    && hasLineOfSight
                    && inShootRange
                    && facingTarget
                    && archer.hasClearShotToTarget(target)) {
                archer.beginBowShot(target);
                ticksUntilNextShot = adjustedTickDelay(MIN_SHOT_COOLDOWN_TICKS + archer.getRandom().nextInt(SHOT_COOLDOWN_RANDOM_TICKS));
            } else if (ticksUntilNextShot <= 0 && hasLineOfSight && inShootRange && facingTarget) {
                ticksUntilNextShot = adjustedTickDelay(8);
            }
        }

        /** Keeps walking in on the target, swinging as he goes, and stops once he is close enough. */
        private void walkIntoMelee(LivingEntity target, double distanceToTargetSqr) {
            ticksUntilNextMeleeApproachPath = Math.max(ticksUntilNextMeleeApproachPath - 1, 0);
            if (distanceToTargetSqr <= PUNCH_RANGE * PUNCH_RANGE) {
                archer.getNavigation().stop();
            } else if (ticksUntilNextMeleeApproachPath <= 0) {
                // Timer only: an unreachable target leaves the path empty every tick, which would
                // otherwise mean a full A* search per tick.
                boolean pathing = archer.getNavigation().moveTo(target, archer.applyShallowWaterSpeedFactor(speedModifier));
                ticksUntilNextMeleeApproachPath = adjustedTickDelay(pathing ? 4 : 20);
            }
            if (!archer.getNavigation().isDone()) {
                archer.markCombatMovementForAnimation();
            }
        }

        /**
         * On a boar's back he only aims and shoots: no approaching, retreating or punching, and only at
         * targets inside the cone ahead of the boar. The boar's own AI does the moving and the ramming.
         */
        private void tickMounted(LivingEntity target) {
            archer.getNavigation().stop();
            archer.setCombatMoving(false);
            archer.setRetreating(false);
            archer.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (archer.isPreparingBowShot()) {
                archer.tickPendingBowShot();
                if (!archer.isPreparingBowShot()) {
                    ticksUntilNextShot = Math.max(ticksUntilNextShot, adjustedTickDelay(POST_SHOT_IDLE_TICKS));
                }
                return;
            }

            double distanceSqr = archer.distanceToSqr(target);
            boolean canShoot = archer.getSensing().hasLineOfSight(target)
                    && distanceSqr <= SHOOT_RANGE * SHOOT_RANGE
                    && archer.isTargetInMountedCone(target);
            archer.setBowAiming(canShoot);
            ticksUntilNextShot = Math.max(ticksUntilNextShot - 1, 0);
            if (canShoot && ticksUntilNextShot <= 0 && archer.hasClearShotToTarget(target)) {
                archer.beginBowShot(target);
                ticksUntilNextShot = adjustedTickDelay(MIN_SHOT_COOLDOWN_TICKS
                        + archer.getRandom().nextInt(SHOT_COOLDOWN_RANDOM_TICKS));
            }
        }

        private void updateClosePressure(boolean targetPressuringArcher) {
            if (targetPressuringArcher) {
                closePressureTicks = Math.min(closePressureTicks + 1, MIN_CLOSE_PRESSURE_BEFORE_MELEE_TICKS);
            } else {
                closePressureTicks = Math.max(closePressureTicks - 2, 0);
            }
        }

        private void rerollStandAndFightPunchGoal() {
            standAndFightPunchGoal = STAND_AND_FIGHT_MIN_PUNCHES + archer.getRandom().nextInt(STAND_AND_FIGHT_RANDOM_PUNCHES + 1);
        }

        private void startRetreat() {
            postMeleeRetreatTicks = adjustedTickDelay(POST_MELEE_RETREAT_TICKS);
            retreatTargetPos = null;
            retreatCommitTicks = 0;
        }

        private void tickPostMeleeRetreat(LivingEntity target) {
            boolean needsNewRetreatTarget = retreatTargetPos == null
                    || archer.distanceToSqr(retreatTargetPos) < 2.0
                    || archer.getNavigation().isDone();

            if (needsNewRetreatTarget) {
                Vec3 retreatPos = DefaultRandomPos.getPosAway(
                        archer,
                        (int) Math.ceil(POST_MELEE_RETREAT_DISTANCE),
                        4,
                        target.position());
                if (retreatPos == null) {
                    Vec3 away = archer.position().subtract(target.position());
                    if (away.horizontalDistanceSqr() < 1.0E-4) {
                        away = Vec3.directionFromRotation(0.0F, archer.getYRot());
                    }

                    Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z).normalize();
                    retreatPos = archer.position().add(horizontalAway.scale(POST_MELEE_RETREAT_DISTANCE));
                }

                retreatTargetPos = retreatPos;
            }

            archer.getNavigation().moveTo(retreatTargetPos.x, retreatTargetPos.y, retreatTargetPos.z, POST_MELEE_RETREAT_SPEED);
            archer.markCombatMovementForAnimation();
            archer.markRetreating();
        }
    }
}
