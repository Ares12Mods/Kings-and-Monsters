package com.kingsandmonsters.entity;

import com.kingsandmonsters.item.ItemUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.event.ForgeEventFactory;
import software.bernie.geckolib.core.animation.AnimationController;
import com.kingsandmonsters.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/** Fish-like, non-combat juvenile stage of the Bog Toad. */
public final class BogPole extends Animal implements GeoEntity {
    public static final int GROWTH_TICKS = 72_000;
    public static final int MAX_AIR = 1_200;
    private static final int ADULT_LOCAL_CAP = 6;
    public static final int SEEK_AIR_AT = 240;
    public static final int SCARED_TICKS = 160;
    public static final double NORMAL_SWIM_SPEED = 1.0;
    public static final double SCARED_SWIM_SPEED = 1.5;
    private static final double WATER_SPEED_SCALE = 4.5;
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM,
            new ResourceLocation("kingsandmonsters", "bog_toad_food"));

    private static final EntityDataAccessor<Boolean> SCARED =
            SynchedEntityData.defineId(BogPole.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LAND_MOVING =
            SynchedEntityData.defineId(BogPole.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("Run");
    private static final RawAnimation WATER_IDLE = RawAnimation.begin().thenLoop("water idle");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation SWIM_SCARED = RawAnimation.begin().thenLoop("swim scared");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private PathNavigation waterNavigation;
    private PathNavigation groundNavigation;
    private MoveControl fishMoveControl;
    private MoveControl groundMoveControl;
    private boolean landExcursion;
    private int age;
    private int scaredTicks;
    private int returnUnderwaterTicks;
    private int surfaceLoiterTicks;
    @Nullable private LivingEntity threat;

    /** Poles hatch in the swamp's shallows: standing water with room above, near the surface. */
    public static boolean checkBogPoleSpawnRules(EntityType<BogPole> type, ServerLevelAccessor level,
                                                 MobSpawnType spawnType, BlockPos pos,
                                                 RandomSource random) {
        return level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER)
                && pos.getY() >= level.getSeaLevel() - 6;
    }

    public BogPole(EntityType<? extends BogPole> type, Level level) {
        super(type, level);
        fishMoveControl = new FishLikeMoveControl(this);
        groundMoveControl = new MoveControl(this);
        moveControl = fishMoveControl;
        setAirSupply(MAX_AIR);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.ARMOR, 2.0)
                // Swimming speed is scaled separately by FishLikeMoveControl.
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SCARED, false);
        this.entityData.define(LAND_MOVING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        waterNavigation = new WaterBoundPathNavigation(this, level);
        groundNavigation = new AmphibiousPathNavigation(this, level);
        return waterNavigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new SeekAirGoal());
        goalSelector.addGoal(1, new FleeThreatGoal());
        goalSelector.addGoal(2, new ReturnUnderwaterGoal());
        goalSelector.addGoal(4, new StayNearAdultGoal());
        goalSelector.addGoal(5, new VariedDepthRandomSwimmingGoal());
        goalSelector.addGoal(6, new OccasionalLandVisitGoal());
        goalSelector.addGoal(7, new MildWaterPreferenceGoal());
        goalSelector.addGoal(8, new RandomStrollGoal(this, 0.40, 80));
    }

    @Override
    public void tick() {
        boolean water = isInWaterOrBubble();
        boolean completingBreathCycle = returnUnderwaterTicks > 0;
        PathNavigation wantedNavigation = (water || completingBreathCycle) && !landExcursion
                ? waterNavigation : groundNavigation;
        MoveControl wantedControl = water || completingBreathCycle ? fishMoveControl : groundMoveControl;
        if (navigation != wantedNavigation) {
            navigation.stop();
            navigation = wantedNavigation;
        }
        moveControl = wantedControl;
        if (!level().isClientSide) {
            if (scaredTicks > 0) {
                scaredTicks--;
                if (scaredTicks == 0 && threat != null && threat.isAlive() && distanceToSqr(threat) < 64.0) {
                    scaredTicks = 20;
                } else if (scaredTicks == 0 || (threat != null && !threat.isAlive())) {
                    threat = null;
                    setScared(false);
                }
            }
            if (returnUnderwaterTicks > 0) returnUnderwaterTicks--;
            if (water && getAirSupply() > MAX_AIR / 2 && isInTopWaterLayer()) {
                surfaceLoiterTicks++;
            } else {
                surfaceLoiterTicks = 0;
            }
            boolean movedOnLand = !water && ((getX() - xo) * (getX() - xo) + (getZ() - zo) * (getZ() - zo) > 1.0E-6
                    || !navigation.isDone());
            entityData.set(LAND_MOVING, movedOnLand);
            setGrowthAge(age + 1);
        }
        super.tick();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker) {
            threat = attacker;
            scaredTicks = SCARED_TICKS;
            setScared(true);
            if (!level().isClientSide) {
                for (BogToad adult : level().getEntitiesOfClass(BogToad.class,
                        getBoundingBox().inflate(12.0), Animal::isAlive)) {
                    adult.setTarget(attacker);
                }
            }
        }
        return hurt;
    }

    public boolean isScared() {
        return entityData.get(SCARED);
    }

    private void setScared(boolean scared) {
        entityData.set(SCARED, scared);
    }

    /** Uses vanilla fish travel physics, including its submerged buoyancy correction. */
    @Override
    public void travel(Vec3 input) {
        if (isEffectiveAi() && isInWater()) {
            moveRelative(0.01F, input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
            if (getTarget() == null) {
                setDeltaMovement(getDeltaMovement().add(0.0, -0.005, 0.0));
            }
        } else {
            super.travel(input);
        }
    }

    @Override
    public int getMaxAirSupply() {
        return MAX_AIR;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag spawnTag) {
        setAirSupply(MAX_AIR);
        return super.finalizeSpawn(level, difficulty, reason, spawnData, spawnTag);
    }

    @Override
    protected int increaseAirSupply(int currentAir) {
        return Math.min(MAX_AIR, currentAir + 20);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isFood(stack)) {
            ItemUtil.consume(stack, 1, player);
            int seconds = AgeableMob.getSpeedUpSecondsWhenFeeding(Math.max(0, GROWTH_TICKS - age));
            setGrowthAge(age + seconds * 20);
            level().addParticle(ParticleTypes.HAPPY_VILLAGER, getRandomX(1.0), getRandomY() + 0.3,
                    getRandomZ(1.0), 0.0, 0.0, 0.0);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void setGrowthAge(int value) {
        age = value;
        if (age >= GROWTH_TICKS) growUp();
    }

    private void growUp() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        // Wild poles now spawn naturally, so don't let a swamp fill up with adults: wait a minute and retry.
        if (serverLevel.getEntitiesOfClass(BogToad.class, getBoundingBox().inflate(24.0)).size() >= ADULT_LOCAL_CAP) {
            age = GROWTH_TICKS - 20 * 60;
            return;
        }
        if (!ForgeEventFactory.canLivingConvert(this, ModEntities.BOG_TOAD.get(), timer -> {})) return;
        BogToad adult = ModEntities.BOG_TOAD.get().create(level());
        if (adult == null) return;
        ForgeEventFactory.onLivingConvert(this, adult);
        adult.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        adult.finalizeSpawn(serverLevel, level().getCurrentDifficultyAt(blockPosition()), MobSpawnType.CONVERSION, null, null);
        adult.setNoAi(isNoAi());
        if (hasCustomName()) {
            adult.setCustomName(getCustomName());
            adult.setCustomNameVisible(isCustomNameVisible());
        }
        if (isPersistenceRequired()) adult.setPersistenceRequired();
        playSound(SoundEvents.TADPOLE_GROW_UP, 0.4F, 0.8F);
        serverLevel.addFreshEntity(adult);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Age", age);
        tag.putInt("ScaredTicks", scaredTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        age = tag.getInt("Age");
        scaredTicks = tag.getInt("ScaredTicks");
        setScared(scaredTicks > 0);
    }

    @Override @Nullable
    public BogPole getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    /** This entity is already the distinct juvenile form; do not apply Animal's model-scaling age. */
    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    public void setBaby(boolean baby) {
    }

    @Override @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TADPOLE_HURT;
    }

    @Override @Nullable
    protected SoundEvent getDeathSound() {
        return SoundEvents.TADPOLE_DEATH;
    }

    @Override
    public double getBoneResetTime() {
        // Bones a clip stops keying ease back to rest over a few ticks instead of snapping there.
        return 6.0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 3, state -> {
            if (isInWaterOrBubble() || (!onGround() && getAirSupply() < MAX_AIR)) {
                if (isScared()) return state.setAndContinue(SWIM_SCARED);
                if (!navigation.isDone() || state.isMoving()) return state.setAndContinue(SWIM);
                return state.setAndContinue(WATER_IDLE);
            }
            boolean landMoving = entityData.get(LAND_MOVING) || state.isMoving();
            if (!landMoving) return state.setAndContinue(IDLE);
            // A frightened Bog Pole runs for water when stranded on land.
            return state.setAndContinue(isScared() ? RUN : WALK);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private final class SeekAirGoal extends Goal {
        private BlockPos air;

        SeekAirGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isInWaterOrBubble() && getAirSupply() < SEEK_AIR_AT;
        }

        @Override
        public boolean canContinueToUse() {
            return getAirSupply() < MAX_AIR - 40;
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public void start() {
            air = findAir();
            move();
        }

        @Override
        public void tick() {
            if (navigation.isDone() || tickCount % 10 == 0) {
                air = findAir();
                move();
            }
            // Finish the short ascent from the final water node to the breathing point.
            if (air != null && isInWaterOrBubble() && getY() < air.getY() + 0.25) {
                double dx = air.getX() + 0.5 - getX();
                double dz = air.getZ() + 0.5 - getZ();
                if (dx * dx + dz * dz < 4.0) {
                    Vec3 motion = getDeltaMovement();
                    double gentleRise = Mth.clamp(motion.y + 0.006, 0.0, 0.022);
                    setDeltaMovement(motion.x, gentleRise, motion.z);
                }
            }
        }
        @Override
        public void stop() {
            if (getAirSupply() >= MAX_AIR - 40) {
                landExcursion = false;
                returnUnderwaterTicks = 200;
            }
        }
        private void move() {
            if (air != null) {
                navigation.moveTo(air.getX() + 0.5, air.getY() + 0.05, air.getZ() + 0.5, 0.85);
            }
        }

        private BlockPos findAir() {
            BlockPos origin = blockPosition();
            BlockPos best = null;
            for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-4, 0, -4), origin.offset(4, 12, 4))) {
                if ((level().getFluidState(candidate).isEmpty() || level().getBlockState(candidate).is(Blocks.BUBBLE_COLUMN))
                        && level().getBlockState(candidate).isPathfindable(level(), candidate, PathComputationType.LAND)) {
                    if (best == null || candidate.distSqr(origin) < best.distSqr(origin)) {
                        best = candidate.immutable();
                    }
                }
            }
            return best;
        }
    }

    private final class ReturnUnderwaterGoal extends Goal {
        @Nullable
        private BlockPos water;

        ReturnUnderwaterGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if ((returnUnderwaterTicks <= 0 && surfaceLoiterTicks < 40) || isScared()) {
                return false;
            }
            water = findDeepWater(blockPosition(), true);
            return water != null;
        }

        @Override
        public void start() {
            moveToDeepWater();
        }

        @Override
        public boolean canContinueToUse() {
            return !isScared() && !isDeepWater(blockPosition())
                    && (water != null || !navigation.isDone());
        }
        @Override
        public void tick() {
            if (navigation.isDone() || tickCount % 20 == 0) {
                water = findDeepWater(blockPosition(), true);
                moveToDeepWater();
            }
        }
        @Override
        public void stop() {
            if (isDeepWater(blockPosition())) {
                returnUnderwaterTicks = 0;
                surfaceLoiterTicks = 0;
            }
        }
        private void moveToDeepWater() {
            if (water != null) {
                navigation.moveTo(water.getX() + 0.5, water.getY() + 0.5, water.getZ() + 0.5, 1.0);
            }
        }
    }

    /** Vanilla fish random swimming across the water column, with a modest submerged bias. */
    private final class VariedDepthRandomSwimmingGoal extends RandomSwimmingGoal {
        VariedDepthRandomSwimmingGoal() {
            super(BogPole.this, NORMAL_SWIM_SPEED, 40);
        }

        @Override
        @Nullable
        protected Vec3 getPosition() {
            Vec3 fallback = null;
            boolean preferDeep = random.nextFloat() < 0.65F;
            for (int attempt = 0; attempt < 12; attempt++) {
                Vec3 candidate = BehaviorUtils
                        .getRandomSwimmablePos(BogPole.this, 10, 7);
                if (candidate == null) {
                    continue;
                }
                if (fallback == null) {
                    fallback = candidate;
                }
                BlockPos pos = BlockPos.containing(candidate);
                if (!preferDeep || isDeepWater(pos)) {
                    return candidate;
                }
            }
            return fallback;
        }
    }

    /** Occasionally lets an undisturbed Bog Pole explore a nearby bank. */
    private final class OccasionalLandVisitGoal extends Goal {
        @Nullable
        private BlockPos destination;
        private int landTicks;

        OccasionalLandVisitGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!isInWaterOrBubble() || isScared() || getAirSupply() < MAX_AIR / 2 || random.nextInt(600) != 0) {
                return false;
            }
            BlockPos origin = blockPosition();
            destination = BlockPos.findClosestMatch(origin, 10, 3, p -> {
                if (!level().getFluidState(p).isEmpty()
                        || !level().getBlockState(p).getCollisionShape(level(), p).isEmpty()) {
                    return false;
                }
                if (level().getBlockState(p.below()).getCollisionShape(level(), p.below()).isEmpty()) {
                    return false;
                }
                Path path = groundNavigation.createPath(p, 0);
                return path != null && path.canReach();
            }).orElse(null);
            return destination != null;
        }

        @Override
        public void start() {
            landExcursion = true;
            landTicks = 60 + random.nextInt(61);
        }

        @Override
        public void tick() {
            if (destination == null) {
                return;
            }
            if (isInWaterOrBubble()) {
                if (navigation.isDone()) {
                    navigation.moveTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, 0.45);
                }
            } else {
                navigation.stop();
                landTicks--;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return destination != null && landTicks > 0 && !isScared() && getAirSupply() >= SEEK_AIR_AT;
        }

        @Override
        public void stop() {
            landExcursion = false;
            destination = null;
        }
    }

    @Nullable
    private BlockPos findDeepWater(BlockPos origin, boolean preferBelow) {
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-6, -7, -6), origin.offset(6, 2, 6))) {
            if (!isDeepWater(p)) continue;
            double score = p.distSqr(origin);
            if (preferBelow && p.getY() > origin.getY() - 2) score += 100.0;
            if (score < bestScore) {
                Path path = waterNavigation.createPath(p, 0);
                if (path != null && path.canReach()) {
                    best = p.immutable();
                    bestScore = score;
                }
            }
        }
        return best;
    }

    private boolean isDeepWater(BlockPos pos) {
        return level().getFluidState(pos).is(FluidTags.WATER)
                && level().getFluidState(pos.above()).is(FluidTags.WATER)
                && level().getFluidState(pos.above(2)).is(FluidTags.WATER);
    }

    private boolean isInTopWaterLayer() {
        BlockPos pos = blockPosition();
        return level().getFluidState(pos).is(FluidTags.WATER)
                && !level().getFluidState(pos.above()).is(FluidTags.WATER);
    }

    private final class FleeThreatGoal extends Goal {
        FleeThreatGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return scaredTicks > 0 && threat != null && threat.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (!navigation.isDone() && tickCount % 20 != 0) {
                return;
            }
            Vec3 destination = isInWaterOrBubble()
                    ? DefaultRandomPos.getPosAway(BogPole.this, 12, 6, threat.position())
                    : nearestWater();
            if (destination == null) {
                destination = DefaultRandomPos.getPosAway(BogPole.this, 10, 5, threat.position());
            }
            if (destination != null) {
                navigation.moveTo(destination.x, destination.y, destination.z,
                        isInWaterOrBubble() ? SCARED_SWIM_SPEED : 1.15);
            }
        }

        @Nullable
        private Vec3 nearestWater() {
            BlockPos origin = blockPosition();
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-10, -2, -10), origin.offset(10, 3, 10))) {
                double distance = p.distSqr(origin);
                if (!level().getFluidState(p).is(FluidTags.WATER) || distance >= bestDistance) {
                    continue;
                }
                Path path = navigation.createPath(p, 0);
                if (path != null && path.canReach()) {
                    best = p.immutable();
                    bestDistance = distance;
                }
            }
            return best == null ? null : Vec3.atCenterOf(best);
        }
    }

    private final class StayNearAdultGoal extends Goal {
        @Nullable
        private BogToad adult;

        StayNearAdultGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isScared() || getAirSupply() < SEEK_AIR_AT || random.nextInt(80) != 0) {
                return false;
            }
            adult = level().getNearestEntity(BogToad.class,
                    TargetingConditions.forNonCombat().range(12.0),
                    BogPole.this, getX(), getY(), getZ(), getBoundingBox().inflate(12.0));
            return adult != null && distanceToSqr(adult) > 64.0;
        }
        @Override
        public void start() {
            if (adult != null) {
                navigation.moveTo(adult, 0.8);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return adult != null && adult.isAlive() && distanceToSqr(adult) > 36.0 && !isScared();
        }
    }

    private final class MildWaterPreferenceGoal extends Goal {
        @Nullable
        private BlockPos water;

        MildWaterPreferenceGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isInWaterOrBubble() || isScared() || random.nextInt(160) != 0) {
                return false;
            }
            water = BlockPos.findClosestMatch(blockPosition(), 8, 2,
                    p -> level().getFluidState(p).is(FluidTags.WATER)).orElse(null);
            return water != null;
        }

        @Override
        public void start() {
            navigation.moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 0.5);
        }

        @Override
        public boolean canContinueToUse() {
            return !isInWaterOrBubble() && !navigation.isDone();
        }
    }

    /** Vanilla AbstractFish steering, usable by an amphibious Animal. */
    private static final class FishLikeMoveControl extends MoveControl {
        private final BogPole pole;

        FishLikeMoveControl(BogPole pole) {
            super(pole);
            this.pole = pole;
        }

        @Override
        public void tick() {
            if (pole.isEyeInFluid(FluidTags.WATER)) {
                pole.setDeltaMovement(pole.getDeltaMovement().add(0.0, 0.005, 0.0));
            }
            if (operation == Operation.MOVE_TO && !pole.getNavigation().isDone()) {
                float speed = (float) (speedModifier
                        * pole.getAttributeValue(Attributes.MOVEMENT_SPEED)
                        * WATER_SPEED_SCALE);
                pole.setSpeed(Mth.lerp(0.125F, pole.getSpeed(), speed));
                double x = wantedX - pole.getX();
                double y = wantedY - pole.getY();
                double z = wantedZ - pole.getZ();
                if (y != 0.0) {
                    // Keep vertical steering independent of the horizontal swim-speed scale.
                    double length = Math.sqrt(x * x + y * y + z * z);
                    float verticalSpeed = (float) (speedModifier
                            * pole.getAttributeValue(Attributes.MOVEMENT_SPEED));
                    pole.setDeltaMovement(pole.getDeltaMovement().add(0.0, verticalSpeed * (y / length) * 0.1, 0.0));
                }
                if (x != 0.0 || z != 0.0) {
                    float yaw = (float) (Mth.atan2(z, x) * 180.0F / Math.PI) - 90.0F;
                    pole.setYRot(rotlerp(pole.getYRot(), yaw, 90.0F));
                    pole.yBodyRot = pole.getYRot();
                }
            } else {
                pole.setSpeed(0.0F);
            }
        }
    }
}
