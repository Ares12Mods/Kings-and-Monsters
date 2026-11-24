package com.kingsandmonsters.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.kingsandmonsters.Config;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.effect.CombatEffects;
import com.kingsandmonsters.effect.BogrotMechanics;
import com.kingsandmonsters.entity.animation.CanonicalOneShotState;
import com.kingsandmonsters.entity.animation.SynchronizedAnimationController;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.TryFindLand;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.UUID;

/** Large amphibious swamp predator. */
public class BogToad extends Animal implements GeoEntity {
    public static boolean checkBogToadSpawnRules(EntityType<BogToad> type, ServerLevelAccessor level,
                                                 MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // The biome modifier controls distribution; placement accepts water and solid banks.
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        if (pos.getY() < surfaceY - 3 || pos.getY() > surfaceY + 1) {
            return false;
        }
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            return true;
        }
        return level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        // Animal's default spawn check rejects water-filled bounding boxes.
        return level.isUnobstructed(this);
    }

    private static final double LANDING_EFFECT_RADIUS = 2.1;
    private static final double LANDING_VERTICAL_TOLERANCE = 1.5;
    private static final float LANDING_DAMAGE = 5.0F;
    private static final int LANDING_DAZED_TICKS = 40;
    private static final EntityDataAccessor<Byte> DATA_JUMP_PHASE =
            SynchedEntityData.defineId(BogToad.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_BITING =
            SynchedEntityData.defineId(BogToad.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> DATA_BITE_START_TICK =
            SynchedEntityData.defineId(BogToad.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_AGGRO =
            SynchedEntityData.defineId(BogToad.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BITE_IN_WATER =
            SynchedEntityData.defineId(BogToad.class, EntityDataSerializers.BOOLEAN);

    private static final byte GROUND = 0;
    private static final byte TAKEOFF = 1;
    private static final byte AIRBORNE = 2;
    private static final byte LANDING = 3;
    private static final int TAKEOFF_LAUNCH_TICK = 9;
    private static final float JUMP_AIM_TURN_LERP = 0.3F;
    private static final int TAKEOFF_ANIMATION_TICKS = 10;
    private static final int LANDING_ANIMATION_TICKS = 5;
    private static final int MIN_JUMP_COOLDOWN_TICKS = 200;
    private static final int MAX_JUMP_COOLDOWN_TICKS = 400;
    private static final int MIN_COMBAT_JUMP_COOLDOWN_TICKS = 140;
    private static final int MAX_COMBAT_JUMP_COOLDOWN_TICKS = 260;
    private static final int BITE_ANIMATION_TICKS = 13;
    private static final int SWIM_BITE_ANIMATION_TICKS = 15; // 0.75s "swim_bite" clip.
    // Updated animator contact is ~0.38 seconds; tick 8 (0.40s) is the closest 20 TPS frame.
    private static final int BITE_DAMAGE_TICK = 8;
    // 0.25s-0.38s at 20 TPS, matching the animation's actual lunge window.
    private static final int BITE_LUNGE_START_TICK = 5;
    private static final int BITE_LUNGE_END_TICK = 8;
    private static final double BITE_LUNGE_PER_TICK = 0.19;
    private static final int BITE_COOLDOWN_TICKS = 25;
    private static final double BITE_START_SURFACE_GAP = 1.35;
    private static final double BITE_HIT_SURFACE_GAP = 1.65;
    private static final double WATER_BITE_START_VERTICAL_GAP = 0.55;
    private static final double WATER_BITE_HIT_VERTICAL_GAP = 0.75;
    private static final double IDLE_WANDER_SPEED_MODIFIER = 0.55;
    private static final double AGGRO_SPEED_MODIFIER = 1.25;
    // Movement thrust is quadratic, so this is roughly 13% stronger than idle movement.
    private static final double WATER_AGGRO_SPEED_MODIFIER = 0.83;
    private static final int UNDERWATER_PATH_FAILURE_GRACE_TICKS = 50;
    private static final int UNDERWATER_PATH_RETRY_TICKS = 5;
    private static final int WATER_MOTION_GRACE_TICKS = 6;
    private static final double JUMP_VERTICAL_SPEED = 0.62;
    private static final double JUMP_HORIZONTAL_SPEED = 0.30;
    private static final double COMBAT_JUMP_HORIZONTAL_SPEED = 1.0;
    private static final double LILY_PAD_CLIMB_PER_TICK = 0.16;
    private static final double LILY_PAD_MAX_CLIMB_HEIGHT = 1.25;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation AGGRO_WALK = RawAnimation.begin().thenLoop("aggro walk");
    private static final RawAnimation JUMP = RawAnimation.begin().thenPlay("jump");
    private static final RawAnimation AIRBORNE_ANIMATION = RawAnimation.begin().thenLoop("airborne");
    private static final RawAnimation LANDED = RawAnimation.begin().thenPlay("landed");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("bite");
    private static final RawAnimation WATER_IDLE = RawAnimation.begin().thenLoop("water_idle");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation SWIM_BITE = RawAnimation.begin().thenPlay("swim_bite");

    private static final ImmutableList<SensorType<? extends Sensor<? super BogToad>>> WATER_SENSOR_TYPES =
            ImmutableList.of(SensorType.IS_IN_WATER);
    private static final ImmutableList<MemoryModuleType<?>> WATER_MEMORY_TYPES = ImmutableList.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.PATH,
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.IS_IN_WATER);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final CanonicalOneShotState visualBite = new CanonicalOneShotState();
    private int jumpPhaseTicks;
    private int waterMotionGraceTicks;
    private int jumpCooldownTicks = MIN_JUMP_COOLDOWN_TICKS;
    private int biteTicks;
    private int biteCooldownTicks;
    private boolean combatJump;
    private Vec3 jumpDirection = Vec3.ZERO;
    private Vec3 biteDirection = Vec3.ZERO;
    private PathNavigation groundNavigation;
    private PathNavigation swimNavigation;
    private MoveControl landMoveControl;
    private MoveControl swimMoveControl;
    private float anticipatedLandYaw = Float.NaN;
    @Nullable
    private UUID unreachableUnderwaterTargetId;

    public BogToad(EntityType<? extends BogToad> type, Level level) {
        super(type, level);
        // Water and shoreline nodes are valid destinations for this amphibious mob.
        setPathfindingMalus(PathType.WATER, 0.0F);
        setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
        landMoveControl = new BogToadLandMoveControl(this);
        // Normalize Frog-style swimming against the Bog Toad's lower movement attribute.
        swimMoveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.50F, 0.1F, true);
        moveControl = isInWater() ? swimMoveControl : landMoveControl;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.70)
                // 1.21.1 PathNavigation needs this during the Mob constructor. 16 was barely past his
                // bite/leap range, so a chase could drop the target for a tick and let a wander goal
                // veer him off course.
                .add(Attributes.FOLLOW_RANGE, 32.0)
                // A full-block step lets the wide body clear normal shoreline lips.
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_JUMP_PHASE, GROUND);
        builder.define(DATA_BITING, false);
        builder.define(DATA_BITE_START_TICK, 0L);
        builder.define(DATA_AGGRO, false);
        builder.define(DATA_BITE_IN_WATER, false);
    }

    @Override
    protected void registerGoals() {
        // Toads never breed: new ones come only from hatching Bog Toad Eggs, so there is no breeding goal.
        goalSelector.addGoal(2, new BogToadBiteGoal());
        goalSelector.addGoal(4, new NoTargetWaterAvoidingStrollGoal(this, IDLE_WANDER_SPEED_MODIFIER));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Always hostile to players: nothing calms a toad, not fish in hand and not a recent mating.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                candidate -> unreachableUnderwaterTargetId == null
                        || !candidate.getUUID().equals(unreachableUnderwaterTargetId)
                        || !candidate.isUnderWater()));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, OgreGrunt.class, true,
                candidate -> !(candidate instanceof OgreGruntCaptain)
                        && !(candidate instanceof OgreBrute)
                        && !(candidate instanceof OgreLord)
                        && !(candidate instanceof PlagueOgre)));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof PlagueOgre) && super.canAttack(target);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof PlagueOgre || super.isAlliedTo(entity);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_PROJECTILE) ? amount * 0.75F : amount);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        // Plain GroundPathNavigation, like OgreGrunt: the amphibious node evaluator gives worse
        // waypoints for ordinary ground pursuit. Swimming still needs the amphibious one, so both
        // exist and tick() swaps between them.
        groundNavigation = new BogToadGroundNavigation(this, level);
        swimNavigation = new BogToadAmphibiousNavigation(this, level);
        return swimNavigation;
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected Brain.Provider<BogToad> brainProvider() {
        return Brain.provider(WATER_MEMORY_TYPES, WATER_SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        Brain<BogToad> brain = brainProvider().makeBrain(dynamic);
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(new MoveToTargetSink()));
        brain.addActivityWithConditions(
                Activity.SWIM,
                ImmutableList.of(
                        Pair.of(3, TryFindLand.create(8, 1.5F)),
                        Pair.of(5, new GateBehavior<>(
                                ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                                ImmutableSet.of(),
                                GateBehavior.OrderPolicy.ORDERED,
                                GateBehavior.RunningPolicy.TRY_ALL,
                                ImmutableList.of(
                                        Pair.of(RandomStroll.swim(0.75F), 1),
                                        Pair.of(RandomStroll.stroll(1.0F, true), 1),
                                        Pair.of(SetWalkTargetFromLookTarget.create(1.0F, 3), 1),
                                        Pair.of(BehaviorBuilder.triggerIf(Entity::isInWaterOrBubble), 5))))),
                ImmutableSet.of(Pair.of(MemoryModuleType.IS_IN_WATER, MemoryStatus.VALUE_PRESENT)));
        brain.addActivity(Activity.IDLE, 0, ImmutableList.of());
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Brain<BogToad> getBrain() {
        return (Brain<BogToad>) super.getBrain();
    }

    @Override
    protected void customServerAiStep() {
        if (getTarget() == null) {
            getBrain().tick((ServerLevel) level(), this);
            getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.SWIM, Activity.IDLE));
        } else {
            getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            getBrain().eraseMemory(MemoryModuleType.PATH);
        }
        super.customServerAiStep();
    }

    @Override
    public int getMaxHeadYRot() {
        // Keep the head aligned with the body while the combat goal owns look control.
        return 0;
    }

    /**
     * Matches vanilla Frog: swim under the toad's own power instead of vanilla's default swim
     * gravity and drag. moveRelative() scales the already speed-scaled thrust by getSpeed() again,
     * so the velocity added each tick is roughly proportional to speed^2, with 0.9 retention.
     */
    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            tickLilyPadClimb();
            boolean finishingShorePath = navigation == swimNavigation
                    && !swimNavigation.isDone()
                    && !onGround();
            PathNavigation desiredNavigation = isInWater() || finishingShorePath
                    ? swimNavigation
                    : groundNavigation;
            // Preserve swim control through brief surface breaches on amphibious paths.
            MoveControl desiredMoveControl = desiredNavigation == swimNavigation
                    ? swimMoveControl
                    : landMoveControl;
            if (moveControl != desiredMoveControl) {
                moveControl = desiredMoveControl;
            }
            if (navigation != desiredNavigation) {
                navigation.stop();
                navigation = desiredNavigation;
                LivingEntity transitionTarget = getTarget();
                if (transitionTarget != null && transitionTarget.isAlive()
                        && !entityData.get(DATA_BITING)) {
                    navigation.moveTo(transitionTarget,
                            isInWater() ? WATER_AGGRO_SPEED_MODIFIER : AGGRO_SPEED_MODIFIER);
                }
            }
            LivingEntity target = getTarget();
            entityData.set(DATA_AGGRO, target != null && target.isAlive());
            tickBite();
            tickJumpSequence();
        }
    }

    private void tickJumpSequence() {
        byte phase = entityData.get(DATA_JUMP_PHASE);
        if (isInWater()) {
            // Never hop while swimming; onGround() can stay false indefinitely while
            // floating, which would otherwise strand the takeoff/airborne/landing phases.
            if (phase != GROUND) {
                setJumpPhase(GROUND);
                combatJump = false;
                jumpDirection = Vec3.ZERO;
            }
            jumpPhaseTicks = 0;
            return;
        }
        if (phase == GROUND) {
            jumpPhaseTicks = 0;
            if (entityData.get(DATA_BITING)) {
                return;
            }
            if (jumpCooldownTicks > 0) {
                jumpCooldownTicks--;
            } else if (onGround()) {
                setJumpPhase(TAKEOFF);
                LivingEntity target = getTarget();
                boolean inCombat = target != null && target.isAlive();
                combatJump = inCombat;
                jumpDirection = inCombat ? horizontalDirectionTo(target) : facingDirection();
                jumpCooldownTicks = Mth.nextInt(getRandom(),
                        inCombat ? MIN_COMBAT_JUMP_COOLDOWN_TICKS : MIN_JUMP_COOLDOWN_TICKS,
                        inCombat ? MAX_COMBAT_JUMP_COOLDOWN_TICKS : MAX_JUMP_COOLDOWN_TICKS);
            }
            return;
        }

        jumpPhaseTicks++;
        if (phase == TAKEOFF) {
            getNavigation().stop();
            LivingEntity target = getTarget();
            // Re-aim only until the leap fires, and ease into it. Snapping rotation every tick could
            // spin the whole body in one tick if the target moved during the windup. After launch,
            // keep the heading he already has.
            if (combatJump && target != null && target.isAlive() && jumpPhaseTicks < TAKEOFF_LAUNCH_TICK) {
                jumpDirection = horizontalDirectionTo(target);
                faceJumpDirection();
            }
            if (jumpPhaseTicks == TAKEOFF_LAUNCH_TICK) {
                launchFromAnimation();
            }
            if (jumpPhaseTicks >= TAKEOFF_ANIMATION_TICKS) {
                setJumpPhase(AIRBORNE);
            }
        } else if (phase == AIRBORNE && onGround()) {
            spawnLandingImpact();
            applyCombatLandingImpact();
            setDeltaMovement(Vec3.ZERO);
            setJumpPhase(LANDING);
        } else if (phase == LANDING) {
            getNavigation().stop();
            if (jumpPhaseTicks >= LANDING_ANIMATION_TICKS) {
                setJumpPhase(GROUND);
                combatJump = false;
                jumpDirection = Vec3.ZERO;
            }
        }
    }

    private void launchFromAnimation() {
        Vec3 direction = jumpDirection.lengthSqr() > 1.0E-6 ? jumpDirection : facingDirection();
        double horizontalSpeed = combatJump ? COMBAT_JUMP_HORIZONTAL_SPEED : JUMP_HORIZONTAL_SPEED;
        setDeltaMovement(direction.x * horizontalSpeed,
                JUMP_VERTICAL_SPEED,
                direction.z * horizontalSpeed);
    }

    private Vec3 horizontalDirectionTo(LivingEntity target) {
        Vec3 direction = new Vec3(target.getX() - getX(), 0.0, target.getZ() - getZ());
        return direction.lengthSqr() > 1.0E-6 ? direction.normalize() : facingDirection();
    }

    private Vec3 facingDirection() {
        float radians = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(radians), 0.0, Mth.cos(radians));
    }

    private void faceJumpDirection() {
        float yawTarget = (float) (Mth.atan2(-jumpDirection.x, jumpDirection.z) * Mth.RAD_TO_DEG);
        float yaw = getYRot() + Mth.wrapDegrees(yawTarget - getYRot()) * JUMP_AIM_TURN_LERP;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    private void setJumpPhase(byte phase) {
        entityData.set(DATA_JUMP_PHASE, phase);
        jumpPhaseTicks = 0;
    }

    private void startBite() {
        entityData.set(DATA_BITING, true);
        entityData.set(DATA_BITE_IN_WATER, isInWater());
        entityData.set(DATA_BITE_START_TICK, level().getGameTime());
        biteTicks = 0;
        biteDirection = biteDirectionTo(getTarget());
        getNavigation().stop();
    }

    private int currentBiteAnimationTicks() {
        return entityData.get(DATA_BITE_IN_WATER) ? SWIM_BITE_ANIMATION_TICKS : BITE_ANIMATION_TICKS;
    }

    private void tickBite() {
        if (biteCooldownTicks > 0) {
            biteCooldownTicks--;
        }
        if (!entityData.get(DATA_BITING)) {
            return;
        }

        biteTicks++;
        getNavigation().stop();
        LivingEntity target = getTarget();
        if (entityData.get(DATA_BITE_IN_WATER) && biteTicks < BITE_LUNGE_START_TICK
                && target != null && target.isAlive()) {
            biteDirection = biteDirectionTo(target);
        }
        if (biteTicks >= BITE_LUNGE_START_TICK && biteTicks <= BITE_LUNGE_END_TICK) {
            Vec3 movement = getDeltaMovement();
            Vec3 lunge = entityData.get(DATA_BITE_IN_WATER) ? biteDirection : facingDirection();
            setDeltaMovement(
                    movement.x + lunge.x * BITE_LUNGE_PER_TICK,
                    movement.y + lunge.y * BITE_LUNGE_PER_TICK,
                    movement.z + lunge.z * BITE_LUNGE_PER_TICK);
        }
        if (biteTicks == BITE_DAMAGE_TICK) {
            playSound(SoundEvents.FOX_BITE, 0.95F, 0.72F);
            boolean closeEnough = target != null && target.isAlive() && canAttack(target)
                    && (entityData.get(DATA_BITE_IN_WATER)
                    ? isWithinWaterBiteReach(target, BITE_HIT_SURFACE_GAP, WATER_BITE_HIT_VERTICAL_GAP)
                    : isWithinHorizontalSurfaceGap(target, BITE_HIT_SURFACE_GAP));
            if (closeEnough) {
                if (doHurtTarget(target)) {
                    CombatEffects.applyCrippled(target, Config.CRIPPLED_BITE_DURATION_TICKS.get(), this);
                    target.addEffect(BogrotMechanics.naturalBite(), this);
                }
            }
        }
        if (biteTicks >= currentBiteAnimationTicks()) {
            if (entityData.get(DATA_BITE_IN_WATER)) {
                Vec3 movement = getDeltaMovement();
                setDeltaMovement(0.0, movement.y, 0.0);
            }
            entityData.set(DATA_BITING, false);
            entityData.set(DATA_BITE_START_TICK, 0L);
            biteTicks = 0;
            biteDirection = Vec3.ZERO;
            biteCooldownTicks = BITE_COOLDOWN_TICKS;
        }
    }

    /** Helps a swimming Toad clear a lily pad's thin rim after it reaches the edge. */
    private void tickLilyPadClimb() {
        if (!isInWater() || !horizontalCollision || entityData.get(DATA_JUMP_PHASE) != GROUND
                || entityData.get(DATA_BITING)) {
            return;
        }

        double nearestDistanceSqr = Double.MAX_VALUE;
        double climbHeight = 0.0;
        BlockPos center = blockPosition();
        for (int x = center.getX() - 2; x <= center.getX() + 2; x++) {
            for (int z = center.getZ() - 2; z <= center.getZ() + 2; z++) {
                for (int y = center.getY(); y <= center.getY() + 2; y++) {
                    BlockPos padPos = new BlockPos(x, y, z);
                    BlockState state = level().getBlockState(padPos);
                    if (!state.is(Blocks.LILY_PAD)) continue;

                    double dx = x + 0.5 - getX();
                    double dz = z + 0.5 - getZ();
                    double distanceSqr = dx * dx + dz * dz;
                    double padTop = y + state.getCollisionShape(level(), padPos).max(Direction.Axis.Y);
                    double neededLift = padTop + 0.02 - getY();
                    if (distanceSqr > 2.25 * 2.25 || distanceSqr >= nearestDistanceSqr
                            || neededLift <= 0.0 || neededLift > LILY_PAD_MAX_CLIMB_HEIGHT) continue;
                    nearestDistanceSqr = distanceSqr;
                    climbHeight = neededLift;
                }
            }
        }

        if (climbHeight > 0.0) {
            // Use collision-aware movement so ceilings and other blocks still stop the climb.
            move(MoverType.SELF, new Vec3(0.0, Math.min(LILY_PAD_CLIMB_PER_TICK, climbHeight), 0.0));
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, 0.0, motion.z);
            hasImpulse = true;
        }
    }

    private Vec3 biteDirectionTo(@Nullable LivingEntity target) {
        if (target == null) {
            return facingDirection();
        }
        Vec3 direction = target.getBoundingBox().getCenter().subtract(getBoundingBox().getCenter());
        return direction.lengthSqr() > 1.0E-6 ? direction.normalize() : facingDirection();
    }

    /** Server-authoritative touchdown hook; future landing AoE belongs here as well. */
    private void spawnLandingImpact() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BogToadLandingImpact.spawnVisuals(serverLevel, this, combatJump);
    }

    /** Combat hops damage and briefly Daze players caught in the touchdown footprint. */
    private void applyCombatLandingImpact() {
        if (!combatJump || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double radiusSqr = LANDING_EFFECT_RADIUS * LANDING_EFFECT_RADIUS;
        AABB searchArea = new AABB(
                getX() - LANDING_EFFECT_RADIUS, getY() - LANDING_VERTICAL_TOLERANCE,
                getZ() - LANDING_EFFECT_RADIUS,
                getX() + LANDING_EFFECT_RADIUS, getY() + LANDING_VERTICAL_TOLERANCE,
                getZ() + LANDING_EFFECT_RADIUS);
        for (Player player : serverLevel.getEntitiesOfClass(Player.class, searchArea,
                candidate -> candidate.isAlive() && !candidate.isSpectator() && canAttack(candidate))) {
            AABB playerBox = player.getBoundingBox();
            double gapX = Math.max(0.0, Math.max(playerBox.minX - getX(), getX() - playerBox.maxX));
            double gapZ = Math.max(0.0, Math.max(playerBox.minZ - getZ(), getZ() - playerBox.maxZ));
            if (gapX * gapX + gapZ * gapZ > radiusSqr
                    || Math.abs(player.getY() - getY()) > LANDING_VERTICAL_TOLERANCE) {
                continue;
            }

            if (player.hurt(damageSources().mobAttack(this), LANDING_DAMAGE)) {
                CombatEffects.applyDazed(player, LANDING_DAZED_TICKS, this);
            }
        }
    }

    private long lastMovingAnimationTick = Long.MIN_VALUE / 2;

    @Override
    public double getBoneResetTime() {
        // Bones a clip stops keying ease back to rest over a few ticks instead of snapping there.
        return 6.0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Three ticks of blend: one tick is close to a hard cut between idle, walk and the hop phases.
        controllers.add(new SynchronizedAnimationController<>(this, "Bog Toad Movement", 3, state -> {
            state.setControllerSpeed(1.0F);
            if (isDeadOrDying()) {
                // A bite or hop in progress is cancelled the moment it dies.
                visualBite.clear();
                return state.setAndContinue(isInWater() ? WATER_IDLE : IDLE);
            }
            int activeBite = visualBite.update(state.getController(),
                    entityData.get(DATA_BITING) ? (entityData.get(DATA_BITE_IN_WATER) ? 2 : 1) : 0,
                    entityData.get(DATA_BITE_START_TICK),
                    type -> type == 2 ? SWIM_BITE_ANIMATION_TICKS : BITE_ANIMATION_TICKS,
                    level().getGameTime());
            if (activeBite == 2) {
                return state.setAndContinue(SWIM_BITE);
            }
            if (activeBite != 0) {
                return state.setAndContinue(BITE);
            }
            return switch (entityData.get(DATA_JUMP_PHASE)) {
                case TAKEOFF -> state.setAndContinue(JUMP);
                case AIRBORNE -> state.setAndContinue(AIRBORNE_ANIMATION);
                case LANDING -> state.setAndContinue(LANDED);
                default -> {
                    boolean physicallyMoving = getDeltaMovement().horizontalDistanceSqr() > 1.0E-5;
                    boolean pathing = !getNavigation().isDone();
                    boolean movingNow = state.isMoving() || physicallyMoving || pathing;
                    // A short grace, counted in game ticks (this runs once per rendered frame), so an
                    // instant of standing still between hops does not flash the idle pose.
                    long now = level().getGameTime();
                    if (movingNow) {
                        lastMovingAnimationTick = now;
                    }
                    boolean moving = movingNow || now - lastMovingAnimationTick <= 3;
                    boolean aggro = entityData.get(DATA_AGGRO);
                    if (isInWater()) {
                        // Small hysteresis so brief gaps between swim-goal targets (or momentum
                        // decaying just under the threshold) don't flicker back to idle for a
                        // frame or two before the next stroke picks back up.
                        if (moving) {
                            waterMotionGraceTicks = WATER_MOTION_GRACE_TICKS;
                        } else if (waterMotionGraceTicks > 0) {
                            waterMotionGraceTicks--;
                        }
                        if (!moving && waterMotionGraceTicks <= 0) {
                            yield state.setAndContinue(WATER_IDLE);
                        }
                        state.setControllerSpeed(1.0F);
                        yield state.setAndContinue(SWIM);
                    }
                    if (!moving) {
                        yield state.setAndContinue(IDLE);
                    }
                    if (aggro) {
                        state.setControllerSpeed((float) AGGRO_SPEED_MODIFIER);
                        yield state.setAndContinue(AGGRO_WALK);
                    }
                    state.setControllerSpeed((float) IDLE_WANDER_SPEED_MODIFIER);
                    yield state.setAndContinue(WALK);
                }
            };
        }));
    }

    /** Matches the proportional waypoint turning used by the working Ogre ground controller. */
    private static final class BogToadLandMoveControl extends MoveControl {
        private final BogToad toad;

        private BogToadLandMoveControl(BogToad toad) {
            super(toad);
            this.toad = toad;
        }

        @Override
        protected float rotlerp(float currentAngle, float targetAngle, float maxSpeed) {
            float desiredAngle = targetAngle;
            if (Float.isFinite(toad.anticipatedLandYaw)) {
                // Anticipate the next path segment while remaining anchored to the current node.
                desiredAngle += Mth.wrapDegrees(toad.anticipatedLandYaw - targetAngle) * 0.65F;
            }
            return currentAngle + Mth.wrapDegrees(desiredAngle - currentAngle) * 0.36F;
        }
    }

    /** Advances a simple, reachable next node before the wide body reaches the current node center. */
    private static final class BogToadGroundNavigation extends GroundPathNavigation {
        private static final double NODE_LOOK_AHEAD_DISTANCE_SQR = 1.0;
        private final BogToad toad;

        private BogToadGroundNavigation(BogToad toad, Level level) {
            super(toad, level);
            this.toad = toad;
        }

        @Override
        protected void followThePath() {
            toad.anticipatedLandYaw = Float.NaN;
            if (path != null && path.getNextNodeIndex() + 1 < path.getNodeCount()) {
                Vec3 currentNode = path.getNextEntityPos(mob);
                Node nextNode = path.getNode(path.getNextNodeIndex() + 1);
                Vec3 lookahead = path.getEntityPosAtNode(mob, path.getNextNodeIndex() + 1);
                double lookDx = lookahead.x - mob.getX();
                double lookDz = lookahead.z - mob.getZ();
                if (lookDx * lookDx + lookDz * lookDz > 1.0E-6) {
                    toad.anticipatedLandYaw = (float) (Mth.atan2(lookDz, lookDx) * Mth.RAD_TO_DEG) - 90.0F;
                }
                double dx = mob.getX() - currentNode.x;
                double dz = mob.getZ() - currentNode.z;
                if (dx * dx + dz * dz <= NODE_LOOK_AHEAD_DISTANCE_SQR
                        && Math.abs(nextNode.y - currentNode.y) <= 1.0
                        && canCutCorner(nextNode.type)) {
                    path.advance();
                }
            }
            super.followThePath();
        }
    }

    /** Vanilla Frog's amphibious navigation details. */
    private static final class BogToadAmphibiousNavigation extends AmphibiousPathNavigation {
        private BogToadAmphibiousNavigation(Mob mob, Level level) {
            super(mob, level);
        }

        @Override
        public boolean canCutCorner(PathType pathType) {
            return pathType != PathType.WATER_BORDER && super.canCutCorner(pathType);
        }

        @Override
        protected PathFinder createPathFinder(int maxVisitedNodes) {
            nodeEvaluator = new BogToadNodeEvaluator(true);
            nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(nodeEvaluator, maxVisitedNodes);
        }

    }

    /** Vanilla Frog's water start node and preferred-jump block handling. */
    private static final class BogToadNodeEvaluator extends AmphibiousNodeEvaluator {
        private final BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();

        private BogToadNodeEvaluator(boolean prefersShallowSwimming) {
            super(prefersShallowSwimming);
        }

        @Override
        public Node getStart() {
            return !mob.isInWater() ? super.getStart() : getStartNode(new BlockPos(
                    Mth.floor(mob.getBoundingBox().minX),
                    Mth.floor(mob.getBoundingBox().minY),
                    Mth.floor(mob.getBoundingBox().minZ)));
        }

        @Override
        public PathType getPathType(PathfindingContext context, int x, int y, int z) {
            belowPos.set(x, y - 1, z);
            return context.getBlockState(belowPos).is(BlockTags.FROG_PREFER_JUMP_TO)
                    ? PathType.OPEN
                    : super.getPathType(context, x, y, z);
        }
    }

    /** Land-only idle wandering; Frog's Brain owns water movement. */
    private final class NoTargetWaterAvoidingStrollGoal extends WaterAvoidingRandomStrollGoal {
        private NoTargetWaterAvoidingStrollGoal(PathfinderMob mob, double speedModifier) {
            super(mob, speedModifier);
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && !isInWater() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && !isInWater() && super.canContinueToUse();
        }
    }

    private final class BogToadBiteGoal extends Goal {
        private int ticksUntilNextPathRecalculation;
        private Path pursuitPath;
        private double lastPathedTargetX;
        private double lastPathedTargetY;
        private double lastPathedTargetZ;
        private boolean wasTargetSubmerged;
        private int underwaterPathFailureTicks;
        private int underwaterPathRetryTicks;
        @Nullable
        private Path underwaterApproachPath;

        private BogToadBiteGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || !canAttack(target)) {
                return false;
            }

            pursuitPath = getNavigation().createPath(target, 0);
            return pursuitPath != null || isWithinBiteSetupReach(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null
                    && target.isAlive()
                    && canAttack(target)
                    && (!(target instanceof Player player) || !player.isSpectator() && !player.isCreative());
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (pursuitPath != null) {
                getNavigation().moveTo(pursuitPath, isInWater() ? WATER_AGGRO_SPEED_MODIFIER : AGGRO_SPEED_MODIFIER);
            }
            if (target != null) {
                rememberPathedTarget(target);
            }
            ticksUntilNextPathRecalculation = adjustedTickDelay(10);
        }

        @Override
        public void stop() {
            getNavigation().stop();
            pursuitPath = null;
            ticksUntilNextPathRecalculation = 0;
            wasTargetSubmerged = false;
            underwaterPathFailureTicks = 0;
            underwaterPathRetryTicks = 0;
            underwaterApproachPath = null;
            directChaseActive = false;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }

            if (entityData.get(DATA_BITING) || entityData.get(DATA_JUMP_PHASE) != GROUND) {
                getNavigation().stop();
                return;
            }

            boolean targetSubmerged = target.isUnderWater();
            if (!targetSubmerged) {
                if (wasTargetSubmerged) {
                    // Replace the submerged approach path as soon as the target surfaces.
                    getNavigation().stop();
                    ticksUntilNextPathRecalculation = 0;
                    underwaterApproachPath = null;
                }
                underwaterPathFailureTicks = 0;
                underwaterPathRetryTicks = 0;
                if (target.getUUID().equals(unreachableUnderwaterTargetId)) {
                    unreachableUnderwaterTargetId = null;
                }
            } else if (isInWater()) {
                boolean followingReachableApproach = underwaterApproachPath != null
                        && !getNavigation().isDone();
                if (followingReachableApproach) {
                    // A reachable path resets the consecutive-failure timer.
                    underwaterPathFailureTicks = 0;
                    underwaterPathRetryTicks = 0;
                } else {
                    underwaterPathFailureTicks++;
                    underwaterPathRetryTicks = Math.max(0, underwaterPathRetryTicks - 1);
                    if (underwaterPathRetryTicks <= 0) {
                        underwaterApproachPath = createUnderwaterApproachPath(target);
                        if (underwaterApproachPath != null) {
                            underwaterPathFailureTicks = 0;
                            getNavigation().moveTo(underwaterApproachPath, WATER_AGGRO_SPEED_MODIFIER);
                            rememberPathedTarget(target);
                            ticksUntilNextPathRecalculation = adjustedTickDelay(10);
                        } else {
                            underwaterPathRetryTicks = UNDERWATER_PATH_RETRY_TICKS;
                        }
                    }
                }
                if (underwaterPathFailureTicks >= UNDERWATER_PATH_FAILURE_GRACE_TICKS) {
                    disengageUnreachableUnderwaterTarget(target);
                    return;
                }
            }
            wasTargetSubmerged = targetSubmerged;
            if (targetSubmerged
                    && underwaterPathFailureTicks >= UNDERWATER_PATH_FAILURE_GRACE_TICKS) {
                disengageUnreachableUnderwaterTarget(target);
                return;
            }
            if (isWithinBiteSetupReach(target)) {
                // Single source of truth for stopping movement in both mediums now that water
                // pursuit is navigation-driven too (see below) instead of a raw MoveControl homing
                // call that bypassed navigation and needed its own separate reset.
                getNavigation().stop();
                ticksUntilNextPathRecalculation = 0;
                if (biteCooldownTicks <= 0 && (onGround() || isInWater())) {
                    startBite();
                }
            } else if (targetSubmerged && isInWater()
                    && (underwaterApproachPath == null || getNavigation().isDone())) {
                // Avoid following a partial path during the bounded reachability grace period.
                getNavigation().stop();
                ticksUntilNextPathRecalculation = 0;
            } else {
                // Frog-style swimming and Ogre-style walking own the actual movement and facing; this
                // goal only requests a destination. Re-path only when the path has ended or the target
                // moved a block (the throttle vanilla's Dolphin attack goal uses). Driving MoveControl
                // every tick with no lead made toads circle a moving target and fought the buoyancy system.
                if (!isInWater() && !targetSubmerged && tickDirectChase(target)) {
                    return;
                }
                ticksUntilNextPathRecalculation = Math.max(ticksUntilNextPathRecalculation - 1, 0);
                boolean currentPathEnded = getNavigation().isDone();
                boolean targetMoved = target.distanceToSqr(
                        lastPathedTargetX, lastPathedTargetY, lastPathedTargetZ) >= 1.0;
                boolean shouldRepath = isInWater() ? currentPathEnded || targetMoved : true;
                if (ticksUntilNextPathRecalculation <= 0 && shouldRepath) {
                    boolean diving = targetSubmerged && isInWater();
                    Path divePath = diving ? createUnderwaterApproachPath(target) : null;
                    boolean foundPath = diving
                            ? divePath != null && getNavigation().moveTo(divePath, WATER_AGGRO_SPEED_MODIFIER)
                            : getNavigation().moveTo(target,
                            isInWater() ? WATER_AGGRO_SPEED_MODIFIER : AGGRO_SPEED_MODIFIER);
                    underwaterApproachPath = divePath;
                    if (foundPath) {
                        underwaterPathFailureTicks = 0;
                    }
                    rememberPathedTarget(target);
                    ticksUntilNextPathRecalculation = adjustedTickDelay(
                            (foundPath ? 10 : 20) + getRandom().nextInt(5));
                }
            }
        }

        private int directChaseRecheckTicks;
        private boolean directChaseActive;

        /**
         * On open dry ground the toad walks straight at its target instead of along path nodes, which read as angled,
         * zigzagging steps. Water, obstacles and gaps fall back to normal pathing; see {@link OpenGroundChase}.
         */
        private boolean tickDirectChase(LivingEntity target) {
            if (--directChaseRecheckTicks <= 0) {
                directChaseRecheckTicks = 4;
                boolean open = onGround() && OpenGroundChase.isClear(BogToad.this, target);
                if (directChaseActive && !open) {
                    ticksUntilNextPathRecalculation = 0;
                }
                directChaseActive = open;
            }
            if (!directChaseActive) {
                return false;
            }
            getNavigation().stop();
            // No path to look ahead along, so drop the last segment's anticipated heading.
            anticipatedLandYaw = Float.NaN;
            getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), AGGRO_SPEED_MODIFIER);
            return true;
        }

        private void rememberPathedTarget(LivingEntity target) {
            lastPathedTargetX = target.getX();
            lastPathedTargetY = target.getY();
            lastPathedTargetZ = target.getZ();
        }

        private void disengageUnreachableUnderwaterTarget(LivingEntity target) {
            unreachableUnderwaterTargetId = target.getUUID();
            getNavigation().stop();
            setTarget(null);
            pursuitPath = null;
            underwaterApproachPath = null;
            wasTargetSubmerged = false;
            underwaterPathFailureTicks = 0;
            underwaterPathRetryTicks = 0;
            ticksUntilNextPathRecalculation = 0;
        }

        @Nullable
        private Path createUnderwaterApproachPath(LivingEntity target) {
            // Diving requires a reachable submerged node sized for the Toad's full footprint.
            BlockPos center = BlockPos.containing(target.getBoundingBox().getCenter());
            int[][] offsets = {
                    {0, 0, 0}, {2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2},
                    {2, 0, 2}, {2, 0, -2}, {-2, 0, 2}, {-2, 0, -2},
                    {0, -1, 0}, {2, -1, 0}, {-2, -1, 0}, {0, -1, 2}, {0, -1, -2}
            };
            for (int[] offset : offsets) {
                BlockPos candidate = center.offset(offset[0], offset[1], offset[2]);
                if (!level().getFluidState(candidate).is(FluidTags.WATER)) {
                    continue;
                }
                Path path = getNavigation().createPath(candidate, 0);
                if (path != null && path.canReach()) {
                    return path;
                }
            }
            return null;
        }

    }

    /** Measures the real horizontal air gap between hitboxes instead of center-to-center distance. */
    private boolean isWithinHorizontalSurfaceGap(LivingEntity target, double allowedGap) {
        AABB toadBox = getBoundingBox();
        AABB targetBox = target.getBoundingBox();
        double gapX = Math.max(0.0,
                Math.max(targetBox.minX - toadBox.maxX, toadBox.minX - targetBox.maxX));
        double gapZ = Math.max(0.0,
                Math.max(targetBox.minZ - toadBox.maxZ, toadBox.minZ - targetBox.maxZ));
        return gapX * gapX + gapZ * gapZ <= allowedGap * allowedGap;
    }

    private boolean isWithinBiteSetupReach(LivingEntity target) {
        return isInWater()
                ? isWithinWaterBiteReach(target, BITE_START_SURFACE_GAP, WATER_BITE_START_VERTICAL_GAP)
                : isWithinHorizontalSurfaceGap(target, BITE_START_SURFACE_GAP);
    }

    private boolean isWithinWaterBiteReach(LivingEntity target, double allowedGap, double allowedVerticalGap) {
        AABB toadBox = getBoundingBox();
        AABB targetBox = target.getBoundingBox();
        double gapX = Math.max(0.0,
                Math.max(targetBox.minX - toadBox.maxX, toadBox.minX - targetBox.maxX));
        double gapY = Math.max(0.0,
                Math.max(targetBox.minY - toadBox.maxY, toadBox.minY - targetBox.maxY));
        double gapZ = Math.max(0.0,
                Math.max(targetBox.minZ - toadBox.maxZ, toadBox.minZ - targetBox.maxZ));
        return gapY <= allowedVerticalGap
                && gapX * gapX + gapY * gapY + gapZ * gapZ <= allowedGap * allowedGap;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Vanilla's wet frog footfalls, lowered and given more body for this much larger toad.
        playSound(SoundEvents.FROG_STEP, 0.32F, 0.66F + getRandom().nextFloat() * 0.08F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.BOG_TOAD_IDLE_DEEP_A.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        // About 15 seconds between idle sounds on average (vanilla adds roughly two seconds to this).
        return 260;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.BOG_TOAD_HURT_DEEP_A.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.BOG_TOAD_DEATH_DEEP.get();
    }

    /** Players cannot breed toads; new ones only come from hatching Bog Toad Eggs. */
    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    @Nullable
    public BogPole getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }
}
