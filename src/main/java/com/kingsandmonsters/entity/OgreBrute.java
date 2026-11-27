package com.kingsandmonsters.entity;

import java.util.Map;
import com.kingsandmonsters.Config;
import com.kingsandmonsters.effect.CombatEffects;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.network.ScreenShakePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import com.kingsandmonsters.entity.animation.SynchronizedAnimationController;
import com.kingsandmonsters.entity.animation.CanonicalOneShotState;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OgreBrute extends OgreGrunt {
    private static final float BRUTE_VOCAL_VOLUME = 1.5F;
    private static final String TANTRUM_MILESTONE_QUEUED_TAG = "TemperTantrumMilestoneQueued";
    private static final String TANTRUM_MILESTONE_CONSUMED_TAG = "TemperTantrumMilestoneConsumed";
    private static final int AMBIENT_HUFF_MIN_COOLDOWN_TICKS = 760;
    private static final int AMBIENT_HUFF_RANDOM_COOLDOWN_TICKS = 120;
    private static final int AMBIENT_HUFF_CHANCE = 80;
    private static final int LEVEL_15_XP_REWARD = 315;

    @Override
    protected float getStepPitchMultiplier() {
        return 0.72F;
    }

    @Override
    protected float getStepVolumeMultiplier() {
        return 1.9F;
    }

    @Override
    protected int getMinimumStepSoundIntervalTicks() {
        return 8;
    }

    @Override
    protected boolean usesAnimatedFootsteps() {
        return true;
    }

    // Animation ticks where a foot lands, from the leg keyframes.
    private static final Map<String, double[]> FOOT_CONTACTS = Map.of(
            "walk", new double[]{0.0, 20.0},
            "combat walk", new double[]{0.0, 15.84});

    /** Client-side: a dull thud each time a foot visibly lands, a softer take on the King's. */
    private void playFootfall() {
        if (!onGround() || isInWater() || isSilent()) {
            return;
        }
        float variation = getRandom().nextFloat() * 0.08F;
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.WARDEN_STEP, SoundSource.HOSTILE,
                0.8F, 0.7F + variation, false);
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.HOSTILE_BIG_FALL, SoundSource.HOSTILE,
                0.35F, 0.75F + variation, false);
        BlockPos ground = getOnPos();
        SoundType surface = level().getBlockState(ground).getSoundType(level(), ground, this);
        level().playLocalSound(getX(), getY(), getZ(), surface.getStepSound(), SoundSource.HOSTILE,
                0.7F, 0.65F + variation, false);
    }
    private static final EntityDataAccessor<Integer> DATA_BRUTE_ATTACK_ID =
            SynchedEntityData.defineId(OgreBrute.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_BRUTE_ATTACK_START_TICK =
            SynchedEntityData.defineId(OgreBrute.class, EntityDataSerializers.LONG);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation IDLE_2 = RawAnimation.begin().thenLoop("idle2");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation COMBAT_WALK = RawAnimation.begin().thenLoop("combat walk");
    private static final RawAnimation REGULAR_ATTACK = RawAnimation.begin().thenPlay("Regular attack 1");
    private static final RawAnimation REGULAR_ATTACK_2 = RawAnimation.begin().thenPlay("Regular attack 2");
    private static final RawAnimation REGULAR_ATTACK_3 = RawAnimation.begin().thenPlay("Regular attack 3");
    private static final RawAnimation SIDE_SWIPE = RawAnimation.begin().thenPlay("side swipe");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("Bite");
    private static final RawAnimation JUMP_SMASH = RawAnimation.begin().thenPlay("jump smash");
    private static final RawAnimation TEMPER_TANTRUM = RawAnimation.begin().thenPlay("temper tantrum");
    // idle2 is 2.857s long, so the variant plays out one whole loop before the plain idle returns.
    private static final int IDLE_VARIANT_TICKS = 57;
    // Attack ids: 1 and 6 are the two regular slams (right arm, left arm), 5 the side swipe, 3 the
    // jump smash and 4 the temper tantrum. The first three are thrown over the combat walk.
    private static final int ATTACK_ID_REGULAR_ATTACK = 1;
    private static final int ATTACK_ID_JUMP_SMASH = 3;
    private static final int ATTACK_ID_TEMPER_TANTRUM = 4;
    private static final int ATTACK_ID_SIDE_SWIPE = 5;
    private static final int ATTACK_ID_REGULAR_ATTACK_2 = 6;
    private static final int ATTACK_ID_REGULAR_ATTACK_3 = 7;
    private static final int ATTACK_ID_BITE = 8;
    private static final int BITE_ANIMATION_TICKS = 45;
    private static final int REGULAR_ATTACK_ANIMATION_TICKS = 50;
    private static final int SIDE_SWIPE_ANIMATION_TICKS = 25;
    /** How long the last pose of a swing takes to ease out once it ends. Read by the model. */
    public static final float ATTACK_OVERLAY_FADE_OUT_TICKS = 4.0F;
    // Fraction of his movement speed while he is in a fight: about 0.21 against the 0.325 attribute,
    // a little under the King's 0.225 (0.25 * 0.90) so the boss stays the faster of the two.
    private static final double COMBAT_WALK_SPEED = 0.643;
    // He stops closing in once he is this far (block edge to block edge) from his target.
    private static final double COMBAT_WALK_TARGET_GAP = 0.9;
    private static final double COMBAT_WALK_MAX_VERTICAL_DIFFERENCE = 3.5;
    // jump smash animation_length grew from 2.75s to 3.25s (a uniform +0.5s
    // windup added before everything else, confirmed keyframe-by-keyframe),
    // so every tick offset tied to its timeline below shifts by +10 ticks.
    private static final int BELLY_SLAM_ANIMATION_TICKS = 65;
    private static final int TEMPER_TANTRUM_ANIMATION_TICKS = 105;
    // Contact keyframes at 28/43/63 ticks. Other Brute attacks add ATTACK_SYNC_DELAY_TICKS (3,
    // declared further down) for animation sync lag; these three didn't, so the hits landed
    // just before the hands reached the ground.
    private static final int TEMPER_TANTRUM_FIRST_IMPACT_TICK = 28 + 3;
    private static final int TEMPER_TANTRUM_SECOND_IMPACT_TICK = 43 + 3;
    private static final int TEMPER_TANTRUM_FINAL_IMPACT_TICK = 63 + 3;
    private static final double TEMPER_TANTRUM_HAND_FORWARD_OFFSET = 1.65;
    private static final double TEMPER_TANTRUM_HAND_SIDE_OFFSET = 1.15;
    private static final double TEMPER_TANTRUM_FINAL_FORWARD_OFFSET = 1.8;
    private static final double TEMPER_TANTRUM_MINI_DIRECT_HALF_WIDTH = 1.25;
    private static final double TEMPER_TANTRUM_FINAL_DIRECT_HALF_WIDTH = 1.9;
    private static final double TEMPER_TANTRUM_DIRECT_HEIGHT = 3.1;
    private static final double TEMPER_TANTRUM_MINI_VERTICAL_LAUNCH = 0.25;
    private static final double TEMPER_TANTRUM_FINAL_VERTICAL_LAUNCH = 1.38;
    private static final int RIPPLE_JUMP_GRACE_TICKS = 2;
    private static final int FORCED_MINI_LAUNCH_DODGE_SUPPRESSION_TICKS = 24;
    private static final int ATTACK_SYNC_DELAY_TICKS = 3;
    // Both regular slams land at 1.17s into their clip (tick 23); the side swipe at 0.54s (tick 11).
    private static final int REGULAR_ATTACK_IMPACT_TICK = 23 + ATTACK_SYNC_DELAY_TICKS;
    private static final int SIDE_SWIPE_IMPACT_TICK = 11 + ATTACK_SYNC_DELAY_TICKS;
    // The double overhead slam lands 1.33s into its clip (tick 27).
    private static final int DOUBLE_SLAM_IMPACT_TICK = 27 + ATTACK_SYNC_DELAY_TICKS;
    private static final int DOUBLE_SLAM_RECOVERY_TICKS = 70;
    // The bite closes at 1.21s into its clip (tick 24). He lunges about a block as he snaps, so it
    // reaches a little past the slams' start range, and takes the one target nearest in front.
    private static final int BITE_IMPACT_TICK = 24 + ATTACK_SYNC_DELAY_TICKS;
    private static final int BITE_RECOVERY_TICKS = 70;
    private static final double BITE_REACH = 3.4;
    private static final double BITE_HALF_WIDTH = 1.3;
    private static final float BITE_ARMOR_PIERCE_FRACTION = 0.15F;
    private static final float BITE_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.BRUTE, OgreDamage.Blow.STANDARD);
    // A Grunt's bite heals with Regeneration II; the Brute's heals with Regeneration III.
    private static final int BITE_REGENERATION_TICKS = 80;
    private static final int BITE_REGENERATION_AMPLIFIER = 2;
    // Both fists come down together a little way in front of him and the blow spreads from there,
    // weaker the further out it reaches. The inner part of it also leaves a target dazed.
    private static final double DOUBLE_SLAM_REACH = 2.4;
    private static final float DOUBLE_SLAM_RADIUS = 6.5F;
    private static final float DOUBLE_SLAM_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.BRUTE, OgreDamage.Blow.HEAVY);
    private static final float DOUBLE_SLAM_EDGE_SHARE = OgreDamage.BLAST_EDGE_SHARE;
    private static final double DOUBLE_SLAM_DAZE_FRACTION = 0.55;
    private static final double DOUBLE_SLAM_PUSH = 1.0;
    private static final double DOUBLE_SLAM_LIFT = 0.35;
    private static final float GAUNTLET_BLOCK_BREAK_CHANCE = 0.6F;
    private static final int BELLY_SLAM_GRUNT_SOUND_DELAY_TICKS = 23;
    private static final int BELLY_SLAM_IMPACT_SOUND_DELAY_TICKS = 38 + ATTACK_SYNC_DELAY_TICKS;
    private static final int BELLY_SLAM_COOLDOWN_TICKS = 600;
    private static final int BELLY_SLAM_FLIGHT_TICKS = 14;
    private static final int TEMPER_TANTRUM_COOLDOWN_TICKS = 560;
    private static final int TEMPER_TANTRUM_WHINE_DELAY_TICKS = 5;
    // His gauntlets are iron: a slam's hit point sits about this far in front of him.
    private static final double GAUNTLET_REACH = 2.6;
    // The fist comes down along a path from just in front of him out to the impact point, offset
    // toward the swinging arm. Anyone in that path (widened by the radius) is struck outright.
    private static final double GAUNTLET_STRIKE_START = 0.9;
    private static final double GAUNTLET_SIDE_OFFSET = 0.55;
    private static final double GAUNTLET_STRIKE_RADIUS = 1.7;
    // The ring around the impact takes less the further out it is.
    private static final float GAUNTLET_RING_RADIUS = 3.2F;
    // Someone the fist drives through is carried this fast along the slam and lifted a little.
    private static final double SLAM_CARRY_PUSH = 1.5;
    private static final double SLAM_CARRY_LIFT = 0.3;
    // The side swipe covers an arc this far in front of him and this far to either side.
    private static final double SWIPE_REACH = 3.4;
    private static final double SWIPE_HALF_WIDTH = 3.0;
    private static final double SWIPE_RADIUS = Math.hypot(SWIPE_REACH, SWIPE_HALF_WIDTH);
    // Area hits skip the line-of-sight check inside this range.
    private static final double CLOSE_HIT_RANGE = 4.5;
    private static final double REGULAR_ATTACK_START_RANGE = 3.7;
    private static final double BELLY_SLAM_RANGE = 7.0;
    private static final double BELLY_SLAM_MIN_RANGE = 3.25;
    private static final double TEMPER_TANTRUM_START_RANGE = 6.25;
    private static final double TEMPER_TANTRUM_SLOWNESS_RADIUS = 7.0;
    private static final double ATTACK_AOE_SCALE = 1.2;

    /** Only Temper Tantrum draws the ripple; its slams are the only Brute areas wider than 7 blocks. */
    @Override
    protected boolean showsGroundRippleVisual(MeleeAttackHit hit, GroundRippleProfile profile) {
        return hit.areaRadius() > 7.0F;
    }
    private static final double ATTACK_KNOCKBACK_SCALE = 1.2;
    // Eased back up from the 43/41/41/53 values that made him attack a bit too often — still a net
    // cut from the original 50/48/48/62, just not as aggressive a reduction.
    // Belly Slam/Temper Tantrum keep their own separate, untouched cooldowns.
    // A swing is thrown while he keeps walking, so the recovery has to outlast the clip (50 / 25
    // ticks) or the next swing would start on top of the last one.
    private static final int REGULAR_ATTACK_RECOVERY_TICKS = 70;
    private static final int SIDE_SWIPE_RECOVERY_TICKS = 45;
    // Multipliers on the 11.0 base ATTACK_DAMAGE, tuned against full diamond armor. Kept below
    // OgreLord's so the Brute reads weaker: slam ~3.2 hearts, side swipe ~2.3.
    private static final float REGULAR_ATTACK_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.BRUTE, OgreDamage.Blow.STANDARD);
    private static final float SIDE_SWIPE_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.BRUTE, OgreDamage.Blow.LIGHT);
    // Flat raw damage: 2.5 per mini slam hit, 4.5 for the final slam, before armor. Written as
    // raw/11 because doScaledHurtTarget takes a multiplier on the 11.0 base ATTACK_DAMAGE.
    private static final float TEMPER_TANTRUM_MINI_SLAM_DAMAGE_MULTIPLIER = 2.5F / 11.0F;
    private static final float TEMPER_TANTRUM_BIG_SLAM_DAMAGE_MULTIPLIER = 4.5F / 11.0F;
    private static final float TEMPER_TANTRUM_MINI_DIRECT_DAMAGE_MULTIPLIER = 2.5F / 11.0F;
    private static final float TEMPER_TANTRUM_FINAL_DIRECT_DAMAGE_MULTIPLIER = 4.5F / 11.0F;
    // Belly slam (the jump attack) — also scaled down from the old 2.25F, to stay in line with the rest.
    private static final float BELLY_SLAM_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.BRUTE, OgreDamage.Blow.STANDARD);

    private int idleAnimationTicks;
    private int idleVariantTicks;
    private int attackAnimationTicks;
    private int bellySlamCooldownTicks;
    private int temperTantrumCooldownTicks;
    private int bellySlamTakeoffDelayTicks;
    private int bellySlamFlightTicks;
    private int bellySlamFacingLockTicks;
    private int temperTantrumWhineDelayTicks = -1;
    private int strikeDelayTicks = -1;
    private int bellySlamGruntSoundDelayTicks = -1;
    private int bellySlamImpactSoundDelayTicks = -1;
    private int activeBruteAttackId;
    private int lastRegularAttackId;
    private boolean attackOverlayVisible;
    private boolean jumpSmashReady;
    private int ambientHuffCooldownTicks = AMBIENT_HUFF_MIN_COOLDOWN_TICKS;
    private int lastAnimationTick = -1;
    private long lastMovingAnimationTick = Long.MIN_VALUE / 2;
    private static final int MOVEMENT_ANIMATION_GRACE_TICKS = 4;
    private final CanonicalOneShotState visualOneShot = new CanonicalOneShotState();
    private final CanonicalOneShotState visualOverlayOneShot = new CanonicalOneShotState();
    private RawAnimation activeIdleAnimation = IDLE;
    private LivingEntity bellySlamTarget;
    private Vec3 bellySlamDirection = Vec3.ZERO;
    private float bellySlamYaw;
    private final Set<UUID> pendingTemperTantrumRippleExclusions = new HashSet<>();
    private boolean temperTantrumMilestoneQueued;
    private boolean temperTantrumMilestoneConsumed;

    public OgreBrute(EntityType<? extends OgreBrute> type, Level level) {
        super(type, level);
        ensureCaptainTitle();
        avoidWaterPathfinding();
        xpReward = LEVEL_15_XP_REWARD;
    }

    private void ensureCaptainTitle() {
        if (!hasCustomName()) {
            setCustomName(Component.translatable("title.kingsandmonsters.brute_captain"));
        }
        setCustomNameVisible(Config.SHOW_CAPTAIN_NAMETAGS.get());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        temperTantrumMilestoneQueued = tag.getBoolean(TANTRUM_MILESTONE_QUEUED_TAG);
        temperTantrumMilestoneConsumed = tag.getBoolean(TANTRUM_MILESTONE_CONSUMED_TAG);
        ensureCaptainTitle();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TANTRUM_MILESTONE_QUEUED_TAG, temperTantrumMilestoneQueued);
        tag.putBoolean(TANTRUM_MILESTONE_CONSUMED_TAG, temperTantrumMilestoneConsumed);
    }

    // ------------------------------------------------------------------ //
    //  Attributes                                                        //
    // ------------------------------------------------------------------ //

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.MOVEMENT_SPEED, 0.325)
                .add(Attributes.FOLLOW_RANGE, 30.0)
                .add(Attributes.ARMOR, 18.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.STEP_HEIGHT, 1.2)
                .add(Attributes.SAFE_FALL_DISTANCE, 5.0)
                // See OgreGrunt#createAttributes — default 0 caps water acceleration at a tiny fixed
                // baseline regardless of requested speed, which would make applyShallowWaterSpeedFactor's
                // 50% reduction meaningless since he's already moving slower than that.
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Passive trait left over from the removed defensive phase: each projectile hit has a
        // 1-in-3 chance to be rejected. Returning false lets arrows use their normal failed-hit
        // response instead of embedding in the brute.
        if (!level().isClientSide
                && source.is(DamageTypeTags.IS_PROJECTILE)
                && getRandom().nextInt(3) == 0) {
            level().playSound(
                    null,
                    getX(), getY() + getBbHeight() * 0.55, getZ(),
                    SoundEvents.SHIELD_BLOCK,
                    SoundSource.HOSTILE,
                    1.0F,
                    0.65F + getRandom().nextFloat() * 0.1F);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        getX(), getY() + getBbHeight() * 0.65, getZ(), 10, 0.3, 0.3, 0.3, 0.3);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        getX(), getY() + getBbHeight() * 0.65, getZ(), 6, 0.25, 0.25, 0.25, 0.4);
            }
            return false;
        }

        return super.hurt(source, amount);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BRUTE_ATTACK_ID, 0);
        builder.define(DATA_BRUTE_ATTACK_START_TICK, 0L);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isDeadOrDying()) {
            // Dying cancels the swing, the belly slam's flight and its delayed impact sounds.
            strikeDelayTicks = -1;
            bellySlamTakeoffDelayTicks = 0;
            bellySlamFlightTicks = 0;
            bellySlamTarget = null;
            return;
        }
        if (!level().isClientSide) {
            if (!temperTantrumMilestoneConsumed && getHealth() <= getMaxHealth() * 0.5F) {
                temperTantrumMilestoneQueued = true;
            }
            tickAmbientHuff();
            bellySlamCooldownTicks = Math.max(bellySlamCooldownTicks - 1, 0);
            temperTantrumCooldownTicks = Math.max(temperTantrumCooldownTicks - 1, 0);
            tickTemperTantrumWhineSound();
            tickTemperTantrumSlowness();
            tickDelayedImpactSounds();
            tickBellySlamFlight();
            tickBellySlamFacingLock();
            tickStrike();
            tickJumpSmashReady();
        }
    }

    private void tickAmbientHuff() {
        if (ambientHuffCooldownTicks > 0) {
            ambientHuffCooldownTicks--;
            return;
        }
        if (getTarget() != null || isAggressive() || activeBruteAttackId != 0
                || getRandom().nextInt(AMBIENT_HUFF_CHANCE) != 0) {
            return;
        }

        level().playSound(null, getX(), getY(), getZ(),
                ModSoundEvents.OGRE_BRUTE_IDLE.get(), SoundSource.HOSTILE, BRUTE_VOCAL_VOLUME, 1.0F);
        ambientHuffCooldownTicks = AMBIENT_HUFF_MIN_COOLDOWN_TICKS
                + getRandom().nextInt(AMBIENT_HUFF_RANDOM_COOLDOWN_TICKS + 1);
    }

    @Override
    public void applyConfiguredCombatAttributes(boolean healToMax) {
        if (!Config.isLoaded()) {
            return;
        }

        setAttributeBaseValue(Attributes.MAX_HEALTH, Config.OGRE_BRUTE_MAX_HEALTH.get());
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, Config.OGRE_BRUTE_ATTACK_DAMAGE.get());
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, Config.OGRE_BRUTE_MOVEMENT_SPEED.get());
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, Config.OGRE_BRUTE_FOLLOW_RANGE.get());
        setAttributeBaseValue(Attributes.ARMOR, Config.OGRE_BRUTE_ARMOR.get());
        applyConfiguredHealth(healToMax);
    }

    @Override
    protected void applyAttackHitExtras(LivingEntity target, MeleeAttackHit hit) {
        super.applyAttackHitExtras(target, hit);
        // Attack ids 3 and 4 are Belly Slam and Temper Tantrum respectively.
        if ((activeBruteAttackId == 3 || activeBruteAttackId == 4) && !(target instanceof OgreGrunt)) {
            CombatEffects.applyDazed(target, Config.DAZED_HEAVY_ATTACK_DURATION_TICKS.get(), this);
        }
    }

    @Override
    protected MeleeAttackPlan startMeleeAttack() {
        LivingEntity target = getTarget();
        if (canStartTemperTantrum(target)) {
            temperTantrumMilestoneQueued = false;
            temperTantrumMilestoneConsumed = true;
            playAttackAnimation(4);
            temperTantrumCooldownTicks = TEMPER_TANTRUM_COOLDOWN_TICKS;
            temperTantrumWhineDelayTicks = TEMPER_TANTRUM_WHINE_DELAY_TICKS;
            getNavigation().stop();
            setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
            warmUpRippleBlockVisual();
            return new MeleeAttackPlan(125, TEMPER_TANTRUM_ANIMATION_TICKS, List.of(
                    MeleeAttackHit.areaSpecial(TEMPER_TANTRUM_FIRST_IMPACT_TICK, TEMPER_TANTRUM_MINI_SLAM_DAMAGE_MULTIPLIER, (float) (6.0 * ATTACK_AOE_SCALE), true,
                                    0.15F, 0.0, 0.0F, TEMPER_TANTRUM_MINI_VERTICAL_LAUNCH, 0, 0, true)
                            .withAreaOffset(TEMPER_TANTRUM_HAND_FORWARD_OFFSET, -TEMPER_TANTRUM_HAND_SIDE_OFFSET)
                            .withVerticalHitRange(5.0)
                            .withScreenShake(1.05F)
                            .withBlockRippleParticles(),
                    MeleeAttackHit.areaSpecial(TEMPER_TANTRUM_SECOND_IMPACT_TICK, TEMPER_TANTRUM_MINI_SLAM_DAMAGE_MULTIPLIER, (float) (6.0 * ATTACK_AOE_SCALE), true,
                                    0.15F, 0.0, 0.0F, TEMPER_TANTRUM_MINI_VERTICAL_LAUNCH, 0, 0, true)
                            .withAreaOffset(TEMPER_TANTRUM_HAND_FORWARD_OFFSET, TEMPER_TANTRUM_HAND_SIDE_OFFSET)
                            .withVerticalHitRange(5.0)
                            .withScreenShake(1.05F)
                            .withBlockRippleParticles(),
                    MeleeAttackHit.areaSpecial(TEMPER_TANTRUM_FINAL_IMPACT_TICK, TEMPER_TANTRUM_BIG_SLAM_DAMAGE_MULTIPLIER, (float) (11.0 * ATTACK_AOE_SCALE), true,
                                    0.35F, 0.0, 0.0F, TEMPER_TANTRUM_FINAL_VERTICAL_LAUNCH, 60, 0, true)
                            .withAreaOffset(TEMPER_TANTRUM_FINAL_FORWARD_OFFSET, 0.0)
                            .withVerticalHitRange(5.5)
                            .withScreenShake(1.65F)
                            .withBlockRippleParticles()));
        }

        if (jumpSmashReady && target != null) {
            jumpSmashReady = false;
            return startBellySlamAttack(target);
        }

        // Both regular slams and the side swipe are thrown while he keeps walking. The pick rotates
        // roughly 3/3/4 and never repeats the previous swing when another is available.
        int pick = chooseRegularAttack();
        return switch (pick) {
            case ATTACK_ID_REGULAR_ATTACK, ATTACK_ID_REGULAR_ATTACK_2 -> {
                playAttackAnimation(pick);
                strikeDelayTicks = REGULAR_ATTACK_IMPACT_TICK;
                // The slam itself is thrown from aiStep at the clip's 1.17s impact (see tickStrike), so
                // the plan only carries the animation and the recovery.
                yield new MeleeAttackPlan(REGULAR_ATTACK_RECOVERY_TICKS, REGULAR_ATTACK_ANIMATION_TICKS, List.of());
            }
            case ATTACK_ID_REGULAR_ATTACK_3 -> {
                playAttackAnimation(pick);
                strikeDelayTicks = DOUBLE_SLAM_IMPACT_TICK;
                yield new MeleeAttackPlan(DOUBLE_SLAM_RECOVERY_TICKS, REGULAR_ATTACK_ANIMATION_TICKS, List.of());
            }
            case ATTACK_ID_BITE -> {
                playAttackAnimation(pick);
                strikeDelayTicks = BITE_IMPACT_TICK;
                yield new MeleeAttackPlan(BITE_RECOVERY_TICKS, BITE_ANIMATION_TICKS, List.of());
            }
            default -> {
                playAttackAnimation(ATTACK_ID_SIDE_SWIPE);
                strikeDelayTicks = SIDE_SWIPE_IMPACT_TICK;
                yield new MeleeAttackPlan(SIDE_SWIPE_RECOVERY_TICKS, SIDE_SWIPE_ANIMATION_TICKS, List.of());
            }
        };
    }

    /** Picks the next swing: a slam with either arm or the side swipe, never a repeat of the last one when another exists. */
    private int chooseRegularAttack() {
        List<Integer> choices = new ArrayList<>(List.of(
                ATTACK_ID_REGULAR_ATTACK, ATTACK_ID_REGULAR_ATTACK, ATTACK_ID_REGULAR_ATTACK,
                ATTACK_ID_REGULAR_ATTACK_2, ATTACK_ID_REGULAR_ATTACK_2, ATTACK_ID_REGULAR_ATTACK_2,
                ATTACK_ID_SIDE_SWIPE, ATTACK_ID_SIDE_SWIPE, ATTACK_ID_SIDE_SWIPE, ATTACK_ID_SIDE_SWIPE,
                ATTACK_ID_BITE, ATTACK_ID_BITE, ATTACK_ID_BITE,
                // The double overhead slam is the rare one.
                ATTACK_ID_REGULAR_ATTACK_3));
        List<Integer> fresh = new ArrayList<>(choices);
        fresh.removeIf(id -> id == lastRegularAttackId);
        if (!fresh.isEmpty()) {
            choices = fresh;
        }
        return choices.get(getRandom().nextInt(choices.size()));
    }

    private MeleeAttackPlan startBellySlamAttack(LivingEntity target) {
        if (target == null) {
            return new MeleeAttackPlan(6, List.of());
        }

        playAttackAnimation(ATTACK_ID_JUMP_SMASH);
        // The clip crouches in place for its first second; don't let the walk's momentum slide him.
        getNavigation().stop();
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        bellySlamCooldownTicks = BELLY_SLAM_COOLDOWN_TICKS;
        bellySlamTarget = target;
        lockBellySlamDirection(target);
        // Windup lengthened by 10 ticks (0.5s) along with the rest of the animation timeline.
        bellySlamTakeoffDelayTicks = 24;
        bellySlamFlightTicks = BELLY_SLAM_FLIGHT_TICKS;
        bellySlamFacingLockTicks = BELLY_SLAM_ANIMATION_TICKS;
        bellySlamGruntSoundDelayTicks = BELLY_SLAM_GRUNT_SOUND_DELAY_TICKS;
        bellySlamImpactSoundDelayTicks = BELLY_SLAM_IMPACT_SOUND_DELAY_TICKS;
        // Match BELLY_SLAM_ANIMATION_TICKS explicitly. The default (the hit's 41-tick delay) lets
        // the goal take navigation back mid-flight while tickBellySlamFlight() still drives velocity.
        return new MeleeAttackPlan(85, BELLY_SLAM_ANIMATION_TICKS, List.of(MeleeAttackHit.areaSpecial(38 + ATTACK_SYNC_DELAY_TICKS, BELLY_SLAM_DAMAGE_MULTIPLIER, (float) (4.4 * ATTACK_AOE_SCALE), true,
                0.3F, 1.4 * ATTACK_KNOCKBACK_SCALE, 0.0F, 0.0, 100, 0, true).withVerticalHitRange(5.0)));
    }

    @Override
    protected double getMeleeAttackReachSqr(LivingEntity target) {
        // While the jump smash is ready he starts his attack routine from its longer range; the
        // swings only come out once the target is inside the regular reach (see startMeleeAttack).
        double reach = jumpSmashReady ? BELLY_SLAM_RANGE : REGULAR_ATTACK_START_RANGE;
        return reach * reach;
    }

    @Override
    public boolean canDisableShield() {
        // The slams put the target's shield on cooldown too, same as the grunt's bite.
        return activeBruteAttackId == ATTACK_ID_REGULAR_ATTACK
                || activeBruteAttackId == ATTACK_ID_REGULAR_ATTACK_2
                || activeBruteAttackId == ATTACK_ID_REGULAR_ATTACK_3
                || activeBruteAttackId == ATTACK_ID_BITE || super.canDisableShield();
    }

    /** The swings that are thrown over the combat walk instead of stopping him. */
    private static boolean isWalkingAttack(int attackId) {
        return attackId == ATTACK_ID_REGULAR_ATTACK || attackId == ATTACK_ID_REGULAR_ATTACK_2
                || attackId == ATTACK_ID_REGULAR_ATTACK_3 || attackId == ATTACK_ID_BITE
                || attackId == ATTACK_ID_SIDE_SWIPE;
    }

    private boolean isMidSwing() {
        return activeBruteAttackId != 0 && attackAnimationTicks > 0;
    }

    @Override
    protected boolean isAttackCommitted() {
        // Every attack plays out once it has started, so a swing whose target dies or dodges
        // still lands on the ground with its impact.
        return activeBruteAttackId != 0;
    }

    @Override
    protected boolean shouldFaceMeleeTarget(LivingEntity target) {
        // The planted attacks lock their facing once triggered: letting the look control keep
        // rotating mid-swing turns the body away from where the hit was aimed. The swings thrown
        // while walking keep turning him toward his target instead.
        return bellySlamFacingLockTicks <= 0 && !isTemperTantrumActive()
                && (!isMidSwing() || isWalkingAttack(activeBruteAttackId));
    }

    @Override
    protected boolean canCombatWalkDuringAttack() {
        return isWalkingAttack(activeBruteAttackId);
    }

    @Override
    protected boolean keepsMovingThroughAttackRange() {
        return true;
    }

    @Override
    protected boolean shouldApproachDuringMeleeRecovery() {
        return true;
    }

    @Override
    protected boolean shouldUseCombatWalk(LivingEntity target, boolean closeEnoughToAttack,
                                          int ticksUntilNextAttack, boolean currentlyCombatWalking) {
        // With a target, the combat walk is his only gait; the jump smash and the tantrum are what
        // stop him.
        return target != null && target.isAlive()
                && (!isMidSwing() || isWalkingAttack(activeBruteAttackId));
    }

    @Override
    protected double getCombatWalkSpeedModifier() {
        return COMBAT_WALK_SPEED;
    }

    @Override
    protected boolean shouldHoldCombatSpacing(LivingEntity target) {
        // Closes in to a body's width of his target and stays there, instead of shoving into them.
        double x = target.getX() - getX();
        double z = target.getZ() - getZ();
        double preferred = (getBbWidth() + target.getBbWidth()) * 0.5 + COMBAT_WALK_TARGET_GAP;
        return x * x + z * z <= preferred * preferred
                && Math.abs(target.getY() - getY()) <= COMBAT_WALK_MAX_VERTICAL_DIFFERENCE;
    }

    @Override
    protected boolean shouldDirectlyTrackMeleeTargetWhilePursuing() {
        // Let navigation own facing during the approach, just like the grunt. Direct tracking
        // resumes near attack range, preventing look control from fighting path steering.
        return false;
    }

    @Override
    protected boolean shouldStayStationaryDuringMeleeAttack() {
        // The tantrum commits in place. The swings keep him walking and the jump smash drives its
        // own leap (see below), so neither is held still.
        return isMidSwing()
                && !isWalkingAttack(activeBruteAttackId)
                && activeBruteAttackId != ATTACK_ID_JUMP_SMASH;
    }

    @Override
    protected boolean shouldSuspendNavigationDuringMeleeAttack() {
        // The jump smash supplies its own velocity (via aiStep, not the shared catch hook), so
        // block path navigation without zeroing the motion it sets.
        return activeBruteAttackId == ATTACK_ID_JUMP_SMASH && attackAnimationTicks > 0;
    }

    @Override
    protected double getMeleePursuitSpeedModifier() {
        return 1.0;
    }

    @Override
    protected double getWanderSpeedModifier() {
        // Navigation goals take a multiplier; this yields an effective wandering speed
        // of 0.12 against the Brute's default 0.325 movement attribute.
        return 0.12 / 0.325;
    }

    @Override
    protected int getPathRecalculationTicks() {
        // The brute is wider and faster than a grunt, so the grunt's 4–7 tick repath cadence
        // produces visible left-right corrections. A short extra commitment smooths his heading
        // without changing the underlying vanilla path navigation.
        return 8 + getRandom().nextInt(4);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        if (shouldSuppressReactiveSounds()) {
            return null;
        }

        return ModSoundEvents.OGRE_BRUTE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.OGRE_BRUTE_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return BRUTE_VOCAL_VOLUME;
    }

    @Override
    public float getVoicePitch() {
        return 0.96F + getRandom().nextFloat() * 0.08F;
    }

    private boolean shouldSuppressReactiveSounds() {
        return isTemperTantrumActive()
                || activeBruteAttackId == 3 && attackAnimationTicks > 0;
    }

    // ------------------------------------------------------------------ //
    //  Animation controllers                                             //
    // ------------------------------------------------------------------ //

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new SynchronizedAnimationController<>(this, "Brute Movement", 5, this::animateBrute)
                .withFootfalls(FOOT_CONTACTS, this::playFootfall));
        // Registered second so it is applied after the walk: his swings only key the upper body,
        // and this controller blends them in from whatever pose the walk has him in. Same 5-tick
        // blend-in as the movement controller, so the hit timings calibrated against it
        // (ATTACK_SYNC_DELAY_TICKS) hold for these swings too. See OgreBruteModel for the blend out.
        controllers.add(new SynchronizedAnimationController<>(this, "Brute Attack Overlay", 5,
                this::animateAttackOverlay));
    }

    /** True on the client while a swing is playing over the walk. */
    public boolean isAttackOverlayVisible() {
        return attackOverlayVisible;
    }

    @Override
    public double getBoneResetTime() {
        // Bones an animation stops keying (the legs when the jump smash takes over) ease back to
        // rest over a few ticks instead of snapping there.
        return 6.0;
    }

    private PlayState animateAttackOverlay(AnimationState<OgreBrute> state) {
        state.setControllerSpeed(1.0F);
        if (isDeadOrDying()) {
            visualOverlayOneShot.clear();
            attackOverlayVisible = false;
            return PlayState.STOP;
        }
        int syncedAttackId = entityData.get(DATA_BRUTE_ATTACK_ID);
        boolean overlayAttack = isWalkingAttack(syncedAttackId);
        int visualAttackId = visualOverlayOneShot.update(state.getController(),
                overlayAttack ? syncedAttackId : 0,
                overlayAttack ? entityData.get(DATA_BRUTE_ATTACK_START_TICK) : 0L,
                this::getAttackAnimationTicks, level().getGameTime());
        attackOverlayVisible = visualAttackId > 0;
        return attackOverlayVisible ? state.setAndContinue(getAttackAnimation(visualAttackId)) : PlayState.STOP;
    }

    private PlayState animateBrute(AnimationState<OgreBrute> state) {
        if (isDeadOrDying()) {
            // A swing, slam or tantrum in progress is cancelled the moment he dies.
            visualOneShot.clear();
            state.setControllerSpeed(1.0F);
            return state.setAndContinue(IDLE);
        }
        // The swings thrown while walking belong to the overlay controller; this one keeps playing
        // the walk underneath them.
        int syncedAttackId = entityData.get(DATA_BRUTE_ATTACK_ID);
        boolean overlayAttack = isWalkingAttack(syncedAttackId);
        int visualAttackId = visualOneShot.update(state.getController(),
                overlayAttack ? 0 : syncedAttackId,
                overlayAttack ? 0L : entityData.get(DATA_BRUTE_ATTACK_START_TICK),
                this::getAttackAnimationTicks, level().getGameTime());
        if (visualAttackId > 0) {
            return state.setAndContinue(getAttackAnimation(visualAttackId));
        }

        // isAggressive() is a synced data flag (set by the inherited melee goal), unlike getTarget()
        // which is server-only and always null on the client — using target here left clients stuck on WALK.
        // Heading to the King or home runs at the same combat-walk pace, so it gets the same clip.
        if (isAggressive() || isRunMovementState()) {
            idleAnimationTicks = 0;
            idleVariantTicks = 0;
            // Follow actual movement rather than recomputing target distance on the client.
            if (isMovingForAnimation(state)) {
                return state.setAndContinue(COMBAT_WALK);
            }
            return state.setAndContinue(IDLE);
        }

        if (isMovingForAnimation(state)) {
            idleAnimationTicks = 0;
            idleVariantTicks = 0;
            return state.setAndContinue(WALK);
        }

        updateIdleAnimationTimers(state);
        if (idleVariantTicks > 0) {
            return state.setAndContinue(activeIdleAnimation);
        }
        return state.setAndContinue(IDLE);
    }

    private boolean isMovingForAnimation(AnimationState<OgreBrute> state) {
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

    private boolean canStartTemperTantrum(LivingEntity target) {
        if (!canReachForTemperTantrum(target)) {
            return false;
        }

        return temperTantrumMilestoneQueued;
    }

    private boolean canReachForTemperTantrum(LivingEntity target) {
        return temperTantrumCooldownTicks <= 0
                && !isTemperTantrumActive()
                && target != null
                && target.isAlive()
                && distanceToSqr(target) <= TEMPER_TANTRUM_START_RANGE * TEMPER_TANTRUM_START_RANGE
                && getSensing().hasLineOfSight(target);
    }

    public boolean isTemperTantrumActive() {
        return activeBruteAttackId == 4 && attackAnimationTicks > 0;
    }

    /** Synced, render-only exclusion for Jump Slam and Temper Tantrum. */
    public boolean isProceduralHeadTrackingSuppressed() {
        int attackId = entityData.get(DATA_BRUTE_ATTACK_ID);
        return attackId == 3 || attackId == 4;
    }

    @Override
    protected boolean shouldDodgeRippleHit(LivingEntity target) {
        if (!isTemperTantrumActive()) {
            return false;
        }

        // Temper tantrum is an unblockable ground slam — the only way to avoid it is to jump
        // at the right moment so the shockwave passes underneath.
        boolean intentionalJump = hasRecentIntentionalJump(target, RIPPLE_JUMP_GRACE_TICKS);
        boolean airborneFromMiniImpact = RippleJumpTracker.wasRecentlyForcedUpward(
                target, FORCED_MINI_LAUNCH_DODGE_SUPPRESSION_TICKS);
        return intentionalJump || !target.onGround() && !airborneFromMiniImpact;
    }

    @Override
    protected boolean applyGroundRippleHit(LivingEntity target, MeleeAttackHit hit, GroundRippleProfile profile,
                                           Vec3 origin) {
        boolean ogreAlly = target instanceof OgreGrunt;
        float multiplier = ogreAlly ? hit.damageMultiplier() * 0.5F : hit.damageMultiplier();
        if (doScaledHurtTarget(target, multiplier, hit.armorPierceFraction())) {
            if (isTemperTantrumActive() && hit.delayTicks() != TEMPER_TANTRUM_FINAL_IMPACT_TICK) {
                RippleJumpTracker.recordForcedRippleLaunch(target);
            }
            applyAttackHitExtras(target, hit);
            return true;
        }
        return false;
    }

    @Override
    protected void applyDirectShockwaveWeaponHits(List<LivingEntity> targets, Vec3 areaCenter,
                                                  MeleeAttackHit hit) {
        pendingTemperTantrumRippleExclusions.clear();
        if (!isTemperTantrumActive()) {
            return;
        }

        boolean finalImpact = hit.delayTicks() == TEMPER_TANTRUM_FINAL_IMPACT_TICK;
        double halfWidth = finalImpact
                ? TEMPER_TANTRUM_FINAL_DIRECT_HALF_WIDTH
                : TEMPER_TANTRUM_MINI_DIRECT_HALF_WIDTH;
        AABB handImpact = new AABB(
                areaCenter.x - halfWidth, getY() - 0.75, areaCenter.z - halfWidth,
                areaCenter.x + halfWidth, getY() + TEMPER_TANTRUM_DIRECT_HEIGHT, areaCenter.z + halfWidth);
        for (LivingEntity target : targets) {
            if (!target.getBoundingBox().intersects(handImpact)) {
                continue;
            }
            pendingTemperTantrumRippleExclusions.add(target.getUUID());
            float directMultiplier = finalImpact
                    ? TEMPER_TANTRUM_FINAL_DIRECT_DAMAGE_MULTIPLIER
                    : TEMPER_TANTRUM_MINI_DIRECT_DAMAGE_MULTIPLIER;
            float multiplier = target instanceof OgreGrunt ? directMultiplier * 0.5F : directMultiplier;
            if (doScaledHurtTarget(target, multiplier, hit.armorPierceFraction())) {
                if (!finalImpact) {
                    RippleJumpTracker.recordForcedRippleLaunch(target);
                }
                applyAttackHitExtras(target, hit.withoutHorizontalKnockback());
            }
        }
    }

    @Override
    protected Set<UUID> consumeDirectShockwaveRippleExclusions() {
        Set<UUID> exclusions = Set.copyOf(pendingTemperTantrumRippleExclusions);
        pendingTemperTantrumRippleExclusions.clear();
        return exclusions;
    }

    /** His iron gauntlets ring on the ground: a clang for each fist, and a heavier one on the final slam. */
    @Override
    protected void playShockwaveLayers(Vec3 center, MeleeAttackHit hit) {
        if (!isTemperTantrumActive()) {
            return;
        }
        boolean finalImpact = hit.delayTicks() == TEMPER_TANTRUM_FINAL_IMPACT_TICK;
        level().playSound(null, center.x, center.y, center.z, SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, finalImpact ? 0.9F : 0.6F, finalImpact ? 0.55F : 0.7F);
        level().playSound(null, center.x, center.y, center.z, SoundEvents.IRON_GOLEM_ATTACK,
                SoundSource.HOSTILE, finalImpact ? 1.2F : 0.8F, finalImpact ? 0.5F : 0.65F);
    }

    private void tickTemperTantrumSlowness() {
        if (!isTemperTantrumActive()) {
            return;
        }

        List<LivingEntity> nearbyPlayers = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(TEMPER_TANTRUM_SLOWNESS_RADIUS),
                entity -> entity instanceof Player && entity.isAlive());

        for (LivingEntity player : nearbyPlayers) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, true));
        }
    }

    private void tickTemperTantrumWhineSound() {
        if (temperTantrumWhineDelayTicks < 0) {
            return;
        }

        if (!isTemperTantrumActive()) {
            temperTantrumWhineDelayTicks = -1;
            return;
        }

        if (temperTantrumWhineDelayTicks-- <= 0) {
            level().playSound(
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    ModSoundEvents.OGRE_BRUTE_TANTRUM_WHINE.get(),
                    SoundSource.HOSTILE,
                    BRUTE_VOCAL_VOLUME,
                    1.0F);
            temperTantrumWhineDelayTicks = -1;
        }
    }

    private void tickDelayedImpactSounds() {
        bellySlamImpactSoundDelayTicks = tickDelayedImpactSound(
                bellySlamImpactSoundDelayTicks,
                3,
                () -> {
                    // Lighter than the King's slam, but the ground still cracks under it.
                    playSlamLayers(position(), 1.0F, 0.0F);
                });
        bellySlamGruntSoundDelayTicks = tickDelayedImpactSound(
                bellySlamGruntSoundDelayTicks,
                3,
                ModSoundEvents.OGRE_BRUTE_BELLY_SLAM_GRUNT.get(),
                BRUTE_VOCAL_VOLUME,
                0.84F);
    }

    /** Throws the swing on the clip's impact tick, with the arm the clip is swinging. */
    private void tickStrike() {
        if (strikeDelayTicks < 0) {
            return;
        }
        if (!isWalkingAttack(activeBruteAttackId) || attackAnimationTicks <= 0) {
            strikeDelayTicks = -1;
            return;
        }
        if (strikeDelayTicks-- > 0) {
            return;
        }
        strikeDelayTicks = -1;
        if (activeBruteAttackId == ATTACK_ID_SIDE_SWIPE) {
            performSideSwipe();
        } else if (activeBruteAttackId == ATTACK_ID_BITE) {
            performBite();
        } else if (activeBruteAttackId == ATTACK_ID_REGULAR_ATTACK_3) {
            performDoubleSlam();
        } else {
            // Entity-right is the model's right arm: attack 1 swings it, attack 6 swings the left.
            performGauntletSlam(activeBruteAttackId == ATTACK_ID_REGULAR_ATTACK ? 1.0 : -1.0);
        }
    }

    /**
     * The backhand sweeps the whole arc in front of him, right to left, so everything in front
     * and within arm's reach is hit, whatever height it is at. Tested against the target's whole
     * body, not the point at its feet, so someone hopping in close can't slip under or over it.
     */
    private void performSideSwipe() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double search = SWIPE_REACH + SWIPE_HALF_WIDTH;
        MeleeAttackHit knockback = MeleeAttackHit.areaSpecial(
                        SIDE_SWIPE_IMPACT_TICK, SIDE_SWIPE_DAMAGE_MULTIPLIER, 0.0F, true,
                        0.0F, 2.65 * ATTACK_KNOCKBACK_SCALE, 0.0F, 0.66, 0, 0, false)
                .withDirectionalKnockback(1.0, -1.0);

        List<LivingEntity> candidates = level().getEntitiesOfClass(
                LivingEntity.class, getBoundingBox().inflate(search, 4.0, search),
                target -> target != this && target.isAlive() && canAttackOgreTarget(target));
        for (LivingEntity target : candidates) {
            AABB bounds = target.getBoundingBox();
            Vec3 relative = bounds.getCenter().subtract(position());
            double halfWidth = target.getBbWidth() * 0.5;
            double ahead = relative.dot(forward);
            double across = relative.dot(right);
            boolean inArc = ahead >= -0.3 - halfWidth && ahead <= SWIPE_REACH + halfWidth
                    && Math.abs(across) <= SWIPE_HALF_WIDTH + halfWidth;
            boolean reachable = bounds.maxY >= getY() - 0.5 && bounds.minY <= getY() + 3.8;
            // The far end of the arc lands softer than the part right in front of him.
            float falloff = areaFalloff(Math.hypot(ahead, across), SWIPE_RADIUS, OgreDamage.ARC_EDGE_SHARE);
            if (inArc && reachable && doScaledHurtTarget(target, SIDE_SWIPE_DAMAGE_MULTIPLIER * falloff)) {
                applyAttackHitExtras(target, knockback);
                if (!(target instanceof OgreGrunt)) {
                    CombatEffects.applyDazed(target, Config.DAZED_HEAVY_ATTACK_DURATION_TICKS.get(), this);
                }
            }
        }

        playGauntletSwipeImpact();
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 at = gauntletImpactPoint();
            PacketDistributor.sendToPlayersNear(serverLevel, null, at.x, at.y, at.z, 24.0,
                    new ScreenShakePayload(at.x, at.y, at.z, 0.4F));
        }
    }

    /**
     * The Brute's bite: his jaws close on the one target nearest in front of him. It hits like his
     * slams, cripples what it catches and leaves him on Regeneration III, so a long fight feeds him.
     * The chomp sounds whether or not anything was caught.
     */
    private void performBite() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double search = BITE_REACH + BITE_HALF_WIDTH + 1.0;
        LivingEntity bitten = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity target : level().getEntitiesOfClass(
                LivingEntity.class, getBoundingBox().inflate(search, 2.5, search),
                candidate -> candidate != this && candidate.isAlive() && canAttackOgreTarget(candidate))) {
            AABB bounds = target.getBoundingBox();
            Vec3 relative = bounds.getCenter().subtract(position());
            double halfWidth = target.getBbWidth() * 0.5;
            double ahead = relative.dot(forward);
            double across = relative.dot(right);
            boolean inReach = ahead >= -0.3 - halfWidth && ahead <= BITE_REACH + halfWidth
                    && Math.abs(across) <= BITE_HALF_WIDTH + halfWidth
                    && bounds.maxY >= getY() - 0.5 && bounds.minY <= getY() + 3.0;
            if (inReach && ahead < nearest) {
                bitten = target;
                nearest = ahead;
            }
        }

        if (bitten != null && doScaledHurtTarget(bitten, BITE_DAMAGE_MULTIPLIER, BITE_ARMOR_PIERCE_FRACTION)) {
            CombatEffects.applyCrippled(bitten, Config.CRIPPLED_BITE_DURATION_TICKS.get(), this);
            addEffect(new MobEffectInstance(MobEffects.REGENERATION, BITE_REGENERATION_TICKS,
                    BITE_REGENERATION_AMPLIFIER));
        }
        playBiteImpact();
    }

    /** The Grunt's two-layer bite, deepened slightly, with a faint Ravager impact underneath. */
    private void playBiteImpact() {
        Vec3 mouth = position().add(Vec3.directionFromRotation(0.0F, getYRot()).scale(1.6)).add(0.0, 1.9, 0.0);
        level().playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.FOX_BITE,
                SoundSource.HOSTILE, 1.0F, 0.58F);
        level().playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.GENERIC_EAT,
                SoundSource.HOSTILE, 0.38F, 0.54F);
        level().playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.RAVAGER_ATTACK,
                SoundSource.HOSTILE, 0.28F, 0.68F);
    }

    /**
     * The iron fist driving through whatever is in front of him and into the ground. Anyone in the
     * path it comes down along takes the full hit and is carried through it; the rest of the
     * ring around the impact takes a share. The ground is hit either way, so the slam always
     * lands, hit or miss. Nothing here asks for line of sight, so trees and leaves can't make
     * it whiff.
     */
    private void performGauntletSlam(double rightSign) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 side = right.scale(GAUNTLET_SIDE_OFFSET * rightSign);
        Vec3 strikeStart = position().add(forward.scale(GAUNTLET_STRIKE_START)).add(side);
        Vec3 impact = position().add(forward.scale(GAUNTLET_REACH)).add(side);
        AABB strikePath = new AABB(
                Math.min(strikeStart.x, impact.x) - GAUNTLET_STRIKE_RADIUS, getY() - 1.0,
                Math.min(strikeStart.z, impact.z) - GAUNTLET_STRIKE_RADIUS,
                Math.max(strikeStart.x, impact.x) + GAUNTLET_STRIKE_RADIUS, getY() + 3.5,
                Math.max(strikeStart.z, impact.z) + GAUNTLET_STRIKE_RADIUS);
        double ringSqr = (double) GAUNTLET_RING_RADIUS * GAUNTLET_RING_RADIUS;
        double search = GAUNTLET_REACH + GAUNTLET_RING_RADIUS + 1.0;

        List<LivingEntity> candidates = level().getEntitiesOfClass(
                LivingEntity.class, getBoundingBox().inflate(search, 3.5, search),
                target -> target != this && target.isAlive() && canAttackOgreTarget(target));
        for (LivingEntity target : candidates) {
            AABB bounds = target.getBoundingBox();
            double dx = target.getX() - impact.x;
            double dz = target.getZ() - impact.z;
            boolean struck = bounds.intersects(strikePath);
            boolean inRing = dx * dx + dz * dz <= ringSqr
                    && bounds.maxY >= getY() - 1.0 && bounds.minY <= getY() + 3.5;
            if (!struck && !inRing) {
                continue;
            }

            float share = struck ? 1.0F : areaFalloff(Math.sqrt(dx * dx + dz * dz), GAUNTLET_RING_RADIUS, OgreDamage.BLAST_EDGE_SHARE);
            if (!doScaledHurtTarget(target, REGULAR_ATTACK_DAMAGE_MULTIPLIER * share)) {
                continue;
            }
            double carried = 1.0 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            Vec3 motion = target.getDeltaMovement();
            if (struck && !(target instanceof OgreGrunt)) {
                CombatEffects.applyDazed(target, Config.DAZED_HEAVY_ATTACK_DURATION_TICKS.get(), this);
            }
            if (struck) {
                // Carried through along the slam and driven back down, not flung into the air.
                target.setDeltaMovement(forward.x * SLAM_CARRY_PUSH * carried,
                        Math.max(motion.y, SLAM_CARRY_LIFT * carried), forward.z * SLAM_CARRY_PUSH * carried);
            } else {
                target.setDeltaMovement(motion.x, Math.max(motion.y, SLAM_CARRY_LIFT * 0.5 * carried), motion.z);
            }
            target.hasImpulse = true;
        }

        playGauntletSlamImpact(impact);
        spawnGauntletGroundImpact(serverLevel, impact);
    }

    /**
     * Both iron fists down together: a big area around the impact, hit hardest at its centre and
     * less and less toward the edge. The inner part also leaves a target dazed. No line of sight is
     * needed, so trees can't shelter anyone.
     */
    private void performDoubleSlam() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 impact = position().add(forward.scale(DOUBLE_SLAM_REACH));
        double radiusSqr = (double) DOUBLE_SLAM_RADIUS * DOUBLE_SLAM_RADIUS;

        List<LivingEntity> candidates = level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(impact.x - DOUBLE_SLAM_RADIUS, getY() - 1.5, impact.z - DOUBLE_SLAM_RADIUS,
                        impact.x + DOUBLE_SLAM_RADIUS, getY() + 4.0, impact.z + DOUBLE_SLAM_RADIUS),
                target -> target != this && target.isAlive() && canAttackOgreTarget(target));
        for (LivingEntity target : candidates) {
            double dx = target.getX() - impact.x;
            double dz = target.getZ() - impact.z;
            double distanceSqr = dx * dx + dz * dz;
            if (distanceSqr > radiusSqr) {
                continue;
            }

            double distance = Math.sqrt(distanceSqr);
            float falloff = areaFalloff(distance, DOUBLE_SLAM_RADIUS, DOUBLE_SLAM_EDGE_SHARE);
            if (!doScaledHurtTarget(target, DOUBLE_SLAM_DAMAGE_MULTIPLIER * falloff)) {
                continue;
            }
            if (distance <= DOUBLE_SLAM_RADIUS * DOUBLE_SLAM_DAZE_FRACTION && !(target instanceof OgreGrunt)) {
                CombatEffects.applyDazed(target, Config.DAZED_HEAVY_ATTACK_DURATION_TICKS.get(), this);
            }
            // Shoved out from the impact, harder the closer it was.
            double carried = 1.0 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            Vec3 away = distance < 1.0E-3 ? forward : new Vec3(dx, 0.0, dz).normalize();
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(away.x * DOUBLE_SLAM_PUSH * falloff * carried,
                    Math.max(motion.y, DOUBLE_SLAM_LIFT * falloff * carried), away.z * DOUBLE_SLAM_PUSH * falloff * carried);
            target.hasImpulse = true;
        }

        playGauntletSlamImpact(impact);
        playSlamLayers(impact, 1.3F, 0.0F);
        spawnGauntletGroundImpact(serverLevel, impact);
        // The same expanding ring of cloud the jump smash draws, out at the slam's full radius, so it
        // is clear how far the blow reaches.
        StaticShockwaveParticles.spawn(serverLevel, impact, DOUBLE_SLAM_RADIUS);
        // The double slam shatters a few more soft blocks around the impact, but no crater.
        OgreGroundBreaker.breakAround(serverLevel, this, impact, 2.0, 3, 0.4F);
        serverLevel.sendParticles(ParticleTypes.POOF, impact.x, impact.y + 0.3, impact.z,
                24, 2.4, 0.15, 2.4, 0.03);
        PacketDistributor.sendToPlayersNear(serverLevel, null, impact.x, impact.y, impact.z, 28.0,
                new ScreenShakePayload(impact.x, impact.y, impact.z, 1.0F));
    }

    /** Debris off the block he hit, a puff of dust and a little screen shake. */
    private void spawnGauntletGroundImpact(ServerLevel serverLevel, Vec3 impact) {
        BlockPos ground = BlockPos.containing(impact.x, impact.y - 0.5, impact.z);
        BlockState state = serverLevel.getBlockState(ground);
        // The iron fist usually breaks the soft block it lands on, and sometimes one beside it.
        boolean broke = OgreGroundBreaker.breakBlock(serverLevel, this, ground, GAUNTLET_BLOCK_BREAK_CHANCE);
        OgreGroundBreaker.breakAround(serverLevel, this, impact, 1.2, 2, 0.35F);
        if (state.isAir()) {
            ground = getOnPos();
            state = serverLevel.getBlockState(ground);
        }
        if (!state.isAir()) {
            if (!broke) {
                serverLevel.levelEvent(2001, ground, Block.getId(state));
            }
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    impact.x, impact.y + 0.1, impact.z, 28, 0.9, 0.15, 0.9, 0.12);
        }
        serverLevel.sendParticles(ParticleTypes.POOF, impact.x, impact.y + 0.2, impact.z,
                10, 0.8, 0.1, 0.8, 0.02);
        PacketDistributor.sendToPlayersNear(serverLevel, null, impact.x, impact.y, impact.z, 24.0,
                new ScreenShakePayload(impact.x, impact.y, impact.z, 0.6F));
    }

    @Override
    protected boolean canAreaHitReach(LivingEntity target) {
        // Leaves and trunks block the sight ray, so a target stood among trees used to dodge the
        // area attacks just by being behind one. Up close it is a hit regardless.
        return getSensing().hasLineOfSight(target) || distanceToSqr(target) <= CLOSE_HIT_RANGE * CLOSE_HIT_RANGE;
    }

    /** Where an iron fist comes down: in front of him, at the ground. */
    private Vec3 gauntletImpactPoint() {
        return position().add(Vec3.directionFromRotation(0.0F, getYRot()).scale(GAUNTLET_REACH));
    }

    /**
     * An iron-fisted slam: a deep boom, the belly-slam body hit, the clang of metal on the ground
     * and the ground's own crack. Lighter than the King's slam, but it still has to land hard.
     */
    private void playGauntletSlamImpact(Vec3 at) {
        level().playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 0.9F, 0.7F);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 0.6F, 0.6F);
        playSlamLayers(at, 0.9F, 0.0F);
    }

    /** The backhand: a heavy whack with a metal clang on top of it. */
    private void playGauntletSwipeImpact() {
        Vec3 at = gauntletImpactPoint();
        level().playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.HOSTILE, 1.3F, 0.6F);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.IRON_GOLEM_ATTACK,
                SoundSource.HOSTILE, 1.2F, 0.5F);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 0.45F, 0.75F);
    }

    private int tickDelayedImpactSound(int ticks, int attackId, SoundEvent sound, float volume, float pitch) {
        return tickDelayedImpactSound(ticks, attackId, () ->
                level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, volume, pitch));
    }

    private int tickDelayedImpactSound(int ticks, int attackId, Runnable play) {
        if (ticks < 0) {
            return ticks;
        }

        if (activeBruteAttackId != attackId || attackAnimationTicks <= 0) {
            return -1;
        }

        if (ticks <= 0) {
            play.run();
            return -1;
        }

        return ticks - 1;
    }

    @Override
    protected void onMeleeAttackPlanFinished() {
        activeBruteAttackId = 0;
        super.onMeleeAttackPlanFinished();
        if (!level().isClientSide) {
            entityData.set(DATA_BRUTE_ATTACK_ID, 0);
            entityData.set(DATA_BRUTE_ATTACK_START_TICK, 0L);
        }
    }

    /**
     * Rolls for the jump smash on its own schedule, so the walk toward his target is left alone
     * until the jump actually starts. Once it is ready, his attack range stretches out to the
     * jump's, and the next attack he starts is the jump.
     */
    private void tickJumpSmashReady() {
        LivingEntity target = getTarget();
        if (isMidSwing() || !canReachForBellySlam(target)) {
            jumpSmashReady = false;
            return;
        }
        if (!jumpSmashReady) {
            int odds = distanceToSqr(target) >= BELLY_SLAM_MIN_RANGE * BELLY_SLAM_MIN_RANGE ? 60 : 240;
            jumpSmashReady = getRandom().nextInt(odds) == 0;
        }
    }

    private boolean canReachForBellySlam(LivingEntity target) {
        return bellySlamCooldownTicks <= 0
                && target != null
                && target.isAlive()
                && distanceToSqr(target) <= BELLY_SLAM_RANGE * BELLY_SLAM_RANGE
                && getSensing().hasLineOfSight(target);
    }

    private void tickBellySlamFlight() {
        if (bellySlamTakeoffDelayTicks > 0) {
            bellySlamTakeoffDelayTicks--;
            faceBellySlamDirection();
            return;
        }

        if (bellySlamFlightTicks <= 0) {
            bellySlamTarget = null;
            bellySlamDirection = Vec3.ZERO;
            return;
        }

        if (bellySlamFlightTicks == BELLY_SLAM_FLIGHT_TICKS && level() instanceof ServerLevel serverLevel) {
            // The King's takeoff puff, sized down to the Brute's feet.
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(),
                    20, 0.85, 0.1, 0.85, 0.05);
        }
        bellySlamFlightTicks--;
        if (bellySlamDirection.lengthSqr() < 1.0E-4) {
            return;
        }

        faceBellySlamDirection();
        pushBellySlamBlockers();
        Vec3 movement = getDeltaMovement();
        double verticalVelocity = getBellySlamVerticalVelocity();
        setDeltaMovement(bellySlamDirection.x * 0.32, verticalVelocity, bellySlamDirection.z * 0.32);
        hasImpulse = true;
    }

    private void tickBellySlamFacingLock() {
        if (bellySlamFacingLockTicks <= 0) {
            return;
        }

        bellySlamFacingLockTicks--;
        faceBellySlamDirection();
    }

    private double getBellySlamVerticalVelocity() {
        if (bellySlamFlightTicks > 8) {
            return 0.42;
        }

        if (bellySlamFlightTicks > 3) {
            return 0.06;
        }

        return -0.82;
    }

    private void pushBellySlamBlockers() {
        List<LivingEntity> blockers = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(0.95, 0.65, 0.95),
                entity -> entity != this && entity.isAlive());

        for (LivingEntity blocker : blockers) {
            Vec3 away = blocker.position().subtract(position());
            Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);
            if (horizontalAway.lengthSqr() < 1.0E-4) {
                horizontalAway = bellySlamDirection;
            }

            Vec3 direction = horizontalAway.normalize();
            Vec3 movement = blocker.getDeltaMovement();
            blocker.setDeltaMovement(direction.x * 0.55, Math.max(movement.y, 0.25), direction.z * 0.55);
            blocker.hasImpulse = true;
        }
    }

    private void lockBellySlamDirection(LivingEntity target) {
        Vec3 towardTarget = target.position().subtract(position());
        Vec3 horizontal = new Vec3(towardTarget.x, 0.0, towardTarget.z);
        if (horizontal.lengthSqr() < 1.0E-4) {
            horizontal = Vec3.directionFromRotation(0.0F, getYRot());
        }

        bellySlamDirection = horizontal.normalize();
        bellySlamYaw = (float) (Math.atan2(bellySlamDirection.z, bellySlamDirection.x) * 180.0 / Math.PI) - 90.0F;
        faceBellySlamDirection();
    }

    private void faceBellySlamDirection() {
        setYRot(bellySlamYaw);
        yBodyRot = bellySlamYaw;
        yHeadRot = bellySlamYaw;
    }

    private void playAttackAnimation(int attackId) {
        if (!level().isClientSide) {
            entityData.set(DATA_BRUTE_ATTACK_ID, attackId);
            entityData.set(DATA_BRUTE_ATTACK_START_TICK, level().getGameTime());
        }

        // The two slams and the side swipe are the "regular" pool chooseRegularAttack() rotates
        // through; the jump smash and the tantrum have their own cooldown-gated eligibility and are
        // intentionally left out of this tracking.
        if (isWalkingAttack(attackId)) {
            lastRegularAttackId = attackId;
        }

        attackAnimationTicks = getAttackAnimationTicks(attackId);
        activeBruteAttackId = attackId;
    }

    private RawAnimation getAttackAnimation(int attackId) {
        return switch (attackId) {
            case ATTACK_ID_JUMP_SMASH -> JUMP_SMASH;
            case ATTACK_ID_TEMPER_TANTRUM -> TEMPER_TANTRUM;
            case ATTACK_ID_SIDE_SWIPE -> SIDE_SWIPE;
            case ATTACK_ID_REGULAR_ATTACK_2 -> REGULAR_ATTACK_2;
            case ATTACK_ID_REGULAR_ATTACK_3 -> REGULAR_ATTACK_3;
            case ATTACK_ID_BITE -> BITE;
            default -> REGULAR_ATTACK;
        };
    }

    private int getAttackAnimationTicks(int attackId) {
        return switch (attackId) {
            case ATTACK_ID_JUMP_SMASH -> BELLY_SLAM_ANIMATION_TICKS;
            case ATTACK_ID_TEMPER_TANTRUM -> TEMPER_TANTRUM_ANIMATION_TICKS;
            case ATTACK_ID_SIDE_SWIPE -> SIDE_SWIPE_ANIMATION_TICKS;
            case ATTACK_ID_BITE -> BITE_ANIMATION_TICKS;
            default -> REGULAR_ATTACK_ANIMATION_TICKS;
        };
    }

    private void updateIdleAnimationTimers(AnimationState<OgreBrute> state) {
        int tickDelta = updateLastAnimationTick();
        if (tickDelta == 0) {
            return;
        }

        if (idleVariantTicks > 0) {
            idleVariantTicks = Math.max(0, idleVariantTicks - tickDelta);
            if (idleVariantTicks <= 0) {
                activeIdleAnimation = IDLE;
                state.getController().forceAnimationReset();
            }
            return;
        }

        idleAnimationTicks += tickDelta;
        if (idleAnimationTicks >= 160 && getRandom().nextInt(180) == 0) {
            idleAnimationTicks = 0;
            idleVariantTicks = IDLE_VARIANT_TICKS;
            activeIdleAnimation = IDLE_2;
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
}
