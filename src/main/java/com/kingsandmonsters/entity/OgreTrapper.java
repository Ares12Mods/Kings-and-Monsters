package com.kingsandmonsters.entity;

import com.kingsandmonsters.Config;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.effect.CombatEffects;
import com.kingsandmonsters.entity.animation.CanonicalOneShotState;
import com.kingsandmonsters.entity.animation.SynchronizedAnimationController;
import com.kingsandmonsters.item.SkinningKnifeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The Trapper, the tribe's hunter. Quick and mean: two swings thrown over his combat walk, a knife he
 * brings out once he is hurt, and a Bogjaw trap he carries on his back. He either sets the trap down where he stands
 * or pulls it off his back and lobs it at his target.
 */
public class OgreTrapper extends OgreGrunt {
    /** Where the trap is: on his back, in his left hand, or nowhere while he waits for a spare. */
    public static final int TRAP_ON_BACK = 0;
    public static final int TRAP_IN_HAND = 1;
    public static final int TRAP_GONE = 2;
    /** The attack id that plays the full-body deploy clip, where the clip itself moves the trap. */
    public static final int ATTACK_ID_DEPLOY = 5;

    private static final EntityDataAccessor<Integer> DATA_ATTACK_ID =
            SynchedEntityData.defineId(OgreTrapper.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_ATTACK_START_TICK =
            SynchedEntityData.defineId(OgreTrapper.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_TRAP_STATE =
            SynchedEntityData.defineId(OgreTrapper.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_KNIFE_OUT =
            SynchedEntityData.defineId(OgreTrapper.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("IDLE");
    private static final RawAnimation IDLE_2 = RawAnimation.begin().thenLoop("IDLE2");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("WALK");
    private static final RawAnimation COMBAT_WALK = RawAnimation.begin().thenLoop("COMBAT WALK");
    private static final RawAnimation SWING_1 = RawAnimation.begin().thenPlay("REGULAR_ATTACK_1");
    private static final RawAnimation SWING_2 = RawAnimation.begin().thenPlay("REGULAR_ATTACK_2");
    private static final RawAnimation GRAB = RawAnimation.begin().thenPlay("GRAB_BOGJAW_TRAP");
    private static final RawAnimation DEPLOY = RawAnimation.begin().thenPlay("BOGJAW_DEPLOY");
    private static final RawAnimation LOB = RawAnimation.begin().thenPlay("BOGJAW_TRAP_LOB");
    private static final RawAnimation KNIFE_PULL_OUT = RawAnimation.begin().thenPlay("KNIFE_PULL_OUT");
    private static final RawAnimation KNIFE_SLASH = RawAnimation.begin().thenPlay("KNIFE_SLASH");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("BITE");

    // Attack ids: 1 and 2 are the two swings (left arm, right arm), 4 the grab that moves the trap to
    // his hand, 5 the deploy, 6 the lob and 7 the knife coming out. Everything but the deploy and the
    // lob is thrown over the combat walk.
    private static final int ATTACK_ID_SWING_LEFT = 1;
    private static final int ATTACK_ID_SWING_RIGHT = 2;
    private static final int ATTACK_ID_GRAB = 4;
    private static final int ATTACK_ID_LOB = 6;
    private static final int ATTACK_ID_KNIFE_PULL_OUT = 7;
    private static final int ATTACK_ID_KNIFE_SLASH = 8;
    private static final int ATTACK_ID_BITE = 9;

    /** Read by the model for the blend out of a swing thrown over the walk. */
    public static final float ATTACK_OVERLAY_FADE_OUT_TICKS = 4.0F;
    // His clips are 2.25s (45 ticks); the knife pull-out is 2.29s (46 ticks).
    private static final int ATTACK_ANIMATION_TICKS = 45;
    private static final int KNIFE_PULL_OUT_ANIMATION_TICKS = 46;
    private static final int ATTACK_SYNC_DELAY_TICKS = 3;
    // Moments inside the clips, in ticks: the left arm lands at 1.25s, the trap leaves his back for his
    // hand at 1.17s, is set down at 1.08s and is let go of at 1.13s, and the knife is in his fist at 0.88s.
    private static final int SWING_IMPACT_TICK = 25 + ATTACK_SYNC_DELAY_TICKS;
    private static final int GRAB_SWAP_TICK = 23 + ATTACK_SYNC_DELAY_TICKS;
    private static final int DEPLOY_PLACE_TICK = 22 + ATTACK_SYNC_DELAY_TICKS;
    private static final int LOB_RELEASE_TICK = 23 + ATTACK_SYNC_DELAY_TICKS;
    private static final int KNIFE_DRAW_TICK = 18 + ATTACK_SYNC_DELAY_TICKS;
    private static final int KNIFE_IMPACT_TICK = 23 + ATTACK_SYNC_DELAY_TICKS;
    // The bite clip is 2s; his head snaps down on it at 1.13s.
    private static final int BITE_ANIMATION_TICKS = 40;
    private static final int BITE_IMPACT_TICK = 23 + ATTACK_SYNC_DELAY_TICKS;
    private static final int BITE_RECOVERY_TICKS = 50;
    private static final int BITE_ODDS = 4;
    // Like the Brute's bite: a little armor pierce, Regeneration III for four seconds, and the target crippled.
    private static final float BITE_DAMAGE_MULTIPLIER =
            OgreDamage.multiplier(OgreDamage.Tier.TRAPPER, OgreDamage.Blow.LIGHT);
    private static final float BITE_ARMOR_PIERCE_FRACTION = 0.15F;
    private static final int BITE_REGENERATION_TICKS = 80;
    private static final double BITE_REACH = 1.7;
    private static final double BITE_RADIUS = 1.5;
    private static final int SWING_RECOVERY_TICKS = 55;
    private static final int KNIFE_PULL_OUT_RECOVERY_TICKS = 50;
    // He draws the knife once he is down to this share of his health, and puts it away again after
    // this long without a target.
    private static final float KNIFE_HEALTH_FRACTION = 0.7F;
    private static final int KNIFE_STOW_TICKS = 200;
    // A short pause after the grab so he is seen carrying the trap before he throws it.
    private static final int GRAB_RECOVERY_TICKS = 25;
    private static final int TRAP_ACTION_RECOVERY_TICKS = 35;

    // Raw damage before armor: a bit under the Brute's slams, with a small area around each blow.
    private static final float SWING_DAMAGE_MULTIPLIER =
            OgreDamage.multiplier(OgreDamage.Tier.TRAPPER, OgreDamage.Blow.STANDARD);
    private static final double SWING_REACH = 2.1;
    private static final double SWING_SIDE_OFFSET = 0.5;
    private static final float SWING_RADIUS = 2.2F;
    private static final double SWING_PUSH = 0.75;
    // The knife cuts deeper than a fist: a tenth over a swing, still under a Brute's slam.
    private static final float KNIFE_DAMAGE_MULTIPLIER =
            OgreDamage.multiplier(OgreDamage.Tier.TRAPPER, OgreDamage.Blow.STANDARD) * 1.1F;
    private static final double KNIFE_REACH = 2.2;
    private static final float KNIFE_RADIUS = 1.7F;
    private static final double SWING_START_RANGE = 3.6;

    // Combat speed: 0.34 x 0.735 is 0.25, a little ahead of the Brute's 0.21 without being a sprinter.
    private static final double COMBAT_WALK_SPEED = 0.735;
    private static final double COMBAT_WALK_TARGET_GAP = 0.9;
    private static final double COMBAT_WALK_MAX_VERTICAL_DIFFERENCE = 3.5;

    // He decides to use a trap from far out, and rolls often enough that he usually does before he closes in.
    private static final double TRAP_RANGE = 26.0;
    private static final double TRAP_MIN_RANGE = 8.0;
    private static final int TRAP_ROLL_ODDS = 6;
    private static final float DEPLOY_CHANCE = 0.4F;
    // How long he waits for a spare once a trap is spent, and how long he will carry one unused.
    private static final int TRAP_REARM_TICKS = 160;
    private static final int TRAP_HOLD_TICKS = 200;
    private static final int MAX_LIVE_TRAPS = 3;
    // Fighting around the traps: how far he looks for one, the radius of ground around it he fights on,
    // how far inside its edge he waits, the target distance inside which he just fights, how long he waits
    // before giving up, and how long before he tries again.
    private static final double LURE_TRAP_SEARCH = 20.0;
    private static final double LURE_ZONE_RADIUS = 12.0;
    private static final double LURE_EDGE_INSET = 3.0;
    private static final double LURE_MIN_TARGET_DISTANCE = 6.0;
    // Hit and set a trap: after a slash he may back off a few blocks and put a trap down between them.
    private static final float SKIRMISH_CHANCE = 0.5F;
    private static final int SKIRMISH_RETREAT_RADIUS = 9;
    private static final int SKIRMISH_MAX_TICKS = 45;
    private static final double SKIRMISH_MAX_START_DISTANCE = 4.5;
    private static final int LURE_MAX_TICKS = 160;
    private static final int LURE_COOLDOWN_TICKS = 240;
    private static final double THROW_SPEED = 0.7;
    private static final int IDLE_VARIANT_TICKS = 80;
    private static final int MOVEMENT_ANIMATION_GRACE_TICKS = 4;
    private static final int TRAPPER_XP_REWARD = 150;

    // Animation ticks where a foot lands, from the leg keyframes.
    private static final Map<String, double[]> FOOT_CONTACTS = Map.of(
            "WALK", new double[]{0.0, 20.0},
            "COMBAT WALK", new double[]{0.0, 15.4});

    private int idleAnimationTicks;
    private int idleVariantTicks;
    private int lastAnimationTick = -1;
    private long lastMovingAnimationTick = Long.MIN_VALUE / 2;
    private int activeAttackId;
    private int strikeDelayTicks = -1;
    private int knifeIdleTicks;
    private Vec3 lureSpot;
    private Vec3 retreatSpot;
    private int skirmishTicks;
    private boolean placeTrapNext;
    private int lureTicks;
    private int lureCooldown;
    private int trapRearmTicks;
    private int heldTrapTicks;
    private boolean trapActionReady;
    private boolean attackOverlayVisible;
    private RawAnimation activeIdleAnimation = IDLE;
    private final CanonicalOneShotState visualOneShot = new CanonicalOneShotState();
    private final CanonicalOneShotState visualOverlayOneShot = new CanonicalOneShotState();

    public OgreTrapper(EntityType<? extends OgreTrapper> type, Level level) {
        super(type, level);
        avoidWaterPathfinding();
        xpReward = TRAPPER_XP_REWARD;
    }

    @Override

    public float maxUpStep() {

        return 1.0F;

    }


    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 130.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.FOLLOW_RANGE, 34.0)
                .add(Attributes.ARMOR, 13.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    public void applyConfiguredCombatAttributes(boolean healToMax) {
        // Not configurable yet: keep the attributes above rather than the Grunt's configured ones.
        applyConfiguredHealth(healToMax);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ATTACK_ID, 0);
        this.entityData.define(DATA_ATTACK_START_TICK, 0L);
        this.entityData.define(DATA_TRAP_STATE, TRAP_ON_BACK);
        this.entityData.define(DATA_KNIFE_OUT, false);
    }

    /** Whether the knife is in his right hand: from the moment he draws it until the fight is over. */
    public boolean isKnifeOut() {
        return entityData.get(DATA_KNIFE_OUT);
    }

    /** Where the trap is right now, for the model to show it in the right place. */
    public int getTrapState() {
        return entityData.get(DATA_TRAP_STATE);
    }

    /** The synced attack id. While the deploy plays, the clip decides which trap model shows. */
    public int getSyncedAttackId() {
        return entityData.get(DATA_ATTACK_ID);
    }

    private void setTrapState(int state) {
        entityData.set(DATA_TRAP_STATE, state);
    }

    // ------------------------------------------------------------------ //
    //  Server tick                                                        //
    // ------------------------------------------------------------------ //

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        if (isDeadOrDying()) {
            strikeDelayTicks = -1;
            return;
        }
        tickKnife();
        tickLure();
        tickSkirmish();
        tickTrapSupply();
        tickTrapActionReady();
        tickStrike();
    }

    /**
     * After a hit he sometimes backs off and sets a trap down in the gap, then goes back to fighting.
     * It uses the walk he already has for the retreat and the deploy clip he already has for the trap.
     */
    private void maybeStartSkirmish() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || getTrapState() != TRAP_ON_BACK || skirmishTicks > 0 || placeTrapNext
                || distanceToSqr(target) > SKIRMISH_MAX_START_DISTANCE * SKIRMISH_MAX_START_DISTANCE
                || getRandom().nextFloat() >= SKIRMISH_CHANCE
                || level().getEntitiesOfClass(BogjawTrap.class, getBoundingBox().inflate(64.0),
                        other -> getUUID().equals(other.getOwnerId()) && other.getPhase() != BogjawTrap.PHASE_CLAMPED)
                        .size() >= MAX_LIVE_TRAPS) {
            return;
        }
        Vec3 spot = LandRandomPos.getPosAway(this, SKIRMISH_RETREAT_RADIUS, 4, target.position());
        if (spot != null) {
            retreatSpot = spot;
            skirmishTicks = SKIRMISH_MAX_TICKS;
            trapActionReady = false;
        }
    }

    private void tickSkirmish() {
        LivingEntity target = getTarget();
        if (placeTrapNext && (target == null || !target.isAlive() || getTrapState() != TRAP_ON_BACK)) {
            placeTrapNext = false;
        }
        if (skirmishTicks <= 0) {
            return;
        }
        skirmishTicks--;
        boolean arrived = retreatSpot == null || distanceToSqr(retreatSpot) <= 2.25;
        if (target == null || !target.isAlive() || getTrapState() != TRAP_ON_BACK) {
            skirmishTicks = 0;
        } else if (arrived || skirmishTicks == 0) {
            skirmishTicks = 0;
            placeTrapNext = true;
        }
        if (skirmishTicks == 0) {
            retreatSpot = null;
        }
    }

    /**
     * Fighting around his traps: he keeps to the ground around an armed trap, and does not wait there
     * forever for a target that will not come. Once the target is caught he goes in for it.
     */
    private void tickLure() {
        LivingEntity target = getTarget();
        if (lureCooldown > 0) {
            lureCooldown--;
        }
        if (target == null || !target.isAlive() || lureCooldown > 0 || target.hasEffect(ModMobEffects.HAMSTRUNG.get())) {
            lureSpot = null;
            lureTicks = 0;
            return;
        }
        if (lureSpot != null && ++lureTicks > LURE_MAX_TICKS) {
            lureSpot = null;
            lureTicks = 0;
            lureCooldown = LURE_COOLDOWN_TICKS;
            return;
        }
        if (tickCount % 5 == 0) {
            lureSpot = findLureSpot(target);
            if (lureSpot == null) {
                lureTicks = 0;
            }
        }
    }

    /**
     * With a trap armed nearby he fights anywhere within a wide circle around it. A target outside the
     * circle does not draw him out of it: he waits at the near edge, facing it, for the target to come in.
     * Inside the circle, or once the target is close, he fights as normal.
     */
    private Vec3 findLureSpot(LivingEntity target) {
        if (distanceToSqr(target) < LURE_MIN_TARGET_DISTANCE * LURE_MIN_TARGET_DISTANCE) {
            return null;
        }
        BogjawTrap trap = level().getEntitiesOfClass(BogjawTrap.class, getBoundingBox().inflate(LURE_TRAP_SEARCH),
                        other -> getUUID().equals(other.getOwnerId()) && other.getPhase() == BogjawTrap.PHASE_ARMED)
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (trap == null) {
            return null;
        }
        double dx = target.getX() - trap.getX();
        double dz = target.getZ() - trap.getZ();
        double fromTrap = Math.sqrt(dx * dx + dz * dz);
        if (fromTrap <= LURE_ZONE_RADIUS) {
            return null;
        }
        double edge = LURE_ZONE_RADIUS - LURE_EDGE_INSET;
        return new Vec3(trap.getX() + dx / fromTrap * edge, trap.getY(), trap.getZ() + dz / fromTrap * edge);
    }

    @Override
    protected Vec3 getPursuitGoalOverride(LivingEntity target) {
        return skirmishTicks > 0 ? retreatSpot : lureSpot;
    }

    /** The knife stays in his hand for the whole fight and goes away once he has been left alone a while. */
    private void tickKnife() {
        if (!isKnifeOut()) {
            return;
        }
        if (getTarget() != null || isMidAction()) {
            knifeIdleTicks = 0;
        } else if (++knifeIdleTicks >= KNIFE_STOW_TICKS) {
            entityData.set(DATA_KNIFE_OUT, false);
            knifeIdleTicks = 0;
        }
    }

    /** A spent trap is replaced after a while; one he is carrying and never used goes back on his belt. */
    private void tickTrapSupply() {
        int state = getTrapState();
        if (state == TRAP_GONE) {
            if (trapRearmTicks > 0 && --trapRearmTicks == 0) {
                setTrapState(TRAP_ON_BACK);
            }
        } else if (state == TRAP_IN_HAND && !isMidAction() && heldTrapTicks > 0 && --heldTrapTicks == 0) {
            setTrapState(TRAP_ON_BACK);
        }
    }

    /**
     * Rolls for a trap action on its own schedule, so the walk toward his target is left alone until
     * he actually uses it. Once it is ready his attack range stretches out to the throw's, and the
     * next attack he starts is the trap.
     */
    private void tickTrapActionReady() {
        LivingEntity target = getTarget();
        int state = getTrapState();
        if (isMidAction() || state == TRAP_GONE || target == null || !target.isAlive()
                || distanceToSqr(target) > TRAP_RANGE * TRAP_RANGE || !getSensing().hasLineOfSight(target)) {
            trapActionReady = false;
            return;
        }
        if (state == TRAP_IN_HAND) {
            trapActionReady = true;
        } else if (!trapActionReady && distanceToSqr(target) >= TRAP_MIN_RANGE * TRAP_MIN_RANGE) {
            trapActionReady = getRandom().nextInt(TRAP_ROLL_ODDS) == 0;
        }
    }

    private boolean isMidAction() {
        return activeAttackId != 0;
    }

    private static boolean isWalkingAttack(int attackId) {
        return attackId != 0 && !isPlantedAction(attackId);
    }

    /** The deploy and the throw plant his feet; the swings and the grab happen on the move. */
    private static boolean isPlantedAction(int attackId) {
        return attackId == ATTACK_ID_DEPLOY || attackId == ATTACK_ID_LOB;
    }

    // ------------------------------------------------------------------ //
    //  Attack choice                                                      //
    // ------------------------------------------------------------------ //

    /** Hurt past the line he draws the knife as soon as the animation he is in is over, close or not. */
    @Override
    protected boolean wantsImmediateAttack() {
        if (isMidAction() || getTarget() == null) {
            return false;
        }
        boolean hurtPastTheLine = !isKnifeOut() && getHealth() <= getMaxHealth() * KNIFE_HEALTH_FRACTION;
        return hurtPastTheLine || placeTrapNext && skirmishTicks == 0;
    }

    @Override
    protected MeleeAttackPlan startMeleeAttack() {
        // Hurt past the line, the next thing he does is bring out the knife.
        if (!isKnifeOut() && getHealth() <= getMaxHealth() * KNIFE_HEALTH_FRACTION) {
            trapActionReady = false;
            playAttackAnimation(ATTACK_ID_KNIFE_PULL_OUT);
            strikeDelayTicks = KNIFE_DRAW_TICK;
            return new MeleeAttackPlan(KNIFE_PULL_OUT_RECOVERY_TICKS, KNIFE_PULL_OUT_ANIMATION_TICKS, List.of());
        }
        // Backed off after a hit: put the trap down in the gap, facing his target, whatever he had rolled.
        if (placeTrapNext && getTrapState() == TRAP_ON_BACK) {
            placeTrapNext = false;
            trapActionReady = false;
            return startDeploy();
        }
        int state = getTrapState();
        if (trapActionReady && state == TRAP_IN_HAND) {
            trapActionReady = false;
            playAttackAnimation(ATTACK_ID_LOB);
            strikeDelayTicks = LOB_RELEASE_TICK;
            return new MeleeAttackPlan(TRAP_ACTION_RECOVERY_TICKS, ATTACK_ANIMATION_TICKS, List.of());
        }
        if (trapActionReady && state == TRAP_ON_BACK) {
            trapActionReady = false;
            if (getRandom().nextFloat() < DEPLOY_CHANCE) {
                return startDeploy();
            }
            playAttackAnimation(ATTACK_ID_GRAB);
            strikeDelayTicks = GRAB_SWAP_TICK;
            return new MeleeAttackPlan(GRAB_RECOVERY_TICKS, ATTACK_ANIMATION_TICKS, List.of());
        }

        // One attack in four is the bite.
        if (getRandom().nextInt(BITE_ODDS) == 0) {
            playAttackAnimation(ATTACK_ID_BITE);
            strikeDelayTicks = BITE_IMPACT_TICK;
            return new MeleeAttackPlan(BITE_RECOVERY_TICKS, BITE_ANIMATION_TICKS, List.of());
        }
        // With the knife in his right fist only the left swing is left beside the blade.
        if (isKnifeOut() && getRandom().nextBoolean()) {
            playAttackAnimation(ATTACK_ID_KNIFE_SLASH);
            strikeDelayTicks = KNIFE_IMPACT_TICK;
        } else {
            playAttackAnimation(isKnifeOut() || getRandom().nextBoolean() ? ATTACK_ID_SWING_LEFT : ATTACK_ID_SWING_RIGHT);
            strikeDelayTicks = SWING_IMPACT_TICK;
        }
        return new MeleeAttackPlan(SWING_RECOVERY_TICKS, ATTACK_ANIMATION_TICKS, List.of());
    }

    /** Sets his feet and starts the deploy clip; the trap itself goes down at the clip's placing tick. */
    private MeleeAttackPlan startDeploy() {
        playAttackAnimation(ATTACK_ID_DEPLOY);
        strikeDelayTicks = DEPLOY_PLACE_TICK;
        getNavigation().stop();
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        return new MeleeAttackPlan(TRAP_ACTION_RECOVERY_TICKS, ATTACK_ANIMATION_TICKS, List.of());
    }

    private void playAttackAnimation(int attackId) {
        if (!level().isClientSide) {
            entityData.set(DATA_ATTACK_ID, attackId);
            entityData.set(DATA_ATTACK_START_TICK, level().getGameTime());
        }
        activeAttackId = attackId;
    }

    @Override
    protected void onMeleeAttackPlanFinished() {
        activeAttackId = 0;
        super.onMeleeAttackPlanFinished();
        if (!level().isClientSide) {
            entityData.set(DATA_ATTACK_ID, 0);
            entityData.set(DATA_ATTACK_START_TICK, 0L);
        }
    }

    private int getAttackAnimationTicks(int attackId) {
        return switch (attackId) {
            case ATTACK_ID_KNIFE_PULL_OUT -> KNIFE_PULL_OUT_ANIMATION_TICKS;
            case ATTACK_ID_BITE -> BITE_ANIMATION_TICKS;
            default -> ATTACK_ANIMATION_TICKS;
        };
    }

    private RawAnimation getAttackAnimation(int attackId) {
        return switch (attackId) {
            case ATTACK_ID_SWING_RIGHT -> SWING_2;
            case ATTACK_ID_KNIFE_PULL_OUT -> KNIFE_PULL_OUT;
            case ATTACK_ID_KNIFE_SLASH -> KNIFE_SLASH;
            case ATTACK_ID_BITE -> BITE;
            case ATTACK_ID_GRAB -> GRAB;
            case ATTACK_ID_DEPLOY -> DEPLOY;
            case ATTACK_ID_LOB -> LOB;
            default -> SWING_1;
        };
    }

    // ------------------------------------------------------------------ //
    //  What each clip does at its key moment                              //
    // ------------------------------------------------------------------ //

    private void tickStrike() {
        if (strikeDelayTicks < 0) {
            return;
        }
        if (activeAttackId == 0) {
            strikeDelayTicks = -1;
            return;
        }
        if (strikeDelayTicks-- == 0) {
            strikeDelayTicks = -1;
            onStrike();
        }
    }

    private void onStrike() {
        switch (activeAttackId) {
            // The model's left arm swings first (entity-right is the other side).
            case ATTACK_ID_SWING_LEFT -> {
                performSwing(-1.0, SWING_DAMAGE_MULTIPLIER);
                maybeStartSkirmish();
            }
            case ATTACK_ID_SWING_RIGHT -> {
                performSwing(1.0, SWING_DAMAGE_MULTIPLIER);
                maybeStartSkirmish();
            }
            case ATTACK_ID_KNIFE_SLASH -> {
                performKnifeSlash();
                maybeStartSkirmish();
            }
            case ATTACK_ID_BITE -> performBite();
            case ATTACK_ID_KNIFE_PULL_OUT -> {
                entityData.set(DATA_KNIFE_OUT, true);
                level().playSound(null, getX(), getY() + 1.5, getZ(), SoundEvents.CHAIN_PLACE,
                        SoundSource.HOSTILE, 1.0F, 1.6F);
                level().playSound(null, getX(), getY() + 1.5, getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.HOSTILE, 0.6F, 1.5F);
            }
            case ATTACK_ID_GRAB -> {
                setTrapState(TRAP_IN_HAND);
                heldTrapTicks = TRAP_HOLD_TICKS;
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.0F, 0.7F);
            }
            case ATTACK_ID_DEPLOY -> placeTrap();
            case ATTACK_ID_LOB -> throwTrap();
            default -> {
            }
        }
    }

    /**
     * One swing: the blow comes down a little way in front of him, to the side of the swinging arm,
     * and hurts a small area around that point, hardest at the centre. It is a plain thud of flesh
     * and dirt, with no metal in it.
     */
    private void performSwing(double rightSign, float damageMultiplier) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 impact = position().add(forward.scale(SWING_REACH)).add(right.scale(SWING_SIDE_OFFSET * rightSign));
        double radiusSqr = (double) SWING_RADIUS * SWING_RADIUS;

        List<LivingEntity> candidates = level().getEntitiesOfClass(LivingEntity.class,
                new AABB(impact.x - SWING_RADIUS, getY() - 1.0, impact.z - SWING_RADIUS,
                        impact.x + SWING_RADIUS, getY() + 3.0, impact.z + SWING_RADIUS),
                target -> target != this && target.isAlive() && canAttackOgreTarget(target));
        for (LivingEntity target : candidates) {
            double dx = target.getX() - impact.x;
            double dz = target.getZ() - impact.z;
            double distanceSqr = dx * dx + dz * dz;
            if (distanceSqr > radiusSqr) {
                continue;
            }
            float falloff = areaFalloff(Math.sqrt(distanceSqr), SWING_RADIUS, OgreDamage.BLAST_EDGE_SHARE);
            if (doScaledHurtTarget(target, damageMultiplier * falloff)) {
                Vec3 away = distanceSqr < 1.0E-3 ? forward : new Vec3(dx, 0.0, dz).normalize();
                double carried = 1.0 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
                Vec3 motion = target.getDeltaMovement();
                // The shove stays firm toward the rim; only the damage fades there.
                double push = SWING_PUSH * Math.max(falloff, 0.7F) * carried;
                target.setDeltaMovement(away.x * push, Math.max(motion.y, 0.25 * carried), away.z * push);
                target.hasImpulse = true;
            }
        }
        playSwingImpact(serverLevel, impact);
    }

    /**
     * The bite: one target, the nearest within reach of his jaws. It pierces a little armor, cripples
     * the target and heals him, as a Grunt's bite does, with the same sounds a notch deeper.
     */
    private void performBite() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 jaws = position().add(forward.scale(BITE_REACH));
        LivingEntity victim = level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(jaws.x - BITE_RADIUS, getY() - 1.0, jaws.z - BITE_RADIUS,
                                jaws.x + BITE_RADIUS, getY() + 3.0, jaws.z + BITE_RADIUS),
                        target -> target != this && target.isAlive() && canAttackOgreTarget(target)).stream()
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(jaws.x, target.getY(), jaws.z)))
                .orElse(null);
        level().playSound(null, getX(), getY() + 1.5, getZ(), SoundEvents.FOX_BITE, SoundSource.HOSTILE, 0.85F, 0.6F);
        level().playSound(null, getX(), getY() + 1.5, getZ(), SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 0.35F, 0.55F);
        if (victim != null && doScaledHurtTarget(victim, BITE_DAMAGE_MULTIPLIER, BITE_ARMOR_PIERCE_FRACTION)) {
            CombatEffects.applyCrippled(victim, Config.CRIPPLED_BITE_DURATION_TICKS.get(), this);
            addEffect(new MobEffectInstance(MobEffects.REGENERATION, BITE_REGENERATION_TICKS, 2));
        }
    }

    /**
     * The knife slash: a narrower arc than a swing, from the right hand, that hits harder and cuts. Whatever
     * it catches is slowed, hamstrung and left bleeding, as the trapper's knife does in a player's hand.
     */
    private void performKnifeSlash() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 impact = position().add(forward.scale(KNIFE_REACH)).add(right.scale(SWING_SIDE_OFFSET));
        boolean cut = false;
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                new AABB(impact.x - KNIFE_RADIUS, getY() - 1.0, impact.z - KNIFE_RADIUS,
                        impact.x + KNIFE_RADIUS, getY() + 3.0, impact.z + KNIFE_RADIUS),
                target -> target != this && target.isAlive() && canAttackOgreTarget(target))) {
            double dx = target.getX() - impact.x;
            double dz = target.getZ() - impact.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > KNIFE_RADIUS) {
                continue;
            }
            float falloff = areaFalloff(distance, KNIFE_RADIUS, OgreDamage.ARC_EDGE_SHARE);
            if (doScaledHurtTarget(target, KNIFE_DAMAGE_MULTIPLIER * falloff)) {
                SkinningKnifeItem.applyKnifeCut(target, this);
                cut = true;
            }
        }
        // A bright swish through the air, and a crit ping when the blade lands.
        serverLevel.playSound(null, impact.x, impact.y + 1.0, impact.z, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.1F, 1.25F);
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, impact.x, impact.y + 1.0, impact.z, 1, 0.0, 0.0, 0.0, 0.0);
        if (cut) {
            serverLevel.playSound(null, impact.x, impact.y + 1.0, impact.z, SoundEvents.PLAYER_ATTACK_CRIT,
                    SoundSource.HOSTILE, 0.8F, 1.3F);
            serverLevel.sendParticles(ParticleTypes.CRIT, impact.x, impact.y + 1.0, impact.z, 12, 0.3, 0.3, 0.3, 0.4);
        }
    }

    /** The Grunt's smack, a notch heavier: the thump, the mud and ground layers, debris, dust and shake. */
    private void playSwingImpact(ServerLevel serverLevel, Vec3 impact) {
        float power = getSmackPower();
        serverLevel.playSound(null, impact.x, impact.y, impact.z, SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.HOSTILE, 0.8F * power, 0.75F / power);
        serverLevel.playSound(null, impact.x, impact.y, impact.z, SoundEvents.MUD_HIT,
                SoundSource.HOSTILE, 0.5F * power, 0.7F);
        playSlamLayers(impact, 0.55F * power, 0.1F - 0.05F * (power - 1.0F));
        spawnSmackGroundImpact(serverLevel, impact);
    }

    @Override
    protected float getSmackPower() {
        return 1.15F;
    }

    /** Sets the trap down in front of him; the deploy clip has already taken it off his back. */
    private void placeTrap() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 spot = position().add(forward.scale(1.7)).subtract(right.scale(0.3));
        Vec3 from = new Vec3(spot.x, getY() + 1.0, spot.z);
        BlockHitResult ground = level().clip(new ClipContext(from, from.add(0.0, -4.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double y = ground.getType() == HitResult.Type.MISS ? getY() : ground.getLocation().y;
        spawnTrap(BogjawTrap.placed(level(), this, new Vec3(spot.x, y, spot.z)));
        spendTrap();
    }

    /**
     * Lobs the trap from his left hand at his target. The velocity is worked out so that, with the
     * trap's own gravity and no drag, it arrives at the target's feet.
     */
    private void throwTrap() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 hand = position().add(forward.scale(0.9)).subtract(right.scale(1.0)).add(0.0, 1.9, 0.0);
        LivingEntity target = getTarget();
        Vec3 goal = target != null && target.isAlive() ? target.position() : position().add(forward.scale(8.0));
        double dx = goal.x - hand.x;
        double dz = goal.z - hand.z;
        int flightTicks = Mth.clamp((int) Math.ceil(Math.sqrt(dx * dx + dz * dz) / THROW_SPEED), 10, 40);
        double vy = (goal.y + 0.15 - hand.y + BogjawTrap.GRAVITY * flightTicks * (flightTicks - 1) / 2.0) / flightTicks;
        spawnTrap(BogjawTrap.thrown(level(), this, hand, new Vec3(dx / flightTicks, vy, dz / flightTicks)));
        level().playSound(null, getX(), getY() + 1.5, getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.0F, 0.6F);
        level().playSound(null, getX(), getY() + 1.5, getZ(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 0.8F, 0.8F);
        spendTrap();
    }

    private void spawnTrap(BogjawTrap trap) {
        level().addFreshEntity(trap);
        List<BogjawTrap> mine = new ArrayList<>(level().getEntitiesOfClass(BogjawTrap.class,
                getBoundingBox().inflate(64.0),
                other -> getUUID().equals(other.getOwnerId()) && other.getPhase() != BogjawTrap.PHASE_CLAMPED));
        // Past his limit the oldest trap is picked up and forgotten.
        mine.sort(Comparator.comparingInt((BogjawTrap other) -> other.tickCount).reversed());
        while (mine.size() > MAX_LIVE_TRAPS) {
            mine.remove(0).discard();
        }
    }

    private void spendTrap() {
        setTrapState(TRAP_GONE);
        trapRearmTicks = TRAP_REARM_TICKS;
        heldTrapTicks = 0;
    }

    // ------------------------------------------------------------------ //
    //  Melee flow: the same walking-fighter set-up as the Brute           //
    // ------------------------------------------------------------------ //

    @Override
    protected double getMeleeAttackReachSqr(LivingEntity target) {
        double reach = trapActionReady ? TRAP_RANGE : SWING_START_RANGE;
        return reach * reach;
    }

    @Override
    protected boolean isAttackCommitted() {
        return activeAttackId != 0;
    }

    @Override
    protected boolean shouldFaceMeleeTarget(LivingEntity target) {
        return true;
    }

    @Override
    protected boolean canCombatWalkDuringAttack() {
        return isWalkingAttack(activeAttackId);
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
        return target != null && target.isAlive() && (!isMidAction() || isWalkingAttack(activeAttackId));
    }

    @Override
    protected double getCombatWalkSpeedModifier() {
        return COMBAT_WALK_SPEED;
    }

    @Override
    protected boolean shouldHoldCombatSpacing(LivingEntity target) {
        double x = target.getX() - getX();
        double z = target.getZ() - getZ();
        double preferred = (getBbWidth() + target.getBbWidth()) * 0.5 + COMBAT_WALK_TARGET_GAP;
        return x * x + z * z <= preferred * preferred
                && Math.abs(target.getY() - getY()) <= COMBAT_WALK_MAX_VERTICAL_DIFFERENCE;
    }

    @Override
    protected boolean shouldDirectlyTrackMeleeTargetWhilePursuing() {
        return false;
    }

    @Override
    protected boolean shouldStayStationaryDuringMeleeAttack() {
        return isPlantedAction(activeAttackId);
    }

    @Override
    protected double getMeleePursuitSpeedModifier() {
        return 1.0;
    }

    @Override
    protected double getWanderSpeedModifier() {
        return 0.14 / 0.34;
    }

    @Override
    protected int getPathRecalculationTicks() {
        return 6 + getRandom().nextInt(4);
    }

    @Override
    public boolean canDisableShield() {
        // The bite puts a shield on cooldown, as it does for a Grunt.
        return activeAttackId == ATTACK_ID_SWING_LEFT || activeAttackId == ATTACK_ID_SWING_RIGHT
                || activeAttackId == ATTACK_ID_BITE || super.canDisableShield();
    }

    // ------------------------------------------------------------------ //
    //  Sounds                                                             //
    // ------------------------------------------------------------------ //

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.OGRE_TRAPPER_HURT.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.OGRE_TRAPPER_IDLE.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.OGRE_TRAPPER_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return 1.0F;
    }

    @Override
    protected boolean usesAnimatedFootsteps() {
        return true;
    }

    @Override
    protected float getStepVolumeMultiplier() {
        return 1.2F;
    }

    /** Client-side: a dull thud each time a foot visibly lands. */
    private void playFootfall() {
        if (!onGround() || isInWater() || isSilent()) {
            return;
        }
        float variation = getRandom().nextFloat() * 0.08F;
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.WARDEN_STEP, SoundSource.HOSTILE,
                0.5F, 0.95F + variation, false);
        BlockPos ground = getOnPos();
        var surface = level().getBlockState(ground).getSoundType(level(), ground, this);
        level().playLocalSound(getX(), getY(), getZ(), surface.getStepSound(), SoundSource.HOSTILE,
                0.55F, 0.8F + variation, false);
    }

    // ------------------------------------------------------------------ //
    //  Animation                                                          //
    // ------------------------------------------------------------------ //

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new SynchronizedAnimationController<>(this, "Trapper Movement", 5, this::animateTrapper)
                .withFootfalls(FOOT_CONTACTS, this::playFootfall));
        // Registered second so it is applied after the walk: his swings only key the upper body, and
        // this controller blends them in from whatever pose the walk has him in.
        controllers.add(new SynchronizedAnimationController<>(this, "Trapper Attack Overlay", 5,
                this::animateAttackOverlay));
    }

    @Override
    public boolean isAttackOverlayVisible() {
        return attackOverlayVisible;
    }

    @Override
    public double getBoneResetTime() {
        return 6.0;
    }

    private PlayState animateAttackOverlay(AnimationState<OgreTrapper> state) {
        state.setControllerSpeed(1.0F);
        if (isDeadOrDying()) {
            visualOverlayOneShot.clear();
            attackOverlayVisible = false;
            return PlayState.STOP;
        }
        int syncedAttackId = entityData.get(DATA_ATTACK_ID);
        boolean overlayAttack = isWalkingAttack(syncedAttackId);
        int visualAttackId = visualOverlayOneShot.update(state.getController(),
                overlayAttack ? syncedAttackId : 0,
                overlayAttack ? entityData.get(DATA_ATTACK_START_TICK) : 0L,
                this::getAttackAnimationTicks, level().getGameTime());
        attackOverlayVisible = visualAttackId > 0;
        return attackOverlayVisible ? state.setAndContinue(getAttackAnimation(visualAttackId)) : PlayState.STOP;
    }

    private PlayState animateTrapper(AnimationState<OgreTrapper> state) {
        if (isDeadOrDying()) {
            visualOneShot.clear();
            state.setControllerSpeed(1.0F);
            return state.setAndContinue(IDLE);
        }
        int syncedAttackId = entityData.get(DATA_ATTACK_ID);
        boolean overlayAttack = isWalkingAttack(syncedAttackId);
        int visualAttackId = visualOneShot.update(state.getController(),
                overlayAttack ? 0 : syncedAttackId,
                overlayAttack ? 0L : entityData.get(DATA_ATTACK_START_TICK),
                this::getAttackAnimationTicks, level().getGameTime());
        if (visualAttackId > 0) {
            return state.setAndContinue(getAttackAnimation(visualAttackId));
        }

        // isAggressive() is synced, unlike getTarget(), which is always null on the client.
        if (isAggressive() || isRunMovementState()) {
            idleAnimationTicks = 0;
            idleVariantTicks = 0;
            return state.setAndContinue(isMovingForAnimation(state) ? COMBAT_WALK : IDLE);
        }
        if (isMovingForAnimation(state)) {
            idleAnimationTicks = 0;
            idleVariantTicks = 0;
            return state.setAndContinue(WALK);
        }

        updateIdleAnimationTimers(state);
        return state.setAndContinue(idleVariantTicks > 0 ? activeIdleAnimation : IDLE);
    }

    private boolean isMovingForAnimation(AnimationState<OgreTrapper> state) {
        boolean moving = state.isMoving() || getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
        long now = level().getGameTime();
        if (moving) {
            lastMovingAnimationTick = now;
            return true;
        }
        return now - lastMovingAnimationTick <= MOVEMENT_ANIMATION_GRACE_TICKS;
    }

    /** Now and then, while he stands about, he plays the rare idle where he looks around. */
    private void updateIdleAnimationTimers(AnimationState<OgreTrapper> state) {
        if (lastAnimationTick == tickCount) {
            return;
        }
        int tickDelta = lastAnimationTick < 0 ? 1 : Math.max(1, tickCount - lastAnimationTick);
        lastAnimationTick = tickCount;

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
}
