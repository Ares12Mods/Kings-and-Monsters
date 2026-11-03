package com.kingsandmonsters.entity;

import com.kingsandmonsters.Config;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.effect.CombatEffects;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.network.GroundRipplePayload;
import com.kingsandmonsters.entity.animation.SynchronizedAnimationController;
import com.kingsandmonsters.entity.animation.CanonicalOneShotState;
import com.kingsandmonsters.network.ScreenShakePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class OgreGrunt extends Monster implements GeoEntity {
    public static final String ROYAL_PATROL_SQUAD_TAG = "KingsAndMonstersRoyalPatrolSquad";
    public static final String OUTPOST_REINFORCEMENT_SQUAD_TAG = "KingsAndMonstersOutpostReinforcementSquad";
    private static final String ROYAL_PATROL_GRUNTS_SPAWNED_TAG = "KingsAndMonstersRoyalPatrolGruntsSpawned";
    private static final String ROYAL_PATROL_ARCHERS_SPAWNED_TAG = "KingsAndMonstersRoyalPatrolArchersSpawned";
    private static final int ROYAL_PATROL_GRUNT_COUNT = 2;
    private static final int ROYAL_GUARD_PATROL_GRUNT_COUNT = 3;
    private static final int ROYAL_PATROL_ARCHER_COUNT = 1;
    private static final int OUTPOST_REINFORCEMENT_GRUNT_COUNT = 1;
    private static final int OUTPOST_REINFORCEMENT_ARCHER_COUNT = 2;
    private static final String CROWN_PROVOCATION_COUNT_TAG = "CrownProvocationCount";
    private static final String CROWN_PROVOCATION_PLAYER_TAG = "CrownProvocationPlayer";
    private static final String OUTPOST_HOME_TAG = "OutpostHome";
    private static final String OUTPOST_CAPTAIN_TAG = "OutpostCaptain";
    private static final String MERCHANT_OWNER_TAG = "MerchantOwner";
    private static final int OUTPOST_HOME_RADIUS = 32;
    private static final int OUTPOST_CAPTAIN_ALERT_RADIUS = 30;
    private static final int OUTPOST_RESIDENT_ALERT_RADIUS = 40;
    private static final int OUTPOST_MAX_CHASE_RADIUS = 64;
    private static final int OUTPOST_MAX_CHASE_TICKS = 30 * 20;
    private static final int OUTPOST_RETREAT_COUNTERATTACK_TICKS = 10 * 20;
    private static final double OUTPOST_RETURN_SPEED = 1.0;
    private static final int ROYAL_COMMAND_DURATION_TICKS = 10 * 20;
    private final Set<UUID> crownProvokedPlayers = new HashSet<>();
    private OgreGruntMeleeGoal meleeGoal;
    private static final ResourceLocation BITE_ARMOR_PIERCE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "ogre_grunt_bite_armor_pierce");
    private static final EntityDataAccessor<Integer> DATA_GRUNT_ATTACK_ID =
            SynchedEntityData.defineId(OgreGrunt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_GRUNT_ATTACK_START_TICK =
            SynchedEntityData.defineId(OgreGrunt.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_BOOSTED_MOVEMENT =
            SynchedEntityData.defineId(OgreGrunt.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_MOVEMENT_STATE =
            SynchedEntityData.defineId(OgreGrunt.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RARE_IDLE = RawAnimation.begin().then("idle2", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("upward smack");
    private static final RawAnimation ATTACK_2 = RawAnimation.begin().thenPlay("bite");
    // Diamond armor's reduction curve eats most flat damage increases, so these raw values are
    // calibrated against full diamond armor (20 armor / 8 toughness) rather than picked arbitrarily.
    private static final float UPPER_SMACK_DAMAGE = 12.0F;
    private static final float BITE_DAMAGE = 13.0F;
    private static final float BITE_ARMOR_PIERCE_FRACTION = 0.15F;
    private static final double GRUNT_ATTACK_START_RANGE = 2.5;
    private static final double PLAIN_GRUNT_ATTACK_START_RANGE = 2.25;
    private static final double GRUNT_ATTACK_HIT_RANGE = 3.25;
    private static final int ATTACK_IMPACT_SOUND_LEAD_TICKS = 2;
    private static final int GRUNT_ATTACK_SYNC_DELAY_TICKS = 2;
    private static final int UPPER_SMACK_IMPACT_TICKS = 8;
    private static final int BITE_IMPACT_TICKS = 8;
    private static final int UPPER_SMACK_ANIMATION_TICKS = 20;
    private static final int BITE_ANIMATION_TICKS = 15;
    private static final int BITE_ATTACK_COOLDOWN_TICKS = 49;
    private static final int UPPER_SMACK_ATTACK_COOLDOWN_TICKS = 37;
    private static final int IDLE_VARIANT_TICKS = 80;
    private static final int WALK_CYCLE_TICKS = 40;
    private static final double SUPERIOR_GUARD_SEARCH_RANGE = 24.0;
    private static final double SUPERIOR_GUARD_START_RANGE = 12.0;
    private static final double SUPERIOR_GUARD_STOP_RANGE = 9.0;
    private static final double SUPERIOR_GUARD_SPEED = 1.0;
    private static final double GRUNT_WALK_SPEED = 0.4;
    // Navigation goals take a multiplier, not a raw movement speed. Against the default
    // 0.2772 movement attribute this produces an effective pursuit speed of about 0.33 —
    // slightly below the Captain's 0.35, so the Captain reads as the faster pursuer.
    private static final double GRUNT_CHASE_SPEED = 0.33 / 0.2772;
    private static final int MOVEMENT_IDLE = 0;
    private static final int MOVEMENT_WALKING = 1;
    private static final int MOVEMENT_CHASING = 2;
    private static final int MOVEMENT_ATTACKING = 3;
    // Block-ripple visual (e.g. brute temper tantrum) outward propagation speed and per-block duration.
    private static final int RIPPLE_RING_DELAY_TICKS = 2;
    private static final int RIPPLE_BLOCK_VISIBLE_TICKS = 14;
    // Never speed the walk/run cycle up past its authored pace — only ever slow it down to match
    // actual (water-dragged) movement, and never let it stall out to a dead stop mid-stride.
    private static final float MIN_MOVEMENT_ANIM_SPEED = 0.55F;
    private static final float MAX_MOVEMENT_ANIM_SPEED = 1.0F;
    private static final double SHALLOW_WATER_SPEED_FACTOR = 0.5;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int idleAnimationTicks;
    private int idleVariantTicks;
    private int lastStepSoundTick = -1_000_000;
    private int lastAnimationTick = -1;
    private int movementAnimationGraceTicks;
    private int activeGruntAttackId;
    private final CanonicalOneShotState visualOneShot = new CanonicalOneShotState();
    private final List<ActiveGroundRipple> activeGroundRipples = new ArrayList<>();
    private boolean royalPatrolSquadLeader;
    private boolean outpostReinforcementSquadLeader;
    private int royalPatrolGruntsSpawned;
    private int royalPatrolArchersSpawned;
    @Nullable
    private BlockPos outpostHome;
    private boolean outpostCaptain;
    private boolean runningToSuperior;
    private boolean returningToOutpost;
    private int outpostChaseTicks;
    private int outpostRetreatCounterattackTicks;
    @Nullable
    private LivingEntity royalCommandTarget;
    private int royalCommandTicks;
    @Nullable
    private UUID merchantOwnerId;
    @Nullable
    private UUID merchantThreatId;
    private int merchantThreatTicks;

    public OgreGrunt(EntityType<? extends OgreGrunt> type, Level level) {
        super(type, level);
        if (usesReferenceGroundMovement()) {
            this.moveControl = new OgreGruntMoveControl(this);
        }
        setPathfindingMalus(PathType.LEAVES, 0.0F);
        // Pointed dripstone and similar damaging floor nodes can strand the Ogre's wide collision
        // box and cause repeated path recalculation/turning. Never select them as walk nodes.
        setPathfindingMalus(PathType.DAMAGE_OTHER, -1.0F);
        // Grunts keep vanilla's discouraged-but-crossable water behavior so they can fight in one
        // block of shallow water. Larger and ranged subclasses opt into fully blocked water nodes.
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return usesReferenceGroundMovement()
                ? new OgreGruntNavigation(this, level)
                : super.createNavigation(level);
    }

    private boolean usesReferenceGroundMovement() {
        Class<?> type = getClass();
        return type == OgreGrunt.class
                || type == OgreGruntCaptain.class
                || type == OgreBrute.class
                || type == OgreGuard.class
                || type == OgreArcher.class
                || type == OgreMage.class
                || type == OgreLord.class;
    }

    protected final void avoidWaterPathfinding() {
        // Negative water malus makes water nodes impassable to ground navigation. FloatGoal stays
        // available as an emergency escape if the ogre is physically knocked or spawned into water.
        setPathfindingMalus(PathType.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.ATTACK_DAMAGE, 10.5)
                .add(Attributes.MOVEMENT_SPEED, 0.2772)
                .add(Attributes.FOLLOW_RANGE, 30.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55)
                .add(Attributes.ATTACK_KNOCKBACK, 0.9)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.SAFE_FALL_DISTANCE, 4.0)
                // Default is 0, which caps LivingEntity#travel()'s water acceleration at a tiny fixed
                // baseline regardless of requested speed — with that, halving the requested pursuit
                // speed in shallow water (see applyShallowWaterSpeedFactor) wouldn't actually change
                // his real velocity at all, since he'd already be capped below it. This unlocks normal
                // acceleration so the 50% reduction is the thing actually felt.
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0);
    }

    public void applyConfiguredCombatAttributes(boolean healToMax) {
        if (!Config.isLoaded()) {
            return;
        }

        setAttributeBaseValue(Attributes.MAX_HEALTH, Config.OGRE_GRUNT_MAX_HEALTH.get());
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, Config.OGRE_GRUNT_ATTACK_DAMAGE.get());
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, Config.OGRE_GRUNT_MOVEMENT_SPEED.get());
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, Config.OGRE_GRUNT_FOLLOW_RANGE.get());
        setAttributeBaseValue(Attributes.ARMOR, Config.OGRE_GRUNT_ARMOR.get());
        setAttributeBaseValue(Attributes.ATTACK_KNOCKBACK, Config.OGRE_GRUNT_ATTACK_KNOCKBACK.get());
        setAttributeBaseValue(Attributes.STEP_HEIGHT, Config.OGRE_GRUNT_STEP_HEIGHT.get());
        setAttributeBaseValue(Attributes.SAFE_FALL_DISTANCE, Config.OGRE_GRUNT_SAFE_FALL_DISTANCE.get());

        if (healToMax) {
            setHealth(getMaxHealth());
        } else if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    protected void setAttributeBaseValue(net.minecraft.core.Holder<Attribute> attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    protected void applyConfiguredHealth(boolean healToMax) {
        if (healToMax) {
            setHealth(getMaxHealth());
        } else if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GRUNT_ATTACK_ID, 0);
        builder.define(DATA_GRUNT_ATTACK_START_TICK, 0L);
        builder.define(DATA_BOOSTED_MOVEMENT, false);
        builder.define(DATA_MOVEMENT_STATE, MOVEMENT_IDLE);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            if (merchantThreatTicks > 0 && --merchantThreatTicks == 0) {
                merchantThreatId = null;
                setTarget(null);
            }
            tickRoyalCommandTarget();
            tickOutpostPlayerDetection();
            tickOutpostCombatLeash();
            entityData.set(DATA_BOOSTED_MOVEMENT, hasRunSpeedEffect());
            updateIntendedMovementState();
            tickGroundRippleCombat();
            deployRoyalPatrolSquadIfNeeded();
        }
    }

    protected boolean hasRecentIntentionalJump(LivingEntity target, int graceTicks) {
        return RippleJumpTracker.hasRecentIntentionalJump(target, graceTicks);
    }

    private void updateIntendedMovementState() {
        int movementState;
        if (activeGruntAttackId != 0) {
            movementState = MOVEMENT_ATTACKING;
        } else if (getTarget() != null && isAggressive() || runningToSuperior || returningToOutpost) {
            movementState = MOVEMENT_CHASING;
        } else if (!getNavigation().isDone()) {
            movementState = MOVEMENT_WALKING;
        } else {
            movementState = MOVEMENT_IDLE;
        }
        entityData.set(DATA_MOVEMENT_STATE, movementState);
    }

    /** Synced movement intent used by subclasses whose render controller is separate from the Grunt's. */
    protected boolean isRunMovementState() {
        return entityData.get(DATA_MOVEMENT_STATE) == MOVEMENT_CHASING;
    }

    protected boolean usesRunningTurnSmoothing() {
        return isRunMovementState();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        meleeGoal = new OgreGruntMeleeGoal(this, getMeleePursuitSpeedModifier());
        goalSelector.addGoal(1, meleeGoal);
        goalSelector.addGoal(3, new FollowAssignedMerchantGoal(this));
        goalSelector.addGoal(4, new GuardSuperiorGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, getWanderSpeedModifier()));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        registerTerritorialTargetGoal();
    }

    protected final void registerTerritorialTargetGoal() {
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this, Monster.class, true, this::isTerritorialInvader));
    }

    private boolean isTerritorialInvader(LivingEntity target) {
        return !isMerchantGuard()
                && !(target instanceof OgreGrunt)
                && (target instanceof Zombie
                || target instanceof AbstractSkeleton
                || target instanceof Spider
                || target instanceof Witch
                || target instanceof Slime);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return (!isMerchantGuard() || target.getUUID().equals(merchantThreatId)
                    || target == getLastHurtByMob())
                && !(target instanceof OgreGrunt)
                && sovereigntyAllowsAttack(target)
                && super.canAttack(target);
    }

    private void tickOutpostCombatLeash() {
        if (outpostHome == null) {
            returningToOutpost = false;
            return;
        }

        long homeDistanceSqr = horizontalDistanceSqr(outpostHome, blockPosition());
        if (getTarget() != null && outpostRetreatCounterattackTicks > 0) {
            outpostRetreatCounterattackTicks--;
            returningToOutpost = false;
            return;
        }
        if (getTarget() == null) {
            outpostRetreatCounterattackTicks = 0;
        }
        // Preserve the original combat AI inside a generous pursuit envelope, then force a
        // clean disengage so a resident cannot be dragged indefinitely across the world.
        if (getTarget() != null
                && ++outpostChaseTicks <= OUTPOST_MAX_CHASE_TICKS
                && homeDistanceSqr <= (long) OUTPOST_MAX_CHASE_RADIUS * OUTPOST_MAX_CHASE_RADIUS) {
            returningToOutpost = false;
            return;
        }

        if (getTarget() != null) {
            setTarget(null);
            setAggressive(false);
            getNavigation().stop();
            returningToOutpost = true;
            outpostChaseTicks = 0;
            navigateBackToOutpost();
            return;
        }
        outpostChaseTicks = 0;

        int radius = OUTPOST_HOME_RADIUS;
        boolean outsideBoundary = homeDistanceSqr > (long) radius * radius;

        if (returningToOutpost) {
            int arrivalRadius = radius - 4;
            if (homeDistanceSqr <= (long) arrivalRadius * arrivalRadius) {
                returningToOutpost = false;
                getNavigation().stop();
                return;
            }
            setTarget(null);
            setAggressive(false);
            if (tickCount % 10 == 0) {
                navigateBackToOutpost();
            }
            return;
        }

        if (!outsideBoundary) {
            return;
        }

        setAggressive(false);
        getNavigation().stop();
        returningToOutpost = true;
        navigateBackToOutpost();
    }

    private void navigateBackToOutpost() {
        getNavigation().moveTo(
                outpostHome.getX() + 0.5,
                outpostHome.getY(),
                outpostHome.getZ() + 0.5,
                OUTPOST_RETURN_SPEED);
    }

    private void tickOutpostPlayerDetection() {
        if (outpostHome == null || returningToOutpost || getTarget() != null || tickCount % 10 != 0
                || horizontalDistanceSqr(outpostHome, blockPosition())
                > (long) OUTPOST_HOME_RADIUS * OUTPOST_HOME_RADIUS) {
            return;
        }

        int alertRadius = outpostCaptain
                ? OUTPOST_CAPTAIN_ALERT_RADIUS
                : OUTPOST_RESIDENT_ALERT_RADIUS;
        Player player = level().getNearestPlayer(this, alertRadius);
        if (player == null || player.isCreative() || player.isSpectator()
                || !canAttack(player) || !hasLineOfSight(player)) {
            return;
        }
        setTarget(player);
    }

    private static long horizontalDistanceSqr(BlockPos first, BlockPos second) {
        long dx = first.getX() - second.getX();
        long dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    @Override
    public boolean canDisableShield() {
        // The bite puts the target's shield on cooldown, same as an axe would.
        return activeGruntAttackId == 2 || super.canDisableShield();
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target == null || target instanceof OgreGrunt || !canAttack(target) ? null : target);
    }

    /**
     * Makes this Ogre obey the King's explicit pointing command. The King has already selected a
     * valid enemy, so this intentionally bypasses lesser-Ogre crown hesitation while retaining the
     * basic rule that Ogres cannot be ordered to attack one another.
     */
    public void setRoyalCommandTarget(@Nullable LivingEntity target) {
        if (target instanceof OgreGrunt || target == null || !target.isAlive()) {
            royalCommandTarget = null;
            royalCommandTicks = 0;
            return;
        }
        royalCommandTarget = target;
        royalCommandTicks = ROYAL_COMMAND_DURATION_TICKS;
        super.setTarget(target);
    }

    private void tickRoyalCommandTarget() {
        if (royalCommandTicks <= 0 || royalCommandTarget == null || !royalCommandTarget.isAlive()) {
            royalCommandTarget = null;
            royalCommandTicks = 0;
            return;
        }
        royalCommandTicks--;
        if (getTarget() != royalCommandTarget) {
            super.setTarget(royalCommandTarget);
        }
    }

    /**
     * The Ogre King's Crown is recognized by lesser ogres until its wearer personally
     * attacks them. Grunt Captains, Mages, and Brutes are leaders, and a stable quarter reject
     * a given wearer's claim; the King never submits.
     */
    private boolean sovereigntyAllowsAttack(@Nullable LivingEntity target) {
        if (!(target instanceof Player player)
                || !player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OGRE_KINGS_CROWN.get())
                || this instanceof OgreLord) {
            return true;
        }
        if (crownProvokedPlayers.contains(player.getUUID())) {
            return true;
        }
        if (this instanceof OgreGruntCaptain || this instanceof OgreMage || this instanceof OgreBrute) {
            int challengeRoll = getUUID().hashCode() ^ player.getUUID().hashCode();
            return Math.floorMod(challengeRoll, 4) == 0;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isMerchantGuard() && source.getEntity() instanceof LivingEntity attacker
                && !(attacker instanceof OgreGrunt)) {
            defendMerchantFrom(attacker);
        }
        if (hurt && returningToOutpost && outpostRetreatCounterattackTicks <= 0
                && source.getEntity() instanceof LivingEntity attacker
                && !(attacker instanceof OgreGrunt) && canAttack(attacker)) {
            returningToOutpost = false;
            outpostRetreatCounterattackTicks = OUTPOST_RETREAT_COUNTERATTACK_TICKS;
            getNavigation().stop();
            setTarget(attacker);
        }
        if (hurt && source.getEntity() instanceof Player player
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OGRE_KINGS_CROWN.get())) {
            crownProvokedPlayers.add(player.getUUID());
        }
        return hurt;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(CROWN_PROVOCATION_COUNT_TAG, crownProvokedPlayers.size());
        int index = 0;
        for (UUID playerId : crownProvokedPlayers) {
            tag.putUUID(CROWN_PROVOCATION_PLAYER_TAG + index++, playerId);
        }
        tag.putBoolean(ROYAL_PATROL_SQUAD_TAG, royalPatrolSquadLeader);
        tag.putBoolean(OUTPOST_REINFORCEMENT_SQUAD_TAG, outpostReinforcementSquadLeader);
        tag.putInt(ROYAL_PATROL_GRUNTS_SPAWNED_TAG, royalPatrolGruntsSpawned);
        tag.putInt(ROYAL_PATROL_ARCHERS_SPAWNED_TAG, royalPatrolArchersSpawned);
        if (outpostHome != null) {
            tag.putLong(OUTPOST_HOME_TAG, outpostHome.asLong());
            tag.putBoolean(OUTPOST_CAPTAIN_TAG, outpostCaptain);
        }
        if (merchantOwnerId != null) tag.putUUID(MERCHANT_OWNER_TAG, merchantOwnerId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        crownProvokedPlayers.clear();
        int count = tag.getInt(CROWN_PROVOCATION_COUNT_TAG);
        for (int index = 0; index < count; index++) {
            String key = CROWN_PROVOCATION_PLAYER_TAG + index;
            if (tag.hasUUID(key)) {
                crownProvokedPlayers.add(tag.getUUID(key));
            }
        }
        royalPatrolSquadLeader = tag.getBoolean(ROYAL_PATROL_SQUAD_TAG);
        outpostReinforcementSquadLeader = tag.getBoolean(OUTPOST_REINFORCEMENT_SQUAD_TAG);
        royalPatrolGruntsSpawned = Mth.clamp(tag.getInt(ROYAL_PATROL_GRUNTS_SPAWNED_TAG),
                0, getRoyalPatrolGruntCount());
        royalPatrolArchersSpawned = Mth.clamp(tag.getInt(ROYAL_PATROL_ARCHERS_SPAWNED_TAG),
                0, getRoyalPatrolArcherCount());
        if (tag.contains(OUTPOST_HOME_TAG)) {
            assignOutpostResident(BlockPos.of(tag.getLong(OUTPOST_HOME_TAG)),
                    tag.getBoolean(OUTPOST_CAPTAIN_TAG));
        }
        merchantOwnerId = tag.hasUUID(MERCHANT_OWNER_TAG) ? tag.getUUID(MERCHANT_OWNER_TAG) : null;
    }

    public void assignOutpostResident(BlockPos campOrigin, boolean captain) {
        outpostHome = campOrigin.immutable();
        outpostCaptain = captain;
        // Returning is managed explicitly after combat. A vanilla restriction can interfere
        // with melee pursuit before the target is actually lost, so residents do not use one.
        clearRestriction();
        setPersistenceRequired();
    }

    public boolean isOutpostResident() {
        return outpostHome != null;
    }

    public void assignMerchantGuard(UUID merchantId) {
        merchantOwnerId = merchantId;
        setCustomName(net.minecraft.network.chat.Component.literal("Merchant Guard"));
        setCustomNameVisible(false);
        setPersistenceRequired();
        setTarget(null);
    }

    public boolean isMerchantGuard() { return merchantOwnerId != null; }

    public boolean isMerchantGuardFor(UUID merchantId) {
        return merchantOwnerId != null && merchantOwnerId.equals(merchantId);
    }

    public void defendMerchantFrom(LivingEntity attacker) {
        if (!isMerchantGuard() || attacker instanceof OgreGrunt || !attacker.isAlive()) return;
        if (attacker instanceof Player player) {
            crownProvokedPlayers.add(player.getUUID());
        }
        merchantThreatId = attacker.getUUID();
        merchantThreatTicks = 20 * 30;
        setTarget(attacker);
    }

    public boolean isOutpostResidentAt(BlockPos campOrigin) {
        return outpostHome != null && outpostHome.equals(campOrigin);
    }

    public boolean isOutpostCaptain() {
        return outpostCaptain;
    }

    @Nullable
    public BlockPos getOutpostHome() {
        return outpostHome;
    }

    private void deployRoyalPatrolSquadIfNeeded() {
        int gruntCount = getRoyalPatrolGruntCount();
        int archerCount = getRoyalPatrolArcherCount();
        if (!royalPatrolSquadLeader && !outpostReinforcementSquadLeader
                || royalPatrolGruntsSpawned >= gruntCount
                && royalPatrolArchersSpawned >= archerCount
                || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        TrialSpawnerBlockEntity owner = findOwningRoyalPatrolSpawner(serverLevel);
        if (owner == null) {
            return;
        }

        while (royalPatrolGruntsSpawned < gruntCount) {
            if (!spawnTrackedRoyalPatrolMember(serverLevel, owner, ModEntities.OGRE_GRUNT.get())) {
                break;
            }
            royalPatrolGruntsSpawned++;
        }
        while (royalPatrolArchersSpawned < archerCount) {
            if (!spawnTrackedRoyalPatrolMember(serverLevel, owner, ModEntities.OGRE_ARCHER.get())) {
                break;
            }
            royalPatrolArchersSpawned++;
        }
    }

    public boolean isRoyalPatrolSquadLeader() {
        return royalPatrolSquadLeader;
    }

    private int getRoyalPatrolGruntCount() {
        if (outpostReinforcementSquadLeader) {
            return OUTPOST_REINFORCEMENT_GRUNT_COUNT;
        }
        return this instanceof OgreGuard || this instanceof OgreGruntCaptain
                ? ROYAL_GUARD_PATROL_GRUNT_COUNT
                : ROYAL_PATROL_GRUNT_COUNT;
    }

    private int getRoyalPatrolArcherCount() {
        return outpostReinforcementSquadLeader
                ? OUTPOST_REINFORCEMENT_ARCHER_COUNT
                : ROYAL_PATROL_ARCHER_COUNT;
    }

    private TrialSpawnerBlockEntity findOwningRoyalPatrolSpawner(ServerLevel serverLevel) {
        BlockPos center = blockPosition();
        for (BlockPos candidate : BlockPos.betweenClosed(
                center.offset(-6, -3, -6), center.offset(6, 3, 6))) {
            if (serverLevel.getBlockEntity(candidate) instanceof TrialSpawnerBlockEntity spawner
                    && isTrackedByTrialSpawner(serverLevel, spawner, getUUID())) {
                return spawner;
            }
        }
        return null;
    }

    private boolean spawnTrackedRoyalPatrolMember(ServerLevel serverLevel,
                                                   TrialSpawnerBlockEntity owner,
                                                   EntityType<? extends OgreGrunt> entityType) {
        OgreGrunt member = entityType.create(serverLevel);
        if (member == null) {
            return false;
        }

        BlockPos spawnerPos = owner.getBlockPos();
        for (int attempt = 0; attempt < 20; attempt++) {
            double x = spawnerPos.getX() + 0.5 + (getRandom().nextDouble() - getRandom().nextDouble()) * 4.5;
            double y = spawnerPos.getY() + getRandom().nextInt(3) - 1;
            double z = spawnerPos.getZ() + 0.5 + (getRandom().nextDouble() - getRandom().nextDouble()) * 4.5;
            member.moveTo(x, y, z, getRandom().nextFloat() * 360.0F, 0.0F);
            if (!serverLevel.noCollision(member) || !member.checkSpawnObstruction(serverLevel)) {
                continue;
            }

            EventHooks.finalizeMobSpawnSpawner(member, serverLevel,
                    serverLevel.getCurrentDifficultyAt(member.blockPosition()),
                    MobSpawnType.TRIAL_SPAWNER, null, owner.getTrialSpawner(), true);
            member.setPersistenceRequired();
            if (!serverLevel.addFreshEntity(member)) {
                return false;
            }
            addTrackedTrialMob(serverLevel, owner, member.getUUID());
            owner.getTrialSpawner().markUpdated();
            return true;
        }
        return false;
    }

    private static boolean isTrackedByTrialSpawner(ServerLevel serverLevel,
                                                    TrialSpawnerBlockEntity spawner,
                                                    UUID entityId) {
        CompoundTag savedData = spawner.saveWithoutMetadata(serverLevel.registryAccess());
        ListTag trackedMobs = savedData.getList("current_mobs", Tag.TAG_INT_ARRAY);
        for (Tag trackedMob : trackedMobs) {
            if (NbtUtils.loadUUID(trackedMob).equals(entityId)) {
                return true;
            }
        }
        return false;
    }

    private static void addTrackedTrialMob(ServerLevel serverLevel,
                                           TrialSpawnerBlockEntity spawner,
                                           UUID entityId) {
        CompoundTag savedData = spawner.saveWithoutMetadata(serverLevel.registryAccess());
        ListTag trackedMobs = savedData.getList("current_mobs", Tag.TAG_INT_ARRAY);
        trackedMobs.add(NbtUtils.createUUID(entityId));
        savedData.put("current_mobs", trackedMobs);
        spawner.loadCustomOnly(savedData, serverLevel.registryAccess());
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        // Freshly loading (world launch, or a chunk reloading, e.g. after the player respawns
        // nearby) should never leave a mob visually stuck mid-attack-animation and still aggro'd
        // on whatever it was doing before the save — force a clean idle, unaggro'd state and let
        // the goal selector re-acquire a target normally from here.
        resetTransientCombatState();
    }

    /**
     * Clears any in-progress attack/animation state left over from before this entity was
     * (re)loaded. Subclasses with their own pending-attack timers should override and call super.
     */
    protected void resetTransientCombatState() {
        setTarget(null);
        setAggressive(false);
        activeGruntAttackId = 0;
        clearSyncedAttackAnimation();
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof OgreGrunt || super.isAlliedTo(entity);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return super.doHurtTarget(target);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.OGRE_GRUNT_HURT.get();
    }

    protected float getStepPitchMultiplier() {
        return 1.0F;
    }

    protected float getStepVolumeMultiplier() {
        return 1.0F;
    }

    protected int getMinimumStepSoundIntervalTicks() {
        // Their long stride should produce deliberate, separated impacts even while pursuing.
        return 7;
    }

    private boolean consumeStepSoundInterval() {
        int minimumInterval = getMinimumStepSoundIntervalTicks();
        if (minimumInterval > 0 && tickCount - lastStepSoundTick < minimumInterval) {
            return false;
        }
        lastStepSoundTick = tickCount;
        return true;
    }

    private void playSurfaceStepSound(BlockState state, BlockPos pos, float volumeScale, float pitchScale) {
        var soundType = state.getSoundType(level(), pos, this);
        playSound(soundType.getStepSound(),
                soundType.getVolume() * volumeScale * getStepVolumeMultiplier(),
                soundType.getPitch() * pitchScale * getStepPitchMultiplier());
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (consumeStepSoundInterval()) {
            playSurfaceStepSound(state, pos, 0.15F, 1.0F);
        }
    }

    @Override
    protected void playCombinationStepSounds(BlockState primaryState, BlockState secondaryState,
                                             BlockPos primaryPos, BlockPos secondaryPos) {
        if (!consumeStepSoundInterval()) {
            return;
        }
        playSurfaceStepSound(primaryState, primaryPos, 0.15F, 1.0F);
        playSurfaceStepSound(secondaryState, secondaryPos, 0.05F, 0.8F);
    }

    @Override
    protected void playMuffledStepSound(BlockState state, BlockPos pos) {
        if (consumeStepSoundInterval()) {
            playSurfaceStepSound(state, pos, 0.05F, 0.8F);
        }
    }

    @Override
    public void animateHurt(float yaw) {
        // GeckoLib drives the ogre body animations; the vanilla hurt pose can snap one-shot attacks back to frame 0.
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Used to be 1 tick (near-instant) because state.isMoving() lagged navigation, which read as
        // sliding out of idle — isMovingForAnimation() now catches movement the same tick it starts,
        // so this can afford a short crossfade without reintroducing that. Mainly here to soften the
        // run<->swim snap on entering/exiting water; this is a single shared value for every
        // transition on this controller, so keep it short enough that idle<->walk still looks snappy.
        controllers.add(new SynchronizedAnimationController<>(this, "Grunt Movement", 4, this::animateGrunt));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    protected MeleeAttackPlan startMeleeAttack() {
        if (!isRegularGruntRole()) {
            return defaultMeleeAttackPlan();
        }

        float baseAttack = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        LivingEntity target = getTarget();
        if (usesOnlyBiteAttack() || (canStartBiteAttack(target) && getRandom().nextInt(4) == 0)) {
            return startGruntAttack(2, BITE_ATTACK_COOLDOWN_TICKS, new MeleeAttackHit(BITE_IMPACT_TICKS + GRUNT_ATTACK_SYNC_DELAY_TICKS, BITE_DAMAGE / baseAttack, 0.0F, false,
                    BITE_ARMOR_PIERCE_FRACTION, 0.0, 0.0F, 0.0, 0, 0, false,
                    0.0F, false, 0.0, 0.0, 2.5, 0.0, 0.0));
        }

        return startGruntAttack(1, UPPER_SMACK_ATTACK_COOLDOWN_TICKS, new MeleeAttackHit(UPPER_SMACK_IMPACT_TICKS + GRUNT_ATTACK_SYNC_DELAY_TICKS, UPPER_SMACK_DAMAGE / baseAttack, 0.0F, false,
                0.0F, 0.0, 0.0F, 0.0, 0, 0, false, 0.0F, false, 0.0, 0.0, 2.5, 0.0, 0.0));
    }

    /** Allows specialized Grunts to retain the bite while replacing the standard upswing. */
    protected boolean usesOnlyBiteAttack() {
        return false;
    }

    protected MeleeAttackPlan defaultMeleeAttackPlan() {
        return new MeleeAttackPlan(24, List.of(MeleeAttackHit.fullDamage(10)));
    }

    protected boolean doScaledHurtTarget(LivingEntity target, float damageMultiplier) {
        return doScaledHurtTarget(target, damageMultiplier, 0.0F);
    }

    protected boolean doScaledHurtTarget(LivingEntity target, float damageMultiplier, float armorPierceFraction) {
        if (damageMultiplier == 1.0F) {
            return hurtTargetWithTemporaryArmorPierce(target, getAttributeValue(Attributes.ATTACK_DAMAGE), armorPierceFraction);
        }

        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * damageMultiplier;
        return hurtTargetWithTemporaryArmorPierce(target, damage, armorPierceFraction);
    }

    private boolean hurtTargetWithTemporaryArmorPierce(LivingEntity target, double damage, float armorPierceFraction) {
        if (armorPierceFraction <= 0.0F) {
            return target.hurt(damageSources().mobAttack(this), (float) damage);
        }

        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null || armor.getValue() <= 0.0) {
            return target.hurt(damageSources().mobAttack(this), (float) damage);
        }

        armor.removeModifier(BITE_ARMOR_PIERCE_MODIFIER_ID);
        armor.addTransientModifier(new AttributeModifier(
                BITE_ARMOR_PIERCE_MODIFIER_ID,
                -armorPierceFraction,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        try {
            return target.hurt(damageSources().mobAttack(this), (float) damage);
        } finally {
            armor.removeModifier(BITE_ARMOR_PIERCE_MODIFIER_ID);
        }
    }

    private boolean canStartBiteAttack(LivingEntity target) {
        return target != null
                && target.isAlive()
                && distanceToSqr(target) <= getMeleeAttackReachSqr(target)
                && getSensing().hasLineOfSight(target);
    }

    protected MeleeAttackPlan startGruntAttack(int attackId, int cooldownTicks, MeleeAttackHit... hits) {
        entityData.set(DATA_GRUNT_ATTACK_ID, attackId);
        entityData.set(DATA_GRUNT_ATTACK_START_TICK, level().getGameTime());
        activeGruntAttackId = attackId;
        return new MeleeAttackPlan(cooldownTicks, List.of(hits));
    }

    private PlayState animateGrunt(AnimationState<OgreGrunt> state) {
        int visualAttackId = visualOneShot.update(state.getController(),
                entityData.get(DATA_GRUNT_ATTACK_ID), entityData.get(DATA_GRUNT_ATTACK_START_TICK),
                this::getGruntAttackAnimationTicks, level().getGameTime());
        if (visualAttackId > 0) {
            state.setControllerSpeed(1.0F);
            return state.setAndContinue(getGruntAttackAnimation(visualAttackId));
        }

        int movementState = entityData.get(DATA_MOVEMENT_STATE);
        boolean moving = isMovingForGruntAnimation(state);
        if (movementState == MOVEMENT_CHASING) {
            idleAnimationTicks = 0;
            idleVariantTicks = 0;
            if (moving) {
                state.setControllerSpeed(isInWater() ? computeMovementAnimSpeed() : 1.0F);
                return state.setAndContinue(RUN);
            }
            state.setControllerSpeed(1.0F);
            return state.setAndContinue(IDLE);
        }

        if (movementState == MOVEMENT_WALKING && moving) {
            idleAnimationTicks = 0;
            idleVariantTicks = 0;
            state.setControllerSpeed(isInWater() ? computeMovementAnimSpeed() : 1.0F);
            return state.setAndContinue(WALK);
        }

        state.setControllerSpeed(1.0F);
        updateIdleAnimationTimers(state);
        if (idleVariantTicks > 0 || state.isCurrentAnimation(RARE_IDLE) && !state.getController().hasAnimationFinished()) {
            return state.setAndContinue(RARE_IDLE);
        }
        return state.setAndContinue(IDLE);
    }

    protected RawAnimation getGruntAttackAnimation(int attackId) {
        return attackId == 2 ? ATTACK_2 : ATTACK;
    }

    protected int getGruntAttackAnimationTicks(int attackId) {
        return attackId == 2 ? BITE_ANIMATION_TICKS : UPPER_SMACK_ANIMATION_TICKS;
    }

    protected boolean isMovingForGruntAnimation(AnimationState<OgreGrunt> state) {
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

    private void updateIdleAnimationTimers(AnimationState<OgreGrunt> state) {
        int tickDelta = updateLastAnimationTick();
        if (tickDelta == 0) {
            return;
        }

        if (idleVariantTicks > 0) {
            idleVariantTicks = Math.max(0, idleVariantTicks - tickDelta);
            return;
        }

        idleAnimationTicks += tickDelta;
        if (idleAnimationTicks >= 160 && getRandom().nextInt(240) == 0) {
            idleAnimationTicks = 0;
            idleVariantTicks = IDLE_VARIANT_TICKS;
            state.getController().forceAnimationReset();
        }
    }

    private int updateLastAnimationTick() {
        if (lastAnimationTick == tickCount) {
            return 0;
        }

        int tickDelta = lastAnimationTick < 0 ? 1 : Math.max(1, tickCount - lastAnimationTick);
        lastAnimationTick = tickCount;
        return tickDelta;
    }

    protected record MeleeAttackPlan(int cooldownTicks, int activeDurationTicks, List<MeleeAttackHit> hits) {
        protected MeleeAttackPlan(int cooldownTicks, List<MeleeAttackHit> hits) {
            this(cooldownTicks, lastHitDelay(hits), hits);
        }

        private static int lastHitDelay(List<MeleeAttackHit> hits) {
            return hits.stream()
                    .mapToInt(MeleeAttackHit::delayTicks)
                    .max()
                    .orElse(0);
        }
    }

    protected record MeleeAttackHit(int delayTicks, float damageMultiplier, float areaRadius,
                                    boolean hitsOgreAllies, float armorPierceFraction,
                                    double knockbackStrength, float coneDegrees,
                                    double verticalKnockbackStrength,
                                    int slownessTicks, int nauseaTicks, boolean shockwaveParticles,
                                    float screenShakeIntensity, boolean blockRippleParticles,
                                    double forwardOffset, double rightOffset,
                                    double verticalHitRange,
                                    double knockbackForward, double knockbackRight) {
        private static MeleeAttackHit fullDamage(int delayTicks) {
            return new MeleeAttackHit(delayTicks, 1.0F, 0.0F, false, 0.0F, 0.0, 0.0F, 0.0,
                    0, 0, false, 0.0F, false, 0.0, 0.0, 2.5, 0.0, 0.0);
        }

        protected static MeleeAttackHit areaDamage(int delayTicks, float damageMultiplier, float areaRadius, boolean hitsOgreAllies) {
            return new MeleeAttackHit(delayTicks, damageMultiplier, areaRadius, hitsOgreAllies, 0.0F, 0.0, 0.0F, 0.0,
                    0, 0, false, 0.0F, false, 0.0, 0.0, 2.5, 0.0, 0.0);
        }

        protected static MeleeAttackHit areaSpecial(int delayTicks, float damageMultiplier, float areaRadius,
                                                    boolean hitsOgreAllies, double knockbackStrength,
                                                    float coneDegrees, int slownessTicks, int nauseaTicks) {
            return new MeleeAttackHit(delayTicks, damageMultiplier, areaRadius, hitsOgreAllies, 0.0F,
                    knockbackStrength, coneDegrees, 0.0, slownessTicks, nauseaTicks, false, 0.0F, false,
                    0.0, 0.0, 2.5, 0.0, 0.0);
        }

        protected static MeleeAttackHit areaSpecial(int delayTicks, float damageMultiplier, float areaRadius,
                                                    boolean hitsOgreAllies, float armorPierceFraction,
                                                    double knockbackStrength, float coneDegrees,
                                                    int slownessTicks, int nauseaTicks, boolean shockwaveParticles) {
            return new MeleeAttackHit(delayTicks, damageMultiplier, areaRadius, hitsOgreAllies, armorPierceFraction,
                    knockbackStrength, coneDegrees, 0.0, slownessTicks, nauseaTicks, shockwaveParticles,
                    shockwaveParticles ? 1.0F : 0.0F, false, 0.0, 0.0, 2.5, 0.0, 0.0);
        }

        protected static MeleeAttackHit areaSpecial(int delayTicks, float damageMultiplier, float areaRadius,
                                                    boolean hitsOgreAllies, float armorPierceFraction,
                                                    double knockbackStrength, float coneDegrees,
                                                    double verticalKnockbackStrength,
                                                    int slownessTicks, int nauseaTicks, boolean shockwaveParticles) {
            return new MeleeAttackHit(delayTicks, damageMultiplier, areaRadius, hitsOgreAllies, armorPierceFraction,
                    knockbackStrength, coneDegrees, verticalKnockbackStrength, slownessTicks, nauseaTicks, shockwaveParticles,
                    shockwaveParticles ? 1.0F : 0.0F, false, 0.0, 0.0, 2.5, 0.0, 0.0);
        }

        protected MeleeAttackHit withScreenShake(float screenShakeIntensity) {
            return new MeleeAttackHit(delayTicks, damageMultiplier, areaRadius, hitsOgreAllies, armorPierceFraction,
                    knockbackStrength, coneDegrees, verticalKnockbackStrength, slownessTicks, nauseaTicks,
                    shockwaveParticles, screenShakeIntensity, blockRippleParticles, forwardOffset, rightOffset, verticalHitRange,
                    knockbackForward, knockbackRight);
        }

        protected MeleeAttackHit withBlockRippleParticles() {
            return new MeleeAttackHit(delayTicks, damageMultiplier, areaRadius, hitsOgreAllies, armorPierceFraction,
                    knockbackStrength, coneDegrees, verticalKnockbackStrength, slownessTicks, nauseaTicks,
                    shockwaveParticles, screenShakeIntensity, true, forwardOffset, rightOffset, verticalHitRange,
                    knockbackForward, knockbackRight);
        }

        protected MeleeAttackHit withAreaOffset(double forwardOffset, double rightOffset) {
            return new MeleeAttackHit(delayTicks, damageMultiplier, areaRadius, hitsOgreAllies, armorPierceFraction,
                    knockbackStrength, coneDegrees, verticalKnockbackStrength, slownessTicks, nauseaTicks,
                    shockwaveParticles, screenShakeIntensity, blockRippleParticles, forwardOffset, rightOffset, verticalHitRange,
                    knockbackForward, knockbackRight);
        }

        protected MeleeAttackHit withVerticalHitRange(double verticalHitRange) {
            return new MeleeAttackHit(delayTicks, damageMultiplier, areaRadius, hitsOgreAllies, armorPierceFraction,
                    knockbackStrength, coneDegrees, verticalKnockbackStrength, slownessTicks, nauseaTicks,
                    shockwaveParticles, screenShakeIntensity, blockRippleParticles, forwardOffset, rightOffset, verticalHitRange,
                    knockbackForward, knockbackRight);
        }

        protected MeleeAttackHit withDirectionalKnockback(double knockbackForward, double knockbackRight) {
            return new MeleeAttackHit(delayTicks, damageMultiplier, areaRadius, hitsOgreAllies, armorPierceFraction,
                    knockbackStrength, coneDegrees, verticalKnockbackStrength, slownessTicks, nauseaTicks,
                    shockwaveParticles, screenShakeIntensity, blockRippleParticles, forwardOffset, rightOffset, verticalHitRange,
                    knockbackForward, knockbackRight);
        }

        protected MeleeAttackHit withoutHorizontalKnockback() {
            return new MeleeAttackHit(delayTicks, damageMultiplier, areaRadius, hitsOgreAllies, armorPierceFraction,
                    0.0, coneDegrees, verticalKnockbackStrength, slownessTicks, nauseaTicks,
                    shockwaveParticles, screenShakeIntensity, blockRippleParticles, forwardOffset, rightOffset, verticalHitRange,
                    0.0, 0.0);
        }
    }

    protected boolean isBoostedMovementForAnimation() {
        return entityData.get(DATA_BOOSTED_MOVEMENT) || hasRunSpeedEffect();
    }

    private boolean hasRunSpeedEffect() {
        return hasEffect(MobEffects.MOVEMENT_SPEED);
    }

    /**
     * Scales the walk/run animation's playback speed to match how fast the mob is actually moving
     * (e.g. water drag) instead of always playing at the pace the clip was authored for.
     */
    protected float computeMovementAnimSpeed() {
        double referenceSpeed = getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (referenceSpeed <= 1.0E-4) {
            return 1.0F;
        }

        float ratio = (float) (getDeltaMovement().horizontalDistance() / referenceSpeed);
        return Mth.clamp(ratio, MIN_MOVEMENT_ANIM_SPEED, MAX_MOVEMENT_ANIM_SPEED);
    }

    /**
     * Halves the requested pursuit speed while wading through (shallow) water — shared by every
     * subclass's pursuit/combat goal so the reduction is the same across the roster rather than a
     * per-mob special case.
     */
    protected double applyShallowWaterSpeedFactor(double baseSpeedModifier) {
        return isInWater() ? baseSpeedModifier * SHALLOW_WATER_SPEED_FACTOR : baseSpeedModifier;
    }

    protected boolean canGuardSuperiors() {
        // Authored fort residents already have a broad home restriction covering the compound.
        // Following the sleeping King overrides their stroll behavior and pulls them back to his hut.
        if (getTags().stream().anyMatch(tag -> tag.startsWith("KingsAndMonstersFortResident_"))) {
            return false;
        }
        return !isMerchantGuard() && (isRegularGruntRole()
                || this instanceof OgreArcher
                || this instanceof OgreGuard
                || this instanceof OgreMage
                || this instanceof OgreBrute);
    }

    private static class FollowAssignedMerchantGoal extends Goal {
        private final OgreGrunt guard;
        private OgreMerchant merchant;
        private int repathTicks;

        private FollowAssignedMerchantGoal(OgreGrunt guard) {
            this.guard = guard;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (!guard.isMerchantGuard() || guard.getTarget() != null
                    || !(guard.level() instanceof ServerLevel server)) return false;
            Entity entity = server.getEntity(guard.merchantOwnerId);
            merchant = entity instanceof OgreMerchant candidate && candidate.isAlive() ? candidate : null;
            return merchant != null && guard.distanceToSqr(merchant) > 12.0 * 12.0;
        }

        @Override public boolean canContinueToUse() {
            return guard.getTarget() == null && merchant != null && merchant.isAlive()
                    && guard.distanceToSqr(merchant) > 8.0 * 8.0;
        }

        @Override public void start() { guard.runningToSuperior = true; repathTicks = 0; }
        @Override public void stop() {
            guard.runningToSuperior = false; merchant = null; repathTicks = 0;
            guard.getNavigation().stop();
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            if (merchant == null) return;
            guard.getLookControl().setLookAt(merchant, 20, 20);
            if (--repathTicks <= 0 || guard.getNavigation().isDone()) {
                guard.getNavigation().moveTo(merchant, SUPERIOR_GUARD_SPEED);
                repathTicks = adjustedTickDelay(10);
            }
        }
    }

    protected static class GuardSuperiorGoal extends Goal {
        private final OgreGrunt guard;
        private OgreGrunt superior;
        private boolean guardingSuperior;
        private int repathTicks;

        protected GuardSuperiorGoal(OgreGrunt guard) {
            this.guard = guard;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!guard.canGuardSuperiors() || guard.getTarget() != null) {
                return false;
            }

            superior = findNearestSuperior();
            guardingSuperior = superior != null;
            return superior != null
                    && guard.distanceToSqr(superior) > SUPERIOR_GUARD_START_RANGE * SUPERIOR_GUARD_START_RANGE;
        }

        @Override
        public boolean canContinueToUse() {
            return guard.getTarget() == null
                    && superior != null
                    && superior.isAlive()
                    && guard.distanceToSqr(superior) > SUPERIOR_GUARD_STOP_RANGE * SUPERIOR_GUARD_STOP_RANGE;
        }

        @Override
        public void start() {
            guard.runningToSuperior = guard.getClass() == OgreGrunt.class
                    || guard instanceof OgreArcher
                    || guard instanceof OgreGuard;
        }

        @Override
        public void stop() {
            superior = null;
            guardingSuperior = false;
            guard.runningToSuperior = false;
            repathTicks = 0;
            guard.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (superior == null) {
                return;
            }

            LivingEntity superiorTarget = superior.getTarget();
            if (superiorTarget != null && superiorTarget.isAlive() && guard.canAttack(superiorTarget)) {
                guard.setTarget(superiorTarget);
                return;
            }

            guard.getLookControl().setLookAt(superior, 20.0F, 20.0F);
            repathTicks = Math.max(repathTicks - 1, 0);
            if (repathTicks <= 0 || guard.getNavigation().isDone()) {
                // Regular Grunts and Guards use the same catch-up pace as Archers and play their
                // run animation until they close back to the superior's nine-block stopping range.
                // Their unrelated random stroll and combat speeds remain unchanged.
                double followSpeed = guard.getClass() == OgreGrunt.class
                        ? SUPERIOR_GUARD_SPEED
                        : guard.isRegularGruntRole() ? GRUNT_WALK_SPEED : SUPERIOR_GUARD_SPEED;
                guard.getNavigation().moveTo(superior, followSpeed);
                repathTicks = adjustedTickDelay(10);
            }
        }

        private OgreGrunt findNearestSuperior() {
            // The lord outranks the mage — guard him first if he's around, otherwise fall back to
            // whichever mage is closest (this also covers a mage guarding the lord itself).
            OgreGrunt lord = guard.level().getEntitiesOfClass(
                            OgreGrunt.class,
                            guard.getBoundingBox().inflate(SUPERIOR_GUARD_SEARCH_RANGE),
                            ogre -> ogre != guard
                                    && ogre.isAlive()
                                    && ogre instanceof OgreLord)
                    .stream()
                    .min(Comparator.comparingDouble(guard::distanceToSqr))
                    .orElse(null);
            if (lord != null) {
                return lord;
            }

            // Mage and brute are peer captains. Both follow a nearby king, but neither
            // captain follows the other when the king is absent.
            if (guard instanceof OgreMage || guard instanceof OgreBrute || guard instanceof OgreGruntCaptain) {
                return null;
            }

            return guard.level().getEntitiesOfClass(
                            OgreGrunt.class,
                            guard.getBoundingBox().inflate(SUPERIOR_GUARD_SEARCH_RANGE),
                            ogre -> ogre != guard
                                    && ogre.isAlive()
                                    && (ogre instanceof OgreMage
                                    || ogre instanceof OgreBrute
                                    || ogre instanceof OgreGruntCaptain))
                    .stream()
                    .min(Comparator.comparingDouble(guard::distanceToSqr))
                    .orElse(null);
        }

    }

    private static class OgreGruntMeleeGoal extends Goal {
        private final OgreGrunt ogre;
        private final double speedModifier;
        private int ticksUntilNextPathRecalculation;
        private int ticksUntilNextAttack;
        private int activeAttackTicks;
        private int nextAttackHitIndex;
        private LivingEntity pendingAttackTarget;
        private MeleeAttackPlan activeAttackPlan;
        private boolean attackImpactSoundPlayed;
        private boolean combatWalking;
        // Hysteresis for the stop-and-swing vs. keep-approaching decision. A single threshold flickered every tick from
        // ordinary position drift right at the boundary, reading as constant stop/start micro-
        // adjustment. Once stopped, stay stopped until the target passes the wider hit-landing
        // range rather than the tighter attack-start range.
        private boolean withinAttackRange;

        private OgreGruntMeleeGoal(OgreGrunt ogre, double speedModifier) {
            this.ogre = ogre;
            this.speedModifier = speedModifier;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = ogre.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }

            return ogre.getNavigation().createPath(target, 0) != null || isCloseEnoughToAttack(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = ogre.getTarget();
            return target != null
                    && target.isAlive()
                    && (!(target instanceof Player player) || !player.isSpectator() && !player.isCreative())
                    && ogre.isWithinRestriction(target.blockPosition());
        }

        @Override
        public void start() {
            ogre.setAggressive(true);
            ticksUntilNextPathRecalculation = 0;
            ticksUntilNextAttack = 0;
            combatWalking = false;
            withinAttackRange = false;
            ogre.setUsingCombatWalk(false);
        }

        @Override
        public void stop() {
            ogre.setAggressive(false);
            ogre.getNavigation().stop();
            ogre.onMeleeAttackPlanFinished();
            pendingAttackTarget = null;
            activeAttackPlan = null;
            activeAttackTicks = 0;
            nextAttackHitIndex = 0;
            attackImpactSoundPlayed = false;
            combatWalking = false;
            withinAttackRange = false;
            ogre.setUsingCombatWalk(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = ogre.getTarget();
            if (target == null) {
                return;
            }

            if (ogre.isPerformingNonMeleeAction()) {
                ogre.getNavigation().stop();
                Vec3 movement = ogre.getDeltaMovement();
                ogre.setDeltaMovement(0.0, movement.y, 0.0);
                return;
            }

            // The Grunt, Captain, and Brute all share the finished, vanilla-like pursuit/commit
            // flow below; only the King still opts into the older hysteresis-based pursuit via
            // usesPolishedMeleePursuit().
            boolean usesFinishedMeleeFlow = ogre.isRegularGruntRole() || ogre instanceof OgreBrute;
            boolean polishedMeleePursuit = ogre.usesPolishedMeleePursuit();
            boolean closeEnoughToAttack;
            if (polishedMeleePursuit) {
                closeEnoughToAttack = withinAttackRange
                        ? isCloseEnoughToLandHit(target)
                        : isCloseEnoughToAttack(target);
                withinAttackRange = closeEnoughToAttack;
            } else {
                closeEnoughToAttack = isCloseEnoughToAttack(target);
            }
            ticksUntilNextPathRecalculation = Math.max(ticksUntilNextPathRecalculation - 1, 0);
            boolean cooldownRunsDuringAttack = usesFinishedMeleeFlow || polishedMeleePursuit;
            if (cooldownRunsDuringAttack) {
                // For the Grunt/Captain/Brute and King, recovery is measured from attack
                // commitment, not after the animation ends. Previously this timer froze inside the
                // early-return animation lock, creating several seconds of unexplained staring
                // after otherwise finished hits.
                ticksUntilNextAttack = Math.max(ticksUntilNextAttack - 1, 0);
            }

            boolean useCombatWalk = activeAttackPlan == null
                    && ogre.shouldUseCombatWalk(target, closeEnoughToAttack,
                    ticksUntilNextAttack, combatWalking);
            if (useCombatWalk != combatWalking) {
                combatWalking = useCombatWalk;
                ticksUntilNextPathRecalculation = 0;
                double transitionSpeed = combatWalking
                        ? ogre.getCombatWalkSpeedModifier()
                        : speedModifier;
                ogre.getNavigation().setSpeedModifier(
                        ogre.applyShallowWaterSpeedFactor(transitionSpeed));
                ogre.setUsingCombatWalk(combatWalking);
            }

            boolean shouldDirectlyTrackTarget = ogre.shouldDirectlyTrackMeleeTargetWhilePursuing()
                    || activeAttackPlan != null
                    || combatWalking && !(ogre instanceof OgreLord)
                    || ogre.distanceToSqr(target) <= ogre.getMeleeAttackReachSqr(target) * 2.25;
            if (ogre.shouldFaceMeleeTarget(target) && shouldDirectlyTrackTarget) {
                ogre.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            if (activeAttackPlan != null && ogre.shouldStayStationaryDuringMeleeAttack()) {
                ogre.getNavigation().stop();
                Vec3 movement = ogre.getDeltaMovement();
                ogre.setDeltaMovement(0.0, movement.y, 0.0);
                tickActiveAttack();
                return;
            }

            if (activeAttackPlan != null && ogre.shouldSuspendNavigationDuringMeleeAttack()) {
                // Some attacks provide their own movement (for example the brute's belly-slam
                // leap). Stop path navigation without cancelling that scripted velocity.
                ogre.getNavigation().stop();
                tickActiveAttack();
                return;
            }

            boolean shouldApproachDuringRecovery = activeAttackPlan == null
                    && ticksUntilNextAttack > 0
                    && ogre.shouldApproachDuringMeleeRecovery();
            boolean holdingCombatSpacing = combatWalking && ogre.shouldHoldCombatSpacing(target);
            boolean readyToCommitAttack = activeAttackPlan == null
                    && ticksUntilNextAttack <= 0
                    && closeEnoughToAttack
                    && ogre.getSensing().hasLineOfSight(target);
            boolean shouldStopForAttackRange = closeEnoughToAttack
                    && !shouldApproachDuringRecovery
                    && (!usesFinishedMeleeFlow || readyToCommitAttack);
            if (holdingCombatSpacing || shouldStopForAttackRange) {
                ogre.getNavigation().stop();
                if (usesFinishedMeleeFlow && readyToCommitAttack) {
                    Vec3 movement = ogre.getDeltaMovement();
                    ogre.setDeltaMovement(0.0, movement.y, 0.0);
                }
            } else {
                tickGroundPursuit(target);
            }

            if (!cooldownRunsDuringAttack) {
                ticksUntilNextAttack = Math.max(ticksUntilNextAttack - 1, 0);
            }
            if (activeAttackPlan != null) {
                tickActiveAttack();
            } else if (readyToCommitAttack) {
                // Snap the body/head yaw to face the target exactly as the swing starts — the gradual
                // look-control rotation can lag or get fought by collision avoidance when ogres are
                // crowded together, leaving the visible swing facing the wrong way.
                ogre.snapFaceTarget(target);
                activeAttackPlan = ogre.startMeleeAttack();
                ticksUntilNextAttack = adjustedTickDelay(activeAttackPlan.cooldownTicks());
                activeAttackTicks = 0;
                nextAttackHitIndex = 0;
                pendingAttackTarget = target;
                attackImpactSoundPlayed = false;
                ogre.swing(InteractionHand.MAIN_HAND);
            }
        }

        private void tickGroundPursuit(LivingEntity target) {
            // Respect the repath timer even when a short path completes. Rebuilding a path every
            // tick makes navigation and look control repeatedly fight over the grunt's heading.
            if (ticksUntilNextPathRecalculation <= 0) {
                double effectiveSpeed = ogre.applyShallowWaterSpeedFactor(getActivePursuitSpeed());
                boolean pathing = ogre.getNavigation().moveTo(target, effectiveSpeed);
                ticksUntilNextPathRecalculation = adjustedTickDelay(
                        pathing ? ogre.getPathRecalculationTicks() : 2);

                double distanceToTargetSqr = ogre.distanceToSqr(target);
                if (distanceToTargetSqr > 1024.0) {
                    ticksUntilNextPathRecalculation += adjustedTickDelay(10);
                } else if (distanceToTargetSqr > 256.0) {
                    ticksUntilNextPathRecalculation += adjustedTickDelay(5);
                }
            }
        }

        private double getActivePursuitSpeed() {
            return combatWalking ? ogre.getCombatWalkSpeedModifier() : speedModifier;
        }

        private void tickActiveAttack() {
            activeAttackTicks++;
            ogre.applyMeleeAttackCatch(activeAttackTicks);

            maybePlayAttackImpactSound();
            while (activeAttackPlan != null
                    && nextAttackHitIndex < activeAttackPlan.hits().size()
                    && activeAttackTicks >= activeAttackPlan.hits().get(nextAttackHitIndex).delayTicks()) {
                landAttackHit(activeAttackPlan.hits().get(nextAttackHitIndex));
                nextAttackHitIndex++;
            }

            if (activeAttackPlan != null
                    && nextAttackHitIndex >= activeAttackPlan.hits().size()
                    && activeAttackTicks >= activeAttackPlan.activeDurationTicks()) {
                ogre.onMeleeAttackPlanFinished();
                activeAttackPlan = null;
                pendingAttackTarget = null;
                activeAttackTicks = 0;
                nextAttackHitIndex = 0;
                attackImpactSoundPlayed = false;
            }
        }

        private void cancelActiveAttack() {
            pendingAttackTarget = null;
            activeAttackPlan = null;
            activeAttackTicks = 0;
            nextAttackHitIndex = 0;
            attackImpactSoundPlayed = false;
            ticksUntilNextAttack = 10;
        }

        private void maybePlayAttackImpactSound() {
            if (attackImpactSoundPlayed
                    || activeAttackPlan == null
                    || nextAttackHitIndex >= activeAttackPlan.hits().size()) {
                return;
            }

            MeleeAttackHit hit = activeAttackPlan.hits().get(nextAttackHitIndex);
            if (hit.areaRadius() > 0.0F || activeAttackTicks < Math.max(0, hit.delayTicks() - ATTACK_IMPACT_SOUND_LEAD_TICKS)) {
                return;
            }

            if (ogre.playsAttackImpactSoundOnMiss()) {
                ogre.playGruntAttackImpactSound();
                attackImpactSoundPlayed = true;
                return;
            }

            if (pendingAttackTarget != null
                    && pendingAttackTarget.isAlive()
                    && isCloseEnoughToLandHit(pendingAttackTarget)
                    && !(pendingAttackTarget instanceof OgreGrunt)
                    && ogre.getSensing().hasLineOfSight(pendingAttackTarget)) {
                ogre.playGruntAttackImpactSound();
                attackImpactSoundPlayed = true;
            }
        }

        private void landAttackHit(MeleeAttackHit hit) {
            if (hit.areaRadius() > 0.0F) {
                landAreaAttackHit(hit);
                return;
            }

            if (pendingAttackTarget != null
                    && pendingAttackTarget.isAlive()
                    && isCloseEnoughToLandHit(pendingAttackTarget)
                    && !(pendingAttackTarget instanceof OgreGrunt)
                    && ogre.getSensing().hasLineOfSight(pendingAttackTarget)) {
                if (ogre.doScaledHurtTarget(pendingAttackTarget, hit.damageMultiplier(), hit.armorPierceFraction())) {
                    applyAttackHitExtras(pendingAttackTarget, hit);
                    if (!attackImpactSoundPlayed) {
                        ogre.playGruntAttackImpactSound();
                        attackImpactSoundPlayed = true;
                    }
                }
            }
        }

        private void landAreaAttackHit(MeleeAttackHit hit) {
            Vec3 areaCenter = getAreaCenter(hit);
            if (hit.shockwaveParticles()) {
                spawnShockwaveParticles(areaCenter, hit.areaRadius());
                if (!ogre.suppressShockwaveSound()) {
                    playShockwaveSound(areaCenter);
                }
                sendShockwaveScreenShake(areaCenter, hit.areaRadius(), hit.screenShakeIntensity());
            }

            List<LivingEntity> targets = ogre.level().getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(
                            areaCenter.x - hit.areaRadius(),
                            areaCenter.y - hit.verticalHitRange(),
                            areaCenter.z - hit.areaRadius(),
                            areaCenter.x + hit.areaRadius(),
                            areaCenter.y + hit.verticalHitRange(),
                            areaCenter.z + hit.areaRadius()),
                    target -> target != ogre
                            && target.isAlive()
                            && (hit.hitsOgreAllies() || !(target instanceof OgreGrunt))
                            && horizontalDistanceToSqr(areaCenter, target) <= hit.areaRadius() * hit.areaRadius()
                            && Math.abs(areaCenter.y - target.getY()) <= hit.verticalHitRange()
                            && ogre.getSensing().hasLineOfSight(target));

            if (hit.blockRippleParticles()) {
                ogre.applyDirectShockwaveWeaponHits(targets, areaCenter, hit);
                spawnBlockRipple(areaCenter, hit, ogre.consumeDirectShockwaveRippleExclusions());
            } else {
                for (LivingEntity target : targets) {
                    if (hit.coneDegrees() > 0.0F && !isInsideAttackCone(target, hit.coneDegrees())) {
                        continue;
                    }

                    float damageMultiplier = target instanceof OgreGrunt ? hit.damageMultiplier() * 0.5F : hit.damageMultiplier();
                    ogre.doScaledHurtTarget(target, damageMultiplier, hit.armorPierceFraction());
                    applyAttackHitExtras(target, hit);
                }
            }
        }

        private Vec3 getAreaCenter(MeleeAttackHit hit) {
            Vec3 forward = Vec3.directionFromRotation(0.0F, ogre.getYRot());
            Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
            return ogre.position()
                    .add(forward.scale(hit.forwardOffset()))
                    .add(right.scale(hit.rightOffset()));
        }

        private void spawnShockwaveParticles(Vec3 center, float radius) {
            if (!(ogre.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            StaticShockwaveParticles.spawn(serverLevel, center, radius);
        }

        private void spawnBlockRipple(Vec3 center, MeleeAttackHit hit, Set<UUID> alreadyHit) {
            if (!(ogre.level() instanceof ServerLevel)) {
                return;
            }

            float radius = hit.areaRadius();


            // No block-count cap here — the ring/point counts below are already bounded by radius
            // (rings) and a 24-point-per-ring ceiling, so this can't run away. A flat cap used to
            // cut the outer rings short on big hits, leaving the raised-block ripple visibly smaller
            // than the (uncapped) shockwave particle ring drawn at the same radius.
            int rings = Math.max(2, (int) Math.ceil(radius / 1.25F));
            float propagationSpeed = radius / (rings * (float) RIPPLE_RING_DELAY_TICKS);
            float maxLift = ogre instanceof OgreLord ? 0.44F : 0.36F;
            ogre.startGroundRipple(center, hit, new GroundRippleProfile(
                    radius, propagationSpeed, 1.125F, maxLift, 1.0F,
                    hit.verticalHitRange(), false, GroundRippleKind.STANDARD), alreadyHit);
        }

        private void playShockwaveSound(Vec3 center) {
            ogre.level().playSound(
                    null,
                    center.x,
                    center.y,
                    center.z,
                    SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE,
                    1.25F,
                    0.55F);
        }

        private void sendShockwaveScreenShake(Vec3 center, float radius, float intensity) {
            if (intensity > 0.0F && ogre.level() instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersNear(
                        serverLevel,
                        null,
                        center.x,
                        center.y,
                        center.z,
                        Math.max(18.0, radius * 4.0),
                        new ScreenShakePayload(center.x, center.y, center.z, intensity));
            }
        }

        private boolean isInsideAttackCone(LivingEntity target, float coneDegrees) {
            Vec3 toTarget = target.position().subtract(ogre.position());
            Vec3 horizontalToTarget = new Vec3(toTarget.x, 0.0, toTarget.z);
            if (horizontalToTarget.lengthSqr() < 1.0E-4) {
                return true;
            }

            Vec3 forward = Vec3.directionFromRotation(0.0F, ogre.getYRot());
            double dot = forward.normalize().dot(horizontalToTarget.normalize());
            return dot >= Math.cos(Math.toRadians(coneDegrees * 0.5F));
        }

        private double horizontalDistanceToSqr(Vec3 center, LivingEntity target) {
            double x = center.x - target.getX();
            double z = center.z - target.getZ();
            return x * x + z * z;
        }

        private void applyAttackHitExtras(LivingEntity target, MeleeAttackHit hit) {
            ogre.applyAttackHitExtras(target, hit);
        }

        private boolean isCloseEnoughToAttack(LivingEntity target) {
            return ogre.distanceToSqr(target) <= ogre.getMeleeAttackReachSqr(target)
                    && Math.abs(ogre.getY() - target.getY()) <= 3.0;
        }

        private boolean isCloseEnoughToLandHit(LivingEntity target) {
            return ogre.distanceToSqr(target) <= ogre.getMeleeHitReachSqr(target)
                    && Math.abs(ogre.getY() - target.getY()) <= 3.0;
        }

    }

    protected double getMeleeAttackReachSqr(LivingEntity target) {
        if (getClass() == OgreGrunt.class) {
            return PLAIN_GRUNT_ATTACK_START_RANGE * PLAIN_GRUNT_ATTACK_START_RANGE;
        }
        if (isRegularGruntRole()) {
            return GRUNT_ATTACK_START_RANGE * GRUNT_ATTACK_START_RANGE;
        }

        double reach = getBbWidth() * 1.45F + target.getBbWidth();
        return reach * reach;
    }

    protected double getMeleeHitReachSqr(LivingEntity target) {
        if (isRegularGruntRole()) {
            return GRUNT_ATTACK_HIT_RANGE * GRUNT_ATTACK_HIT_RANGE;
        }

        return getMeleeAttackReachSqr(target);
    }

    protected boolean shouldFaceMeleeTarget(LivingEntity target) {
        return true;
    }

    protected boolean shouldDirectlyTrackMeleeTargetWhilePursuing() {
        return !isRegularGruntRole();
    }

    protected void snapFaceTarget(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        yRotO = yaw;
        yBodyRotO = yaw;
        yHeadRotO = yaw;
    }

    protected boolean shouldStayStationaryDuringMeleeAttack() {
        // The grunt's short windup is its dodge window. Committing its feet until contact keeps
        // the visible attack and server hit volume together instead of letting it slide after a target.
        return isRegularGruntRole();
    }

    protected boolean shouldSuspendNavigationDuringMeleeAttack() {
        return false;
    }

    protected boolean shouldApproachDuringMeleeRecovery() {
        return false;
    }

    protected boolean shouldUseCombatWalk(LivingEntity target, boolean closeEnoughToAttack,
                                          int ticksUntilNextAttack, boolean currentlyCombatWalking) {
        return false;
    }

    protected void setUsingCombatWalk(boolean usingCombatWalk) {
    }

    protected double getCombatWalkSpeedModifier() {
        return getMeleePursuitSpeedModifier();
    }

    protected boolean shouldHoldCombatSpacing(LivingEntity target) {
        return false;
    }

    protected double getMeleePursuitSpeedModifier() {
        // Navigation modifiers, not attribute mutations, own the Grunt's movement modes.
        return isRegularGruntRole() ? GRUNT_CHASE_SPEED : 1.0;
    }

    protected double getWanderSpeedModifier() {
        return isRegularGruntRole() ? GRUNT_WALK_SPEED : 0.8;
    }

    protected void onMeleeAttackPlanFinished() {
        activeGruntAttackId = 0;
        clearSyncedAttackAnimation();
    }

    /** Adds slight physical follow-through just before an attack connects. */
    protected void applyMeleeAttackCatch(int activeAttackTicks) {
        double forwardSpeed = getMeleeAttackCatchSpeed(activeGruntAttackId, activeAttackTicks);
        LivingEntity target = getTarget();
        if (forwardSpeed <= 0.0 || target == null || !target.isAlive()
                || distanceToSqr(target) > getMeleeHitReachSqr(target)) {
            return;
        }

        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance < 1.0E-4) {
            return;
        }

        Vec3 movement = getDeltaMovement();
        setDeltaMovement(movement.x + dx / horizontalDistance * forwardSpeed,
                movement.y,
                movement.z + dz / horizontalDistance * forwardSpeed);
        hasImpulse = true;
    }

    protected double getMeleeAttackCatchSpeed(int attackId, int activeAttackTicks) {
        // Carry the body through the authored bite lunge, ending immediately before impact.
        // Grunt and Captain share the same visible lunge; other roles use a lighter catch.
        if (attackId == 2 && activeAttackTicks >= 4 && activeAttackTicks <= 9) {
            return isRegularGruntRole() ? 0.14 : 0.055;
        }
        // Give the upswing a short planted step through the visible forward swing.
        return attackId == 1 && activeAttackTicks >= 5 && activeAttackTicks <= 9
                ? (getClass() == OgreGrunt.class ? 0.0 : 0.055)
                : 0.0;
    }

    /** Drops every pending timeline hit from the current melee attack immediately. */
    protected void cancelActiveMeleeAttack() {
        if (meleeGoal != null) {
            meleeGoal.cancelActiveAttack();
        }
        onMeleeAttackPlanFinished();
    }

    protected boolean hasActiveMeleeAttack() {
        return meleeGoal != null && meleeGoal.activeAttackPlan != null;
    }

    /** Lets subclasses reserve the shared movement/attack controller for a non-melee animation. */
    protected boolean isPerformingNonMeleeAction() {
        return false;
    }

    protected void clearSyncedAttackAnimation() {
        if (!level().isClientSide) {
            entityData.set(DATA_GRUNT_ATTACK_ID, 0);
            entityData.set(DATA_GRUNT_ATTACK_START_TICK, 0L);
        }
    }

    protected void playGruntAttackImpactSound() {
        if (activeGruntAttackId == 2) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.FOX_BITE, SoundSource.HOSTILE, 0.85F, 0.7F);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 0.35F, 0.65F);
            return;
        }

        if (activeGruntAttackId == 1) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.75F, 0.8F);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.MUD_HIT, SoundSource.HOSTILE, 0.35F, 0.75F);
        }
    }

    /** Whether an authored attack cue should play on its timeline even if the target evades it. */
    protected boolean playsAttackImpactSoundOnMiss() {
        return true;
    }

    protected void applyAttackHitExtras(LivingEntity target, MeleeAttackHit hit) {
        if (hit.knockbackStrength() > 0.0) {
            if (Math.abs(hit.knockbackForward()) > 1.0E-4 || Math.abs(hit.knockbackRight()) > 1.0E-4) {
                Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
                Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
                Vec3 direction = forward.scale(hit.knockbackForward()).add(right.scale(hit.knockbackRight()));
                if (direction.lengthSqr() > 1.0E-4) {
                    direction = direction.normalize();
                    Vec3 movement = target.getDeltaMovement();
                    target.setDeltaMovement(
                            direction.x * hit.knockbackStrength(),
                            movement.y,
                            direction.z * hit.knockbackStrength());
                    target.hasImpulse = true;
                }
            } else {
                target.knockback(hit.knockbackStrength(), getX() - target.getX(), getZ() - target.getZ());
            }
        }

        if (hit.verticalKnockbackStrength() > 0.0) {
            Vec3 movement = target.getDeltaMovement();
            target.setDeltaMovement(movement.x, Math.max(movement.y, hit.verticalKnockbackStrength()), movement.z);
            target.hasImpulse = true;
        }

        if (hit.slownessTicks() > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, hit.slownessTicks(), 1));
        }

        if (hit.nauseaTicks() > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, hit.nauseaTicks(), 0));
        }

        if (isRegularGruntRole() && activeGruntAttackId == 2) {
            CombatEffects.applyCrippled(target, Config.CRIPPLED_BITE_DURATION_TICKS.get(), this);
        }
    }

    protected void warmUpRippleBlockVisual() {
        // The replacement ripple is cached and rendered directly on the client, so it has no
        // entity renderer to warm up. Retain this hook to keep attack call sites stable.
    }

    protected void startGroundRipple(Vec3 origin, MeleeAttackHit hit, GroundRippleProfile profile) {
        startGroundRipple(origin, hit, profile, Set.of());
    }

    protected void startGroundRipple(Vec3 origin, MeleeAttackHit hit, GroundRippleProfile profile,
                                     Set<UUID> alreadyHit) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        activeGroundRipples.add(new ActiveGroundRipple(origin, hit, profile, alreadyHit));
        PacketDistributor.sendToPlayersNear(serverLevel, null, origin.x, origin.y, origin.z,
                Math.max(32.0, profile.maxRadius() * 4.0),
                new GroundRipplePayload(origin.x, origin.y, origin.z, profile.maxRadius(),
                        profile.propagationSpeed(), RIPPLE_BLOCK_VISIBLE_TICKS, profile.maxVisualLift()));
    }

    private void tickGroundRippleCombat() {
        Iterator<ActiveGroundRipple> iterator = activeGroundRipples.iterator();
        while (iterator.hasNext()) {
            ActiveGroundRipple ripple = iterator.next();
            ripple.ageTicks++;
            float currentRadius = ripple.ageTicks * ripple.profile.propagationSpeed();
            float outerRadius = Math.min(ripple.profile.maxRadius(), currentRadius + ripple.profile.hitBandHalfWidth());
            float innerRadius = Math.max(0.0F, currentRadius - ripple.profile.hitBandHalfWidth());
            if (innerRadius <= ripple.profile.maxRadius()) {
                damageEntitiesInRippleBand(ripple, innerRadius, outerRadius);
            }
            if (currentRadius - ripple.profile.hitBandHalfWidth() > ripple.profile.maxRadius()) {
                iterator.remove();
            }
        }
    }

    private void damageEntitiesInRippleBand(ActiveGroundRipple ripple, float innerRadius, float outerRadius) {
        double verticalRange = ripple.profile.verticalHitRange();
        double searchRadius = ripple.profile.maxRadius() + ripple.profile.hitBandHalfWidth();
        AABB search = new AABB(ripple.origin, ripple.origin).inflate(searchRadius, verticalRange, searchRadius);
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, search,
                target -> target != this && target.isAlive()
                        && (ripple.hit.hitsOgreAllies() || !(target instanceof OgreGrunt))
                        && !ripple.hitTargets.contains(target.getUUID())
                        && target.getBoundingBox().maxY >= ripple.origin.y - verticalRange
                        && target.getBoundingBox().minY <= ripple.origin.y + verticalRange
                        && (!ripple.profile.requiresLineOfSight() || getSensing().hasLineOfSight(target)));

        for (LivingEntity target : targets) {
            if (!horizontalBoundsIntersectRing(target.getBoundingBox(), ripple.origin, innerRadius, outerRadius)
                    || shouldDodgeRippleHit(target)) {
                continue;
            }
            ripple.hitTargets.add(target.getUUID());
            applyGroundRippleHit(target, ripple.hit, ripple.profile);
        }
    }

    private static boolean horizontalBoundsIntersectRing(AABB bounds, Vec3 origin, float innerRadius, float outerRadius) {
        double nearestX = origin.x < bounds.minX ? bounds.minX - origin.x
                : origin.x > bounds.maxX ? origin.x - bounds.maxX : 0.0;
        double nearestZ = origin.z < bounds.minZ ? bounds.minZ - origin.z
                : origin.z > bounds.maxZ ? origin.z - bounds.maxZ : 0.0;
        double farthestX = Math.max(Math.abs(bounds.minX - origin.x), Math.abs(bounds.maxX - origin.x));
        double farthestZ = Math.max(Math.abs(bounds.minZ - origin.z), Math.abs(bounds.maxZ - origin.z));
        double nearestDistanceSqr = nearestX * nearestX + nearestZ * nearestZ;
        double farthestDistanceSqr = farthestX * farthestX + farthestZ * farthestZ;
        return nearestDistanceSqr <= outerRadius * outerRadius && farthestDistanceSqr >= innerRadius * innerRadius;
    }

    protected void applyGroundRippleHit(LivingEntity target, MeleeAttackHit hit, GroundRippleProfile profile) {
        boolean ogreAlly = target instanceof OgreGrunt;
        float adjustedDamageMultiplier = ogreAlly ? hit.damageMultiplier() * 0.5F : hit.damageMultiplier();
        if (doScaledHurtTarget(target, adjustedDamageMultiplier, hit.armorPierceFraction())) {
            applyAttackHitExtras(target, hit);
        }
    }

    protected boolean shouldDodgeRippleHit(LivingEntity target) {
        return false;
    }

    protected void applyDirectShockwaveWeaponHits(List<LivingEntity> targets, Vec3 areaCenter,
                                                  MeleeAttackHit hit) {
    }

    protected Set<UUID> consumeDirectShockwaveRippleExclusions() {
        return Set.of();
    }

    protected boolean suppressShockwaveSound() {
        return false;
    }

    protected int getPathRecalculationTicks() {
        return isRegularGruntRole()
                ? 8 + getRandom().nextInt(5)
                : 4 + getRandom().nextInt(4);
    }

    protected boolean usesPolishedMeleePursuit() {
        // Grunt and Captain both use the finished vanilla-style pursuit branch below. Only the
        // King still opts into the older hysteresis-based pursuit via its own override.
        return false;
    }

    /** Regular Grunts and the Grunt Captain share the same stabilized movement and combat logic. */
    protected boolean isRegularGruntRole() {
        return getClass() == OgreGrunt.class || this instanceof OgreGruntCaptain;
    }

    protected enum GroundRippleKind { STANDARD, KING_CLUB_DOUBLE, KING_CLUB_SINGLE, KING_CLUB_PULL_OUT }

    protected record GroundRippleProfile(float maxRadius, float propagationSpeed, float hitBandHalfWidth,
                                         float maxVisualLift, float particleIntensity, double verticalHitRange,
                                         boolean requiresLineOfSight, GroundRippleKind kind) {
    }

    private static final class ActiveGroundRipple {
        private final Vec3 origin;
        private final MeleeAttackHit hit;
        private final GroundRippleProfile profile;
        private final Set<UUID> hitTargets = new HashSet<>();
        private int ageTicks;

        private ActiveGroundRipple(Vec3 origin, MeleeAttackHit hit, GroundRippleProfile profile, Set<UUID> alreadyHit) {
            this.origin = origin;
            this.hit = hit;
            this.profile = profile;
            this.hitTargets.addAll(alreadyHit);
        }
    }
}
