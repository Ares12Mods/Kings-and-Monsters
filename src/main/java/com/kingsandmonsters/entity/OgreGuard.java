package com.kingsandmonsters.entity;

import com.kingsandmonsters.Config;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.effect.CombatEffects;
import com.kingsandmonsters.enchantment.ModEnchantmentEffects;
import com.kingsandmonsters.enchantment.ModEnchantments;
import com.kingsandmonsters.entity.animation.SynchronizedAnimationController;
import com.kingsandmonsters.entity.animation.CanonicalOneShotState;
import com.kingsandmonsters.network.ScreenShakePayload;
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
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.kingsandmonsters.network.ModNetwork;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.EnumSet;
import java.util.List;

public class OgreGuard extends OgreGrunt {
    private static final int IDLE_SOUND_INTERVAL_TICKS = 20 * 60;
    private static final String ROYAL_THRONE_GUARD_TAG_PREFIX =
            "KingsAndMonstersFortResident_throne_guard_";
    private static final EntityDataAccessor<Long> DATA_GUARD_SHOT_START_TICK =
            SynchedEntityData.defineId(OgreGuard.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_GUARD_PUNCH_START_TICK =
            SynchedEntityData.defineId(OgreGuard.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_GUARD_AIMING =
            SynchedEntityData.defineId(OgreGuard.class, EntityDataSerializers.BOOLEAN);
    // True from the moment the thrown spear entity spawns until the throw animation ends, so the
    // held copy is not rendered alongside the projectile.
    private static final EntityDataAccessor<Boolean> DATA_GUARD_SPEAR_THROWN =
            SynchedEntityData.defineId(OgreGuard.class, EntityDataSerializers.BOOLEAN);
    // True while his target is within reach of the spear: the held spear is then drawn pointing forward for a stab.
    private static final EntityDataAccessor<Boolean> DATA_GUARD_SPEAR_STAB =
            SynchedEntityData.defineId(OgreGuard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_GUARD_SWING_ID =
            SynchedEntityData.defineId(OgreGuard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_GUARD_RETREATING =
            SynchedEntityData.defineId(OgreGuard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GUARD_COMBAT_MOVING =
            SynchedEntityData.defineId(OgreGuard.class, EntityDataSerializers.BOOLEAN);
    // getTarget() is server-only, so animateMovement() (client-side) always reads null and used
    // to fall back to WALK while chasing. DATA_GUARD_HAS_TARGET is the synced stand-in.
    private static final EntityDataAccessor<Boolean> DATA_GUARD_HAS_TARGET =
            SynchedEntityData.defineId(OgreGuard.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation COMBAT_WALK = RawAnimation.begin().thenLoop("Combat walk");
    // The full run is kept for his retreat once he is hurt.
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("throw");
    // Both melee clips are thrown over the combat walk, like the other ogres' swings.
    private static final RawAnimation STAB = RawAnimation.begin().thenPlay("spear_stab");
    private static final RawAnimation SLAM = RawAnimation.begin().thenPlay("slam");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("Bite");
    public static final float ATTACK_OVERLAY_FADE_OUT_TICKS = 8.0F;
    private static final int STAB_ID = 1;
    private static final int SLAM_ID = 2;
    private static final int BITE_ID = 3;
    // Tick marks of the foot landings in each walk clip, so steps sound with the visible foot.
    private static final java.util.Map<String, double[]> FOOT_CONTACTS = java.util.Map.of(
            "walk", new double[]{0.0, 26.67},
            "Combat walk", new double[]{0.0, 13.33});
    private static final int SHOOT_ANIMATION_TICKS = 45;
    private static final int BOW_DRAW_START_TICKS = 1;
    // Release is at the 1.42s keyframe (tick 28), but the client only starts the animation once
    // it receives the synced DATA_GUARD_SHOT_START_TICK, a tick or more after the server counter
    // starts. Nudged later to compensate, and again after in-game feedback. Still an estimate.
    private static final int ARROW_RELEASE_TICKS = 34;
    // Both clips run 1.5s, but they end at rest pose while the walk has the arms swinging. The overlay lets go
    // at 1.35s, partway through the return, and the model eases the rest of the way into the walk.
    private static final int SWING_ANIMATION_TICKS = 27;
    // Contact must follow the rendered weapon/hand, including the delayed client overlay start.
    // The previous +3 estimate still made both attacks damage before visible contact in game.
    private static final int STAB_IMPACT_TICKS = 19;
    private static final int SLAM_IMPACT_TICKS = 23;
    // The authored bite closes at 0.79s; nearest server tick is 16 (0.80s).
    private static final int BITE_IMPACT_TICKS = 16;
    private static final int BITE_REGENERATION_TICKS = 100;
    /** Regeneration III (amplifier 2) after a landed bite, matching the Brute and Trapper. */
    private static final int BITE_REGENERATION_AMPLIFIER = 2;
    private static final float STAB_SPEAR_TILT_DEGREES = 18.0F;
    private static final float STAB_SPEAR_TILT_START_TICK = 0.58F * 20.0F;
    private static final float STAB_SPEAR_TILT_PEAK_TICK = 0.71F * 20.0F;
    private static final float STAB_SPEAR_TILT_HOLD_END_TICK = 17.0F;
    private static final float STAB_SPEAR_TILT_END_TICK = 25.0F;
    private static final int STAB_SWOOSH_LEAD_TICKS = 5;
    private static final float SLAM_CHANCE = 0.35F;
    // The slam only comes out at close quarters; further out he reaches with the spear.
    private static final double SLAM_START_RANGE = 2.35;
    private static final double STAB_START_RANGE = 4.0;
    private static final double STAB_HIT_RANGE = 5.0;
    private static final double SLAM_CENTER_REACH = 1.7;
    private static final double SLAM_RADIUS = 2.1;
    private static final float SLAM_DAMAGE = OgreDamage.raw(OgreDamage.Tier.GUARD, OgreDamage.Blow.LIGHT);
    private static final double SLAM_KNOCKBACK_STRENGTH = 0.9;
    // The punch holds full extension at tick 15, but the client animation starts a tick or more
    // after the server counter (it waits on the synced start tick). The +6 that fixed the throw
    // was slightly late here, so this splits the difference.
    // Tiny per-tick step toward the target while the swing is winding up, so he visibly closes
    // the last bit of distance as part of the animation itself instead of standing still — a
    // single upfront velocity kick read as a slide happening before the punch, not part of it.
    // Brief "just noticed you" pause before he can throw his first attack against a freshly
    // (re)acquired target, so a player who respawns already within his reach doesn't get an
    // instant, zero-warning punch the very tick he re-targets them.
    private static final int ENGAGEMENT_REACTION_TICKS = 10;
    private static final int PUNCH_SOUND_LEAD_TICKS = 2;
    private static final int PUNCH_COOLDOWN_TICKS = 44;
    private static final int MIN_SHOT_COOLDOWN_TICKS = 18;
    private static final int SHOT_COOLDOWN_RANDOM_TICKS = 12;
    // Well under OgreArcher's 22.0 — the guard is a brute with a spear-throw option, not a
    // dedicated ranged specialist, so he should close in rather than engaging from way out.
    private static final double SHOOT_RANGE = 12.0;
    private static final double PUNCH_RANGE = 2.35;
    private static final double MELEE_MODE_ENTER_RANGE = 5.0;
    private static final double MELEE_MODE_EXIT_RANGE = SHOOT_RANGE;
    private static final double MELEE_ENGAGE_RANGE = 5.0;
    private static final float PUNCH_DAMAGE = OgreDamage.raw(OgreDamage.Tier.GUARD, OgreDamage.Blow.MINOR);
    private static final double PUNCH_KNOCKBACK_STRENGTH = 0.75;
    private static final float PUNCH_SOUND_VOLUME = 0.75F;
    private static final float PUNCH_SOUND_PITCH = 0.8F;
    // Health fraction below which the guard backs off to put distance between himself and his
    // target. Above it he stands and fights; he's meant to be more aggressive than the archer.
    private static final double LOW_HEALTH_RETREAT_HEALTH_FRACTION = 0.5;
    // He stops as soon as he reaches the outer portion of spear range. The timer is only a
    // pathfinding failsafe so blocked terrain cannot leave him in the retreat state forever.
    private static final int LOW_HEALTH_RETREAT_TICKS = 100;
    private static final double LOW_HEALTH_RETREAT_DISTANCE = 4.0;
    private static final double LOW_HEALTH_RETREAT_STOP_RANGE = SHOOT_RANGE - 0.5;
    // The original pace, kept for the new run clip.
    private static final double LOW_HEALTH_RETREAT_SPEED = 1.08;
    // About 0.254 against his 0.275 attribute: roughly 3.7 blocks a second, a touch faster than the
    // Archer's 3.45, for the new combat walk. Raise or lower this to taste.
    private static final double GUARD_COMBAT_SPEED = 0.925;
    // Matches TridentItem.SHOOT_POWER so the thrown spear flies at the same speed as a player-thrown trident.
    private static final float ARROW_PROJECTILE_SPEED = 2.5F;
    private static final float ARROW_INACCURACY = 4.5F;
    private static final double ARROW_LOB_COMPENSATION = 0.14;
    // Approximate right-hand position at the release pose (both arms forward, ~1.42s keyframe).
    // An estimate, not a measured bone position, so expect tuning once seen in-game.
    private static final double ARROW_SPAWN_FORWARD_OFFSET = 0.85;
    private static final double ARROW_SPAWN_SIDE_OFFSET = 0.35;
    private static final double ARROW_SPAWN_VERTICAL_OFFSET = 0.55;
    // Calibrated against full diamond armor (~1.5 hearts effective per hit) to match the grunt/brute
    // melee tuning pass — arrows hit noticeably harder on lighter armor (~2.4 hearts vs full iron).
    private static final double OGRE_ARROW_BASE_DAMAGE = OgreDamage.raw(OgreDamage.Tier.GUARD, OgreDamage.Blow.STANDARD);
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
    private int activePunchTicks;
    private int punchImpactTicks;
    private int punchCooldownTicks;
    private int combatMovementAnimationTicks;
    private int retreatAnimationTicks;
    private boolean meleeModeActive;
    private boolean stabSwooshPlayed;
    private int activeSwingId;
    private boolean swingOverlayVisible;
    private final CanonicalOneShotState visualSwingOneShot = new CanonicalOneShotState();
    private int lastAnimationTick = -1;
    private long lastMovingAnimationTick = Long.MIN_VALUE / 2;
    private static final int MOVEMENT_ANIMATION_GRACE_TICKS = 4;
    private boolean arrowReleasedThisShot;
    private boolean bowDrawStartedThisShot;
    private boolean punchHitThisPunch;
    private boolean punchImpactSoundPlayed;
    private boolean lowHealthRetreatUsed;
    private LivingEntity pendingShotTarget;
    private LivingEntity pendingPunchTarget;
    private double enchantedBowDamageBonus;
    private boolean enchantedBowHasFlame;
    private double configuredArrowBaseDamage = OGRE_ARROW_BASE_DAMAGE;
    private final CanonicalOneShotState visualOneShot = new CanonicalOneShotState();

    public OgreGuard(EntityType<? extends OgreGuard> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.HUNTERS_SPEAR.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public void rollEliteSpearEnchantment() {
        // Retain the old 25% chance that a guard carries the remaining signature spear
        // enchantment rather than doubling Heavy Throw's occurrence.
        if (level().isClientSide || getRandom().nextInt(4) != 0) {
            return;
        }

        ItemStack spear = new ItemStack(ModItems.HUNTERS_SPEAR.get());
        enchantSpear(spear, ModEnchantments.HEAVY_THROW, 1 + getRandom().nextInt(3));
        setItemSlot(EquipmentSlot.MAINHAND, spear);
    }

    private void enchantSpear(ItemStack spear, net.minecraftforge.registries.RegistryObject<Enchantment> enchantment, int level) {
        spear.enchant(enchantment.get(), level);
    }

    @Override
    @SuppressWarnings("deprecation") // Required 1.21.1 OverrideOnly spawn hook; no replacement exists in this target.
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData, @org.jetbrains.annotations.Nullable net.minecraft.nbt.CompoundTag spawnTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, spawnTag);
        ensureBowEquipped();
        return data;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putDouble("EnchantedBowDamageBonus", enchantedBowDamageBonus);
        compound.putBoolean("EnchantedBowHasFlame", enchantedBowHasFlame);
        compound.putBoolean("LowHealthRetreatUsed", lowHealthRetreatUsed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        enchantedBowDamageBonus = compound.getDouble("EnchantedBowDamageBonus");
        enchantedBowHasFlame = compound.getBoolean("EnchantedBowHasFlame");
        lowHealthRetreatUsed = compound.getBoolean("LowHealthRetreatUsed");
        ensureBowEquipped();
    }

    @Override

    public float maxUpStep() {

        return 1.0F;

    }


    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 70.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.275)
                .add(Attributes.FOLLOW_RANGE, 34.0)
                .add(Attributes.ARMOR, 15.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.65)
                .add(Attributes.ATTACK_KNOCKBACK, 0.35);
    }

    @Override
    protected double getWanderSpeedModifier() {
        // The wander clip is a third slower now (2.67s a stride, was 2s), so the pace drops with it.
        return 0.121 / 0.275;
    }

    @Override
    public void applyConfiguredCombatAttributes(boolean healToMax) {
        if (!Config.isLoaded()) {
            return;
        }

        setAttributeBaseValue(Attributes.MAX_HEALTH, Config.OGRE_GUARD_MAX_HEALTH.get());
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, Config.OGRE_GUARD_MELEE_DAMAGE.get());
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, Config.OGRE_GUARD_MOVEMENT_SPEED.get());
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, Config.OGRE_GUARD_FOLLOW_RANGE.get());
        setAttributeBaseValue(Attributes.ARMOR, Config.OGRE_GUARD_ARMOR.get());
        configuredArrowBaseDamage = Config.OGRE_GUARD_SPEAR_DAMAGE.get();
        applyConfiguredHealth(healToMax);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.OGRE_GUARD_IDLE.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return IDLE_SOUND_INTERVAL_TICKS;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.OGRE_GUARD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.OGRE_GUARD_DEATH.get();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_GUARD_SHOT_START_TICK, 0L);
        this.entityData.define(DATA_GUARD_PUNCH_START_TICK, 0L);
        this.entityData.define(DATA_GUARD_AIMING, false);
        this.entityData.define(DATA_GUARD_SPEAR_THROWN, false);
        this.entityData.define(DATA_GUARD_SPEAR_STAB, false);
        this.entityData.define(DATA_GUARD_SWING_ID, 0);
        this.entityData.define(DATA_GUARD_RETREATING, false);
        this.entityData.define(DATA_GUARD_COMBAT_MOVING, false);
        this.entityData.define(DATA_GUARD_HAS_TARGET, false);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            if (combatMovementAnimationTicks > 0 && --combatMovementAnimationTicks <= 0) {
                setCombatMoving(false);
            }
            if (retreatAnimationTicks > 0 && --retreatAnimationTicks <= 0) {
                entityData.set(DATA_GUARD_RETREATING, false);
            }

            entityData.set(DATA_GUARD_HAS_TARGET, getTarget() != null);
            // Melee mode: once the target is within 5 blocks the spear stays pointed forward, until it is
            // 8 blocks off (or he has backed away that far). It drops back to the throwing grip for a throw.
            LivingEntity stabTarget = getTarget();
            boolean meleeMode = meleeModeActive;
            double meleeRange = meleeMode ? MELEE_MODE_EXIT_RANGE : MELEE_MODE_ENTER_RANGE;
            boolean inMeleeMode = stabTarget != null && distanceToSqr(stabTarget) <= meleeRange * meleeRange;
            meleeModeActive = inMeleeMode;
            entityData.set(DATA_GUARD_SPEAR_STAB, inMeleeMode && !isPreparingBowShot());
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        registerBogToadWarinessGoal();
        goalSelector.addGoal(1, new OgreGuardShootGoal(this, GUARD_COMBAT_SPEED));
        goalSelector.addGoal(4, new GuardSuperiorGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, getWanderSpeedModifier()));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        registerTerritorialTargetGoal();
        registerArmyGoals();
    }

    @Override
    protected boolean canGuardSuperiors() {
        // Fort throne guards are assigned fixed posts around the King. Letting the generic
        // superior-follow goal control them makes all three funnel into his hut and doorway.
        return getTags().stream().noneMatch(tag -> tag.startsWith(ROYAL_THRONE_GUARD_TAG_PREFIX))
                && super.canGuardSuperiors();
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

        // Hide the handheld copy one tick before projectile creation so both spears never render
        // together while the spawn packet reaches the client.
        if (!arrowReleasedThisShot && arrowReleaseTicks <= 1 && !level().isClientSide) {
            entityData.set(DATA_GUARD_SPEAR_THROWN, true);
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
        double xOffset = target.getX() - arrowSpawn.x;
        double zOffset = target.getZ() - arrowSpawn.z;
        double horizontalDistance = Math.sqrt(xOffset * xOffset + zOffset * zOffset);
        double yOffset = target.getY(0.3333333333333333) - arrowSpawn.y;
        Vec3 aim = new Vec3(xOffset, yOffset + horizontalDistance * ARROW_LOB_COMPENSATION, zOffset).normalize();
        ItemStack spearStack = getMainHandItem().copyWithCount(1);
        int heavyThrowLevel = ModEnchantmentEffects.level(
                level(), spearStack, ModEnchantments.HEAVY_THROW);
        float projectileSpeed = ModEnchantmentEffects.heavyThrowSpeed(
                ARROW_PROJECTILE_SPEED, heavyThrowLevel);
        float projectileDamage = ModEnchantmentEffects.heavyThrowDamage(
                (float) configuredArrowBaseDamage, heavyThrowLevel);
        OgreSpear spear = new OgreSpear(
                level(),
                this,
                spearStack,
                arrowSpawn,
                aim.scale(projectileSpeed),
                projectileDamage,
                OgreSpear.Pickup.DISALLOWED);
        level().addFreshEntity(spear);
        entityData.set(DATA_GUARD_SPEAR_THROWN, true);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_THROW,
                SoundSource.HOSTILE, 2.0F, 0.82F + getRandom().nextFloat() * 0.10F);
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

    private void ensureBowEquipped() {
        if (!getMainHandItem().is(ModItems.HUNTERS_SPEAR.get())) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.HUNTERS_SPEAR.get()));
        }
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
        // Clear the synced start tick. A stale value looks like a new trigger if the client's
        // tracking refreshes (out of range and back, respawn) and replays the animation with no
        // attack behind it.
        if (!level().isClientSide) {
            entityData.set(DATA_GUARD_SHOT_START_TICK, 0L);
            // The throw animation is over (or interrupted): show the held spear again.
            entityData.set(DATA_GUARD_SPEAR_THROWN, false);
        }
    }

    private void setBowAiming(boolean aiming) {
        entityData.set(DATA_GUARD_AIMING, aiming);
    }

    private boolean isBowAiming() {
        return entityData.get(DATA_GUARD_AIMING);
    }

    private boolean isPreparingBowShot() {
        return pendingShotTarget != null;
    }

    /** True when the held spear is drawn for a melee stab, not for a throw. */
    public boolean isSpearStabPose() {
        return entityData.get(DATA_GUARD_SPEAR_STAB);
    }

    public boolean shouldRenderHeldSpear() {
        // Visible through the wind-up; hidden from the moment the projectile spawns until the
        // throw animation finishes.
        return !entityData.get(DATA_GUARD_SPEAR_THROWN);
    }

    /** Render-only downward correction for the spear during the stab follow-through. */
    public float getStabSpearTiltDegrees(float partialTick) {
        long startTick = entityData.get(DATA_GUARD_PUNCH_START_TICK);
        if (startTick <= 0L || entityData.get(DATA_GUARD_SWING_ID) != STAB_ID) {
            return 0.0F;
        }

        float elapsed = (float) (level().getGameTime() - startTick) + partialTick;
        float weight;
        if (elapsed < STAB_SPEAR_TILT_START_TICK || elapsed >= STAB_SPEAR_TILT_END_TICK) {
            weight = 0.0F;
        } else if (elapsed < STAB_SPEAR_TILT_PEAK_TICK) {
            weight = (elapsed - STAB_SPEAR_TILT_START_TICK)
                    / (STAB_SPEAR_TILT_PEAK_TICK - STAB_SPEAR_TILT_START_TICK);
        } else if (elapsed <= STAB_SPEAR_TILT_HOLD_END_TICK) {
            weight = 1.0F;
        } else {
            weight = 1.0F - (elapsed - STAB_SPEAR_TILT_HOLD_END_TICK)
                    / (STAB_SPEAR_TILT_END_TICK - STAB_SPEAR_TILT_HOLD_END_TICK);
        }
        return STAB_SPEAR_TILT_DEGREES * Mth.clamp(weight, 0.0F, 1.0F);
    }

    private void beginPunch(LivingEntity target) {
        cancelBowShot();
        pendingPunchTarget = target;
        // Close in he may slam; further out he can only reach with the spear.
        boolean close = distanceToSqr(target) <= SLAM_START_RANGE * SLAM_START_RANGE;
        if (close) {
            float roll = getRandom().nextFloat();
            activeSwingId = roll < SLAM_CHANCE ? SLAM_ID : roll < (SLAM_CHANCE + 0.325F) ? BITE_ID : STAB_ID;
        } else {
            activeSwingId = STAB_ID;
        }
        activePunchTicks = SWING_ANIMATION_TICKS;
        punchImpactTicks = activeSwingId == SLAM_ID ? SLAM_IMPACT_TICKS
                : activeSwingId == BITE_ID ? BITE_IMPACT_TICKS : STAB_IMPACT_TICKS;
        punchHitThisPunch = false;
        stabSwooshPlayed = false;
        punchImpactSoundPlayed = false;
        punchCooldownTicks = PUNCH_COOLDOWN_TICKS;
        playPunchAnimation();
    }

    private void tickPendingPunch() {
        if (pendingPunchTarget == null) {
            return;
        }

        // He keeps walking through the swing: nothing here plants him.
        activePunchTicks--;
        punchImpactTicks--;
        // The spear whips forward from about 0.58s, a few ticks before it lands.
        if (activeSwingId == STAB_ID && !stabSwooshPlayed && punchImpactTicks <= STAB_SWOOSH_LEAD_TICKS) {
            stabSwooshPlayed = true;
            if (!level().isClientSide) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE,
                        0.7F, 1.35F + getRandom().nextFloat() * 0.1F);
            }
        }
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
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (activeSwingId == SLAM_ID) {
            applySlam(serverLevel);
            return;
        }

        LivingEntity target = pendingPunchTarget;
        if (target == null || !target.isAlive() || !stabVolumeIntersects(target)
                || !getSensing().hasLineOfSight(target)) {
            return;
        }

        if (!punchImpactSoundPlayed) {
            playPunchImpactSound();
        }
        boolean hitLanded = target.hurt(damageSources().mobAttack(this), PUNCH_DAMAGE);
        target.knockback(PUNCH_KNOCKBACK_STRENGTH, getX() - target.getX(), getZ() - target.getZ());
        if (hitLanded && activeSwingId == BITE_ID) {
            CombatEffects.applyCrippled(target, Config.CRIPPLED_BITE_DURATION_TICKS.get(), this);
            addEffect(new MobEffectInstance(MobEffects.REGENERATION, BITE_REGENERATION_TICKS,
                    BITE_REGENERATION_AMPLIFIER), this);
        }
    }

    /** Tests the whole forward spear sweep against the target hitbox instead of only entity centres. */
    private boolean stabVolumeIntersects(LivingEntity target) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 start = position().add(0.0, getBbHeight() * 0.48, 0.0);
        Vec3 end = start.add(forward.scale(STAB_HIT_RANGE));
        AABB spearSweep = new AABB(start, end).inflate(0.8, 1.15, 0.8);
        return spearSweep.intersects(target.getBoundingBox());
    }

    /** A short overhead blow into the ground: a small area around the spot he hits, lighter than a Grunt's. */
    private void applySlam(ServerLevel serverLevel) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 impact = position().add(forward.scale(SLAM_CENTER_REACH));
        List<LivingEntity> candidates = level().getEntitiesOfClass(
                LivingEntity.class, getBoundingBox().inflate(SLAM_CENTER_REACH + SLAM_RADIUS + 1.0, 2.0,
                        SLAM_CENTER_REACH + SLAM_RADIUS + 1.0),
                target -> target != this && target.isAlive() && canAttackOgreTarget(target));
        for (LivingEntity target : candidates) {
            double dx = target.getX() - impact.x;
            double dz = target.getZ() - impact.z;
            double reach = SLAM_RADIUS + target.getBbWidth() * 0.5;
            if (dx * dx + dz * dz > reach * reach
                    || target.getBoundingBox().maxY < getY() - 1.0 || target.getBoundingBox().minY > getY() + 2.5) {
                continue;
            }
            float falloff = areaFalloff(Math.sqrt(dx * dx + dz * dz), reach, OgreDamage.BLAST_EDGE_SHARE);
            if (target.hurt(damageSources().mobAttack(this), SLAM_DAMAGE * falloff)) {
                target.knockback(SLAM_KNOCKBACK_STRENGTH * falloff, getX() - target.getX(), getZ() - target.getZ());
            }
        }
        level().playSound(null, impact.x, impact.y, impact.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE,
                0.8F, 0.8F);
        level().playSound(null, impact.x, impact.y, impact.z, SoundEvents.MUD_HIT, SoundSource.HOSTILE, 0.5F, 0.7F);
        spawnSmackGroundImpact(serverLevel, impact);
        ModNetwork.sendToPlayersNear(serverLevel, null, impact.x, impact.y, impact.z, 20.0,
                new ScreenShakePayload(impact.x, impact.y, impact.z, 0.4F));
        punchImpactSoundPlayed = true;
    }

    private void playPunchImpactSound() {
        // The slam makes its own thump when it lands.
        if (level().isClientSide || activeSwingId == SLAM_ID || pendingPunchTarget == null || !pendingPunchTarget.isAlive()) {
            return;
        }

        if (!stabVolumeIntersects(pendingPunchTarget) || !getSensing().hasLineOfSight(pendingPunchTarget)) {
            return;
        }

        if (activeSwingId == BITE_ID) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.FOX_BITE,
                    SoundSource.HOSTILE, 0.65F, 0.9F);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT,
                    SoundSource.HOSTILE, 0.25F, 0.85F);
        } else {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE,
                    PUNCH_SOUND_VOLUME, PUNCH_SOUND_PITCH);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.MUD_HIT, SoundSource.HOSTILE, 0.35F, 0.75F);
        }
        punchImpactSoundPlayed = true;
    }

    private void cancelPunch() {
        pendingPunchTarget = null;
        activeSwingId = 0;
        activePunchTicks = 0;
        punchImpactTicks = 0;
        punchHitThisPunch = false;
        punchImpactSoundPlayed = false;
        // See the matching comment in cancelBowShot(): without this, a stale
        // DATA_GUARD_PUNCH_START_TICK value can replay the punch animation purely visually if the
        // client's tracking of this entity ever gets refreshed after this punch is long over.
        if (!level().isClientSide) {
            entityData.set(DATA_GUARD_PUNCH_START_TICK, 0L);
            entityData.set(DATA_GUARD_SWING_ID, 0);
        }
    }

    @Override
    public void resetTransientCombatState() {
        super.resetTransientCombatState();
        // cancelBowShot()/cancelPunch() reset the server AI counters but not the synced start ticks
        // the client animations key off; a mob reloaded mid-swing would otherwise swing forever.
        cancelBowShot();
        cancelPunch();
        setBowAiming(false);
        if (!level().isClientSide) {
            entityData.set(DATA_GUARD_SHOT_START_TICK, 0L);
            entityData.set(DATA_GUARD_PUNCH_START_TICK, 0L);
        }
    }

    private boolean isPreparingPunch() {
        return pendingPunchTarget != null;
    }

    private boolean canPunch(LivingEntity target) {
        return punchCooldownTicks <= 0
                && target.isAlive()
                && distanceToSqr(target) <= STAB_START_RANGE * STAB_START_RANGE
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
     * Proportional turn easing shared by every Guard body-turn call. {@code lerpFraction} is
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
            entityData.set(DATA_GUARD_SHOT_START_TICK, level().getGameTime());
        }
    }

    private void playPunchAnimation() {
        if (!level().isClientSide) {
            entityData.set(DATA_GUARD_SWING_ID, activeSwingId);
            entityData.set(DATA_GUARD_PUNCH_START_TICK, level().getGameTime());
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new SynchronizedAnimationController<>(this, "Guard Movement", 5, this::animateMovement)
                .withFootfalls(FOOT_CONTACTS, this::playGuardFootfall));
        // Registered second so the stab and slam blend in over whatever the walk is doing; the model
        // eases them back out (see OgreGuardModel).
        controllers.add(new SynchronizedAnimationController<>(this, "Guard Swing Overlay", 5,
                this::animateSwingOverlay));
    }

    @Override
    protected boolean usesAnimatedFootsteps() {
        return true;
    }

    @Override
    public boolean isAttackOverlayVisible() {
        return swingOverlayVisible;
    }

    /** Client-side: a light step each time a foot lands, matching the visible foot. */
    private void playGuardFootfall() {
        if (!onGround() || isInWater() || isSilent()) {
            return;
        }
        float variation = getRandom().nextFloat() * 0.08F;
        net.minecraft.core.BlockPos ground = getOnPos();
        net.minecraft.world.level.block.SoundType surface =
                level().getBlockState(ground).getSoundType(level(), ground, this);
        level().playLocalSound(getX(), getY(), getZ(), surface.getStepSound(), SoundSource.HOSTILE,
                0.18F, 1.0F + variation, false);
    }

    /** Client-side: true while the stab (not the slam) is playing, for the held spear's pose. */
    public boolean isStabSwingVisible() {
        return swingOverlayVisible && visualSwingId == STAB_ID;
    }

    private int visualSwingId;
    private boolean stabSpearWasVisible;
    private float stabSpearPeak;
    private float stabSpearFadeStart = -1.0F;

    /**
     * Client-side: how much the held spear is turned down for the stab. Rises with the arm, is held at its
     * peak while the stab plays, then eases out over the same ticks the swing layer fades its pose.
     * Only the stab ever drives it; the slam, throw and walk leave it at zero.
     */
    public float stabSpearWeight(float lead, float now) {
        if (isStabSwingVisible()) {
            if (!stabSpearWasVisible) {
                stabSpearWasVisible = true;
                stabSpearPeak = 0.0F;
            }
            stabSpearFadeStart = -1.0F;
            stabSpearPeak = Math.max(stabSpearPeak, lead);
            return stabSpearPeak;
        }
        if (stabSpearWasVisible) {
            stabSpearWasVisible = false;
            stabSpearFadeStart = now;
        }
        if (stabSpearFadeStart < 0.0F) {
            return 0.0F;
        }
        float progress = (now - stabSpearFadeStart) / ATTACK_OVERLAY_FADE_OUT_TICKS;
        if (progress >= 1.0F) {
            stabSpearFadeStart = -1.0F;
            stabSpearPeak = 0.0F;
            return 0.0F;
        }
        float eased = progress * progress * (3.0F - 2.0F * progress);
        return stabSpearPeak * (1.0F - eased);
    }

    private PlayState animateSwingOverlay(AnimationState<OgreGuard> state) {
        state.setControllerSpeed(1.0F);
        if (isDeadOrDying()) {
            visualSwingOneShot.clear();
            swingOverlayVisible = false;
            visualSwingId = 0;
            return PlayState.STOP;
        }
        int swingId = entityData.get(DATA_GUARD_SWING_ID);
        long start = entityData.get(DATA_GUARD_PUNCH_START_TICK);
        visualSwingId = visualSwingOneShot.update(state.getController(), swingId,
                swingId > 0 ? start : 0L, id -> SWING_ANIMATION_TICKS, level().getGameTime());
        swingOverlayVisible = visualSwingId > 0;
        if (!swingOverlayVisible) {
            return PlayState.STOP;
        }
        return state.setAndContinue(visualSwingId == SLAM_ID ? SLAM
                : visualSwingId == BITE_ID ? BITE : STAB);
    }

    private PlayState animateMovement(AnimationState<OgreGuard> state) {
        if (isDeadOrDying()) {
            // A throw in progress is cancelled the moment he dies.
            visualOneShot.clear();
            return state.setAndContinue(IDLE);
        }
        long shotStart = entityData.get(DATA_GUARD_SHOT_START_TICK);
        int visualType = visualOneShot.update(state.getController(), shotStart > 0L ? 1 : 0, shotStart,
                id -> SHOOT_ANIMATION_TICKS, level().getGameTime());
        if (visualType > 0) {
            return state.setAndContinue(SHOOT);
        }

        // getTarget() is server-only and always null when this runs client-side for rendering —
        // DATA_GUARD_HAS_TARGET is the synced stand-in so the aggro'd branch actually triggers.
        if (entityData.get(DATA_GUARD_HAS_TARGET)) {
            idleAnimationTicks = 0;
            rareIdleTicks = 0;
            lastAnimationTick = tickCount;
            if (isMovingForAnimation(state) || isCombatMoving()) {
                return state.setAndContinue(entityData.get(DATA_GUARD_RETREATING) ? RUN : COMBAT_WALK);
            }
            return state.setAndContinue(IDLE);
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
        return state.setAndContinue(IDLE);
    }

    private void updateIdleAnimationTimers(AnimationState<OgreGuard> state) {
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

    private boolean isMovingForAnimation(AnimationState<OgreGuard> state) {
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

    private boolean hasActiveCombatTarget() {
        LivingEntity target = getTarget();
        return target != null && target.isAlive();
    }

    private void markCombatMovementForAnimation() {
        combatMovementAnimationTicks = 12;
        setCombatMoving(true);
    }

    private void markRetreatingForAnimation() {
        retreatAnimationTicks = 12;
        entityData.set(DATA_GUARD_RETREATING, true);
    }

    private void setCombatMoving(boolean combatMoving) {
        entityData.set(DATA_GUARD_COMBAT_MOVING, combatMoving);
    }

    private boolean isCombatMoving() {
        return entityData.get(DATA_GUARD_COMBAT_MOVING);
    }

    private static class OgreGuardShootGoal extends Goal {
        private final OgreGuard guard;
        private final double speedModifier;
        private int ticksUntilNextPathRecalculation;
        private int ticksUntilNextMeleeApproachPath;
        private int ticksUntilNextShot;
        private Vec3 lastPursuitTargetPos;
        private Vec3 retreatTargetPos;
        private int lowHealthRetreatTicks;
        private boolean lowHealthRetreating;
        private int engagementReactionTicks;
        private LivingEntity engagedTarget;

        private OgreGuardShootGoal(OgreGuard guard, double speedModifier) {
            this.guard = guard;
            this.speedModifier = speedModifier;
            this.ticksUntilNextShot = guard.getRandom().nextInt(MIN_SHOT_COOLDOWN_TICKS);
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = guard.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = guard.getTarget();
            return guard.isPreparingPunch()
                    || guard.isPreparingBowShot()
                    || target != null
                    && target.isAlive()
                    && (!(target instanceof Player player) || !player.isSpectator() && !player.isCreative())
                    && guard.isWithinRestriction(target.blockPosition());
        }

        @Override
        public void stop() {
            guard.getNavigation().stop();
            guard.setBowAiming(false);
            guard.cancelPunch();
            guard.cancelBowShot();
            ticksUntilNextPathRecalculation = 0;
            ticksUntilNextMeleeApproachPath = 0;
            lastPursuitTargetPos = null;
            retreatTargetPos = null;
            lowHealthRetreatTicks = 0;
            lowHealthRetreating = false;
            engagementReactionTicks = 0;
            engagedTarget = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = guard.getTarget();
            guard.tickPunchCooldown();

            if (target == null) {
                engagedTarget = null;
                guard.setBowAiming(false);
                guard.setCombatMoving(false);
                guard.tickPendingPunch();
                guard.tickPendingBowShot();
                return;
            }

            // Compared by identity: Goal#start() won't fire again if the goal never stopped
            // between engagements, and a newly acquired target should always get the pause.
            if (target != engagedTarget) {
                engagedTarget = target;
                engagementReactionTicks = adjustedTickDelay(ENGAGEMENT_REACTION_TICKS);
                ticksUntilNextShot = Math.max(ticksUntilNextShot, engagementReactionTicks);
            }

            guard.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distanceToTargetSqr = guard.distanceToSqr(target);
            boolean hasLineOfSight = guard.getSensing().hasLineOfSight(target);
            boolean withinMeleeRange = distanceToTargetSqr < MELEE_ENGAGE_RANGE * MELEE_ENGAGE_RANGE;

            if (guard.isPreparingPunch()) {
                guard.setBowAiming(false);
                guard.turnBodyToward(target);
                guard.tickPendingPunch();
                // He keeps walking in while he swings, and stops once he is close enough.
                ticksUntilNextMeleeApproachPath = Math.max(ticksUntilNextMeleeApproachPath - 1, 0);
                if (distanceToTargetSqr <= PUNCH_RANGE * PUNCH_RANGE) {
                    guard.getNavigation().stop();
                } else if (ticksUntilNextMeleeApproachPath <= 0) {
                    boolean pathing = guard.getNavigation().moveTo(target, speedModifier);
                    if (pathing) {
                        guard.markCombatMovementForAnimation();
                    }
                    ticksUntilNextMeleeApproachPath = adjustedTickDelay(pathing ? 4 : 20);
                }
                return;
            }

            if (guard.isPreparingBowShot()) {
                guard.visuallyTrackShotTarget(target);
                guard.getNavigation().stop();
                guard.tickPendingBowShot();
                return;
            }

            engagementReactionTicks = Math.max(engagementReactionTicks - 1, 0);

            // One tactical fallback per guard lifetime. Once firing range is restored (or the
            // pathfinding failsafe expires), normal melee/ranged combat resumes permanently.
            boolean isLowHealth = guard.getHealth() <= guard.getMaxHealth() * LOW_HEALTH_RETREAT_HEALTH_FRACTION;
            if (isLowHealth && !guard.lowHealthRetreatUsed) {
                guard.lowHealthRetreatUsed = true;
                lowHealthRetreating = true;
                lowHealthRetreatTicks = adjustedTickDelay(LOW_HEALTH_RETREAT_TICKS);
                retreatTargetPos = null;
            }

            if (lowHealthRetreating) {
                boolean reachedFiringRange = distanceToTargetSqr
                        >= LOW_HEALTH_RETREAT_STOP_RANGE * LOW_HEALTH_RETREAT_STOP_RANGE;
                if (reachedFiringRange || lowHealthRetreatTicks <= 0) {
                    lowHealthRetreating = false;
                    retreatTargetPos = null;
                    guard.getNavigation().stop();
                } else {
                    guard.setBowAiming(false);
                    lowHealthRetreatTicks--;
                    ticksUntilNextMeleeApproachPath = 0;
                    tickLowHealthRetreat(target);
                    ticksUntilNextShot = Math.max(ticksUntilNextShot, adjustedTickDelay(8));
                    return;
                }
            }

            boolean inShootRange = distanceToTargetSqr <= SHOOT_RANGE * SHOOT_RANGE;

            if (withinMeleeRange) {
                guard.setBowAiming(false);
                guard.turnBodyToward(target);
                retreatTargetPos = null;
                ticksUntilNextMeleeApproachPath = Math.max(ticksUntilNextMeleeApproachPath - 1, 0);
                if (engagementReactionTicks <= 0 && guard.canPunch(target)) {
                    guard.beginPunch(target);
                } else if (distanceToTargetSqr <= PUNCH_RANGE * PUNCH_RANGE) {
                    guard.getNavigation().stop();
                } else if (ticksUntilNextMeleeApproachPath <= 0) {
                    // Waiting on the timer alone, not on an empty path: a target he can't reach leaves
                    // the path empty every tick, which used to cost him a full A* search each time.
                    boolean pathing = guard.getNavigation().moveTo(target, speedModifier);
                    if (pathing) {
                        guard.markCombatMovementForAnimation();
                    }
                    ticksUntilNextMeleeApproachPath = adjustedTickDelay(pathing ? 4 : 20);
                }

                ticksUntilNextShot = Math.max(ticksUntilNextShot, adjustedTickDelay(12));
                return;
            }

            ticksUntilNextMeleeApproachPath = 0;
            retreatTargetPos = null;

            if (!hasLineOfSight || !inShootRange) {
                guard.setBowAiming(false);
                ticksUntilNextPathRecalculation = Math.max(ticksUntilNextPathRecalculation - 1, 0);
                // Only drop a valid path once the target has moved or the path ran out. Repathing
                // on every timer tick causes zigzagging and wastes an A* search on a target that
                // is standing still.
                Vec3 targetPos = target.position();
                boolean targetMoved = lastPursuitTargetPos == null
                        || lastPursuitTargetPos.distanceToSqr(targetPos) > 1.0;
                if (ticksUntilNextPathRecalculation <= 0
                        && (targetMoved || guard.getNavigation().isDone())) {
                    boolean pathing = guard.getNavigation().moveTo(target, speedModifier);
                    if (pathing) {
                        guard.markCombatMovementForAnimation();
                    }
                    lastPursuitTargetPos = targetPos;
                    ticksUntilNextPathRecalculation = adjustedTickDelay(10);
                }
            } else {
                guard.setBowAiming(true);
                guard.getNavigation().stop();
                lastPursuitTargetPos = null;
            }

            // Let vanilla navigation own body yaw while closing in; forcing it toward the target
            // fights the path steering (same fix as OgreArcherShootGoal). Take over facing only
            // once in shoot range.
            boolean facingTarget = hasLineOfSight && inShootRange && guard.turnBodyToward(target);
            ticksUntilNextShot = Math.max(ticksUntilNextShot - 1, 0);
            if (ticksUntilNextShot <= 0
                    && hasLineOfSight
                    && inShootRange
                    && facingTarget
                    && guard.hasClearShotToTarget(target)) {
                guard.beginBowShot(target);
                ticksUntilNextShot = adjustedTickDelay(MIN_SHOT_COOLDOWN_TICKS + guard.getRandom().nextInt(SHOT_COOLDOWN_RANDOM_TICKS));
            } else if (ticksUntilNextShot <= 0 && hasLineOfSight && inShootRange && facingTarget) {
                ticksUntilNextShot = adjustedTickDelay(8);
            }
        }

        private void tickLowHealthRetreat(LivingEntity target) {
            boolean needsNewRetreatTarget = retreatTargetPos == null
                    || guard.distanceToSqr(retreatTargetPos) < 2.0
                    || guard.getNavigation().isDone();

            if (needsNewRetreatTarget) {
                Vec3 retreatPos = DefaultRandomPos.getPosAway(
                        guard,
                        (int) Math.ceil(LOW_HEALTH_RETREAT_DISTANCE),
                        4,
                        target.position());
                if (retreatPos == null) {
                    Vec3 away = guard.position().subtract(target.position());
                    if (away.horizontalDistanceSqr() < 1.0E-4) {
                        away = Vec3.directionFromRotation(0.0F, guard.getYRot());
                    }

                    Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z).normalize();
                    retreatPos = guard.position().add(horizontalAway.scale(LOW_HEALTH_RETREAT_DISTANCE));
                }

                retreatTargetPos = retreatPos;
            }

            guard.getNavigation().moveTo(retreatTargetPos.x, retreatTargetPos.y, retreatTargetPos.z, LOW_HEALTH_RETREAT_SPEED);
            guard.markCombatMovementForAnimation();
            guard.markRetreatingForAnimation();
        }
    }
}
