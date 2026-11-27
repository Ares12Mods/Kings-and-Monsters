package com.kingsandmonsters.entity;

import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import org.joml.Vector3f;
import com.kingsandmonsters.Config;
import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.entity.animation.SynchronizedAnimationController;
import com.kingsandmonsters.entity.animation.CanonicalOneShotState;
import com.kingsandmonsters.world.OgreTrialSpawnerProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class OgreMage extends OgreGrunt {
    private static final int AMBIENT_HUFF_MIN_COOLDOWN_TICKS = 760;
    private static final int AMBIENT_HUFF_RANDOM_COOLDOWN_TICKS = 120;
    private static final int AMBIENT_HUFF_CHANCE = 80;
    private static final int LEVEL_5_XP_REWARD = 55;
    private static final EntityDataAccessor<Long> DATA_MAGE_CAST_START_TICK =
            SynchedEntityData.defineId(OgreMage.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_MAGE_CAST_ID =
            SynchedEntityData.defineId(OgreMage.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_MAGE_RETREATING =
            SynchedEntityData.defineId(OgreMage.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RARE_IDLE = RawAnimation.begin().then("idle2", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    // Slowed with his pace (0.85 at the old 0.288) so his feet don't skate.
    private static final double RUN_ANIMATION_SPEED = 0.72;
    // These identifiers intentionally match the current Blockbench export,
    // including the trailing space in "Bolt ".
    private static final RawAnimation CHANNEL = RawAnimation.begin().thenPlay("Channel animation");
    private static final RawAnimation TOSS_ANIMATION = RawAnimation.begin().thenPlay("toss");
    private static final RawAnimation MAGIC_PUSH_ANIMATION = RawAnimation.begin().thenPlay("Magic push");
    private static final RawAnimation BOLT_ANIMATION = RawAnimation.begin().thenPlay("Bolt ");
    private static final RawAnimation BOGFANG_TRAIL_ANIMATION = RawAnimation.begin().thenPlay("Bogfang_trail");
    private static final RawAnimation SMACK_ANIMATION = RawAnimation.begin().thenPlay("Smack");
    private static final RawAnimation DRINK_ANIMATION = RawAnimation.begin().thenPlay("drink");
    private static final int FOG_COOLDOWN_MIN_TICKS = 220;
    private static final int FOG_COOLDOWN_RANDOM_TICKS = 80;
    private static final int BOLT_COOLDOWN_TICKS = 60;
    private static final double BOLT_RANGE = 20.0;
    private static final int BOGFANG_COOLDOWN_TICKS = 180;
    private static final double BOGFANG_MIN_RANGE = 4.5;
    private static final double BOGFANG_MAX_RANGE = 18.0;
    private static final int BOGFANG_COUNT = 10;
    private static final double BOGFANG_SPACING = 1.35;
    // The Bogfang clip opens with him looking at his wand; the casting sounds wait until it ends.
    private static final int BOGFANG_LOOK_AT_WAND_TICKS = 20;
    // Where the wand tip meets the ground, in front of and to the right of him, as drawn.
    private static final double BOGFANG_START_FORWARD = 2.7;
    private static final double BOGFANG_START_SIDE = 0.65;
    // Where the front of the staff is at the bolt's release (1.0s into the "Bolt " animation, which
    // holds one pose from 0.71s to 1.25s). Worked out from the geo bone chain body > right_arm >
    // staff with GeckoLib's transform order: about 1.8 blocks ahead, 1.0 to the right, 1.1 up.
    private static final double BOLT_RELEASE_HEIGHT_OFFSET = 1.1;
    private static final double BOLT_RELEASE_FORWARD_OFFSET = 1.8;
    private static final double BOLT_RELEASE_SIDE_OFFSET = 0.98;
    private static final int POTION_COOLDOWN_TICKS = 80;
    private static final double POTION_RANGE = 6.0;
    private static final double SUPPORT_RANGE = POTION_RANGE;
    private static final double POTION_RELEASE_SIDE_OFFSET = -1.10;
    private static final double POTION_RELEASE_FORWARD_OFFSET = 0.4;
    private static final double POTION_RELEASE_HEIGHT_OFFSET = 1.28;
    private static final float POTION_THROW_SPEED = 1.15F;
    private static final double POTION_THROW_ARC_PER_BLOCK = 0.18;
    private static final float POTION_THROW_INACCURACY = 2.0F;
    private static final int MAGIC_PUSH_COOLDOWN_TICKS = 260;
    private static final double MAGIC_PUSH_RANGE = 5.75;
    private static final int MAGIC_PUSH_PRESSURE_TICKS = 28;
    private static final double MAGIC_PUSH_CONE_DOT = 0.68;
    // Utility knockback spell — kept light (~1 heart vs diamond) since its value is the push, not damage.
    private static final float MAGIC_PUSH_DAMAGE = OgreDamage.raw(OgreDamage.Tier.MAGE, OgreDamage.Blow.LIGHT);
    private static final double MAGIC_PUSH_KNOCKBACK_STRENGTH = 4.675;
    private static final double MAGIC_PUSH_VERTICAL_LIFT = 1.35;
    private static final double SMACK_RANGE = 3.35;
    // Close-range melee fallback. Kept below the Mage's magic/control kit in raw damage — his
    // threat should come from Bogfume Bolt/poison, not this fallback swing.
    private static final float SMACK_DAMAGE = OgreDamage.raw(OgreDamage.Tier.MAGE, OgreDamage.Blow.LIGHT);
    private static final double SMACK_KNOCKBACK_STRENGTH = 0.55;
    private static final int SMACK_POISON_TICKS = 80;
    // Extra cooldown beyond the cast's own animation time, since the smack hits hard enough now
    // that letting it chain rapidly (e.g. repeatedly during the post-push melee window) is too fast.
    private static final int SMACK_COOLDOWN_TICKS = 25;
    private static final float LOW_HEALTH_RETREAT_RATIO = 0.35F;
    private static final double RETREAT_RANGE = 7.0;
    private static final double RETREAT_DISTANCE = 7.0;
    // Base movement remains 0.189 for calm wandering. This navigation multiplier produces
    // an old man's 0.229 pace while the mage is in combat: 3 blocks a second,
    // since ground speed goes with the square of the speed setting (2.86 x speed squared blocks a tick).
    private static final double COMBAT_SPEED_MODIFIER = 0.229 / 0.189;
    private static final double RETREAT_SPEED = COMBAT_SPEED_MODIFIER;
    private static final int RETREAT_REPATH_TICKS = 24;
    // Fraction of the remaining turn angle closed per tick (proportional lerp, see
    // rotlerpTurn/turnToward) — not a degrees-per-tick cap. Matches OgreGruntMoveControl's
    // 0.12-0.35 walking/running range for the same gentle, natural-feeling deceleration.
    private static final float CAST_TURN_SPEED = 0.15F;
    private static final float MAGIC_PUSH_TRACK_TURN_SPEED = 0.6F;
    private static final double CAST_RANGE = 18.0;
    private static final int POTION_PROJECTILE_SPAWN_DELAY_TICKS = 2;
    private static final int POISON_FOG_CIRCLE_DELAY_TICKS = 4;
    private static final float POISON_FOG_CHANNEL_SOUND_VOLUME = 2.5F;
    private static final int HEAL_DRINK_COOLDOWN_TICKS = 1200;
    private static final int HEAL_DRINK_SOUND_START_TICKS = 10;
    private static final int HEAL_DRINK_SOUND_INTERVAL_TICKS = 4;
    private static final float HEAL_DRINK_MAX_HEALTH_FRACTION = 0.25F;
    private static final String ROYAL_SQUAD_GRUNTS_SPAWNED_TAG = "KingsAndMonstersRoyalSquadGruntsSpawned";
    private static final String ROYAL_SQUAD_ARCHERS_SPAWNED_TAG = "KingsAndMonstersRoyalSquadArchersSpawned";
    private static final int ROYAL_SQUAD_GRUNT_COUNT = 2;
    private static final int ROYAL_SQUAD_ARCHER_COUNT = 1;

    private int idleAnimationTicks;
    private int rareIdleTicks;
    private int castAnimationTicks;
    private int activeCastTicks;
    private int castReleaseTicks;
    private int healDrinkCooldownTicks;
    private int ambientHuffCooldownTicks = AMBIENT_HUFF_MIN_COOLDOWN_TICKS;
    private int lastAnimationTick = -1;
    private long lastMovingAnimationTick = Long.MIN_VALUE / 2;
    private static final int MOVEMENT_ANIMATION_GRACE_TICKS = 4;
    private boolean castReleased;
    private boolean startingCastAnimation;
    private RawAnimation activeCastAnimation = CHANNEL;
    private MageCast pendingCast;
    private MageCast queuedPotionThrow;
    private int queuedPotionThrowTicks;
    private PoisonFogCloud pendingPoisonCloud;
    private final List<FangBurst> fangBursts = new ArrayList<>();
    private SynchronizedAnimationController<OgreMage> castController;
    private final CanonicalOneShotState visualOneShot = new CanonicalOneShotState();
    private boolean royalTrialSquadLeader;
    private int royalSquadGruntsSpawned;
    private int royalSquadArchersSpawned;

    public OgreMage(EntityType<? extends OgreMage> type, Level level) {
        super(type, level);
        ensureCaptainTitle();
        avoidWaterPathfinding();
        xpReward = LEVEL_5_XP_REWARD;
    }

    private void ensureCaptainTitle() {
        if (!hasCustomName()) {
            setCustomName(Component.translatable("title.kingsandmonsters.mage_captain"));
        }
        setCustomNameVisible(Config.SHOW_CAPTAIN_NAMETAGS.get());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isDeadOrDying()) {
            // A cast that has not gone off, a queued potion throw and the fang bursts all stop with him.
            pendingCast = null;
            castAnimationTicks = 0;
            fangBursts.clear();
            return;
        }
        if (!level().isClientSide) {
            tickAmbientHuff();
            deployRoyalTrialSquadIfNeeded();
            healDrinkCooldownTicks = Math.max(healDrinkCooldownTicks - 1, 0);
            tickQueuedPotionThrow();
            tickFangBursts();
            tickPendingCast();
        }
    }

    /** A fang's swamp-magic puff, held back until the fang itself breaks the ground. */
    private static final class FangBurst {
        private final Vec3 at;
        private int ticksLeft;

        private FangBurst(Vec3 at, int ticksLeft) {
            this.at = at;
            this.ticksLeft = ticksLeft;
        }
    }

    private void tickFangBursts() {
        if (fangBursts.isEmpty() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        fangBursts.removeIf(burst -> {
            if (--burst.ticksLeft > 0) {
                return false;
            }
            sendFangBurst(serverLevel, burst.at);
            return true;
        });
    }

    /** A ring of green glow on the ground where a fang is about to come up. */
    private static void sendFangRune(ServerLevel level, Vec3 at) {
        DustParticleOptions glow = new DustParticleOptions(new Vector3f(0.1f, 0.8f, 0.2f), 1.2f);
        level.sendParticles(glow, at.x, at.y + 0.05, at.z, 8, 0.45, 0.02, 0.45, 0.0);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, at.x, at.y + 0.2, at.z, 3, 0.4, 0.15, 0.4, 0.0);
    }

    /** Bog muck and green sparks thrown up as the fang breaks the ground. */
    private static void sendFangBurst(ServerLevel level, Vec3 at) {
        DustParticleOptions glow = new DustParticleOptions(new Vector3f(0.12f, 0.9f, 0.2f), 1.4f);
        level.sendParticles(glow, at.x, at.y + 0.6, at.z, 14, 0.28, 0.5, 0.28, 0.04);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                at.x, at.y + 0.15, at.z, 12, 0.3, 0.1, 0.3, 0.12);
        level.sendParticles(ParticleTypes.WITCH, at.x, at.y + 0.5, at.z, 6, 0.25, 0.4, 0.25, 0.05);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, at.x, at.y + 0.8, at.z, 5, 0.3, 0.5, 0.3, 0.0);
    }

    private void tickAmbientHuff() {
        if (ambientHuffCooldownTicks > 0) {
            ambientHuffCooldownTicks--;
            return;
        }
        if (getTarget() != null || isAggressive() || pendingCast != null || castAnimationTicks > 0
                || getRandom().nextInt(AMBIENT_HUFF_CHANCE) != 0) {
            return;
        }

        level().playSound(null, getX(), getY(), getZ(),
                ModSoundEvents.OGRE_MAGE_HUFF.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
        ambientHuffCooldownTicks = AMBIENT_HUFF_MIN_COOLDOWN_TICKS
                + getRandom().nextInt(AMBIENT_HUFF_RANDOM_COOLDOWN_TICKS + 1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(OgreTrialSpawnerProcessor.ROYAL_MAGE_SQUAD_TAG, royalTrialSquadLeader);
        tag.putInt(ROYAL_SQUAD_GRUNTS_SPAWNED_TAG, royalSquadGruntsSpawned);
        tag.putInt(ROYAL_SQUAD_ARCHERS_SPAWNED_TAG, royalSquadArchersSpawned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ensureCaptainTitle();
        royalTrialSquadLeader = tag.getBoolean(OgreTrialSpawnerProcessor.ROYAL_MAGE_SQUAD_TAG);
        royalSquadGruntsSpawned = Mth.clamp(
                tag.getInt(ROYAL_SQUAD_GRUNTS_SPAWNED_TAG), 0, ROYAL_SQUAD_GRUNT_COUNT);
        royalSquadArchersSpawned = Mth.clamp(
                tag.getInt(ROYAL_SQUAD_ARCHERS_SPAWNED_TAG), 0, ROYAL_SQUAD_ARCHER_COUNT);
    }

    @Override

    public float maxUpStep() {

        return 1.0F;

    }


    // ------------------------------------------------------------------ //
    //  Attributes                                                        //
    // ------------------------------------------------------------------ //

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.189)
                .add(Attributes.FOLLOW_RANGE, 30.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.ATTACK_KNOCKBACK, 0.45);
                // See OgreGrunt#createAttributes — default 0 caps water acceleration at a tiny fixed
                // baseline regardless of requested speed, which would make applyShallowWaterSpeedFactor's
                // 50% reduction meaningless since he's already moving slower than that.;
    }

    private void deployRoyalTrialSquadIfNeeded() {
        if (!royalTrialSquadLeader
                || royalSquadGruntsSpawned >= ROYAL_SQUAD_GRUNT_COUNT
                && royalSquadArchersSpawned >= ROYAL_SQUAD_ARCHER_COUNT
                || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // The spawner search walks about 1,100 blocks, so look once a second rather than every tick.
        if ((tickCount + getId()) % 20 != 0) {
            return;
        }
        BlockPos owner = findOwningTrialSpawner(serverLevel);
        if (owner == null) {
            return;
        }

        while (royalSquadGruntsSpawned < ROYAL_SQUAD_GRUNT_COUNT) {
            if (!spawnTrackedSquadMember(serverLevel, owner, ModEntities.OGRE_GRUNT.get())) {
                break;
            }
            royalSquadGruntsSpawned++;
        }
        while (royalSquadArchersSpawned < ROYAL_SQUAD_ARCHER_COUNT) {
            if (!spawnTrackedSquadMember(serverLevel, owner, ModEntities.OGRE_ARCHER.get())) {
                break;
            }
            royalSquadArchersSpawned++;
        }
    }

    public boolean isRoyalTrialSquadLeader() {
        return royalTrialSquadLeader;
    }

    /** The spawner block this squad leader came out of: any spawner within a few blocks of where it stands. */
    private BlockPos findOwningTrialSpawner(ServerLevel level) {
        BlockPos center = blockPosition();
        for (BlockPos candidate : BlockPos.betweenClosed(
                center.offset(-6, -3, -6), center.offset(6, 3, 6))) {
            if (level.getBlockEntity(candidate) instanceof SpawnerBlockEntity) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private boolean spawnTrackedSquadMember(
            ServerLevel level,
            BlockPos spawnerPos,
            EntityType<? extends OgreGrunt> entityType) {
        OgreGrunt member = entityType.create(level);
        if (member == null) {
            return false;
        }

        for (int attempt = 0; attempt < 20; attempt++) {
            double x = spawnerPos.getX() + 0.5 + (getRandom().nextDouble() - getRandom().nextDouble()) * 4.5;
            double y = spawnerPos.getY() + getRandom().nextInt(3) - 1;
            double z = spawnerPos.getZ() + 0.5 + (getRandom().nextDouble() - getRandom().nextDouble()) * 4.5;
            member.moveTo(x, y, z, getRandom().nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(member) || !member.checkSpawnObstruction(level)) {
                continue;
            }

            member.finalizeSpawn(level, level.getCurrentDifficultyAt(member.blockPosition()),
                    MobSpawnType.SPAWNER, null, null);
            member.setPersistenceRequired();
            return level.addFreshEntity(member);
        }
        return false;
    }

    @Override
    public void applyConfiguredCombatAttributes(boolean healToMax) {
        if (!Config.isLoaded()) {
            return;
        }

        setAttributeBaseValue(Attributes.MAX_HEALTH, Config.OGRE_MAGE_MAX_HEALTH.get());
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, Config.OGRE_MAGE_ATTACK_DAMAGE.get());
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, Config.OGRE_MAGE_MOVEMENT_SPEED.get());
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, Config.OGRE_MAGE_FOLLOW_RANGE.get());
        setAttributeBaseValue(Attributes.ARMOR, Config.OGRE_MAGE_ARMOR.get());
        applyConfiguredHealth(healToMax);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.OGRE_MAGE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.OGRE_MAGE_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.5F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MAGE_CAST_START_TICK, 0L);
        this.entityData.define(DATA_MAGE_CAST_ID, 0);
        this.entityData.define(DATA_MAGE_RETREATING, false);
    }

    // ------------------------------------------------------------------ //
    //  AI goals                                                          //
    // ------------------------------------------------------------------ //

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        registerErrandTalkGoal();
        registerBogToadWarinessGoal();
        goalSelector.addGoal(1, new OgreMageCombatGoal(this, COMBAT_SPEED_MODIFIER));
        goalSelector.addGoal(4, new GuardSuperiorGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        registerTerritorialTargetGoal();
        registerArmyGoals();
    }

    // ------------------------------------------------------------------ //
    //  Animation controllers                                             //
    // ------------------------------------------------------------------ //

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        castController = new SynchronizedAnimationController<>(this, "Mage Movement", 5, this::animateMage)
                .withFootfalls(FOOT_CONTACTS, this::playMageFootfall);
        controllers.add(castController);
    }

    // His robe hides his legs, so each step lands where his body bobs lowest: 0.5s and 1.5s into the
    // 2s walk, 0.29s and 0.88s into the 1.18s run (animation ticks).
    private static final Map<String, double[]> FOOT_CONTACTS = Map.of(
            "walk", new double[]{10.0, 30.0},
            "run", new double[]{5.88, 17.65});

    @Override
    protected boolean usesAnimatedFootsteps() {
        return true;
    }

    /** Client-side: a soft shuffle of an old man's feet, with the hem of his robe dragging after. */
    private void playMageFootfall() {
        if (!onGround() || isInWater() || isSilent()) {
            return;
        }
        float variation = getRandom().nextFloat() * 0.1F;
        BlockPos ground = getOnPos();
        SoundType surface = level().getBlockState(ground).getSoundType(level(), ground, this);
        level().playLocalSound(getX(), getY(), getZ(), surface.getStepSound(), SoundSource.HOSTILE,
                0.25F, 0.85F + variation, false);
        level().playLocalSound(getX(), getY(), getZ(), SoundType.WOOL.getStepSound(), SoundSource.HOSTILE,
                0.4F, 0.75F + variation, false);
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.HOSTILE,
                0.12F, 0.7F + variation, false);
    }

    public ItemStack getVisiblePotionStack() {
        return getItemBySlot(EquipmentSlot.OFFHAND);
    }

    private void beginPoisonFogCast(LivingEntity target) {
        faceToward(target);
        beginCast(new MageCast(CastType.POISON_FOG, target));
        warnTargetedPlayer(target);
        // The warning circle is delayed slightly so it begins after the channel animation
        // is visibly underway and its warning phase ends closer to the animation's finish.
    }

    private void warnTargetedPlayer(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.kingsandmonsters.ogre_shaman_targeting"), true);
        }
    }

    private void beginMagicPush(LivingEntity target) {
        faceToward(target);
        beginCast(new MageCast(CastType.MAGIC_PUSH, target));
    }

    private void beginBoltCast(LivingEntity target) {
        faceToward(target);
        beginCast(new MageCast(CastType.BOLT, target));
    }

    private void beginBogfangTrail(LivingEntity target) {
        faceToward(target);
        beginCast(new MageCast(CastType.BOGFANG_TRAIL, target));
    }

    private void beginPotionThrow(MageCast cast) {
        faceToward(cast.target());
        beginCast(cast);
    }

    private void beginSmack(LivingEntity target) {
        faceToward(target);
        beginCast(new MageCast(CastType.SMACK, target));
    }

    private void beginHealingDrink() {
        ItemStack healingPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING);
        beginCast(new MageCast(CastType.HEAL_DRINK, this, healingPotion));
        healDrinkCooldownTicks = HEAL_DRINK_COOLDOWN_TICKS;
    }

    private void beginCast(MageCast cast) {
        pendingCast = cast;
        activeCastTicks = cast.type().animationTicks;
        castReleaseTicks = cast.type().releaseTicks;
        castReleased = false;
        setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        if (!cast.stack().isEmpty()) {
            setItemSlot(EquipmentSlot.OFFHAND, singlePotionStack(cast.stack()));
        }
        playCastAnimation(cast.type());
    }

    private void tickPendingCast() {
        if (pendingCast == null) {
            return;
        }

        if (pendingCast.type() != CastType.HEAL_DRINK) {
            float turnSpeed = pendingCast.type() == CastType.MAGIC_PUSH && !castReleased
                    ? MAGIC_PUSH_TRACK_TURN_SPEED
                    : CAST_TURN_SPEED;
            turnToward(pendingCast.target(), turnSpeed);
        }
        activeCastTicks--;
        int castElapsed = pendingCast.type().animationTicks - activeCastTicks;
        int castSoundTick = pendingCast.type() == CastType.BOGFANG_TRAIL
                ? BOGFANG_LOOK_AT_WAND_TICKS + POISON_FOG_CIRCLE_DELAY_TICKS
                : POISON_FOG_CIRCLE_DELAY_TICKS;
        if ((pendingCast.type() == CastType.POISON_FOG || pendingCast.type() == CastType.BOGFANG_TRAIL
                || pendingCast.type() == CastType.MAGIC_PUSH) && castElapsed == castSoundTick) {
            if (pendingCast.type() == CastType.POISON_FOG) {
                spawnPendingPoisonCloud(pendingCast.target());
            }
            level().playSound(
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    ModSoundEvents.OGRE_MAGE_CAST_PREPARE.get(),
                    SoundSource.HOSTILE,
                    POISON_FOG_CHANNEL_SOUND_VOLUME,
                    1.0F);
        }
        if (pendingCast.type() == CastType.HEAL_DRINK
                && castElapsed >= HEAL_DRINK_SOUND_START_TICKS
                && castElapsed < pendingCast.type().releaseTicks
                && (castElapsed - HEAL_DRINK_SOUND_START_TICKS) % HEAL_DRINK_SOUND_INTERVAL_TICKS == 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_DRINK, SoundSource.HOSTILE, 0.8F, 0.9F);
        }
        if (!castReleased && --castReleaseTicks <= 0) {
            releasePendingCast();
        }

        if (activeCastTicks <= 0) {
            pendingCast = null;
            pendingPoisonCloud = null;
            activeCastTicks = 0;
            castReleaseTicks = 0;
            castReleased = false;
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            clearSyncedCastAnimation();
        }
    }

    private void releasePendingCast() {
        if (pendingCast == null || castReleased) {
            return;
        }

        castReleased = true;
        if (pendingCast.type() == CastType.POTION) {
            // Both ally buffs and hostile debuffs use this identical release path:
            // hide the held bottle at the authored release pose, then allow two
            // synchronization ticks before spawning the thrown bottle from that hand.
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            queuedPotionThrow = pendingCast;
            queuedPotionThrowTicks = POTION_PROJECTILE_SPAWN_DELAY_TICKS;
        } else if (pendingCast.type() == CastType.BOLT) {
            shootBogfumeBolt();
        } else if (pendingCast.type() == CastType.BOGFANG_TRAIL) {
            spawnBogfangTrail();
        } else if (pendingCast.type() == CastType.SMACK) {
            applySmackImpact();
        } else if (pendingCast.type() == CastType.MAGIC_PUSH) {
            applyMagicPushImpact();
        } else if (pendingCast.type() == CastType.HEAL_DRINK) {
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            heal(getMaxHealth() * HEAL_DRINK_MAX_HEALTH_FRACTION);
        }
    }

    private void tickQueuedPotionThrow() {
        if (queuedPotionThrow == null || --queuedPotionThrowTicks > 0) {
            return;
        }

        throwPotion(queuedPotionThrow);
        queuedPotionThrow = null;
        queuedPotionThrowTicks = 0;
    }

    private void throwPotion(MageCast cast) {
        if (cast == null || cast.stack().isEmpty() || level().isClientSide || !cast.target().isAlive()) {
            return;
        }

        Vec3 releasePos = getPotionReleasePosition();
        ThrownPotion potion = new ThrownPotion(level(), this);
        potion.setItem(singlePotionStack(cast.stack()));
        potion.setPos(releasePos.x, releasePos.y, releasePos.z);

        double xOffset = cast.target().getX() - releasePos.x;
        double zOffset = cast.target().getZ() - releasePos.z;
        double horizontalDistance = Math.sqrt(xOffset * xOffset + zOffset * zOffset);
        double yOffset = cast.target().getY(0.4) - releasePos.y;
        potion.shoot(
                xOffset,
                yOffset + horizontalDistance * POTION_THROW_ARC_PER_BLOCK,
                zOffset,
                POTION_THROW_SPEED,
                POTION_THROW_INACCURACY);
        level().addFreshEntity(potion);
    }

    private Vec3 getPotionReleasePosition() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, yBodyRot);
        Vec3 left = new Vec3(-forward.z(), 0.0, forward.x());
        return position()
                .add(left.scale(POTION_RELEASE_SIDE_OFFSET))
                .add(forward.scale(POTION_RELEASE_FORWARD_OFFSET))
                .add(0.0, POTION_RELEASE_HEIGHT_OFFSET, 0.0);
    }

    private Optional<MageCast> choosePotionThrowInRange(LivingEntity hostileTarget) {
        Optional<MageCast> supportCast = chooseSupportPotion();
        if (supportCast.isPresent()) {
            return supportCast;
        }

        if (distanceToSqr(hostileTarget) <= POTION_RANGE * POTION_RANGE) {
            return Optional.of(chooseDebuffPotion(hostileTarget));
        }

        return Optional.empty();
    }

    private Optional<MageCast> chooseSupportPotion() {
        List<MageCast> supportOptions = new ArrayList<>();
        List<OgreGrunt> allies = level().getEntitiesOfClass(
                OgreGrunt.class,
                getBoundingBox().inflate(SUPPORT_RANGE),
                ogre -> ogre != this && ogre.isAlive());

        for (OgreGrunt ally : allies) {
            if (ally.isOnFire() && !ally.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                supportOptions.add(new MageCast(CastType.POTION, ally, createCustomSplashPotion(MobEffects.FIRE_RESISTANCE, 600, 0)));
            }

            if (!ally.hasEffect(MobEffects.DAMAGE_BOOST)) {
                supportOptions.add(new MageCast(CastType.POTION, ally, createCustomSplashPotion(MobEffects.DAMAGE_BOOST, 600, 0)));
            }

            if (!(ally instanceof OgreLord) && !ally.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                supportOptions.add(new MageCast(CastType.POTION, ally, createCustomSplashPotion(MobEffects.MOVEMENT_SPEED, 600, 0)));
            }

            // Regeneration is a rarer heal-up, only considered for allies who are actually hurt and
            // don't already have it — the low roll chance (vs. the other buffs above, which are
            // added unconditionally when eligible) is what keeps it uncommon.
            if (ally.getHealth() < ally.getMaxHealth() * 0.7F
                    && !ally.hasEffect(MobEffects.REGENERATION)
                    && getRandom().nextInt(6) == 0) {
                supportOptions.add(new MageCast(CastType.POTION, ally, createCustomSplashPotion(MobEffects.REGENERATION, 200, 1)));
            }
        }

        if (supportOptions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(supportOptions.get(getRandom().nextInt(supportOptions.size())));
    }

    private MageCast chooseDebuffPotion(LivingEntity target) {
        ItemStack potion;
        if (!target.hasEffect(MobEffects.WEAKNESS) && getRandom().nextInt(10) == 0) {
            potion = createCustomSplashPotion(MobEffects.WEAKNESS, 300, 0);
        } else {
            potion = getRandom().nextBoolean()
                    ? createCustomSplashPotion(MobEffects.POISON, 200, 1)
                    : createCustomSplashPotion(MobEffects.MOVEMENT_SLOWDOWN, 30 * 20, 0);
        }
        return new MageCast(CastType.POTION, target, potion);
    }

    private ItemStack createCustomSplashPotion(MobEffect effect, int durationTicks, int amplifier) {
        ItemStack stack = new ItemStack(Items.SPLASH_POTION);
        PotionUtils.setCustomEffects(stack,
                List.of(new MobEffectInstance(effect, durationTicks, amplifier)));
        return stack;
    }

    private ItemStack singlePotionStack(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private void applySmackImpact() {
        MageCast cast = pendingCast;
        if (cast == null || level().isClientSide || !cast.target().isAlive()) {
            return;
        }

        // The staff whooshes through the air whether or not it connects.
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.1F, 0.7F);

        LivingEntity target = cast.target();
        if (distanceToSqr(target) > SMACK_RANGE * SMACK_RANGE || !getSensing().hasLineOfSight(target)) {
            return;
        }

        target.hurt(damageSources().mobAttack(this), SMACK_DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, SMACK_POISON_TICKS, 0), this);
        target.knockback(SMACK_KNOCKBACK_STRENGTH, getX() - target.getX(), getZ() - target.getZ());
        // A hard crack of wood, with the bog's wet sting behind it.
        level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.HOSTILE, 1.2F, 0.7F);
        level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundType.WOOD.getBreakSound(),
                SoundSource.HOSTILE, 1.0F, 0.8F);
        level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SLIME_SQUISH,
                SoundSource.HOSTILE, 0.7F, 0.6F);
    }

    private void shootBogfumeBolt() {
        MageCast cast = pendingCast;
        if (cast == null || level().isClientSide || !cast.target().isAlive()) {
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 origin = getBoltReleasePosition();
        if (!hasClearBoltToTarget(cast.target())) {
            return;
        }

        level().playSound(null, getX(), getY(), getZ(), ModSoundEvents.OGRE_MAGE_CAST_RELEASE.get(),
                SoundSource.HOSTILE, 2.0F, 1.0F);
        DustParticleOptions greenBoltRelease =
                new DustParticleOptions(new Vector3f(0.12F, 0.9F, 0.18F), 1.25F);
        serverLevel.sendParticles(greenBoltRelease, origin.x, origin.y, origin.z, 10, 0.1, 0.1, 0.1, 0.02);
        BogfumeBolt.shoot(serverLevel, this, cast.target(), origin);
    }

    private void spawnBogfangTrail() {
        MageCast cast = pendingCast;
        if (cast == null || level().isClientSide || !cast.target().isAlive()) return;

        // The trail starts where the wand lands and runs from there toward the target.
        Vec3 forward = Vec3.directionFromRotation(0.0F, yBodyRot).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 start = position()
                .add(forward.scale(BOGFANG_START_FORWARD))
                .add(right.scale(BOGFANG_START_SIDE));
        Vec3 towardTarget = cast.target().position().subtract(start);
        Vec3 direction = new Vec3(towardTarget.x, 0.0, towardTarget.z);
        if (direction.lengthSqr() < 1.0E-4) {
            direction = forward;
        }
        direction = direction.normalize();

        float yaw = (float) Mth.atan2(direction.z, direction.x) - Mth.HALF_PI;
        int minY = Mth.floor(Math.min(getY(), cast.target().getY())) - 3;
        int maxY = Mth.floor(Math.max(getY(), cast.target().getY())) + 2;
        for (int index = 0; index < BOGFANG_COUNT; index++) {
            double distance = index * BOGFANG_SPACING;
            spawnBogfangAt(
                    start.x + direction.x * distance,
                    start.z + direction.z * distance,
                    minY,
                    maxY,
                    yaw,
                    index * 2);
        }
        level().playSound(null, getX(), getY(), getZ(), ModSoundEvents.OGRE_MAGE_CAST_RELEASE.get(),
                SoundSource.HOSTILE, 2.0F, 1.0F);
    }

    private void spawnBogfangAt(
            double x, double z, int minY, int maxY, float yaw, int warmupDelay) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), maxY, Mth.floor(z));
        while (pos.getY() >= minY) {
            BlockPos below = pos.below();
            BlockState ground = level().getBlockState(below);
            if (ground.isFaceSturdy(level(), below, Direction.UP)) {
                double surfaceOffset = 0.0;
                BlockState atSurface = level().getBlockState(pos);
                if (!atSurface.getCollisionShape(level(), pos).isEmpty()) {
                    surfaceOffset = atSurface.getCollisionShape(level(), pos).max(Direction.Axis.Y);
                }
                level().addFreshEntity(new BogFangs(
                        level(), x, pos.getY() + surfaceOffset, z, yaw, warmupDelay, this));
                if (level() instanceof ServerLevel serverLevel) {
                    Vec3 at = new Vec3(x, pos.getY() + surfaceOffset, z);
                    sendFangRune(serverLevel, at);
                    // The vanilla fang pops up a couple of ticks after its warm-up ends.
                    fangBursts.add(new FangBurst(at, warmupDelay + 2));
                }
                return;
            }
            pos.move(Direction.DOWN);
        }
    }

    private Vec3 getBoltReleasePosition() {
        // The model is drawn with the body rotation, which trails the head while he turns to aim.
        Vec3 forward = Vec3.directionFromRotation(0.0F, yBodyRot).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x).normalize();
        return position()
                .add(0.0, BOLT_RELEASE_HEIGHT_OFFSET, 0.0)
                .add(forward.scale(BOLT_RELEASE_FORWARD_OFFSET))
                .add(right.scale(BOLT_RELEASE_SIDE_OFFSET));
    }

    private boolean hasClearBoltToTarget(LivingEntity target) {
        Vec3 start = getBoltReleasePosition();
        Vec3 end = target.getEyePosition();
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

            var allyHit = ally.getBoundingBox().inflate(0.45).clip(start, end);
            if (allyHit.isPresent() && start.distanceToSqr(allyHit.get()) < targetDistanceSqr) {
                return false;
            }
        }

        return true;
    }

    private void applyMagicPushImpact() {
        MageCast cast = pendingCast;
        if (cast == null || level().isClientSide) {
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 origin = position().add(forward.scale(1.2)).add(0.0, 1.0, 0.0);
        spawnMagicPushParticles(serverLevel, origin);
        // His cast cry, a burst of wind and a low surge of magic make up the blast.
        level().playSound(null, getX(), getY(), getZ(), ModSoundEvents.OGRE_MAGE_CAST_RELEASE.get(),
                SoundSource.HOSTILE, 2.0F, 1.0F);
        level().playSound(null, origin.x, origin.y, origin.z, SoundEvents.FIRECHARGE_USE,
                SoundSource.HOSTILE, 1.6F, 0.75F);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.2F, 0.7F);

        AABB area = getBoundingBox().inflate(MAGIC_PUSH_RANGE, 2.5, MAGIC_PUSH_RANGE);
        List<LivingEntity> targets = level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> target != this && target.isAlive() && !(target instanceof OgreGrunt)
                        && !(isArmyAlly() && spareByArmy(target)));

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(position());
            Vec3 horizontalToTarget = new Vec3(toTarget.x, 0.0, toTarget.z);
            double horizontalDistanceSqr = horizontalToTarget.lengthSqr();
            if (horizontalDistanceSqr < 0.0001 || horizontalDistanceSqr > MAGIC_PUSH_RANGE * MAGIC_PUSH_RANGE) {
                continue;
            }

            Vec3 direction = horizontalToTarget.normalize();
            if (direction.dot(forward) < MAGIC_PUSH_CONE_DOT || !getSensing().hasLineOfSight(target)) {
                continue;
            }

            // Harder up close, weaker toward the edge of the cone, like every other area attack.
            float falloff = OgreDamage.falloff(Math.sqrt(horizontalDistanceSqr), MAGIC_PUSH_RANGE, OgreDamage.ARC_EDGE_SHARE);
            target.hurt(damageSources().mobAttack(this), MAGIC_PUSH_DAMAGE * falloff);
            Vec3 movement = target.getDeltaMovement();
            target.setDeltaMovement(
                    direction.x * MAGIC_PUSH_KNOCKBACK_STRENGTH,
                    Math.max(movement.y, MAGIC_PUSH_VERTICAL_LIFT),
                    direction.z * MAGIC_PUSH_KNOCKBACK_STRENGTH);
            target.hurtMarked = true;
        }
    }

    private void spawnMagicPushParticles(ServerLevel serverLevel, Vec3 origin) {
        var greenDust = new DustParticleOptions(new Vector3f(0.1f, 0.85f, 0.15f), 1.6f);
        serverLevel.sendParticles(greenDust, origin.x, origin.y, origin.z, 32, 0.42, 0.42, 0.42, 0.22);
        serverLevel.sendParticles(ParticleTypes.WITCH, origin.x, origin.y, origin.z, 30, 0.38, 0.38, 0.38, 0.16);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, origin.x, origin.y, origin.z, 3, 0.14, 0.14, 0.14, 0.0);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, origin.x, origin.y, origin.z, 10, 0.32, 0.38, 0.32, 0.02);
        serverLevel.sendParticles(ParticleTypes.SMOKE, origin.x, origin.y, origin.z, 22, 0.28, 0.28, 0.28, 0.05);
        serverLevel.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y, origin.z, 14, 0.5, 0.38, 0.5, 0.14);
        serverLevel.sendParticles(ParticleTypes.POOF, origin.x, origin.y, origin.z, 14, 0.24, 0.22, 0.24, 0.08);
    }

    private void spawnPendingPoisonCloud(LivingEntity target) {
        if (level() instanceof ServerLevel serverLevel) {
            pendingPoisonCloud = PoisonFogCloud.spawn(serverLevel, target.getX(), target.getY(), target.getZ());
            pendingPoisonCloud.setSparesFriends(isArmyAlly());
        }
    }

    private boolean isCasting() {
        return pendingCast != null;
    }

    private void turnToward(LivingEntity target, float turnSpeed) {
        if (target == null || !target.isAlive()) {
            return;
        }

        float targetYaw = getYawToward(target);
        // Mth.rotateIfNecessary turns a fixed number of degrees per tick with no easing, which looks
        // jerky and mechanical against a side-dodging player. Use the same proportional lerp as
        // OgreGruntMoveControl: the correction decays as it closes in and small jitter in the
        // target position gets damped instead of chased 1:1.
        setYRot(rotlerpTurn(getYRot(), targetYaw, turnSpeed));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    /**
     * Proportional turn easing shared by every Mage body-turn call — see the comment in
     * {@link #turnToward}. {@code lerpFraction} is the share of the remaining angle closed each
     * tick (0-1), not a degrees-per-tick cap.
     */
    private static float rotlerpTurn(float currentAngle, float targetAngle, float lerpFraction) {
        float remaining = Mth.wrapDegrees(targetAngle - currentAngle);
        return currentAngle + remaining * lerpFraction;
    }

    private void faceToward(LivingEntity target) {
        turnToward(target, CAST_TURN_SPEED);
    }

    private float getYawToward(LivingEntity target) {
        double xOffset = target.getX() - getX();
        double zOffset = target.getZ() - getZ();
        return (float) (Mth.atan2(zOffset, xOffset) * Mth.RAD_TO_DEG) - 90.0F;
    }

    public void playCastAnimation(CastType type) {
        if (!level().isClientSide) {
            entityData.set(DATA_MAGE_CAST_ID, type.id);
            entityData.set(DATA_MAGE_CAST_START_TICK, level().getGameTime());
        }

        activeCastAnimation = type.animation;
        castAnimationTicks = type.animationTicks;
        startingCastAnimation = true;
    }

    /** Synced, render-only exclusion for Magic Push. */
    public boolean isProceduralHeadTrackingSuppressed() {
        int castId = entityData.get(DATA_MAGE_CAST_ID);
        return castId == CastType.MAGIC_PUSH.id || castId == CastType.BOGFANG_TRAIL.id;
    }

    /**
     * Whether the wand's stone is lit: the Channel from the start until it is cast, the Bolt just
     * before it fires and the Bogfang just before the wand hits the ground. Times are seconds into
     * the clip, read off the animation actually on screen so the glow can't drift from the pose.
     */
    public boolean isStoneGlowing() {
        var current = castController == null ? null : castController.getCurrentAnimation();
        if (current == null) {
            return false;
        }
        float seconds = (float) castController.getElapsedAnimationTicks() / 20.0F;
        return switch (current.animation().name()) {
            case "Channel animation" -> seconds < 1.88F;
            case "Bolt " -> seconds >= 0.5F && seconds < 0.71F;
            case "Bogfang_trail" -> seconds >= 1.75F && seconds < 1.96F;
            default -> false;
        };
    }

    private PlayState animateMage(AnimationState<OgreMage> state) {
        state.getController().setAnimationSpeed(1.0);
        if (isDeadOrDying()) {
            // A cast in progress is cancelled the moment he dies.
            visualOneShot.clear();
            return state.setAndContinue(IDLE);
        }
        int visualCastId = visualOneShot.update(state.getController(),
                entityData.get(DATA_MAGE_CAST_ID), entityData.get(DATA_MAGE_CAST_START_TICK),
                id -> CastType.byId(id).animationTicks, level().getGameTime());
        if (visualCastId > 0) {
            return state.setAndContinue(CastType.byId(visualCastId).animation);
        }

        if (shouldUseRunAnimation()) {
            idleAnimationTicks = 0;
            rareIdleTicks = 0;
            lastAnimationTick = tickCount;
            // Follow actual movement — being "aggressive" doesn't mean currently moving (e.g. stopped
            // to cast), so without this check he'd show RUN even while stationary between casts.
            if (isMovingForAnimation(state)) {
                state.getController().setAnimationSpeed(RUN_ANIMATION_SPEED);
                return state.setAndContinue(RUN);
            }
            return state.setAndContinue(IDLE);
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

    private boolean isMovingForAnimation(AnimationState<OgreMage> state) {
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

    private boolean shouldUseRunAnimation() {
        // isAggressive() is synced (set by the combat goal); getTarget() is server-only
        // and always null on the client, which left clients stuck on WALK while approaching.
        return isAggressive() || isRetreatingForAnimation() || isRunMovementState();
    }

    private void setRetreatingForAnimation(boolean retreating) {
        entityData.set(DATA_MAGE_RETREATING, retreating);
    }

    private boolean isRetreatingForAnimation() {
        return entityData.get(DATA_MAGE_RETREATING);
    }

    private void clearSyncedCastAnimation() {
        if (!level().isClientSide) {
            entityData.set(DATA_MAGE_CAST_ID, 0);
            entityData.set(DATA_MAGE_CAST_START_TICK, 0L);
        }
    }

    private void updateIdleAnimationTimers(AnimationState<OgreMage> state) {
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

    private record MageCast(CastType type, LivingEntity target, ItemStack stack) {
        private MageCast(CastType type, LivingEntity target) {
            this(type, target, ItemStack.EMPTY);
        }
    }

    private enum CastType {
        POISON_FOG(1, CHANNEL, 50, 40),
        MAGIC_PUSH(2, MAGIC_PUSH_ANIMATION, 40, 21),
        BOLT(3, BOLT_ANIMATION, 40, 20),
        // 1.75s clip; the staff lands at 0.625s (tick 12.5), plus 2 ticks for the synced start reaching clients.
        SMACK(4, SMACK_ANIMATION, 35, 15),
        // Keep the held bottle through tick 20, then remove it and create the projectile from the
        // authored hand-side release position on that exact same tick.
        POTION(5, TOSS_ANIMATION, 25, 20),
        // 2.375s authored clip; consume at 2.16s (about tick 43).
        HEAL_DRINK(6, DRINK_ANIMATION, 48, 43),
        // 3.75s clip: he studies the wand for the first second, then casts. The wand meets the ground at
        // 1.96s; the extra 2 ticks cover the synced start reaching clients.
        BOGFANG_TRAIL(7, BOGFANG_TRAIL_ANIMATION, 75, 41);

        private final int id;
        private final RawAnimation animation;
        private final int animationTicks;
        private final int releaseTicks;

        CastType(int id, RawAnimation animation, int animationTicks, int releaseTicks) {
            this.id = id;
            this.animation = animation;
            this.animationTicks = animationTicks;
            this.releaseTicks = releaseTicks;
        }

        private static CastType byId(int id) {
            for (CastType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return POISON_FOG;
        }
    }

    private static class OgreMageCombatGoal extends Goal {
        private final OgreMage mage;
        private final double speedModifier;
        private int ticksUntilNextPathRecalculation;
        private int ticksUntilNextFog;
        private int ticksUntilNextPotion;
        private int ticksUntilNextBolt;
        private int ticksUntilNextBogfang;
        private int ticksUntilNextMagicPush;
        private int ticksUntilNextSmack;
        private int closePressureTicks;
        private int retreatCommitTicks;
        private int lineOfSightTicks;
        private Vec3 retreatTargetPos;
        private Vec3 lastPursuitTargetPos;

        private OgreMageCombatGoal(OgreMage mage, double speedModifier) {
            this.mage = mage;
            this.speedModifier = speedModifier;
            this.ticksUntilNextFog = FOG_COOLDOWN_MIN_TICKS / 2 + mage.getRandom().nextInt(FOG_COOLDOWN_MIN_TICKS / 2);
            this.ticksUntilNextPotion = mage.getRandom().nextInt(POTION_COOLDOWN_TICKS);
            this.ticksUntilNextBolt = mage.getRandom().nextInt(BOLT_COOLDOWN_TICKS);
            this.ticksUntilNextBogfang = BOGFANG_COOLDOWN_TICKS / 2
                    + mage.getRandom().nextInt(BOGFANG_COOLDOWN_TICKS / 2);
            this.ticksUntilNextMagicPush = mage.getRandom().nextInt(MAGIC_PUSH_COOLDOWN_TICKS / 2);
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mage.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = mage.getTarget();
            return mage.isCasting()
                    || target != null
                    && target.isAlive()
                    && (!(target instanceof Player player) || !player.isSpectator() && !player.isCreative())
                    && mage.isWithinRestriction(target.blockPosition());
        }

        @Override
        public void start() {
            mage.setAggressive(true);
        }

        @Override
        public void stop() {
            mage.setAggressive(false);
            mage.getNavigation().stop();
            mage.setRetreatingForAnimation(false);
            ticksUntilNextPathRecalculation = 0;
            closePressureTicks = 0;
            ticksUntilNextSmack = 0;
            retreatCommitTicks = 0;
            retreatTargetPos = null;
            lineOfSightTicks = 0;
            lastPursuitTargetPos = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = mage.getTarget();
            if (target == null) {
                return;
            }

            if (mage.isCasting()) {
                mage.setRetreatingForAnimation(false);
                mage.getNavigation().stop();
                return;
            }

            double distanceToTargetSqr = mage.distanceToSqr(target);
            boolean hasLineOfSight = mage.getSensing().hasLineOfSight(target);
            boolean inCastRange = distanceToTargetSqr <= CAST_RANGE * CAST_RANGE;
            boolean inBoltRange = distanceToTargetSqr <= BOLT_RANGE * BOLT_RANGE;
            boolean inBogfangRange = distanceToTargetSqr >= BOGFANG_MIN_RANGE * BOGFANG_MIN_RANGE
                    && distanceToTargetSqr <= BOGFANG_MAX_RANGE * BOGFANG_MAX_RANGE;
            boolean inMagicPushRange = distanceToTargetSqr <= MAGIC_PUSH_RANGE * MAGIC_PUSH_RANGE;
            boolean inSmackRange = distanceToTargetSqr <= SMACK_RANGE * SMACK_RANGE;
            boolean targetTooClose = distanceToTargetSqr < RETREAT_RANGE * RETREAT_RANGE;
            boolean lowHealth = mage.getHealth() <= mage.getMaxHealth() * LOW_HEALTH_RETREAT_RATIO;
            lineOfSightTicks = hasLineOfSight
                    ? Math.min(lineOfSightTicks + 1, 20)
                    : Math.max(lineOfSightTicks - 1, -20);

            ticksUntilNextSmack = Math.max(ticksUntilNextSmack - 1, 0);

            ticksUntilNextPotion = Math.max(ticksUntilNextPotion - 1, 0);
            ticksUntilNextBolt = Math.max(ticksUntilNextBolt - 1, 0);
            ticksUntilNextBogfang = Math.max(ticksUntilNextBogfang - 1, 0);
            ticksUntilNextMagicPush = Math.max(ticksUntilNextMagicPush - 1, 0);
            if (inMagicPushRange && hasLineOfSight) {
                closePressureTicks = Math.min(closePressureTicks + 1, adjustedTickDelay(MAGIC_PUSH_PRESSURE_TICKS));
            } else {
                closePressureTicks = Math.max(closePressureTicks - 2, 0);
            }

            if (ticksUntilNextSmack <= 0
                    && hasLineOfSight
                    && shouldStandAndSmack(inSmackRange, lowHealth)) {
                mage.getNavigation().stop();
                mage.setRetreatingForAnimation(false);
                mage.beginSmack(target);
                ticksUntilNextSmack = adjustedTickDelay(SMACK_COOLDOWN_TICKS);
                retreatTargetPos = null;
                return;
            }

            if (ticksUntilNextMagicPush <= 0
                    && closePressureTicks >= adjustedTickDelay(MAGIC_PUSH_PRESSURE_TICKS)
                    && inMagicPushRange
                    && hasLineOfSight) {
                mage.getNavigation().stop();
                mage.setRetreatingForAnimation(false);
                mage.beginMagicPush(target);
                ticksUntilNextMagicPush = adjustedTickDelay(MAGIC_PUSH_COOLDOWN_TICKS);
                closePressureTicks = 0;
                ticksUntilNextFog = Math.max(ticksUntilNextFog, adjustedTickDelay(30));
                retreatTargetPos = null;
                return;
            }

            if (lowHealth && mage.healDrinkCooldownTicks <= 0) {
                mage.getNavigation().stop();
                mage.setRetreatingForAnimation(false);
                mage.beginHealingDrink();
                retreatTargetPos = null;
                return;
            }

            if (ticksUntilNextPotion <= 0) {
                Optional<MageCast> potionCast = mage.choosePotionThrowInRange(target);
                if (potionCast.isPresent()
                        && mage.getSensing().hasLineOfSight(potionCast.get().target())) {
                    mage.getNavigation().stop();
                    mage.setRetreatingForAnimation(false);
                    mage.beginPotionThrow(potionCast.get());
                    ticksUntilNextPotion = adjustedTickDelay(POTION_COOLDOWN_TICKS);
                    ticksUntilNextBolt = Math.max(ticksUntilNextBolt, adjustedTickDelay(20));
                    retreatTargetPos = null;
                    return;
                }
            }

            if (targetTooClose && hasLineOfSight) {
                tickRetreat(target);
                return;
            }

            if (ticksUntilNextBogfang <= 0 && hasLineOfSight && inBogfangRange) {
                mage.getNavigation().stop();
                mage.setRetreatingForAnimation(false);
                mage.beginBogfangTrail(target);
                ticksUntilNextBogfang = adjustedTickDelay(BOGFANG_COOLDOWN_TICKS);
                ticksUntilNextBolt = Math.max(ticksUntilNextBolt, adjustedTickDelay(35));
                ticksUntilNextFog = Math.max(ticksUntilNextFog, adjustedTickDelay(45));
                return;
            }

            if (ticksUntilNextBolt <= 0
                    && hasLineOfSight
                    && inBoltRange
                    && mage.hasClearBoltToTarget(target)) {
                mage.getNavigation().stop();
                mage.setRetreatingForAnimation(false);
                mage.beginBoltCast(target);
                ticksUntilNextBolt = adjustedTickDelay(BOLT_COOLDOWN_TICKS);
                return;
            }

            if (!hasLineOfSight || !inCastRange || lineOfSightTicks < 5) {
                tickRangedApproach(target);
            } else {
                mage.getNavigation().stop();
                mage.setRetreatingForAnimation(false);
                // LookControl only turns the head, so the body stayed facing wherever it last pathed
                // and a player circling at range could end up well outside his facing by the time a
                // cast committed, reading as a snap turn. Keep the body gently tracking while waiting.
                mage.faceToward(target);
                mage.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            ticksUntilNextFog = Math.max(ticksUntilNextFog - 1, 0);

            if (ticksUntilNextFog <= 0 && hasLineOfSight && inCastRange) {
                mage.beginPoisonFogCast(target);
                mage.setRetreatingForAnimation(false);
                ticksUntilNextFog = adjustedTickDelay(FOG_COOLDOWN_MIN_TICKS + mage.getRandom().nextInt(FOG_COOLDOWN_RANDOM_TICKS));
                ticksUntilNextMagicPush = Math.max(ticksUntilNextMagicPush, adjustedTickDelay(30));
            }
        }

        private void tickRangedApproach(LivingEntity target) {
            mage.setRetreatingForAnimation(false);
            ticksUntilNextPathRecalculation = Math.max(ticksUntilNextPathRecalculation - 1, 0);

            Vec3 targetPos = target.position();
            boolean targetMoved = lastPursuitTargetPos == null
                    || lastPursuitTargetPos.distanceToSqr(targetPos) > 1.0;
            if (ticksUntilNextPathRecalculation > 0
                    && !mage.getNavigation().isDone()
                    && !targetMoved) {
                return;
            }

            boolean pathing = mage.getNavigation().moveTo(
                    target,
                    mage.applyShallowWaterSpeedFactor(speedModifier));
            lastPursuitTargetPos = targetPos;
            ticksUntilNextPathRecalculation = adjustedTickDelay(
                    pathing ? 8 + mage.getRandom().nextInt(5) : 4);
        }

        private void tickRetreat(LivingEntity target) {
            mage.setRetreatingForAnimation(true);
            retreatCommitTicks = Math.max(retreatCommitTicks - 1, 0);
            boolean needsNewRetreatTarget = retreatTargetPos == null
                    || retreatCommitTicks <= 0
                    || mage.distanceToSqr(retreatTargetPos) < 2.0
                    || mage.getNavigation().isDone();

            if (needsNewRetreatTarget) {
                Vec3 retreatPos = DefaultRandomPos.getPosAway(
                        mage,
                        (int) Math.ceil(RETREAT_DISTANCE),
                        4,
                        target.position());
                if (retreatPos == null) {
                    Vec3 away = mage.position().subtract(target.position());
                    if (away.horizontalDistanceSqr() < 1.0E-4) {
                        away = Vec3.directionFromRotation(0.0F, mage.getYRot());
                    }

                    Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z).normalize();
                    retreatPos = mage.position().add(horizontalAway.scale(RETREAT_DISTANCE));
                }

                retreatTargetPos = retreatPos;
                retreatCommitTicks = adjustedTickDelay(RETREAT_REPATH_TICKS);
                mage.getNavigation().moveTo(
                        retreatTargetPos.x,
                        retreatTargetPos.y,
                        retreatTargetPos.z,
                        RETREAT_SPEED);
            }
        }

        private boolean shouldStandAndSmack(boolean inSmackRange, boolean lowHealth) {
            // A healthy mage will defend himself whenever the player commits to melee.
            // The animation time plus SMACK_COOLDOWN_TICKS still leaves a fair gap
            // between hits; low-health mages prioritize drinking their heal instead.
            return inSmackRange && !lowHealth;
        }

    }
}
