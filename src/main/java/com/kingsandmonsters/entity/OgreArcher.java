package com.kingsandmonsters.entity;

import com.kingsandmonsters.Config;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.entity.animation.SynchronizedAnimationController;
import com.kingsandmonsters.entity.animation.CanonicalOneShotState;
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
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;
import java.util.List;

public class OgreArcher extends OgreGrunt {
    private static final EntityDataAccessor<Long> DATA_ARCHER_SHOT_START_TICK =
            SynchedEntityData.defineId(OgreArcher.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_ARCHER_PUNCH_START_TICK =
            SynchedEntityData.defineId(OgreArcher.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_ARCHER_AIMING =
            SynchedEntityData.defineId(OgreArcher.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ARCHER_COMBAT_MOVING =
            SynchedEntityData.defineId(OgreArcher.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ARCHER_RETREATING =
            SynchedEntityData.defineId(OgreArcher.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RARE_IDLE = RawAnimation.begin().then("idle2", software.bernie.geckolib.animation.Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("Run");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("Shoot");
    private static final RawAnimation PUNCH = RawAnimation.begin().thenPlay("Punch");
    // The authored "Shoot" clip is 2.5s (50 ticks) — this used to be set to 40, cutting the
    // animation's last half-second short every time.
    private static final int SHOOT_ANIMATION_TICKS = 50;
    private static final int BOW_DRAW_START_TICKS = 5;
    // 1.88s into the (now correctly full-length) animation.
    private static final int ARROW_RELEASE_TICKS = 38;
    // Idle beat held after a shot's animation fully finishes, before the next draw can begin —
    // without this, the short inter-shot cooldown (MIN_SHOT_COOLDOWN_TICKS) had already ticked
    // down to 0 while the previous shot's own animation was still playing, so the next draw began
    // the instant this one ended, chaining shots with no breather and reading as jittery.
    private static final int POST_SHOT_IDLE_TICKS = 16;
    private static final int PUNCH_ANIMATION_TICKS = 20;
    private static final int PUNCH_IMPACT_TICKS = 12;
    private static final int PUNCH_SOUND_LEAD_TICKS = 2;
    private static final int PUNCH_COOLDOWN_TICKS = 44;
    private static final int MIN_SHOT_COOLDOWN_TICKS = 6;
    private static final int SHOT_COOLDOWN_RANDOM_TICKS = 5;
    private static final double SHOOT_RANGE = 22.0;
    private static final double PUNCH_RANGE = 2.35;
    private static final double PUNCH_HIT_RANGE = 3.5;
    private static final double RETREAT_RANGE = 6.0;
    private static final double RETREAT_DISTANCE = 7.0;
    // Match retreat movement to the normal punch-engage pace; both use the run animation.
    private static final double RETREAT_SPEED_MODIFIER = 1.0;
    private static final int RETREAT_REPATH_TICKS = 28;
    private static final int POST_MELEE_RETREAT_TICKS = 36;
    private static final double POST_MELEE_RETREAT_DISTANCE = 9.0;
    private static final double POST_MELEE_RETREAT_SPEED = 1.0;
    private static final float PUNCH_DAMAGE = 6.5F;
    private static final double PUNCH_KNOCKBACK_STRENGTH = 0.55;
    private static final float PUNCH_SOUND_VOLUME = 0.75F;
    private static final float PUNCH_SOUND_PITCH = 0.8F;
    private static final int MIN_CLOSE_PRESSURE_BEFORE_MELEE_TICKS = 18;
    private static final int STAND_AND_FIGHT_MIN_PUNCHES = 3;
    private static final int STAND_AND_FIGHT_RANDOM_PUNCHES = 2;
    private static final float ARROW_PROJECTILE_SPEED = 2.3F;
    private static final float ARROW_INACCURACY = 4.5F;
    private static final double ARROW_LOB_COMPENSATION = 0.14;
    private static final double ARROW_SPAWN_FORWARD_OFFSET = 0.45;
    private static final double ARROW_SPAWN_SIDE_OFFSET = 0.20;
    private static final double ARROW_SPAWN_VERTICAL_OFFSET = 0.20;
    // Calibrated against full diamond armor (~1.5 hearts effective per hit) to match the grunt/brute
    // melee tuning pass — arrows hit noticeably harder on lighter armor (~2.4 hearts vs full iron).
    private static final double OGRE_ARROW_BASE_DAMAGE = 11.0;
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
    private int shootAnimationTicks;
    private int activeShotTicks;
    private int arrowReleaseTicks;
    private int punchAnimationTicks;
    private int activePunchTicks;
    private int punchImpactTicks;
    private int punchCooldownTicks;
    private int combatMovementAnimationTicks;
    private int retreatAnimationTicks;
    private int lastAnimationTick = -1;
    private int movementAnimationGraceTicks;
    private boolean arrowReleasedThisShot;
    private boolean bowDrawStartedThisShot;
    private boolean punchHitThisPunch;
    private boolean punchImpactSoundPlayed;
    private boolean startingShootAnimation;
    private boolean startingPunchAnimation;
    private LivingEntity pendingShotTarget;
    private LivingEntity pendingPunchTarget;
    private double enchantedBowDamageBonus;
    private boolean enchantedBowHasFlame;
    private double configuredArrowBaseDamage = OGRE_ARROW_BASE_DAMAGE;
    private final CanonicalOneShotState visualOneShot = new CanonicalOneShotState();

    public OgreArcher(EntityType<? extends OgreArcher> type, Level level) {
        super(type, level);
        avoidWaterPathfinding();
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    @Override
    @SuppressWarnings("deprecation") // Required 1.21.1 OverrideOnly spawn hook; no replacement exists in this target.
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
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

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.288)
                .add(Attributes.FOLLOW_RANGE, 34.0)
                .add(Attributes.ARMOR, 9.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ATTACK_KNOCKBACK, 0.35)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.SAFE_FALL_DISTANCE, 4.0)
                // See OgreGrunt#createAttributes — default 0 caps water acceleration at a tiny fixed
                // baseline regardless of requested speed, which would make applyShallowWaterSpeedFactor's
                // 50% reduction meaningless since he's already moving slower than that.
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0);
    }

    @Override
    protected double getWanderSpeedModifier() {
        return 0.14 / 0.288;
    }

    @Override
    public void applyConfiguredCombatAttributes(boolean healToMax) {
        if (!Config.isLoaded()) {
            return;
        }

        setAttributeBaseValue(Attributes.MAX_HEALTH, Config.OGRE_ARCHER_MAX_HEALTH.get());
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, Config.OGRE_ARCHER_MELEE_DAMAGE.get());
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, Config.OGRE_ARCHER_MOVEMENT_SPEED.get());
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ARCHER_SHOT_START_TICK, 0L);
        builder.define(DATA_ARCHER_PUNCH_START_TICK, 0L);
        builder.define(DATA_ARCHER_AIMING, false);
        builder.define(DATA_ARCHER_COMBAT_MOVING, false);
        builder.define(DATA_ARCHER_RETREATING, false);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            if (combatMovementAnimationTicks > 0 && --combatMovementAnimationTicks <= 0) {
                setCombatMoving(false);
            }
            if (retreatAnimationTicks > 0 && --retreatAnimationTicks <= 0) {
                setRetreating(false);
            }
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OgreArcherShootGoal(this, 1.0));
        goalSelector.addGoal(4, new GuardSuperiorGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, getWanderSpeedModifier()));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        registerTerritorialTargetGoal();
    }

    private void beginBowShot(LivingEntity target) {
        ensureBowEquipped();
        setBowAiming(true);
        pendingShotTarget = target;
        activeShotTicks = SHOOT_ANIMATION_TICKS;
        arrowReleaseTicks = ARROW_RELEASE_TICKS;
        arrowReleasedThisShot = false;
        bowDrawStartedThisShot = false;
        playShootAnimation();
    }

    private void tickPendingBowShot() {
        if (pendingShotTarget == null) {
            return;
        }

        activeShotTicks--;
        int elapsedShotTicks = SHOOT_ANIMATION_TICKS - activeShotTicks;
        if (!bowDrawStartedThisShot && elapsedShotTicks >= BOW_DRAW_START_TICKS) {
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
        Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), getMainHandItem());
        arrow.setPos(arrowSpawn.x, arrowSpawn.y, arrowSpawn.z);

        double xOffset = target.getX() - arrowSpawn.x;
        double zOffset = target.getZ() - arrowSpawn.z;
        double horizontalDistance = Math.sqrt(xOffset * xOffset + zOffset * zOffset);
        double yOffset = target.getY(0.3333333333333333) - arrowSpawn.y;
        arrow.shoot(xOffset, yOffset + horizontalDistance * ARROW_LOB_COMPENSATION, zOffset,
                ARROW_PROJECTILE_SPEED, ARROW_INACCURACY);
        arrow.setBaseDamage(getVelocityAdjustedArrowDamage());
        if (enchantedBowHasFlame) {
            arrow.igniteForSeconds(5.0F);
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
            enchantBow(level, bow, Enchantments.POWER, 1);
            enchantedBowDamageBonus = 0.5;
        } else if (roll < 88) {
            enchantBow(level, bow, Enchantments.POWER, 2);
            enchantedBowDamageBonus = 1.0;
        } else if (roll < 96) {
            enchantBow(level, bow, Enchantments.POWER, 2);
            enchantBow(level, bow, Enchantments.PUNCH, 1);
            enchantedBowDamageBonus = 1.0;
        } else {
            enchantBow(level, bow, Enchantments.POWER, 1);
            enchantBow(level, bow, Enchantments.FLAME, 1);
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

    private void enchantBow(ServerLevelAccessor level, ItemStack bow, net.minecraft.resources.ResourceKey<Enchantment> enchantment, int enchantmentLevel) {
        Holder<Enchantment> holder = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(enchantment);
        bow.enchant(holder, enchantmentLevel);
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

    private boolean isBowAiming() {
        return entityData.get(DATA_ARCHER_AIMING);
    }

    private boolean isPreparingBowShot() {
        return pendingShotTarget != null;
    }

    private void beginPunch(LivingEntity target) {
        cancelBowShot();
        plantForMelee();
        pendingPunchTarget = target;
        activePunchTicks = PUNCH_ANIMATION_TICKS;
        punchImpactTicks = PUNCH_IMPACT_TICKS;
        punchHitThisPunch = false;
        punchImpactSoundPlayed = false;
        punchCooldownTicks = PUNCH_COOLDOWN_TICKS;
        playPunchAnimation();
    }

    private void tickPendingPunch() {
        if (pendingPunchTarget == null) {
            return;
        }

        plantForMelee();
        activePunchTicks--;
        punchImpactTicks--;
        if (!punchImpactSoundPlayed && punchImpactTicks <= PUNCH_SOUND_LEAD_TICKS) {
            playPunchImpactSound();
        }
        if (!punchHitThisPunch && punchImpactTicks <= 0) {
            applyPunchImpact();
        }

        if (activePunchTicks <= 0) {
            cancelPunch();
        }
    }

    private void applyPunchImpact() {
        punchHitThisPunch = true;
        if (level().isClientSide || pendingPunchTarget == null || !pendingPunchTarget.isAlive()) {
            return;
        }

        if (distanceToSqr(pendingPunchTarget) > PUNCH_HIT_RANGE * PUNCH_HIT_RANGE
                || !getSensing().hasLineOfSight(pendingPunchTarget)) {
            return;
        }

        if (!punchImpactSoundPlayed) {
            playPunchImpactSound();
        }
        pendingPunchTarget.hurt(damageSources().mobAttack(this), PUNCH_DAMAGE);
        pendingPunchTarget.knockback(PUNCH_KNOCKBACK_STRENGTH, getX() - pendingPunchTarget.getX(), getZ() - pendingPunchTarget.getZ());
    }

    private void playPunchImpactSound() {
        if (level().isClientSide || pendingPunchTarget == null || !pendingPunchTarget.isAlive()) {
            return;
        }

        if (distanceToSqr(pendingPunchTarget) > PUNCH_HIT_RANGE * PUNCH_HIT_RANGE
                || !getSensing().hasLineOfSight(pendingPunchTarget)) {
            return;
        }

        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE,
                PUNCH_SOUND_VOLUME, PUNCH_SOUND_PITCH);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.MUD_HIT, SoundSource.HOSTILE, 0.35F, 0.75F);
        punchImpactSoundPlayed = true;
    }

    private void cancelPunch() {
        pendingPunchTarget = null;
        activePunchTicks = 0;
        punchImpactTicks = 0;
        punchHitThisPunch = false;
        punchImpactSoundPlayed = false;
        if (!level().isClientSide) {
            entityData.set(DATA_ARCHER_PUNCH_START_TICK, 0L);
        }
    }

    private void plantForMelee() {
        getNavigation().stop();
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(0.0, movement.y, 0.0);
        setSpeed(0.0F);
    }

    private boolean isPreparingPunch() {
        return pendingPunchTarget != null;
    }

    private boolean canPunch(LivingEntity target) {
        return punchCooldownTicks <= 0
                && target.isAlive()
                && distanceToSqr(target) <= PUNCH_RANGE * PUNCH_RANGE
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

        shootAnimationTicks = SHOOT_ANIMATION_TICKS;
        startingShootAnimation = true;
    }

    private void playPunchAnimation() {
        if (!level().isClientSide) {
            entityData.set(DATA_ARCHER_PUNCH_START_TICK, level().getGameTime());
        }

        punchAnimationTicks = PUNCH_ANIMATION_TICKS;
        startingPunchAnimation = true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new SynchronizedAnimationController<>(this, "Archer Movement", 5, this::animateMovement));
    }

    private PlayState animateMovement(AnimationState<OgreArcher> state) {
        long shotStart = entityData.get(DATA_ARCHER_SHOT_START_TICK);
        long punchStart = entityData.get(DATA_ARCHER_PUNCH_START_TICK);
        int observedType = punchStart > shotStart ? 2 : shotStart > 0L ? 1 : 0;
        long observedStart = observedType == 2 ? punchStart : shotStart;
        int visualType = visualOneShot.update(state.getController(), observedType, observedStart,
                id -> id == 2 ? PUNCH_ANIMATION_TICKS : SHOOT_ANIMATION_TICKS,
                level().getGameTime());
        if (visualType > 0) {
            return state.setAndContinue(visualType == 2 ? PUNCH : SHOOT);
        }

        // Retreat and actual combat movement are synced because getTarget() is server-only.
        // Bow aiming is intentionally not a movement signal: between shots the archer is planted
        // in place and should use his idle animation.
        if (isRetreating()) {
            idleAnimationTicks = 0;
            rareIdleTicks = 0;
            lastAnimationTick = tickCount;
            return state.setAndContinue(RUN);
        }

        if (isCombatMoving()) {
            idleAnimationTicks = 0;
            rareIdleTicks = 0;
            lastAnimationTick = tickCount;
            return state.setAndContinue(RUN);
        }

        if (isRunMovementState() && isMovingForAnimation(state)) {
            idleAnimationTicks = 0;
            rareIdleTicks = 0;
            lastAnimationTick = tickCount;
            return state.setAndContinue(RUN);
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

    private boolean isShootAnimationPlaying(AnimationState<OgreArcher> state) {
        return shootAnimationTicks > 0;
    }

    private boolean isPunchAnimationPlaying(AnimationState<OgreArcher> state) {
        return punchAnimationTicks > 0;
    }

    private void updateShootAnimationTimer(AnimationState<OgreArcher> state) {
        if (lastAnimationTick == tickCount) {
            return;
        }

        int tickDelta = lastAnimationTick < 0 ? 1 : Math.max(1, tickCount - lastAnimationTick);
        lastAnimationTick = tickCount;
        shootAnimationTicks = Math.max(0, shootAnimationTicks - tickDelta);
        idleAnimationTicks = 0;
        rareIdleTicks = 0;
    }

    private void updatePunchAnimationTimer(AnimationState<OgreArcher> state) {
        if (lastAnimationTick == tickCount) {
            return;
        }

        int tickDelta = lastAnimationTick < 0 ? 1 : Math.max(1, tickCount - lastAnimationTick);
        lastAnimationTick = tickCount;
        punchAnimationTicks = Math.max(0, punchAnimationTicks - tickDelta);
        idleAnimationTicks = 0;
        rareIdleTicks = 0;
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
        if (moving) {
            movementAnimationGraceTicks = 4;
            return true;
        }
        if (movementAnimationGraceTicks > 0) {
            movementAnimationGraceTicks--;
            return true;
        }
        return false;
    }

    private boolean hasActiveCombatTarget() {
        LivingEntity target = getTarget();
        return target != null && target.isAlive();
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
                archer.getNavigation().stop();
                archer.tickPendingPunch();
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
                    ticksUntilNextMeleeApproachPath = Math.max(ticksUntilNextMeleeApproachPath - 1, 0);
                    if (archer.canPunch(target)) {
                        archer.getNavigation().stop();
                        archer.beginPunch(target);
                        standAndFightPunches++;
                    } else if (distanceToTargetSqr <= PUNCH_RANGE * PUNCH_RANGE) {
                        archer.getNavigation().stop();
                    } else if (ticksUntilNextMeleeApproachPath <= 0 || archer.getNavigation().isDone()) {
                        archer.getNavigation().moveTo(target, archer.applyShallowWaterSpeedFactor(speedModifier));
                        archer.markCombatMovementForAnimation();
                        ticksUntilNextMeleeApproachPath = adjustedTickDelay(4);
                    }

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

            // While closing distance, vanilla ground navigation must own body yaw so its turn
            // decisions remain aligned with the current path node (especially across one-block
            // elevation changes and passable plants). Forcing the body toward the target here
            // made navigation and aiming fight every tick, producing patrol zigzags and spins.
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
