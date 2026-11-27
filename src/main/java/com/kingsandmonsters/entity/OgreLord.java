package com.kingsandmonsters.entity;

import com.kingsandmonsters.ModParticles;
import com.kingsandmonsters.advancement.RoyalAdvancementEvents;
import com.kingsandmonsters.effect.SonicDamage;
import com.kingsandmonsters.enchantment.ModEnchantments;
import com.kingsandmonsters.network.KingDialogueActionPayload;
import com.kingsandmonsters.network.KingDialoguePayload;
import com.kingsandmonsters.network.OgreOverlayPayload;
import com.kingsandmonsters.tribute.BogIronProgression;
import com.kingsandmonsters.tribute.QuestManager;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import com.kingsandmonsters.Config;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.tribute.TributeManager;
import com.kingsandmonsters.tribute.TributeSavedData;
import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.effect.CombatEffects;
import com.kingsandmonsters.entity.animation.SynchronizedAnimationController;
import com.kingsandmonsters.entity.animation.CanonicalOneShotState;
import com.kingsandmonsters.network.ScreenShakePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.ChatFormatting;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class OgreLord extends OgreGrunt {
    private static final int LORD_XP_REWARD = 700;

    @Override
    protected float getStepPitchMultiplier() {
        return 0.55F;
    }

    @Override
    protected float getStepVolumeMultiplier() {
        return 2.5F;
    }

    @Override
    protected boolean usesAnimatedFootsteps() {
        return true;
    }

    // Animation ticks where a foot lands, from the leg keyframes: the foot is lifted half a stride
    // earlier, comes down here, then drags back under the body while the other leg swings.
    private static final Map<String, double[]> FOOT_CONTACTS = Map.of(
            "walk", new double[]{0.0, 40.0},
            "Combat walk", new double[]{0.0, 20.83});

    /** Client-side: a heavy stomp each time a foot visibly lands. */
    private void playFootfall() {
        if (!onGround() || isInWater() || isSilent()) {
            return;
        }
        float variation = getRandom().nextFloat() * 0.08F;
        // A dull thud with no metal in it: the warden's footfall and a heavy landing, pitched down,
        // with the ground's own step sound underneath so mud, stone and wood each sound different.
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.WARDEN_STEP, SoundSource.HOSTILE,
                1.6F, 0.55F + variation, false);
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.HOSTILE_BIG_FALL, SoundSource.HOSTILE,
                0.9F, 0.6F + variation, false);
        BlockPos ground = getOnPos();
        SoundType surface = level().getBlockState(ground).getSoundType(level(), ground, this);
        level().playLocalSound(getX(), getY(), getZ(), surface.getStepSound(), SoundSource.HOSTILE,
                1.4F, 0.5F + variation, false);
    }
    private static final EntityDataAccessor<Integer> DATA_LORD_ATTACK_ID =
            SynchedEntityData.defineId(OgreLord.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_LORD_ATTACK_START_TICK =
            SynchedEntityData.defineId(OgreLord.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_LORD_PHASE_TWO =
            SynchedEntityData.defineId(OgreLord.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LORD_TRANSITIONING =
            SynchedEntityData.defineId(OgreLord.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LORD_CLUB_IN_HAND =
            SynchedEntityData.defineId(OgreLord.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LORD_DYING =
            SynchedEntityData.defineId(OgreLord.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_LORD_LOCOMOTION =
            SynchedEntityData.defineId(OgreLord.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FORT_ENCOUNTER_STATE =
            SynchedEntityData.defineId(OgreLord.class, EntityDataSerializers.INT);
    private static final String PHASE_TWO_TAG = "KingsAndMonstersPhaseTwo";
    private static final String CLUB_IN_HAND_TAG = "KingsAndMonstersClubInHand";
    private static final String PHASE_TRANSITION_TICKS_TAG = "KingsAndMonstersPhaseTransitionTicks";
    private static final String FORT_ENCOUNTER_STATE_TAG = "KingsAndMonstersFortEncounterState";
    private static final String ROYAL_TRIAL_POSITIONS_TAG = "KingsAndMonstersRoyalTrialPositions";
    private static final String COMPLETED_ROYAL_TRIALS_TAG = "KingsAndMonstersCompletedRoyalTrials";
    private static final String DUELIST_TAG = "KingsAndMonstersDuelist";
    private static final String THRONE_POS_TAG = "KingsAndMonstersThronePos";
    private static final String THRONE_YAW_TAG = "KingsAndMonstersThroneYaw";
    private static final String RETURN_TO_THRONE_TAG = "KingsAndMonstersReturnToThrone";
    /** Where his throne is, remembered so a won duel can send him back to it. */
    @Nullable
    private BlockPos thronePos;
    private float throneYaw;
    /** Set when a duel is yielded away from the throne; he goes back the next time his chunk loads. */
    private boolean returnToThroneOnLoad;
    /** True only in the session after a load, so the return never fires in the middle of the duel's aftermath. */
    private boolean throneReturnDue;
    private static final String DUEL_WINS_TAG = "KingsAndMonstersDuelWins";
    /** Players who have beaten him in a duel, and the game time they did it, for his words and the rest between duels. */
    private final Map<UUID, Long> duelWins = new HashMap<>();
    private int throneAwayTicks;
    /** No player this close for this long and he goes back to the throne, loaded or not. */
    private static final double THRONE_RETURN_AWAY_DISTANCE = 64.0;
    private static final int THRONE_RETURN_AWAY_TICKS = 20 * 10;
    private static final String ROYAL_DEFENCE_STARTED_TAG = "KingsAndMonstersRoyalDefenceStarted";
    private static final String WAKE_UP_TICKS_TAG = "KingsAndMonstersWakeUpTicks";
    private static final String KING_PURCHASES_TAG = "KingsAndMonstersKingPurchases";
    private static final String KING_DAILY_TAG = "KingsAndMonstersKingDailyStock";
    private static final float PHASE_TWO_HEALTH_THRESHOLD = 0.5F;
    // He yields a duel once a blow would take him down to this share of his health.
    private static final float DUEL_YIELD_FRACTION = 0.15F;
    // How long between Bogjaw traps catching him.
    private static final int TRAP_COOLDOWN_TICKS = 20 * 12;
    private static final int PULL_OUT_BUFF_DURATION_TICKS = 30 * 20;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation IDLE_2 = RawAnimation.begin().thenLoop("idle 2");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation COMBAT_WALK = RawAnimation.begin().thenLoop("Combat walk");
    // The mixed capitalization below is baked into the exported animation file
    // (ogre_lord.animation.json) — GeckoLib matches these names verbatim.
    private static final RawAnimation ONE_ARM_UPSWING = RawAnimation.begin().thenPlay("lordly one arm upswing");
    private static final RawAnimation REGULAR_ATTACK = RawAnimation.begin().thenPlay("Regular_attack");
    private static final RawAnimation REGULAR_ATTACK_2 = RawAnimation.begin().thenPlay("Regular_attack2");
    private static final RawAnimation DOUBLE_STOMP = RawAnimation.begin().thenPlay("Double stomp");
    private static final RawAnimation DOUBLE_OVERHEAD_SLAM = RawAnimation.begin().thenPlay("double overhead slam");
    private static final RawAnimation ROAR = RawAnimation.begin().thenPlay("Roar");
    private static final RawAnimation SUMMON_POINT = RawAnimation.begin().thenPlay("Summon point");
    private static final RawAnimation BELLY_BUMP = RawAnimation.begin().thenPlay("Lordly belly bump");
    private static final RawAnimation DOUBLE_ARM_UPSWING = RawAnimation.begin().thenPlay("lordly double arm upswing");
    private static final RawAnimation CLUB_HEAD_THRUST = RawAnimation.begin().thenPlay("club head thrust");
    private static final RawAnimation SINGLE_LEG_STOMP = RawAnimation.begin().thenPlay("single leg stomp");
    // The trailing space is part of the exported clip name.
    private static final RawAnimation PULL_OUT = RawAnimation.begin().thenPlay("club pull out ");
    private static final RawAnimation CLUB_DOUBLE_OVERHEAD = RawAnimation.begin().thenPlay("club double overhead");
    private static final RawAnimation SINGLE_OVERHEAD_SLAM = RawAnimation.begin().thenPlay("single overhead slam");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlay("death");
    private static final RawAnimation SLEEPING = RawAnimation.begin().thenLoop("sleeping");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("Stand up animation");
    private static final RawAnimation STARTLE_AWAKE = RawAnimation.begin().thenPlay("Startle awake");
    private static final RawAnimation SITTING_IDLE = RawAnimation.begin().thenLoop("Sitting Idle");
    private static final RawAnimation BUTT_SMASH = RawAnimation.begin().thenPlay("Butt_smash");

    private static final int IDLE_VARIANT_TICKS = 40;
    private static final int ONE_ARM_UPSWING_ANIMATION_TICKS = 50;
    private static final int DOUBLE_STOMP_ANIMATION_TICKS = 75;
    private static final int DOUBLE_OVERHEAD_SLAM_ANIMATION_TICKS = 60;
    private static final int ROAR_ANIMATION_TICKS = 85;
    private static final int SUMMON_POINT_ANIMATION_TICKS = 50;
    // The current exported variants below are all 2.5 seconds (50 ticks).
    private static final int BELLY_BUMP_ANIMATION_TICKS = 50;
    private static final int DOUBLE_ARM_UPSWING_ANIMATION_TICKS = 50;
    private static final int CLUB_HEAD_THRUST_ANIMATION_TICKS = 50;
    private static final int SINGLE_LEG_STOMP_ANIMATION_TICKS = 50;
    private static final int PULL_OUT_ANIMATION_TICKS = 165;
    private static final int CLUB_DOUBLE_OVERHEAD_ANIMATION_TICKS = 60;
    private static final int SINGLE_OVERHEAD_SLAM_ANIMATION_TICKS = 50;
    private static final int REGULAR_ATTACK_ANIMATION_TICKS = 50;
    // Remove the entity on the last death animation frame to avoid a frozen corpse.
    private static final int DEATH_ANIMATION_TICKS = 130;
    // The animation is 2 seconds. Five extra ticks cover the movement
    // controller's polling interval so the ending cannot be clipped.
    private static final int WAKE_UP_ANIMATION_TICKS = 45;
    // Startle awake is 1.5 seconds; same 5-tick margin as the stand-up above.
    private static final int STARTLE_ANIMATION_TICKS = 35;
    private static final int FORT_ENCOUNTER_NORMAL = 0;
    private static final int FORT_ENCOUNTER_SLEEPING = 1;
    private static final int FORT_ENCOUNTER_WAKING = 2;
    // Royal Trials: he stirs when the first trial starts, then sits on the throne watching.
    private static final int FORT_ENCOUNTER_STARTLED = 3;
    private static final int FORT_ENCOUNTER_SEATED = 4;
    // Attack ids for the throne animations (1-14 are used above).
    private static final int ATTACK_ID_STARTLE = 15;
    // The two regular arm slams: right hand, then left hand.
    private static final int ATTACK_ID_REGULAR_ATTACK = 18;
    private static final int ATTACK_ID_REGULAR_ATTACK_2 = 19;
    // Sleepy Zs originate just above the crown, then keep their existing upward animation.
    private static final int SLEEP_Z_INTERVAL_TICKS = 45;
    private static final double SLEEP_Z_HEIGHT = 0.97;
    private static final double SLEEP_Z_FORWARD = -0.4;
    private static final double SLEEP_Z_HORIZONTAL_JITTER = 0.12;
    // Seated head tracking; the limits match the combat model's.
    private static final double SEATED_HEAD_TRACK_RANGE = 40.0;
    private static final float SEATED_HEAD_MAX_YAW = 40.0F;
    private static final float SEATED_HEAD_MAX_UP_PITCH = 22.5F;
    private static final float SEATED_HEAD_MAX_DOWN_PITCH = 6.0F;
    private static final float SEATED_HEAD_TURN_RESPONSE = 0.15F;
    // The body leans a small fraction of the head's turn, capped, and eases there slowly.
    // Set SEATED_BODY_FOLLOW to 0 to keep the body completely still.
    private static final float SEATED_BODY_FOLLOW = 0.12F;
    private static final float SEATED_BODY_MAX_YAW = 6.0F;
    private static final float SEATED_BODY_RESPONSE = 0.08F;
    private static final int ROYAL_TRIAL_COUNT = 3;
    // Holding court: how far a visitor can be and still talk, how far away counts as gone, and
    // how long the hall has to stay empty before he dozes off again.
    private static final double COURT_TALK_RANGE = 10.0;
    // How long the Royal Trials may sit with nobody near the fort before they count as abandoned.
    private static final int ROYAL_TRIAL_ABANDON_TICKS = 20 * 120;
    private static final double COURT_LEAVE_DISTANCE = 24.0;
    private static final int COURT_SLEEP_DELAY_TICKS = 20 * 10;
    private static final int CHALLENGE_ANGER = 10;
    private static final double COURT_SPAWN_DISTANCE = 9.0;
    private static final double COURT_SPAWN_SPACING = 3.5;
    private static final double ROYAL_TRIAL_BAR_RANGE = 96.0;
    private static final double FORT_WAKE_FORWARD_NUDGE = 0.20;
    private static final int FORT_WAKE_NUDGE_START_TICK = 10;
    private static final int FORT_WAKE_NUDGE_TICKS = 8;
    // A player may only be subscribed to one fort encounter bar. This also lets a
    // newly loaded fort evict a stale bar left by a King whose chunk unloaded
    // before its periodic range check ran.
    private static final Map<ServerPlayer, OgreLord> ROYAL_TRIAL_BAR_OWNERS = new WeakHashMap<>();
    private static final int CLUB_VISIBILITY_SWAP_TICK = 108;
    // Offset the hit for the controller's 5-tick polling cadence so it matches the impact frame.
    private static final int ATTACK_SYNC_DELAY_TICKS = 3;
    // The jumping butt smash. The clip itself lifts him, so he never leaves his spot.
    private static final int ATTACK_ID_BUTT_SMASH = 20;
    private static final int BUTT_SMASH_ANIMATION_TICKS = 120;
    // In the clip he leaves the ground at 1.33s and lands at 2.42s.
    private static final int BUTT_SMASH_TAKEOFF_TICK = 27 + ATTACK_SYNC_DELAY_TICKS;
    // Both run 3 ticks past the keyframes (2.42s and 2.5s), which is when playtesting saw them land.
    private static final int BUTT_SMASH_IMPACT_TICK = 51 + ATTACK_SYNC_DELAY_TICKS;
    // His only grunt, as the mouth opens after the landing.
    private static final int BUTT_SMASH_GRUNT_TICK = 53 + ATTACK_SYNC_DELAY_TICKS;
    // He starts to get up at 4.25s.
    private static final int BUTT_SMASH_RECOVER_HUFF_TICK = 85 + ATTACK_SYNC_DELAY_TICKS;
    // He only tries it with at least this much open air above him.
    private static final double BUTT_SMASH_MIN_HEADROOM = 4.0;
    // How far above his hitbox the model rises, so it is still drawn when only that part is on screen.
    private static final double BUTT_SMASH_VISUAL_HEIGHT = 6.0;
    // Long on purpose: it is his big attack, and it should stay a surprise.
    private static final int BUTT_SMASH_COOLDOWN_TICKS = 45 * 20;
    private static final int PHASE_TWO_BUTT_SMASH_COOLDOWN_TICKS = 35 * 20;
    // One in this many chances to use it whenever it is available.
    private static final int BUTT_SMASH_CHANCE = 5;
    private static final int BUTT_SMASH_SWING_COOLDOWN_TICKS = 140;
    // The inner circle hits at once and can't be jumped. It is full strength under him and keeps this
    // share at its rim, which is still a little more than the ripple outside it does.
    private static final float BUTT_SMASH_INNER_RADIUS = 6.0F;
    private static final float BUTT_SMASH_INNER_EDGE_SHARE = 0.75F;
    private static final double BUTT_SMASH_CRUSH_THROW = 1.1;
    private static final double BUTT_SMASH_CRUSH_LIFT = 0.7;
    private static final float BUTT_SMASH_RIPPLE_RADIUS = 15.0F;
    private static final double BUTT_SMASH_RIPPLE_LAUNCH = 1.35;
    private static final int BUTT_SMASH_BLOCKS_DESTROYED = 4;
    private static final int SUMMON_POINT_PUNCH_SOUND_TICK = 22 + ATTACK_SYNC_DELAY_TICKS;
    // The point pose lands at 1.1667s. Retain the same small client animation
    // synchronization allowance used by the King's attack impacts.
    private static final int SUMMON_POINT_VOCAL_TICK = 18;
    private static final int SUMMON_POINT_EVENT_TICK = 23;
    // Bone-chain pivots and GeckoLib's Catmull-Rom interpolation put contact at tick 136.
    // Playtesting added 3 ticks to that estimate. The zero-frame fix removed the old sync lag.
    private static final int PULL_OUT_IMPACT_TICK = 139;
    // The King drops to his knees around 0.42s into the pull-out animation.
    private static final int PULL_OUT_KNEEL_HURT_SOUND_TICK = 8;
    // Raising pitch also shortens/speeds the reused hurt recording, giving this exertion its own identity.
    private static final float PULL_OUT_KNEEL_HURT_SOUND_PITCH = 1.10F;
    // Same forward-kinematics method as PULL_OUT_IMPACT_TICK plus the +3 in-game correction. Double
    // and single overhead are different swings with different windups, so each has its own tick.
    // A further +3 was added, then 1 taken back, per tuning feedback.
    // The reworked double overhead is the same swing as before with a half-second longer wind-up,
    // so its impact moved from tick 30 to 40.
    private static final int CLUB_DOUBLE_OVERHEAD_IMPACT_TICK = 40;
    private static final int SINGLE_OVERHEAD_IMPACT_TICK = 27;
    // Both regular slams land at 1.17s (23.3 ticks) into their clip. Rounded up like the upswing's
    // 1.19s, so the hit never fires before the fist visibly arrives.
    private static final int REGULAR_ATTACK_IMPACT_TICK = 24 + ATTACK_SYNC_DELAY_TICKS;
    private static final int HEAVY_ATTACK_ROAR_LEAD_TICKS = 8;
    private static final int PULL_OUT_ROAR_LEAD_TICKS = 12;
    private static final int SINGLE_OVERHEAD_ROAR_LEAD_TICKS = 13;
    private static final int DOUBLE_OVERHEAD_ROAR_LEAD_TICKS = 17;
    private static final int VOCAL_LAYERS = 2;
    private static final int CLUB_DOUBLE_OVERHEAD_ROAR_LEAD_TICKS = 17;
    // The delivered King vocals are final timed performances; play them without repitching so their
    // duration continues to match the authored jaw motion.
    private static final float PULL_OUT_ROAR_PITCH = 1.0F;
    private static final float DOUBLE_OVERHEAD_ROAR_PITCH = 1.0F;
    private static final float CLUB_DOUBLE_OVERHEAD_ROAR_PITCH = 1.0F;
    private static final float SINGLE_OVERHEAD_ROAR_PITCH = 1.0F;
    private static final float HEAVY_ATTACK_ROAR_PITCH_VARIATION = 0.0F;
    // Impact sounds shift a little each time so repeated slams don't sound identical.
    private static final float IMPACT_PITCH_VARIATION = 0.05F;
    private static final float IMPACT_VOLUME_VARIATION = 0.05F;

    // Shared recovery after one arm upswing/belly bump/double arm upswing/side smack/single leg
    // stomp — counted from attack start, so this leaves a real gap before he can swing again, not
    // back-to-back combos.
    // Tightened from 110/145/140 so the fight has fewer dead gaps; each swing also gets a little
    // random jitter (see swingCooldown) so the rhythm can't be timed exactly.
    private static final int BASIC_ATTACK_COOLDOWN_TICKS = 85;
    // The attacks he throws while walking never cost him time to line up or stop, so they also
    // recover a little slower than the planted ones.
    private static final int WALKING_ATTACK_COOLDOWN_TICKS = 90;
    private static final int DOUBLE_STOMP_SWING_COOLDOWN_TICKS = 110;
    private static final int DOUBLE_OVERHEAD_SLAM_SWING_COOLDOWN_TICKS = 100;
    private static final int SWING_COOLDOWN_JITTER_TICKS = 10;
    private static final double PLOW_TRIGGER_RANGE = 24.0;
    private static final int PLOW_BLOCKS_PER_PULSE = 40;
    // Extra per-move cooldowns on top of the shared swing recovery above, so the big AOE moves
    // don't get reselected back-to-back the way the regular rotation can.
    private static final int DOUBLE_STOMP_COOLDOWN_TICKS = 300;
    private static final int DOUBLE_OVERHEAD_SLAM_COOLDOWN_TICKS = 600;
    private static final int ROAR_COOLDOWN_TICKS = 1800;
    private static final int PHASE_TWO_ROAR_COOLDOWN_TICKS = 1200;
    private static final int PHASE_TWO_FIRST_ROAR_DELAY_TICKS = 160;
    private static final int SUMMON_COOLDOWN_TICKS = 90 * 20;
    // The Roar and Summon are driven by health (see the gates below). These first-use timers are only
    // a fallback so a fight that stalls above the first gate still gets them.
    private static final int ROAR_INITIAL_DELAY_TICKS = 120 * 20;
    private static final int SUMMON_INITIAL_DELAY_TICKS = 60 * 20;
    // Health fractions that make the Summon / Roar ready. Phase 2 starts at 0.5, so the first two
    // summon gates and the first roar gate are phase 1 beats; the rest are for the club phase.
    private static final float[] SUMMON_HEALTH_GATES = {0.85F, 0.65F, 0.30F};
    private static final float[] ROAR_HEALTH_GATES = {0.75F, 0.40F};
    private static final int SPECIAL_CONFLICT_RETRY_MIN_TICKS = 200;
    private static final int SPECIAL_CONFLICT_RETRY_RANDOM_TICKS = 101;
    // The animation opens its mouth at 1.25s and closes at the 4.25s ending frame.
    private static final int ROAR_SOUND_TICK = 25;
    private static final int ROAR_SCREEN_SHAKE_DELAY_TICKS = 5;
    private static final int ROAR_SCREEN_SHAKE_ACTIVE_TICKS = 82;
    private static final float ROAR_SCREEN_SHAKE_FREQUENCY_MULTIPLIER = 1.45F;
    private static final double ROAR_SCREEN_SHAKE_RANGE = 32.0;
    private static final float ROAR_SCREEN_SHAKE_INTENSITY = 0.55F;
    private static final int ROAR_EFFECT_TICK = 40;
    private static final int ROAR_SHOCKWAVE_START_TICK = ROAR_SOUND_TICK;
    // The roar wave crosses its full gameplay radius quickly enough to read as a
    // pressure wave while remaining visible instead of attempting real sound speed.
    private static final int ROAR_SHOCKWAVE_EXPANSION_TICKS = 40;
    private static final float ROAR_SHOCKWAVE_START_RADIUS = 1.0F;
    private static final float ROAR_SHOCKWAVE_END_RADIUS = 20.0F;
    private static final int ROAR_BUFF_DURATION_TICKS = 400;
    private static final int ROAR_WEAKNESS_DURATION_TICKS = 200;
    private static final int ROAR_SLOWNESS_REFRESH_INTERVAL_TICKS = 5;
    private static final int ROAR_SLOWNESS_REFRESH_DURATION_TICKS = 10;
    private static final double ROAR_EFFECT_RADIUS = 16.0;
    private static final int MAX_LIVING_SUMMONS = 6;
    // A call-up needs room for at least a pair, otherwise it waits.
    private static final int MIN_SUMMON_ROOM = 2;
    private static final int SUMMON_FULL_RETRY_TICKS = 10 * 20;
    private static final double ROAR_SHOCK_KNOCKBACK = 1.6;
    private static final double ROAR_SHOCK_TRIGGER_RANGE = 7.0;
    private static final int PHASE_TWO_SINGLE_LEG_STOMP_CHANCE = 7;
    private static final double ROYAL_COMMAND_RADIUS = 32.0;
    // Reinforcements emerge inside the king's broad collision footprint. Their
    // entity collision gives him a small physical shove while he remains planted.
    private static final double SUMMON_SPAWN_RADIUS = 0.65;
    private static final double ROAR_MAX_NUDGE_DISTANCE = 1.15;
    private static final double ROAR_MAX_HORIZONTAL_NUDGE_SPEED = 0.08;
    private static final double ROAR_NUDGE_DAMPING = 0.72;
    // Animation-aligned body sweep: forward -> slight left -> right -> forward.
    private static final int ROAR_TURN_START_TICK = 28; // 1.42s
    private static final int ROAR_LEFT_TICK = 43;       // 2.13s
    private static final int ROAR_RIGHT_TICK = 58;      // 2.88s
    private static final int ROAR_RETURN_START_TICK = 72; // 3.58s
    private static final int ROAR_FORWARD_TICK = 85;    // 4.25s
    private static final float ROAR_LEFT_YAW_OFFSET = -10.0F;
    private static final float ROAR_RIGHT_YAW_OFFSET = 15.0F;

    private static final double REGULAR_ATTACK_START_RANGE = 5.5;
    private static final double STOMP_WAVE_MIN_RANGE = 4.2;
    // One arm upswing impact at 1.19s into "lordly one arm upswing".
    private static final int ONE_ARM_UPSWING_IMPACT_DELAY_TICKS = 24 + ATTACK_SYNC_DELAY_TICKS;
    // Double stomp impacts at 1.13s (left foot) and 2.88s (right foot) into "Double stomp".
    private static final int DOUBLE_STOMP_LEFT_IMPACT_DELAY_TICKS = 23 + ATTACK_SYNC_DELAY_TICKS;
    private static final int DOUBLE_STOMP_RIGHT_IMPACT_DELAY_TICKS = 58 + ATTACK_SYNC_DELAY_TICKS;
    // The 0.5s grunts start as each jaw starts to open (1.0s and 2.75s), so the mouth is open for the whole vocal.
    private static final int DOUBLE_STOMP_LEFT_GRUNT_DELAY_TICKS = 20 + ATTACK_SYNC_DELAY_TICKS;
    private static final int DOUBLE_STOMP_RIGHT_GRUNT_DELAY_TICKS = 55 + ATTACK_SYNC_DELAY_TICKS;
    // Double overhead slam (Phase 1 only, unarmed hands) — forward-kinematics reconstruction put
    // both hands at ground level at tick 24; +6 ticks on top is an empirical correction confirmed
    // good from in-game testing.
    private static final int DOUBLE_OVERHEAD_SLAM_IMPACT_DELAY_TICKS = 30;
    // Belly bump and lordly double-arm upswing impact at 1.17s; single leg stomp at 1.17s.
    private static final int BELLY_BUMP_ROAR_DELAY_TICKS = 20 + ATTACK_SYNC_DELAY_TICKS;
    private static final int BELLY_BUMP_IMPACT_DELAY_TICKS = 23 + ATTACK_SYNC_DELAY_TICKS;
    // The jaw starts opening at 1.0s in the double-arm upswing animation.
    private static final int DOUBLE_ARM_UPSWING_GRUNT_DELAY_TICKS = 23;
    private static final int DOUBLE_ARM_UPSWING_IMPACT_DELAY_TICKS = 23 + ATTACK_SYNC_DELAY_TICKS;
    // Shift thrust sound and impact together by 3 ticks to match the extending hitbox.
    // The club shoots out between 1.0s and 1.13s, so the whoosh starts as it leaves; the metal hit
    // only plays when the club head actually connects (see tickClubHeadThrustHitbox).
    private static final int CLUB_HEAD_THRUST_WHOOSH_DELAY_TICKS = 21 + ATTACK_SYNC_DELAY_TICKS;
    private static final int CLUB_HEAD_THRUST_ACTIVE_START_TICK = 23 + ATTACK_SYNC_DELAY_TICKS;
    private static final int CLUB_HEAD_THRUST_ACTIVE_END_TICK = 31 + ATTACK_SYNC_DELAY_TICKS;
    private static final double CLUB_HEAD_THRUST_START_REACH = 1.5;
    private static final double CLUB_HEAD_THRUST_END_REACH = 6.25;
    private static final double CLUB_HEAD_THRUST_RIGHT_OFFSET = -1.15;
    private static final double CLUB_HEAD_THRUST_HEAD_RADIUS = 1.65;
    private static final int SINGLE_LEG_STOMP_IMPACT_DELAY_TICKS = 23 + ATTACK_SYNC_DELAY_TICKS;
    // Close-pressure gate for the belly bump — punishes standing in his face and swinging away.
    private static final double BELLY_BUMP_CLOSE_PRESSURE_RANGE = 4.0;
    private static final int BELLY_BUMP_CLOSE_PRESSURE_TICKS = 28;
    private static final int BELLY_BUMP_COOLDOWN_TICKS = 400;
    // Extra cooldown on top of the low roll chance in startMeleeAttack() — was getting picked too
    // often back-to-back before this.
    private static final int DOUBLE_ARM_UPSWING_COOLDOWN_TICKS = 200;
    private static final int CLUB_DOUBLE_OVERHEAD_COOLDOWN_TICKS = 360;
    private static final int SINGLE_OVERHEAD_COOLDOWN_TICKS = 180;
    private static final int CLUB_HEAD_THRUST_COOLDOWN_TICKS = 200;
    private static final int LOCOMOTION_IDLE = 0;
    private static final int LOCOMOTION_WALK = 1;
    // Value 2 used to be the run; it is left unused so saved values keep their meaning.
    private static final int LOCOMOTION_COMBAT_WALK = 3;
    // He has no run any more: the combat walk is how he closes distance and how he circles.
    private static final double KING_COMBAT_WALK_SPEED = 0.931;
    // Keep just a touch more breathing room so his long-reach attacks do not crowd the player.
    private static final double COMBAT_WALK_TARGET_GAP = 2.0;
    private static final double COMBAT_WALK_MAX_VERTICAL_DIFFERENCE = 4.5;
    // Frames of blending when an attack that played over the walk lets go of the upper body.
    public static final float ATTACK_OVERLAY_FADE_OUT_TICKS = 4.0F;
    // Strikes skip the line-of-sight check inside this range (see canStrikeReach).
    private static final double CLOSE_STRIKE_RANGE = 10.0;

    // Tuned for 3–4 hearts against fully enchanted diamond armor.
    // Double stomp uses less damage per hit because its circles nearly overlap.
    // The redirected one-arm upswing shares the two-arm multiplier.
    // Walking attacks deal less damage because they do not require planting his feet.
    private static final float ONE_ARM_UPSWING_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.KING, OgreDamage.Blow.STANDARD);
    // Double stomp lands two ripple hits, so each one is a standard blow rather than a heavy one.
    private static final float DOUBLE_STOMP_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.KING, OgreDamage.Blow.STANDARD);
    /** Ripples are 11-14 blocks across at 0.625 blocks per tick, so they outlast the attack by about 20 ticks. */
    private static final int JUMPABLE_RIPPLE_TAIL_TICKS = 24;
    private int jumpableRippleUntilTick;
    private static final float DOUBLE_OVERHEAD_SLAM_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.KING, OgreDamage.Blow.HEAVY);
    private static final float BELLY_BUMP_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.KING, OgreDamage.Blow.HEAVY);
    private static final double BELLY_BUMP_HORIZONTAL_LAUNCH = 1.6;
    private static final float DOUBLE_ARM_UPSWING_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.KING, OgreDamage.Blow.STANDARD);
    private static final float CLUB_HEAD_THRUST_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.KING, OgreDamage.Blow.STANDARD);
    // Regular slams: the hand itself hits for the full amount, anything else in the smoke ring
    // takes less the further out it is.
    private static final float REGULAR_ATTACK_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.KING, OgreDamage.Blow.STANDARD);
    // Flat club bonus is a config value; the walking club attacks apply this fraction of it.
    private static final float MOBILE_CLUB_BONUS_SCALE = 0.5F;
    // Stomp Wave: the right-foot stomp sends a line of ripples straight ahead. It trades the old
    // stomp's unavoidable 6.5-block circle (1.3x) for a longer, dodgeable hit that is stronger,
    // so it must be sidestepped or jumped. Each target is hit once per wave.
    // 20 raw damage against his default 18 attack: the stomp is his repeatable poke, so its wave hits
    // softer than a regular swing (24) and clearly below the club slams' ripples. It scales with the
    // attack attribute like every other blow.
    private static final float STOMP_WAVE_DAMAGE_MULTIPLIER = 20.0F / 18.0F;
    private static final int STOMP_WAVE_PULSES = 6;
    private static final int STOMP_WAVE_PULSE_INTERVAL_TICKS = 4;
    private static final double STOMP_WAVE_FIRST_DISTANCE = 1.8;
    private static final double STOMP_WAVE_SPACING = 2.5;
    /** How far from the stomp the last pulse lands. */
    private static final float STOMP_WAVE_REACH =
            (float) (STOMP_WAVE_FIRST_DISTANCE + (STOMP_WAVE_PULSES - 1) * STOMP_WAVE_SPACING);
    private static final float STOMP_WAVE_PULSE_RADIUS = 2.3F;
    private static final double STOMP_WAVE_VERTICAL_HIT_RANGE = 1.6;
    private static final double STOMP_WAVE_LAUNCH = 0.55;
    private static final int STOMP_WAVE_SLOWNESS_TICKS = 30;
    private static final int STOMP_WAVE_MAX_LIFETIME_TICKS = 120;

    private static final float DOUBLE_STOMP_AREA_RADIUS = 11.25F;
    private static final float DOUBLE_OVERHEAD_SLAM_AREA_RADIUS = 13.5F;
    private static final float PULL_OUT_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.KING, OgreDamage.Blow.HEAVY);
    // Hits between the single overhead and the butt smash: a heavy blow plus half the config bonus.
    private static final float CLUB_DOUBLE_OVERHEAD_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.KING, OgreDamage.Blow.HEAVY);
    private static final float CLUB_DOUBLE_OVERHEAD_BONUS_SCALE = 0.5F;
    private static final float SINGLE_OVERHEAD_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.KING, OgreDamage.Blow.STANDARD);

    // Every ripple fades with distance, down to 10 raw damage against his default 18 attack.
    private static final float RIPPLE_EDGE_DAMAGE_MULTIPLIER = 10.0F / 18.0F;

    // The butt smash: its inner circle is the worst place to be, and its ripple leaves the circle
    // hitting harder than the stomp's.
    private static final float BUTT_SMASH_CRUSH_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.KING, OgreDamage.Blow.CRUSHING);
    private static final float BUTT_SMASH_RIPPLE_DAMAGE_MULTIPLIER = OgreDamage.multiplier(OgreDamage.Tier.KING, OgreDamage.Blow.HEAVY);

    // Terrain destruction on the big ground smashes — kept small ("a couple" blocks), not a crater.
    private static final int DOUBLE_STOMP_BLOCKS_DESTROYED = 2;
    private static final int DOUBLE_OVERHEAD_SLAM_BLOCKS_DESTROYED = 3;
    private static final int REGULAR_ATTACK_BLOCKS_DESTROYED = 1;
    private static final int SINGLE_OVERHEAD_BLOCKS_DESTROYED = 2;
    // Where the single-arm slams come down in front of him, and how far their smoke ring reaches.
    private static final double SINGLE_OVERHEAD_REACH = 4.96;
    private static final float SINGLE_OVERHEAD_AREA_RADIUS = 6.0F;
    private static final double REGULAR_SLAM_REACH = 3.4;
    private static final double REGULAR_SLAM_SIDE_OFFSET = 1.3;
    private static final float REGULAR_SLAM_AREA_RADIUS = 4.5F;
    private static final float MAX_DESTRUCTIBLE_HARDNESS = 20.0F;

    private int idleAnimationTicks;
    private int idleVariantTicks;
    private int attackAnimationTicks;
    private int doubleStompCooldownTicks;
    private int doubleOverheadSlamCooldownTicks;
    private int bellyBumpCooldownTicks;
    private int doubleArmUpswingCooldownTicks;
    private int clubDoubleOverheadCooldownTicks;
    private int singleOverheadCooldownTicks;
    private int clubHeadThrustCooldownTicks;
    private int buttSmashCooldownTicks;
    private int buttSmashElapsedTicks;
    /** From leaving the ground until the impact, while the animation has him in the air. */
    private boolean buttSmashAirborne;
    private int closePressureTicks;
    // Starts on cooldown so he can't roar the instant a fight begins.
    private int roarCooldownTicks = ROAR_INITIAL_DELAY_TICKS;
    // Plow-through: when he can't reach a target nearby for a while he loses patience and smashes a
    // path to them. Every number below is re-rolled per attempt so it never plays out the same way.
    private int frustrationTicks;
    private int frustrationThresholdTicks = rollFrustrationThreshold();
    private int plowTicksRemaining;
    private int plowCooldownTicks;
    private double plowHalfWidth;
    private int plowHeight;
    private double plowReach;
    private float plowDropChance;
    private int nextSummonGate;
    private int nextRoarGate;
    private boolean healthGatesInitialised;
    private int roarActiveTicks;
    private boolean roarEffectsApplied;
    private boolean roarSoundPlayed;
    private int roarScreenShakeDelayTicks;
    private int summonCooldownTicks = SUMMON_INITIAL_DELAY_TICKS;
    private int summonActiveTicks;
    private boolean summonEventFired;
    private boolean summonVocalPlayed;
    private boolean summonPunchSoundPlayed;
    private double summonLockedX;
    private double summonLockedZ;
    private float summonLockedYaw;
    private LivingEntity summonTarget;
    private double roarLockedX;
    private double roarLockedZ;
    private float roarLockedYaw;
    private int oneArmUpswingImpactSoundDelayTicks = -1;
    private int doubleStompLeftImpactSoundDelayTicks = -1;
    private int doubleStompRightImpactSoundDelayTicks = -1;
    private int doubleStompLeftGruntDelayTicks = -1;
    private int doubleStompRightGruntDelayTicks = -1;
    private int doubleOverheadSlamImpactSoundDelayTicks = -1;
    private int bellyBumpRoarDelayTicks = -1;
    private int bellyBumpImpactSoundDelayTicks = -1;
    private int doubleArmUpswingGruntDelayTicks = -1;
    private int doubleArmUpswingImpactSoundDelayTicks = -1;
    private int clubHeadThrustImpactSoundDelayTicks = -1;
    private int clubHeadThrustElapsedTicks;
    private double clubHeadThrustOriginX;
    private double clubHeadThrustOriginZ;
    private float clubHeadThrustYaw;
    private Vec3 previousClubHeadThrustPosition;
    private final Set<UUID> clubHeadThrustHitTargets = new HashSet<>();
    /** The thrust's impact sounds play once per swing, however many targets the club head passes through. */
    private boolean clubHeadThrustImpactSoundPlayed;
    private int singleLegStompImpactSoundDelayTicks = -1;
    // Stomp Wave: -1 when idle, otherwise ticks since the stomp began.
    private int stompWaveElapsedTicks = -1;
    private long stompWaveStartTime;
    private Vec3 stompWaveOrigin = Vec3.ZERO;
    private Vec3 stompWaveDirection = Vec3.ZERO;
    private final Set<UUID> stompWaveHitTargets = new HashSet<>();
    private final List<DelayedSound> delayedSounds = new ArrayList<>();
    private int heavyAttackRoarDelayTicks = -1;
    private int heavyAttackRoarAttackId;
    private float heavyAttackRoarPitch = 1.0F;
    private boolean heavyAttackUsesOgrebloodRoar;
    private int activeLordAttackId;
    private int lastSelectedAttackId;
    private int secondLastSelectedAttackId;
    private final Set<UUID> directShockwaveWeaponHitTargets = new HashSet<>();
    private int lastAnimationTick = -1;
    private long lastMovingAnimationTick = Long.MIN_VALUE / 2;
    private static final int MOVEMENT_ANIMATION_GRACE_TICKS = 4;
    private final CanonicalOneShotState visualOneShot = new CanonicalOneShotState();
    // The attacks he throws while walking play on their own controller, over the walk.
    private final CanonicalOneShotState visualOverlayOneShot = new CanonicalOneShotState();
    private boolean attackOverlayVisible;
    private long visualDeathStartGameTime = Long.MIN_VALUE;
    private int phaseTransitionElapsedTicks;
    private int clubAttackElapsedTicks;
    private boolean clubAttackImpactFired;
    private double lockedSpecialX;
    private double lockedSpecialZ;
    private float lockedSpecialYaw;
    private DamageSource delayedDeathSource;
    private RawAnimation activeIdleAnimation = IDLE;
    private final List<BlockPos> royalTrialPositions = new ArrayList<>(ROYAL_TRIAL_COUNT);
    private final Set<UUID> royalDefenceIntroducedPlayers = new HashSet<>();
    private final ServerBossEvent royalTrialProgress = new ServerBossEvent(
            royalUiText("Royal Trials 0/" + ROYAL_TRIAL_COUNT),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);
    private final ServerBossEvent kingBossBar = new ServerBossEvent(
            royalUiText("Ogre King"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);
    private int completedRoyalTrialsMask;
    private boolean royalDefenceStarted;
    private int wakeUpTicks;
    @Nullable private UUID courtGuestId;
    @Nullable private UUID duelistId;
    private int trapReadyAtTick;
    private int courtEmptyTicks;
    private int royalTrialAbandonTicks;
    // Which of the King's wares each player has bought, one bit per ware; his stock is theirs alone.
    private final Map<UUID, Integer> kingPurchases = new HashMap<>();
    /** What each player has bought of his repeatable wares today; stock refills at the start of each day. */
    private final Map<UUID, DailyStock> kingDaily = new HashMap<>();
    private int lastRenderedFortEncounterState = -1;

    public OgreLord(EntityType<? extends OgreLord> type, Level level) {
        super(type, level);
        avoidWaterPathfinding();
        xpReward = LORD_XP_REWARD;
    }

    // ------------------------------------------------------------------ //
    //  Attributes                                                        //
    // ------------------------------------------------------------------ //

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                // A little slower than OgreBrute's chase speed.
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 34.0)
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.STEP_HEIGHT, 1.3)
                .add(Attributes.SAFE_FALL_DISTANCE, 6.0)
                // See OgreGrunt#createAttributes — default 0 caps water acceleration at a tiny fixed
                // baseline regardless of requested speed, which would make applyShallowWaterSpeedFactor's
                // 50% reduction meaningless since he's already moving slower than that.
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0);
    }

    @Override
    @SuppressWarnings("deprecation") // Required 1.21.1 OverrideOnly effect hook; no replacement exists in this target.
    public boolean canBeAffected(MobEffectInstance effect) {
        // Keep the approved run pace stable by ignoring allied speed splashes.
        if (effect.getEffect().equals(MobEffects.MOVEMENT_SPEED)) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LORD_ATTACK_ID, 0);
        builder.define(DATA_LORD_ATTACK_START_TICK, 0L);
        builder.define(DATA_LORD_PHASE_TWO, false);
        builder.define(DATA_LORD_TRANSITIONING, false);
        builder.define(DATA_LORD_CLUB_IN_HAND, false);
        builder.define(DATA_LORD_DYING, false);
        builder.define(DATA_LORD_LOCOMOTION, LOCOMOTION_IDLE);
        builder.define(DATA_FORT_ENCOUNTER_STATE, FORT_ENCOUNTER_NORMAL);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(PHASE_TWO_TAG, isPhaseTwo());
        tag.putBoolean(CLUB_IN_HAND_TAG, isClubInHand());
        if (isPhaseTransitioning()) {
            tag.putInt(PHASE_TRANSITION_TICKS_TAG, phaseTransitionElapsedTicks);
        }
        tag.putInt(FORT_ENCOUNTER_STATE_TAG, entityData.get(DATA_FORT_ENCOUNTER_STATE));
        tag.putLongArray(ROYAL_TRIAL_POSITIONS_TAG,
                royalTrialPositions.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putInt(COMPLETED_ROYAL_TRIALS_TAG, completedRoyalTrialsMask);
        tag.putBoolean(ROYAL_DEFENCE_STARTED_TAG, royalDefenceStarted);
        if (duelistId != null) {
            tag.putUUID(DUELIST_TAG, duelistId);
        }
        if (thronePos != null) {
            tag.putLong(THRONE_POS_TAG, thronePos.asLong());
            tag.putFloat(THRONE_YAW_TAG, throneYaw);
        }
        tag.putBoolean(RETURN_TO_THRONE_TAG, returnToThroneOnLoad);
        ListTag wins = new ListTag();
        duelWins.forEach((playerId, time) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", playerId);
            entry.putLong("time", time);
            wins.add(entry);
        });
        tag.put(DUEL_WINS_TAG, wins);
        if (isWakingFromFortEncounter() || isStartledInFort()) {
            tag.putInt(WAKE_UP_TICKS_TAG, wakeUpTicks);
        }
        ListTag purchases = new ListTag();
        kingPurchases.forEach((playerId, mask) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", playerId);
            entry.putInt("bought", mask);
            purchases.add(entry);
        });
        tag.put(KING_PURCHASES_TAG, purchases);
        ListTag daily = new ListTag();
        kingDaily.forEach((playerId, stock) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", playerId);
            entry.putLong("day", stock.day());
            entry.putIntArray("used", stock.used());
            daily.add(entry);
        });
        tag.put(KING_DAILY_TAG, daily);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ListTag purchases = tag.getList(KING_PURCHASES_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < purchases.size(); i++) {
            CompoundTag entry = purchases.getCompound(i);
            if (entry.hasUUID("player")) {
                kingPurchases.put(entry.getUUID("player"), entry.getInt("bought"));
            }
        }
        ListTag daily = tag.getList(KING_DAILY_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < daily.size(); i++) {
            CompoundTag entry = daily.getCompound(i);
            if (entry.hasUUID("player")) {
                kingDaily.put(entry.getUUID("player"), new DailyStock(entry.getLong("day"), entry.getIntArray("used")));
            }
        }
        if (tag.getBoolean(PHASE_TWO_TAG)) {
            entityData.set(DATA_LORD_PHASE_TWO, true);
        }
        entityData.set(DATA_LORD_CLUB_IN_HAND, tag.getBoolean(CLUB_IN_HAND_TAG));
        if (tag.contains(PHASE_TRANSITION_TICKS_TAG)) {
            phaseTransitionElapsedTicks = Mth.clamp(tag.getInt(PHASE_TRANSITION_TICKS_TAG), 0, PULL_OUT_ANIMATION_TICKS);
            entityData.set(DATA_LORD_PHASE_TWO, true);
            entityData.set(DATA_LORD_TRANSITIONING, phaseTransitionElapsedTicks < PULL_OUT_ANIMATION_TICKS);
            if (phaseTransitionElapsedTicks < PULL_OUT_ANIMATION_TICKS) {
                entityData.set(DATA_LORD_ATTACK_ID, 9);
                entityData.set(DATA_LORD_ATTACK_START_TICK,
                        Math.max(1L, level().getGameTime() - phaseTransitionElapsedTicks));
            }
            if (phaseTransitionElapsedTicks >= CLUB_VISIBILITY_SWAP_TICK) {
                entityData.set(DATA_LORD_CLUB_IN_HAND, true);
            }
        }
        int encounterState = Mth.clamp(tag.getInt(FORT_ENCOUNTER_STATE_TAG),
                FORT_ENCOUNTER_NORMAL, FORT_ENCOUNTER_SEATED);
        entityData.set(DATA_FORT_ENCOUNTER_STATE, encounterState);
        royalTrialPositions.clear();
        for (long packedPos : tag.getLongArray(ROYAL_TRIAL_POSITIONS_TAG)) {
            royalTrialPositions.add(BlockPos.of(packedPos));
        }
        completedRoyalTrialsMask = tag.getInt(COMPLETED_ROYAL_TRIALS_TAG);
        royalDefenceStarted = tag.getBoolean(ROYAL_DEFENCE_STARTED_TAG);
        duelistId = tag.hasUUID(DUELIST_TAG) ? tag.getUUID(DUELIST_TAG) : null;
        thronePos = tag.contains(THRONE_POS_TAG) ? BlockPos.of(tag.getLong(THRONE_POS_TAG)) : null;
        throneYaw = tag.getFloat(THRONE_YAW_TAG);
        returnToThroneOnLoad = tag.getBoolean(RETURN_TO_THRONE_TAG);
        // Being read from a save means the chunk was unloaded and is back: that is when he goes home.
        throneReturnDue = returnToThroneOnLoad;
        duelWins.clear();
        ListTag wins = tag.getList(DUEL_WINS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < wins.size(); i++) {
            CompoundTag entry = wins.getCompound(i);
            if (entry.hasUUID("player")) {
                duelWins.put(entry.getUUID("player"), entry.getLong("time"));
            }
        }
        wakeUpTicks =Mth.clamp(tag.getInt(WAKE_UP_TICKS_TAG), 0, WAKE_UP_ANIMATION_TICKS);
        if (encounterState == FORT_ENCOUNTER_WAKING && wakeUpTicks < WAKE_UP_ANIMATION_TICKS) {
            entityData.set(DATA_LORD_ATTACK_ID, 14);
            entityData.set(DATA_LORD_ATTACK_START_TICK,
                    Math.max(1L, level().getGameTime() - wakeUpTicks));
        } else if (encounterState == FORT_ENCOUNTER_STARTLED) {
            wakeUpTicks = Mth.clamp(wakeUpTicks, 0, STARTLE_ANIMATION_TICKS);
            entityData.set(DATA_LORD_ATTACK_ID, ATTACK_ID_STARTLE);
            entityData.set(DATA_LORD_ATTACK_START_TICK,
                    Math.max(1L, level().getGameTime() - wakeUpTicks));
        }
        if (encounterState != FORT_ENCOUNTER_NORMAL) {
            setNoAi(true);
            setTarget(null);
            setAggressive(false);
        } else if (!isDyingAnimation()) {
            // Clear stale vanilla NoAI data if a save was made during the final
            // wake-state transition.
            setNoAi(false);
        }
    }

    public void setFortEncounterTrials(List<BlockPos> trialPositions) {
        royalTrialPositions.clear();
        royalTrialPositions.addAll(trialPositions.stream().limit(ROYAL_TRIAL_COUNT).toList());
        completedRoyalTrialsMask = 0;
        royalDefenceStarted = false;
        wakeUpTicks = 0;
        thronePos = blockPosition();
        throneYaw = getYRot();
        returnToThroneOnLoad = false;
        entityData.set(DATA_FORT_ENCOUNTER_STATE, FORT_ENCOUNTER_SLEEPING);
        entityData.set(DATA_LORD_LOCOMOTION, LOCOMOTION_IDLE);
        setNoAi(true);
        setTarget(null);
        setAggressive(false);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        royalTrialProgress.setProgress(0.0F);
    }

    @Override
    public void applyConfiguredCombatAttributes(boolean healToMax) {
        if (!Config.isLoaded()) {
            return;
        }

        setAttributeBaseValue(Attributes.MAX_HEALTH, Config.OGRE_LORD_MAX_HEALTH.get());
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, Config.OGRE_LORD_ATTACK_DAMAGE.get());
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, Config.OGRE_LORD_MOVEMENT_SPEED.get());
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, Config.OGRE_LORD_FOLLOW_RANGE.get());
        setAttributeBaseValue(Attributes.ARMOR, Config.OGRE_LORD_ARMOR.get());
        applyConfiguredHealth(healToMax);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            if (isFortEncounterDormant()) {
                tickFortEncounter();
                return;
            }
            kingBossBar.setProgress(getMaxHealth() <= 0.0F ? 0.0F : getHealth() / getMaxHealth());
            // Don't assert a locomotion state while an attack state owns the model. animateLord()'s
            // one-shot check currently masks this write, but ownership should be enforced here too.
            if ((getTarget() == null || !isAggressive())
                    && !isRoaring() && !isPhaseTransitioning() && !isDyingAnimation() && !isMidSwing()) {
                entityData.set(DATA_LORD_LOCOMOTION,
                        getNavigation().isDone() ? LOCOMOTION_IDLE : LOCOMOTION_WALK);
            }
            if (isDyingAnimation()) {
                freezeSpecialAnimationPose();
                return;
            }
            if (thronePos == null && (isSleepingInFort() || isSeatedInFort()) && !returnToThroneOnLoad) {
                // A King placed before the throne was remembered: wherever he rests now is the throne.
                thronePos = blockPosition();
                throneYaw = getYRot();
            }
            if (returnToThroneOnLoad && !throneReturnDue) {
                // The chunk does not always unload when you leave (view and simulation distance keep it
                // alive), so being far away long enough counts as leaving too.
                if (level().getNearestPlayer(this, THRONE_RETURN_AWAY_DISTANCE) == null) {
                    if (++throneAwayTicks >= THRONE_RETURN_AWAY_TICKS) {
                        throneReturnDue = true;
                    }
                } else {
                    throneAwayTicks = 0;
                }
            }
            if (throneReturnDue && returnToThroneOnLoad) {
                returnToThroneAfterReload();
            }
            if (!isPhaseTwo()
                    && isAlive()
                    && getHealth() <= getMaxHealth() * PHASE_TWO_HEALTH_THRESHOLD) {
                enterPhaseTwo();
            }

            if (isPhaseTransitioning()) {
                tickPhaseTransition();
                return;
            }

            // The attack id is cleared when the animation ends, but a stomp's ripple is still travelling
            // for up to ~18 ticks after that. Keep the jump dodge open for the ripple's whole journey.
            if (activeLordAttackId == 2 || activeLordAttackId == 3 || activeLordAttackId == 8
                    || activeLordAttackId == 10 || activeLordAttackId == ATTACK_ID_BUTT_SMASH) {
                jumpableRippleUntilTick = tickCount + JUMPABLE_RIPPLE_TAIL_TICKS;
            }
            tickClubAttackTimeline();
            tickButtSmash();
            tickClubHeadThrustHitbox();
            tickStompWave();
            tickFrustration();

            doubleStompCooldownTicks = Math.max(doubleStompCooldownTicks - 1, 0);
            doubleOverheadSlamCooldownTicks = Math.max(doubleOverheadSlamCooldownTicks - 1, 0);
            bellyBumpCooldownTicks = Math.max(bellyBumpCooldownTicks - 1, 0);
            doubleArmUpswingCooldownTicks = Math.max(doubleArmUpswingCooldownTicks - 1, 0);
            clubDoubleOverheadCooldownTicks = Math.max(clubDoubleOverheadCooldownTicks - 1, 0);
            singleOverheadCooldownTicks = Math.max(singleOverheadCooldownTicks - 1, 0);
            clubHeadThrustCooldownTicks = Math.max(clubHeadThrustCooldownTicks - 1, 0);
            buttSmashCooldownTicks = Math.max(buttSmashCooldownTicks - 1, 0);
            roarCooldownTicks = Math.max(roarCooldownTicks - 1, 0);
            summonCooldownTicks = Math.max(summonCooldownTicks - 1, 0);
            tickHealthGates();

            tickClosePressure();
            tickRoarScreenShake();
            tickRoar();
            maybeStartRoar();
            tickSummonPoint();
            maybeStartSummonPoint();
            tickDelayedImpactSounds();
        }
    }

    private void tickFortEncounter() {
        getNavigation().stop();
        setTarget(null);
        setAggressive(false);
        setDeltaMovement(Vec3.ZERO);
        entityData.set(DATA_LORD_LOCOMOTION, LOCOMOTION_IDLE);

        if (isSleepingInFort()) {
            if (tickCount % SLEEP_Z_INTERVAL_TICKS == 0) {
                emitSleepZ();
            }
            if (tickCount % 10 == 0) {
                updateRoyalTrialProgress();
            }
            if (isSleepingInFort() && royalDefenceStarted) {
                beginStartle();
            }
        } else if (isStartledInFort()) {
            wakeUpTicks++;
            if (tickCount % 10 == 0) {
                updateRoyalTrialProgress();
            }
            if (isStartledInFort() && wakeUpTicks >= STARTLE_ANIMATION_TICKS) {
                enterSeated();
            }
        } else if (isSeatedInFort()) {
            if (tickCount % 10 == 0) {
                updateRoyalTrialProgress();
            }
            if (isSeatedInFort()) {
                trackNearestPlayerWithHead();
                tickCourtSleep();
            }
        } else if (isWakingFromFortEncounter()) {
            wakeUpTicks++;
            nudgeForwardDuringFortWakeUp();
            if (wakeUpTicks >= WAKE_UP_ANIMATION_TICKS) {
                finishFortWakeUp();
            }
        }

        if (tickCount % 20 == 0) {
            updateRoyalTrialBossBarPlayers();
        }
    }

    /**
     * If everyone who started the Royal Trials has died or left, the attempt is abandoned: the
     * trials reset, the ogres still standing in them are sent away, and he can sleep and talk again.
     * Trials already cleared stay cleared. Returns true on the tick it resets.
     */
    private boolean tickRoyalTrialAbandon(ServerLevel serverLevel) {
        if (!royalDefenceStarted || serverLevel.getNearestPlayer(this, ROYAL_TRIAL_BAR_RANGE) != null) {
            royalTrialAbandonTicks = 0;
            return false;
        }
        // Called every 10 ticks.
        royalTrialAbandonTicks += 10;
        if (royalTrialAbandonTicks < ROYAL_TRIAL_ABANDON_TICKS) {
            return false;
        }
        royalTrialAbandonTicks = 0;
        royalDefenceStarted = false;
        royalDefenceIntroducedPlayers.clear();
        clearRoyalTrialBossBarPlayers();
        for (OgreGrunt ogre : serverLevel.getEntitiesOfClass(OgreGrunt.class,
                getBoundingBox().inflate(ROYAL_TRIAL_BAR_RANGE),
                candidate -> candidate != this && candidate.isRoyalTrialOgre() && candidate.isAlive())) {
            serverLevel.sendParticles(ParticleTypes.POOF, ogre.getX(), ogre.getY() + ogre.getBbHeight() * 0.5,
                    ogre.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
            ogre.discard();
        }
        return true;
    }

    private void updateRoyalTrialProgress() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (tickRoyalTrialAbandon(serverLevel)) {
            return;
        }
        // A trial spawner left running with nobody near it does not count as someone being there.
        boolean playersNearby = serverLevel.getNearestPlayer(this, ROYAL_TRIAL_BAR_RANGE) != null;

        for (int index = 0; index < royalTrialPositions.size(); index++) {
            int bit = 1 << index;
            if ((completedRoyalTrialsMask & bit) != 0) {
                continue;
            }

            BlockPos trialPos = royalTrialPositions.get(index);
            if (!serverLevel.hasChunk(trialPos.getX() >> 4, trialPos.getZ() >> 4)) {
                continue;
            }
            BlockState state = serverLevel.getBlockState(trialPos);
            if (!(state.getBlock() instanceof TrialSpawnerBlock)) {
                continue;
            }
            TrialSpawnerState trialState = state.getValue(TrialSpawnerBlock.STATE);
            if (!royalDefenceStarted && playersNearby && trialState == TrialSpawnerState.ACTIVE) {
                royalDefenceStarted = true;
            }
            if (trialState == TrialSpawnerState.WAITING_FOR_REWARD_EJECTION
                    || trialState == TrialSpawnerState.EJECTING_REWARD
                    || trialState == TrialSpawnerState.COOLDOWN) {
                royalDefenceStarted = true;
                completedRoyalTrialsMask |= bit;
            }
        }

        int completed = Integer.bitCount(completedRoyalTrialsMask & 0b111);
        royalTrialProgress.setProgress(completed / (float) ROYAL_TRIAL_COUNT);
        royalTrialProgress.setName(royalUiText(
                "Royal Trials " + completed + "/" + ROYAL_TRIAL_COUNT));
        if (completed >= ROYAL_TRIAL_COUNT) {
            beginFortWakeUp();
        }
    }

    private void beginFortWakeUp() {
        if (!isRoyalDefenceInProgress()) {
            return;
        }
        wakeUpTicks = 0;
        entityData.set(DATA_FORT_ENCOUNTER_STATE, FORT_ENCOUNTER_WAKING);
        playAttackAnimation(14);
        royalTrialProgress.setProgress(1.0F);
        showFortWakeUpTitle();
    }

    private void showFortWakeUpTitle() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double rangeSqr = ROYAL_TRIAL_BAR_RANGE * ROYAL_TRIAL_BAR_RANGE;
        for (ServerPlayer player : serverLevel.getPlayers(candidate ->
                !candidate.isSpectator() && candidate.distanceToSqr(this) <= rangeSqr)) {
            PacketDistributor.sendToPlayer(player,
                    OgreOverlayPayload.major(
                            "THE OGRE KING AWAKENS", ""));
        }
    }

    private static Component royalUiText(String text) {
        return Component.literal(text);
    }

    private void finishFortWakeUp() {
        entityData.set(DATA_FORT_ENCOUNTER_STATE, FORT_ENCOUNTER_NORMAL);
        wakeUpTicks = WAKE_UP_ANIMATION_TICKS;
        activeLordAttackId = 0;
        attackAnimationTicks = 0;
        clearLordAttackSync();
        setNoAi(false);
        setDeltaMovement(Vec3.ZERO);
        reacquireTargetAfterWake();
        clearRoyalTrialBossBarPlayers();
        if (level() instanceof ServerLevel serverLevel) {
            double rangeSqr = ROYAL_TRIAL_BAR_RANGE * ROYAL_TRIAL_BAR_RANGE;
            for (ServerPlayer player : serverLevel.getPlayers(candidate ->
                    !candidate.isSpectator() && candidate.distanceToSqr(this) <= rangeSqr)) {
                kingBossBar.addPlayer(player);
            }
        }
    }

    private void nudgeForwardDuringFortWakeUp() {
        int nudgeEndTick = FORT_WAKE_NUDGE_START_TICK + FORT_WAKE_NUDGE_TICKS;
        if (wakeUpTicks < FORT_WAKE_NUDGE_START_TICK || wakeUpTicks >= nudgeEndTick) {
            return;
        }

        double distanceThisTick = FORT_WAKE_FORWARD_NUDGE / FORT_WAKE_NUDGE_TICKS;
        Vec3 wakeForward = Vec3.directionFromRotation(0.0F, getYRot()).scale(distanceThisTick);
        setPos(getX() + wakeForward.x, getY(), getZ() + wakeForward.z);
    }

    /** A Z of pixels floats up from above his head while he sleeps on the throne. */
    private void emitSleepZ() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
        serverLevel.sendParticles(ModParticles.SLEEP_Z.get(),
                getX() + forward.x * SLEEP_Z_FORWARD
                        + (random.nextDouble() - 0.5) * SLEEP_Z_HORIZONTAL_JITTER,
                getY() + getBbHeight() * SLEEP_Z_HEIGHT,
                getZ() + forward.z * SLEEP_Z_FORWARD
                        + (random.nextDouble() - 0.5) * SLEEP_Z_HORIZONTAL_JITTER,
                1, 0.0, 0.0, 0.0, 0.0);
    }

    private void beginStartle() {
        wakeUpTicks = 0;
        entityData.set(DATA_FORT_ENCOUNTER_STATE, FORT_ENCOUNTER_STARTLED);
        playAttackAnimation(ATTACK_ID_STARTLE);
    }

    private void enterSeated() {
        entityData.set(DATA_FORT_ENCOUNTER_STATE, FORT_ENCOUNTER_SEATED);
        wakeUpTicks = 0;
        activeLordAttackId = 0;
        attackAnimationTicks = 0;
        clearLordAttackSync();
        courtEmptyTicks = 0;
        if (courtGuestId != null && !royalDefenceStarted && level() instanceof ServerLevel serverLevel
                && serverLevel.getPlayerByUUID(courtGuestId) instanceof ServerPlayer guest
                && guest.distanceToSqr(this) <= COURT_TALK_RANGE * COURT_TALK_RANGE) {
            greet(guest);
        }
        courtGuestId = null;
    }

    /**
     * Seated without the trials having started, he is holding court: he sits for whoever woke
     * him and, once nobody is left in his hall, drifts back to sleep.
     */
    private void tickCourtSleep() {
        if (royalDefenceStarted) {
            return;
        }
        if (level().getNearestPlayer(this, COURT_LEAVE_DISTANCE) != null) {
            courtEmptyTicks = 0;
        } else if (++courtEmptyTicks >= COURT_SLEEP_DELAY_TICKS) {
            returnToSleep();
        }
    }

    private void returnToSleep() {
        wakeUpTicks = 0;
        courtEmptyTicks = 0;
        activeLordAttackId = 0;
        attackAnimationTicks = 0;
        clearLordAttackSync();
        setYHeadRot(getYRot());
        setXRot(0.0F);
        entityData.set(DATA_FORT_ENCOUNTER_STATE, FORT_ENCOUNTER_SLEEPING);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean canWake = isSleepingInFort() || isSeatedInFort();
        if (canWake && royalDefenceStarted && !player.isSecondaryUseActive()) {
            // The Royal Trials are under way: he has nothing to say until they are over.
            if (player instanceof ServerPlayer watcher) {
                watcher.displayClientMessage(Component.translatable("dialogue.kingsandmonsters.king_trials_silence")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (!canWake || royalDefenceStarted || player.isSecondaryUseActive()) {
            return super.mobInteract(player, hand);
        }
        if (!(player instanceof ServerPlayer talker)) {
            return InteractionResult.SUCCESS;
        }
        if (isSleepingInFort()) {
            courtGuestId = talker.getUUID();
            beginStartle();
        } else {
            greet(talker);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canGiveErrandsTo(ServerPlayer player) {
        return isSeatedInFort() && !royalDefenceStarted
                && player.distanceToSqr(this) <= COURT_TALK_RANGE * COURT_TALK_RANGE;
    }

    /** A finished errand is settled before anything else; otherwise he opens with the usual audience. */
    private void greet(ServerPlayer player) {
        QuestManager.playDialogueStartParticle(this);
        if (QuestManager.view(player, this).state()
                == QuestManager.READY) {
            QuestManager.open(player, this);
        } else {
            openDialogue(player);
        }
    }

    private void openDialogue(ServerPlayer player) {
        ServerLevel serverLevel = player.serverLevel();
        RoyalAdvancementEvents.awardPath(player, "tribute/audience");
        PacketDistributor.sendToPlayer(player, new KingDialoguePayload(
                getId(), TributeManager.getFavor(serverLevel, player.getUUID()), Config.TRIBUTE_APPEASED_FAVOR.get(),
                Config.TRIBUTE_TRADE_FAVOR.get(), Config.TRIBUTE_DUEL_FAVOR.get(),
                TributeManager.isAppeased(serverLevel, player.getUUID()),
                duelWins.containsKey(player.getUUID()), (duelRestTicks(player) + 19) / 20));
    }

    /** Handles a button pressed on the dialogue screen; the server re-checks everything. */
    public void handleDialogueAction(ServerPlayer player, int action) {
        if (!isSeatedInFort() || royalDefenceStarted
                || player.distanceToSqr(this) > COURT_TALK_RANGE * COURT_TALK_RANGE) {
            return;
        }
        ServerLevel serverLevel = player.serverLevel();
        if (action == KingDialogueActionPayload.TRADE) {
            if (TributeManager.isAppeased(serverLevel, player.getUUID())
                    && TributeManager.getFavor(serverLevel, player.getUUID()) >= Config.TRIBUTE_TRADE_FAVOR.get()) {
                UUID buyer = player.getUUID();
                KingTrades trades = new KingTrades(level(), offersFor(buyer), index -> {
                    recordSale(buyer, index);
                    RoyalAdvancementEvents.awardPath(player, "tribute/royal_patronage");
                });
                todaysWares().stream().filter(Ware::needed).findFirst().ifPresent(ware ->
                        player.displayClientMessage(Component.translatable(
                                "message.kingsandmonsters.king_need",
                                new ItemStack(ware.first().item().value()).getHoverName()), true));
                trades.setTradingPlayer(player);
                trades.openTradingScreen(player, getDisplayName(), 1);
            }
        } else if (action == KingDialogueActionPayload.ERRANDS) {
            QuestManager.open(player, this);
        } else if (action == KingDialogueActionPayload.DUEL) {
            if (TributeManager.getFavor(serverLevel, player.getUUID()) >= Config.TRIBUTE_DUEL_FAVOR.get()
                    && duelRestTicks(player) <= 0) {
                duel(player);
            }
        } else if (action == KingDialogueActionPayload.CHALLENGE) {
            challenge(player);
        } else if (action == KingDialogueActionPayload.OFFER_COINS) {
            TributeManager.offerTribute(player);
            openDialogue(player);
        }
    }

    /**
     * A fair fight for the throne, open to a player the tribe thinks well of: no captains join, and
     * winning costs no favor and angers no one.
     */
    private void duel(ServerPlayer player) {
        duelistId = player.getUUID();
        player.serverLevel().players().stream()
                .filter(nearby -> nearby.distanceToSqr(this) <= COURT_LEAVE_DISTANCE * COURT_LEAVE_DISTANCE)
                .forEach(nearby -> nearby.sendSystemMessage(Component.translatable(
                        "dialogue.kingsandmonsters.king_duel").withStyle(ChatFormatting.GOLD)));
        beginFortWakeUp();
    }

    /** True when this King was fought in a duel: his fall is not an act of war on the tribe. */
    public boolean isDuel() {
        return duelistId != null;
    }

    /** True while this King is in a duel with the player: the fair fight that has not ended yet. */
    public boolean isDuelingWith(Player player) {
        return isAlive() && player.getUUID().equals(duelistId);
    }

    public boolean isDuelist(ServerPlayer player) {
        return player.getUUID().equals(duelistId);
    }

    /**
     * The King yields: he stands down at full health and takes his seat again, no one is hurt in the
     * tribe's eyes, and the bog iron opens to every player. He keeps his crown and club; only
     * a challenge that ends in his death takes them.
     */
    private void yieldDuel(ServerPlayer winner) {
        duelistId = null;
        setHealth(getMaxHealth());
        entityData.set(DATA_LORD_PHASE_TWO, false);
        entityData.set(DATA_LORD_TRANSITIONING, false);
        cancelActiveMeleeAttack();
        stopRoarImmediately();
        stopSummonImmediately();
        setTarget(null);
        setAggressive(false);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        setNoAi(true);
        kingBossBar.removeAllPlayers();
        enterSeated();
        duelWins.put(winner.getUUID(), level().getGameTime());
        sayYield(winner);
        // He sits where the fight ended for now, and goes back to his throne once the chunk reloads.
        returnToThroneOnLoad = thronePos != null && !thronePos.closerToCenterThan(position(), 2.0);
        throneReturnDue = false;
        playSound(ModSoundEvents.OGRE_LORD_HUFF.get(), 1.5F, 0.8F);
        RoyalAdvancementEvents.awardPath(winner, "tribute/fair_fight");
        BogIronProgression.unlock(winner.server,
                "The Ogre King yields to a fair fight. Its buried iron now answers to your kind.");
    }

    private static final int YIELD_LINES = 4;

    /** What he says to whoever has just beaten him, heard by everyone in his hall. */
    private void sayYield(ServerPlayer winner) {
        Component line = Component.translatable("dialogue.kingsandmonsters.king_yield."
                + (1 + getRandom().nextInt(YIELD_LINES))).withStyle(ChatFormatting.GOLD);
        winner.serverLevel().players().stream()
                .filter(nearby -> nearby.distanceToSqr(this) <= COURT_LEAVE_DISTANCE * COURT_LEAVE_DISTANCE)
                .forEach(nearby -> nearby.sendSystemMessage(line));
    }

    /** Ticks before this player may duel him again; 0 when they can, or have never beaten him. */
    private int duelRestTicks(ServerPlayer player) {
        Long won = duelWins.get(player.getUUID());
        if (won == null) {
            return 0;
        }
        long rest = Config.TRIBUTE_DUEL_COOLDOWN_MINUTES.get() * 60L * 20L - (level().getGameTime() - won);
        return (int) Math.max(0L, rest);
    }

    /** The chunk is back after a won duel: he is carried back to his throne, seated, as if he had never left. */
    private void returnToThroneAfterReload() {
        throneReturnDue = false;
        returnToThroneOnLoad = false;
        if (thronePos == null || !isSeatedInFort() || royalDefenceStarted) {
            return;
        }
        moveTo(thronePos.getX() + 0.5, thronePos.getY(), thronePos.getZ() + 0.5, throneYaw, 0.0F);
        setYBodyRot(throneYaw);
        setYHeadRot(throneYaw);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
    }

    /** True while he can be caught in a Bogjaw trap: awake, fighting and not recently held. */
    public boolean canBeTrapped() {
        return !isFortEncounterDormant() && !isDyingAnimation() && !isPhaseTransitioning()
                && !buttSmashAirborne && tickCount >= trapReadyAtTick;
    }

    /** A trap holds him only briefly, and not again for a while, so it can't be chained. */
    public void onTrapped() {
        trapReadyAtTick = tickCount + TRAP_COOLDOWN_TICKS;
    }

    /** Turning on the King ends any peace: favor is gone and the tribe is angry again. */
    private void challenge(ServerPlayer player) {
        duelistId = null;
        ServerLevel serverLevel = player.serverLevel();
        TributeManager.addFavor(serverLevel, player, -TributeSavedData.MAX_FAVOR);
        TributeManager.raiseFactionAnger(serverLevel, CHALLENGE_ANGER);
        RoyalAdvancementEvents.awardPath(player, "tribute/biting_the_hand");
        summonUnfinishedCaptains(serverLevel, player);
        beginFortWakeUp();
    }

    /**
     * Challenging him before the Royal Trials are cleared brings every captain the player has not
     * beaten to his side. Clearing the trials first is how to face him alone. The trials are in the
     * order Mage, Brute, Grunt Captain, one bit each.
     */
    private void summonUnfinishedCaptains(ServerLevel serverLevel, ServerPlayer challenger) {
        List<EntityType<? extends OgreGrunt>> captains = new ArrayList<>();
        if ((completedRoyalTrialsMask & 1) == 0) captains.add(ModEntities.OGRE_MAGE.get());
        if ((completedRoyalTrialsMask & 2) == 0) captains.add(ModEntities.OGRE_BRUTE.get());
        if ((completedRoyalTrialsMask & 4) == 0) captains.add(ModEntities.OGRE_GRUNT_CAPTAIN.get());
        if (captains.isEmpty()) {
            return;
        }
        for (int i = 0; i < captains.size(); i++) {
            BlockPos spawnPos = findCourtSpawnPos(serverLevel, i, captains.size(), captains.get(i));
            if (spawnPos == null) {
                continue;
            }
            OgreGrunt captain = captains.get(i).spawn(serverLevel, spawnPos, MobSpawnType.MOB_SUMMONED);
            if (captain == null) {
                continue;
            }
            captain.setYRot(getYRot());
            captain.setYBodyRot(getYRot());
            captain.setYHeadRot(getYRot());
            captain.markRoyalTrialOgre();
            captain.setPersistenceRequired();
            captain.setRoyalCommandTarget(challenger);
            summonedOgres.add(captain.getUUID());
        }
        playVocal(ModSoundEvents.OGRE_LORD_SUMMON_GRUNT.get(), 5.0F, 1.0F);
        challenger.sendSystemMessage(Component.empty()
                .append(getDisplayName().copy().withStyle(ChatFormatting.GOLD))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("dialogue.kingsandmonsters.king_challenge")
                        .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC)));
    }

    /**
     * The captains arrive at fixed spots out in front of the throne, spread in a line across his
     * view, rather than inside his collision box. Starts at the full distance and works back toward
     * him until there is room, since the throne room is enclosed.
     */
    @Nullable
    private BlockPos findCourtSpawnPos(ServerLevel serverLevel, int index, int count,
                                       EntityType<? extends OgreGrunt> entityType) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double lateral = (index - (count - 1) / 2.0) * COURT_SPAWN_SPACING;
        for (double distance = COURT_SPAWN_DISTANCE; distance >= 2.0; distance -= 1.0) {
            double x = getX() + forward.x * distance + right.x * lateral;
            double z = getZ() + forward.z * distance + right.z * lateral;
            for (int dy : new int[] {0, -1, 1, -2, 2}) {
                BlockPos pos = BlockPos.containing(x, getY() + dy, z);
                BlockPos below = pos.below();
                if (serverLevel.getBlockState(below).getCollisionShape(serverLevel, below).isEmpty()
                        || nearFire(serverLevel, pos)
                        || !serverLevel.getFluidState(pos).isEmpty()
                        || !serverLevel.noCollision(entityType.getDimensions().makeBoundingBox(
                                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
                    continue;
                }
                return pos;
            }
        }
        return null;
    }

    /** The firepit in front of the throne: a captain is never put on it or beside it. */
    private static boolean nearFire(ServerLevel serverLevel, BlockPos pos) {
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            BlockState state = serverLevel.getBlockState(check);
            if (state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES)
                    || state.is(Blocks.MAGMA_BLOCK)
                    || state.is(Blocks.LAVA)) {
                return true;
            }
        }
        return false;
    }

    /** How many of the first wares are one-time artifacts; everything after them is repeatable. */
    private static final int ONE_TIME_WARES = 9;

    private record DailyStock(long day, int[] used) {}

    /** A repeatable ware: what it costs, what it gives, and how many a player may buy per day. */
    private record Ware(ItemCost first, @Nullable ItemCost second, ItemStack result, int perDay, boolean needed) {}

    private static ItemCost cost(ItemLike item, int count) {
        return new ItemCost(item, count);
    }

    /**
     * What the King sells this player. Everything here is his alone (the Merchant stocks none of
     * it), the artifacts are sold once to each player, the crown and club stay with whoever
     * overthrows him, and the rest is the daily market of {@link #todaysWares()}. Order matters: it
     * is the slot each purchase is saved under.
     */
    private MerchantOffers offersFor(UUID buyer) {
        int bought = kingPurchases.getOrDefault(buyer, 0);
        MerchantOffers offers = new MerchantOffers();
        offers.add(artifact(ModItems.OGRE_KINGS_BANNER.get(), cost(Items.GOLD_INGOT, 12), null, (bought & 1) != 0));
        offers.add(artifact(ModItems.HUNTERS_SPEAR.get(), cost(ModItems.GOLDEN_OGRE_TOOTH.get(), 2), cost(ModItems.GOLD_COIN.get(), 3), (bought & 2) != 0));
        offers.add(artifact(ModItems.KINGS_CLEAVER.get(), cost(ModItems.GOLDEN_OGRE_TOOTH.get(), 3), cost(Items.DIAMOND, 6), (bought & 4) != 0));
        offers.add(artifact(ModItems.BRUTES_HELM.get(), cost(ModItems.GOLDEN_OGRE_TOOTH.get(), 3), cost(Items.DIAMOND, 10), (bought & 8) != 0));
        offers.add(artifact(ModItems.OGRE_LOINCLOTH.get(), cost(ModItems.GOLDEN_OGRE_TOOTH.get(), 2), cost(ModItems.GOLD_COIN.get(), 15), (bought & 16) != 0));
        offers.add(artifact(ModItems.BRUTE_GAUNTLETS.get(), cost(ModItems.GOLDEN_OGRE_TOOTH.get(), 2), cost(ModItems.GOLD_COIN.get(), 23), (bought & 32) != 0));
        offers.add(artifact(ModItems.OGRE_STOMACH.get(), cost(ModItems.GOLDEN_OGRE_TOOTH.get(), 8), cost(Items.NETHERITE_INGOT, 3), (bought & 64) != 0));
        offers.add(new MerchantOffer(cost(ModItems.GOLDEN_OGRE_TOOTH.get(), 2), Optional.of(cost(Items.RABBIT, 34)), timberAxe(),
                (bought & 128) != 0 ? 1 : 0, 1, 0, 0.0F));
        // Appended rather than inserted so every older one-time purchase keeps its original save bit.
        offers.add(artifact(ModItems.BOGSTEEL_CHESTPLATE.get(), cost(ModItems.GOLDEN_OGRE_TOOTH.get(), 4),
                cost(Items.DIAMOND, 17), (bought & 256) != 0));
        List<Ware> wares = todaysWares();
        int[] used = usedToday(buyer, wares.size());
        for (int i = 0; i < wares.size(); i++) {
            Ware ware = wares.get(i);
            offers.add(new MerchantOffer(ware.first(), Optional.ofNullable(ware.second()), ware.result().copy(),
                    used[i], ware.perDay(), 0, 0.0F));
        }
        return offers;
    }

    /** A bog iron axe that already carries the Timber enchantment, which only the King can give. */
    private ItemStack timberAxe() {
        ItemStack axe = new ItemStack(ModItems.BOG_IRON_AXE.get());
        axe.set(DataComponents.CUSTOM_NAME,
                Component.literal("Stumpsplitter").withStyle(style -> style.withItalic(false)));
        axe.enchant(level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ModEnchantments.TIMBER), 1);
        return axe;
    }

    /** Cooked meats the tribe pays coin for. Exactly one of these is on offer each day. */
    private static List<Ware> meatPool() {
        ItemStack coin = new ItemStack(ModItems.GOLD_COIN.get());
        return List.of(
                new Ware(cost(ModItems.ROASTED_MUDSNOUT_HAUNCH.get(), 16), null, coin.copy(), 2, false),
                new Ware(cost(Items.COOKED_BEEF, 16), null, coin.copy(), 2, false),
                new Ware(cost(Items.COOKED_PORKCHOP, 16), null, coin.copy(), 2, false),
                new Ware(cost(Items.COOKED_MUTTON, 16), null, coin.copy(), 2, false),
                new Ware(cost(Items.COOKED_CHICKEN, 16), null, coin.copy(), 2, false));
    }

    /** Hides the tribe pays coin for. Exactly one of these is on offer each day. */
    private static List<Ware> leatherPool() {
        ItemStack coin = new ItemStack(ModItems.GOLD_COIN.get());
        return List.of(
                new Ware(cost(ModItems.BLACKHIDE_LEATHER.get(), 16), null, coin.copy(), 3, false),
                new Ware(cost(Items.LEATHER, 16), null, coin.copy(), 3, false));
    }

    /** Affordable Ogre cooking sold for ordinary gold rather than the tribe's favor currency. */
    private static List<Ware> provisionPool() {
        return List.of(
                new Ware(cost(Items.GOLD_INGOT, 8), null, new ItemStack(ModItems.IRONBELLY_STEW.get()), 4, false),
                new Ware(cost(Items.GOLD_INGOT, 6), null, new ItemStack(ModItems.BONE_BROTH.get()), 4, false));
    }

    /**
     * The King's market for today: one meat and one hide the tribe buys, both Ogre meals for gold, and two
     * useful tribal provisions for Gold Coins. There is deliberately no circular exchange into
     * gold, emeralds, or golden teeth. Order matters: it is the slot each purchase is saved under.
     */
    private List<Ware> todaysWares() {
        Random random = new Random(level().getDayTime() / 24000L * 31L + getUUID().hashCode());
        List<Ware> meats = new ArrayList<>(meatPool());
        List<Ware> leathers = new ArrayList<>(leatherPool());
        List<Ware> provisions = new ArrayList<>(provisionPool());
        Collections.shuffle(meats, random);
        Collections.shuffle(leathers, random);
        Collections.shuffle(provisions, random);

        // One meat and one hide, never two of the same kind.
        List<Ware> market = new ArrayList<>();
        market.add(meats.get(0));
        market.add(leathers.get(0));
        market.add(provisions.get(0));
        market.add(provisions.get(1));
        market.add(new Ware(cost(Items.GOLD_NUGGET, 13), null, new ItemStack(Items.IRON_INGOT, 2), 12, false));
        return market;
    }

    /** Today's purchases of each repeatable ware; a new day starts them all over. */
    private int[] usedToday(UUID buyer, int count) {
        long day = level().getDayTime() / 24000L;
        DailyStock stock = kingDaily.get(buyer);
        if (stock == null || stock.day() != day || stock.used().length != count) {
            stock = new DailyStock(day, new int[count]);
            kingDaily.put(buyer, stock);
        }
        return stock.used();
    }

    private void recordSale(UUID buyer, int index) {
        if (index < ONE_TIME_WARES) {
            kingPurchases.merge(buyer, 1 << index, (old, bit) -> old | bit);
            return;
        }
        int[] used = usedToday(buyer, todaysWares().size());
        if (index - ONE_TIME_WARES < used.length) {
            used[index - ONE_TIME_WARES]++;
        }
    }

    private static MerchantOffer artifact(Item result, ItemCost first, @Nullable ItemCost second, boolean soldOut) {
        return new MerchantOffer(first, Optional.ofNullable(second), new ItemStack(result), soldOut ? 1 : 0, 1, 0, 0.0F);
    }

    /**
     * His AI is off while dormant, so nothing turns his head. Turn it toward the nearest player
     * by hand; the model adds the same procedural tracking it uses in combat on top of this.
     */
    private void trackNearestPlayerWithHead() {
        Player nearest = level().getNearestPlayer(this, SEATED_HEAD_TRACK_RANGE);
        float seatYaw = getYRot();
        float desiredYaw = seatYaw;
        float desiredPitch = 0.0F;
        if (nearest != null) {
            double dx = nearest.getX() - getX();
            double dz = nearest.getZ() - getZ();
            double dy = nearest.getY(0.5) - getEyeY();
            desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            desiredPitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI));
        }
        // Keep the head inside what he can turn on the throne, measured from the seat.
        float yawOffset = Mth.clamp(Mth.wrapDegrees(desiredYaw - seatYaw),
                -SEATED_HEAD_MAX_YAW, SEATED_HEAD_MAX_YAW);
        setYHeadRot(Mth.rotLerp(SEATED_HEAD_TURN_RESPONSE, getYHeadRot(), seatYaw + yawOffset));
        setXRot(Mth.lerp(SEATED_HEAD_TURN_RESPONSE, getXRot(),
                Mth.clamp(desiredPitch, -SEATED_HEAD_MAX_UP_PITCH, SEATED_HEAD_MAX_DOWN_PITCH)));
    }

    /**
     * Vanilla swivels a mob's whole body toward its head once the head holds still. On the throne
     * only the head should move, so the body stays on the seat with a very small lean toward the
     * head. Runs on both sides, since the client works out its own body rotation.
     */
    @Override
    protected float tickHeadTurn(float yRot, float animStep) {
        if (!isRoyalDefenceInProgress()) {
            return super.tickHeadTurn(yRot, animStep);
        }
        float seatYaw = getYRot();
        float targetYaw = seatYaw;
        if (isSeatedInFort()) {
            float headOffset = Mth.wrapDegrees(getYHeadRot() - seatYaw);
            targetYaw += Mth.clamp(headOffset * SEATED_BODY_FOLLOW,
                    -SEATED_BODY_MAX_YAW, SEATED_BODY_MAX_YAW);
        }
        yBodyRotO = yBodyRot;
        yBodyRot = Mth.rotLerp(SEATED_BODY_RESPONSE, yBodyRot, targetYaw);
        return animStep;
    }

    private void reacquireTargetAfterWake() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double followRange = getAttributeValue(Attributes.FOLLOW_RANGE);
        double rangeSqr = followRange * followRange;
        Player nearest = serverLevel.getPlayers(player ->
                        !player.isCreative()
                                && !player.isSpectator()
                                && player.distanceToSqr(this) <= rangeSqr
                                && canAttack(player))
                .stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
        if (nearest != null) {
            setTarget(nearest);
        }
    }

    private void updateRoyalTrialBossBarPlayers() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!royalDefenceStarted) {
            clearRoyalTrialBossBarPlayers();
            return;
        }
        double rangeSqr = ROYAL_TRIAL_BAR_RANGE * ROYAL_TRIAL_BAR_RANGE;
        List<ServerPlayer> nearbyPlayers = serverLevel.getPlayers(player ->
                !player.isSpectator() && player.distanceToSqr(this) <= rangeSqr);
        for (ServerPlayer player : List.copyOf(royalTrialProgress.getPlayers())) {
            if (!nearbyPlayers.contains(player)) {
                removeRoyalTrialBossBarPlayer(player);
            }
        }
        for (ServerPlayer player : nearbyPlayers) {
            addRoyalTrialBossBarPlayer(player);
        }
    }

    private void addRoyalTrialBossBarPlayer(ServerPlayer player) {
        OgreLord previousOwner = ROYAL_TRIAL_BAR_OWNERS.put(player, this);
        if (previousOwner != null && previousOwner != this) {
            previousOwner.royalTrialProgress.removePlayer(player);
        }
        royalTrialProgress.addPlayer(player);
        if (royalDefenceStarted
                && isRoyalDefenceInProgress()
                && royalDefenceIntroducedPlayers.add(player.getUUID())) {
            PacketDistributor.sendToPlayer(player,
                    OgreOverlayPayload.major("ROYAL TRIALS", ""));
        }
    }

    private void removeRoyalTrialBossBarPlayer(ServerPlayer player) {
        royalTrialProgress.removePlayer(player);
        ROYAL_TRIAL_BAR_OWNERS.remove(player, this);
    }

    private void clearRoyalTrialBossBarPlayers() {
        for (ServerPlayer player : List.copyOf(royalTrialProgress.getPlayers())) {
            removeRoyalTrialBossBarPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        removeRoyalTrialBossBarPlayer(player);
        kingBossBar.removePlayer(player);
        super.stopSeenByPlayer(player);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!isFortEncounterDormant()) {
            kingBossBar.addPlayer(player);
        }
    }

    private boolean isFortEncounterDormant() {
        return entityData.get(DATA_FORT_ENCOUNTER_STATE) != FORT_ENCOUNTER_NORMAL;
    }

    /** True once the King has fully awakened and is a normal active combat participant. */
    public boolean isFortEncounterActive() {
        return !isFortEncounterDormant();
    }

    /** Client-visible encounter authority for music: awakening begins the encounter, sleep excludes it. */
    public boolean isFortEncounterMusicActive() {
        // The trials play out without the boss theme; it starts when he stands up.
        return !isRoyalDefenceInProgress();
    }

    private boolean isSleepingInFort() {
        return entityData.get(DATA_FORT_ENCOUNTER_STATE) == FORT_ENCOUNTER_SLEEPING;
    }

    private boolean isStartledInFort() {
        return entityData.get(DATA_FORT_ENCOUNTER_STATE) == FORT_ENCOUNTER_STARTLED;
    }

    private boolean isSeatedInFort() {
        return entityData.get(DATA_FORT_ENCOUNTER_STATE) == FORT_ENCOUNTER_SEATED;
    }

    /** True from the start of the trials until he stands up: asleep, startled, or seated. */
    private boolean isRoyalDefenceInProgress() {
        return isSleepingInFort() || isStartledInFort() || isSeatedInFort();
    }

    private boolean isWakingFromFortEncounter() {
        return entityData.get(DATA_FORT_ENCOUNTER_STATE) == FORT_ENCOUNTER_WAKING;
    }

    public boolean isPhaseTwo() {
        return entityData.get(DATA_LORD_PHASE_TWO);
    }

    public boolean isPhaseTransitioning() {
        return entityData.get(DATA_LORD_TRANSITIONING);
    }

    public boolean isClubInHand() {
        return entityData.get(DATA_LORD_CLUB_IN_HAND);
    }

    public boolean isDyingAnimation() {
        return entityData.get(DATA_LORD_DYING);
    }

    /** True on the final authored death-animation tick, immediately before normal removal. */
    public boolean isDyingAnimationFinishing() {
        return isDyingAnimation() && deathTime >= DEATH_ANIMATION_TICKS - 1;
    }

    /** Synced, render-only exclusion for Sleeping, Pull-Out, and Death. */
    public boolean isProceduralHeadTrackingSuppressed() {
        // Seated he watches the room, so only sleeping and the startle suppress tracking.
        return isSleepingInFort() || isStartledInFort()
                || entityData.get(DATA_LORD_TRANSITIONING)
                || entityData.get(DATA_LORD_DYING);
    }

    private void enterPhaseTwo() {
        // Mid-air the pull-out waits; the health check calls this again once he has landed.
        if (isPhaseTwo() || isDyingAnimation() || buttSmashAirborne) {
            return;
        }
        entityData.set(DATA_LORD_PHASE_TWO, true);
        entityData.set(DATA_LORD_TRANSITIONING, true);
        entityData.set(DATA_LORD_CLUB_IN_HAND, false);
        phaseTransitionElapsedTicks = 0;
        lockSpecialAnimationPose();
        cancelActiveMeleeAttack();
        stopRoarImmediately();
        stopSummonImmediately();
        roarCooldownTicks = Math.min(roarCooldownTicks, PHASE_TWO_FIRST_ROAR_DELAY_TICKS);
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, PULL_OUT_BUFF_DURATION_TICKS, 0));
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, PULL_OUT_BUFF_DURATION_TICKS, 0));
        playAttackAnimation(9);
        scheduleHeavyAttackRoar(9, PULL_OUT_IMPACT_TICK, PULL_OUT_ROAR_PITCH, false,
                PULL_OUT_ROAR_LEAD_TICKS);
    }

    private void tickPhaseTransition() {
        freezeSpecialAnimationPose();
        phaseTransitionElapsedTicks++;
        heavyAttackRoarDelayTicks = tickDelayedHeavyAttackRoar(heavyAttackRoarDelayTicks);

        if (phaseTransitionElapsedTicks == PULL_OUT_KNEEL_HURT_SOUND_TICK) {
            playVocal(ModSoundEvents.OGRE_LORD_HURT.get(), 1.25F, PULL_OUT_KNEEL_HURT_SOUND_PITCH);
        }
        if (phaseTransitionElapsedTicks >= CLUB_VISIBILITY_SWAP_TICK && !isClubInHand()) {
            entityData.set(DATA_LORD_CLUB_IN_HAND, true);
        }
        if (phaseTransitionElapsedTicks == PULL_OUT_IMPACT_TICK) {
            performPullOutSwing();
        }
        if (phaseTransitionElapsedTicks >= PULL_OUT_ANIMATION_TICKS) {
            entityData.set(DATA_LORD_TRANSITIONING, false);
            activeLordAttackId = 0;
            attackAnimationTicks = 0;
            clearLordAttackSync();
        }
    }

    private void tickClubAttackTimeline() {
        int attackId = activeLordAttackId;
        if (attackId != 10 && attackId != 11
                && attackId != ATTACK_ID_REGULAR_ATTACK && attackId != ATTACK_ID_REGULAR_ATTACK_2) {
            return;
        }
        // Only the double overhead plants him; the single club slam and the regular slams are thrown
        // while he walks, so they strike wherever he and his heading are at the moment of impact.
        if (attackId == 10) {
            freezeSpecialAnimationPose();
        }
        clubAttackElapsedTicks++;
        int impactTick = switch (attackId) {
            case 10 -> CLUB_DOUBLE_OVERHEAD_IMPACT_TICK;
            case 11 -> SINGLE_OVERHEAD_IMPACT_TICK;
            default -> REGULAR_ATTACK_IMPACT_TICK;
        };
        if (!clubAttackImpactFired && clubAttackElapsedTicks >= impactTick) {
            clubAttackImpactFired = true;
            switch (attackId) {
                case 10 -> performClubDoubleOverhead();
                case 11 -> performSingleOverheadSlam();
                default -> performRegularSlam(attackId == ATTACK_ID_REGULAR_ATTACK ? 1.0 : -1.0);
            }
        }
    }

    private void lockSpecialAnimationPose() {
        lockedSpecialX = getX();
        lockedSpecialZ = getZ();
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            lockedSpecialYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        } else {
            lockedSpecialYaw = getYRot();
        }
        freezeSpecialAnimationPose();
    }

    /** Share of the remaining turn closed each tick when a special attack or roar swings him round. */
    private static final float FACING_EASE_FRACTION = 0.45F;
    /** The roar's own authored sway takes over from the ease after this many ticks. */
    private static final int ROAR_FACING_EASE_TICKS = 6;

    /** One proportional step toward a facing (like the Ogre move control's turning), landing exactly when close. */
    private static float easeFacing(float current, float target, float fraction) {
        float remaining = Mth.wrapDegrees(target - current);
        return Math.abs(remaining) < 0.5F ? target : current + remaining * fraction;
    }

    private void freezeSpecialAnimationPose() {
        getNavigation().stop();
        setPos(lockedSpecialX, getY(), lockedSpecialZ);
        // Eases round to the locked facing over a few ticks instead of snapping to it, and leaves the
        // previous rotations alone so the renderer blends between ticks.
        setYRot(easeFacing(getYRot(), lockedSpecialYaw, FACING_EASE_FRACTION));
        setYBodyRot(easeFacing(yBodyRot, lockedSpecialYaw, FACING_EASE_FRACTION));
        setYHeadRot(easeFacing(getYHeadRot(), lockedSpecialYaw, FACING_EASE_FRACTION));
        setXxa(0.0F);
        setZza(0.0F);
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
    }

    private void stopRoarImmediately() {
        roarActiveTicks = 0;
        roarEffectsApplied = false;
        roarSoundPlayed = false;
    }

    private void stopSummonImmediately() {
        summonActiveTicks = 0;
        summonEventFired = false;
        summonVocalPlayed = false;
        summonPunchSoundPlayed = false;
        summonTarget = null;
    }

    private void clearLordAttackSync() {
        if (!level().isClientSide) {
            entityData.set(DATA_LORD_ATTACK_ID, 0);
            entityData.set(DATA_LORD_ATTACK_START_TICK, 0L);
        }
    }

    private void performPullOutSwing() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, lockedSpecialYaw).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 impact = position().add(forward.scale(4.5)).add(right.scale(-0.5));
        Vec3 sideSwingDirection = forward.add(right.scale(0.7)).normalize();
        Set<UUID> hitTargets = new HashSet<>();
        List<Vec3> samples = new ArrayList<>();
        for (double radius : new double[]{3.0, 5.25, 7.5}) {
            for (int degrees = -95; degrees <= 70; degrees += 15) {
                double angle = Math.toRadians(degrees);
                samples.add(position()
                        .add(forward.scale(Math.cos(angle) * radius))
                        .add(right.scale(Math.sin(angle) * radius)));
            }
        }

        for (LivingEntity target : getClubCandidates(9.5, 6.0)) {
            Vec3 relative = target.position().subtract(position());
            double localForward = relative.dot(forward);
            boolean struckByClub = localForward >= -0.75
                    && intersectsAnySwingSample(target, samples, 2.3);
            if (!struckByClub) {
                continue;
            }
            if (hitTargets.add(target.getUUID())) {
                if (dealClubDamage(target, PULL_OUT_DAMAGE_MULTIPLIER,
                        Config.OGRE_LORD_PULL_OUT_BONUS_DAMAGE.get().floatValue())) {
                    applyHeavyClubHitEffects(target);
                    // Direct contact with the club keeps the lateral force of its side swing.
                    applyClubKnockback(target, sideSwingDirection, 3.2, 0.55);
                }
            }
        }

        playHeavyClubImpact(impact, 2);
        // Dust cloud/ring radius must match the actual ripple radius below (17.0F) — this was still
        // the pre-buff 8.5F, so the visual AOE cue read as half the size of the real 17-block hit.
        spawnClubImpactEffects(impact, 17.0F, 1.8F);
        startClubGroundRipple(impact, 17.0F, 0.55, GroundRippleKind.KING_CLUB_PULL_OUT, hitTargets);
    }

    private void performClubDoubleOverhead() {
        performOverheadClubStrike(
                15.0, 13.5, 20.0F,
                CLUB_DOUBLE_OVERHEAD_DAMAGE_MULTIPLIER,
                Config.OGRE_LORD_CLUB_DOUBLE_OVERHEAD_BONUS_DAMAGE.get().floatValue() * CLUB_DOUBLE_OVERHEAD_BONUS_SCALE,
                4.0, 0.8,
                2.4F, 3);
    }

    /**
     * The single club slam lands wherever he is walking: a smoke-ring burst that hurts everything
     * inside it, harder if the club head itself comes down on someone. No ripple.
     */
    private void performSingleOverheadSlam() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 impact = position().add(forward.scale(SINGLE_OVERHEAD_REACH));
        performAreaSlam(position().add(forward.scale(1.5)), impact, 3.0, SINGLE_OVERHEAD_AREA_RADIUS,
                SINGLE_OVERHEAD_DAMAGE_MULTIPLIER,
                Config.OGRE_LORD_SINGLE_OVERHEAD_BONUS_DAMAGE.get().floatValue() * MOBILE_CLUB_BONUS_SCALE,
                0.65, 1.8F);
        playHeavyClubImpact(impact, SINGLE_OVERHEAD_BLOCKS_DESTROYED);
    }

    /** The regular slams: the same burst from a bare hand, to the right or left of his heading. */
    private void performRegularSlam(double rightSign) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 impact = position()
                .add(forward.scale(REGULAR_SLAM_REACH))
                .add(right.scale(REGULAR_SLAM_SIDE_OFFSET * rightSign));
        Vec3 strikeStart = position()
                .add(forward.scale(1.2))
                .add(right.scale(REGULAR_SLAM_SIDE_OFFSET * rightSign));
        performAreaSlam(strikeStart, impact, 2.6, REGULAR_SLAM_AREA_RADIUS, REGULAR_ATTACK_DAMAGE_MULTIPLIER,
                0.0F, 0.5, 1.3F);
        playRegularSlamImpact(impact);
    }

    /**
     * Damages everything inside the smoke ring around {@code impact}. A target the hand or club
     * head actually lands on takes the full hit and is knocked up; the rest of the ring takes a
     * share of it. Everything is applied at once, so it cannot be jumped like a ripple.
     *
     * <p>The strike zone is the whole path the hand or club head comes down along, from
     * {@code strikeStart} out to the impact, widened by {@code directRadius}. A single circle on the
     * landing spot missed people the fist visibly came down on while he was walking or turning.</p>
     */
    private void performAreaSlam(Vec3 strikeStart, Vec3 impact, double directRadius, float areaRadius,
                                 float damageMultiplier, float bonusDamage, double lift, float shake) {
        AABB directVolume = new AABB(
                Math.min(strikeStart.x, impact.x) - directRadius, getY() - 1.0,
                Math.min(strikeStart.z, impact.z) - directRadius,
                Math.max(strikeStart.x, impact.x) + directRadius, getY() + 6.5,
                Math.max(strikeStart.z, impact.z) + directRadius);
        double areaRadiusSqr = (double) areaRadius * areaRadius;
        double search = impact.distanceTo(position()) + areaRadius + 1.0;

        for (LivingEntity target : getClubCandidates(search, 7.0)) {
            AABB bounds = target.getBoundingBox();
            double dx = target.getX() - impact.x;
            double dz = target.getZ() - impact.z;
            boolean struck = bounds.intersects(directVolume);
            boolean inRing = dx * dx + dz * dz <= areaRadiusSqr
                    && bounds.maxY >= impact.y - 1.0 && bounds.minY <= impact.y + 3.5;
            if (!struck && !inRing) {
                continue;
            }
            // The ring around the impact fades toward its rim, like every other ogre's slam.
            float share = struck ? 1.0F
                    : OgreDamage.falloff(Math.sqrt(dx * dx + dz * dz), areaRadius, OgreDamage.BLAST_EDGE_SHARE);
            if (dealClubDamage(target, damageMultiplier * share, bonusDamage * share)) {
                if (struck) {
                    applyHeavyClubHitEffects(target);
                }
                applyVerticalShockwaveLaunch(target, lift * share);
            }
        }

        spawnClubImpactEffects(impact, areaRadius, shake);
    }

    private void performOverheadClubStrike(double length, double halfWidth, float rippleRadius,
                                           float damageMultiplier, float bonusDamage,
                                           double knockback, double verticalKnockback,
                                           float shake, int blocksDestroyed) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, lockedSpecialYaw).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 impact = position().add(forward.scale(length * 0.62));
        double clubHeadRadius = 3.25;
        AABB directImpactVolume = new AABB(
                impact.x - clubHeadRadius, getY() - 1.0, impact.z - clubHeadRadius,
                impact.x + clubHeadRadius, getY() + 6.5, impact.z + clubHeadRadius);
        Set<UUID> hitTargets = new HashSet<>();

        for (LivingEntity target : getClubCandidates(length + 2.0, 7.0)) {
            Vec3 relative = target.position().subtract(position());
            double localForward = relative.dot(forward);
            double localRight = relative.dot(right);
            boolean withinSweptCorridor = localForward >= 0.25
                    && localForward <= length
                    && Math.abs(localRight) <= halfWidth;
            boolean verticallyReachable = target.getBoundingBox().maxY >= getY() - 1.0
                    && target.getBoundingBox().minY <= getY() + 6.5;
            boolean struckByDescendingClub = target.getBoundingBox().intersects(directImpactVolume);
            if (!withinSweptCorridor || !verticallyReachable || !struckByDescendingClub
                    || !hitTargets.add(target.getUUID())) {
                continue;
            }
            // Direct club contact remains full strength. The former half-strength surrounding
            // shockwave is now delivered by the traveling server-authoritative ripple below.
            if (dealClubDamage(target, damageMultiplier, bonusDamage)) {
                applyHeavyClubHitEffects(target);
                applyVerticalShockwaveLaunch(target, verticalKnockback);
            }
        }

        playHeavyClubImpact(impact, blocksDestroyed);
        spawnClubImpactEffects(impact, rippleRadius, shake);
        startClubGroundRipple(impact, rippleRadius, verticalKnockback, GroundRippleKind.KING_CLUB_DOUBLE,
                hitTargets);
    }

    @Override
    protected boolean showsGroundRippleVisual(MeleeAttackHit hit, GroundRippleProfile profile) {
        return switch (profile.kind()) {
            case KING_STOMP_WAVE, KING_CLUB_DOUBLE, KING_CLUB_PULL_OUT, KING_BUTT_SMASH -> true;
            case STANDARD -> hit.areaRadius() == DOUBLE_OVERHEAD_SLAM_AREA_RADIUS
                    || hit.areaRadius() == DOUBLE_STOMP_AREA_RADIUS;
            default -> false;
        };
    }

    private void startClubGroundRipple(Vec3 impact, float radius, double verticalKnockback,
                                       GroundRippleKind kind, Set<UUID> directHits) {
        int rings = Math.max(2, (int) Math.ceil(radius / 1.25F));
        float propagationSpeed = radius / (rings * 2.0F);
        MeleeAttackHit rippleHit = MeleeAttackHit.areaSpecial(0, 0.0F, radius, false,
                        0.0F, 0.0, 0.0F, verticalKnockback, 0, 0, true)
                .withVerticalHitRange(7.0);
        startGroundRipple(impact, rippleHit,
                new GroundRippleProfile(radius, propagationSpeed, 1.125F, 0.44F,
                        kind == GroundRippleKind.KING_CLUB_DOUBLE ? 1.25F : 1.1F,
                        7.0, true, kind), directHits);
    }

    private List<LivingEntity> getClubCandidates(double horizontalRange, double verticalRange) {
        return level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(horizontalRange, verticalRange, horizontalRange),
                target -> target != this
                        && target.isAlive()
                        && (!(target instanceof OgreGrunt) || isPlagueEnemy(target))
                        && canStrikeReach(target));
    }

    /**
     * Allow close strikes through blocked sight rays so trees can't nullify hits within reach.
     */
    private boolean canStrikeReach(LivingEntity target) {
        return getSensing().hasLineOfSight(target)
                || distanceToSqr(target) <= CLOSE_STRIKE_RANGE * CLOSE_STRIKE_RANGE;
    }

    @Override
    protected boolean canAreaHitReach(LivingEntity target) {
        return canStrikeReach(target);
    }

    private boolean intersectsAnySwingSample(LivingEntity target, List<Vec3> samples, double radius) {
        double radiusSqr = radius * radius;
        AABB bounds = target.getBoundingBox();
        for (Vec3 sample : samples) {
            Vec3 clubHead = sample.add(0.0, 2.5, 0.0);
            double dx = distanceToInterval(clubHead.x, bounds.minX, bounds.maxX);
            double dy = distanceToInterval(clubHead.y, bounds.minY, bounds.maxY);
            double dz = distanceToInterval(clubHead.z, bounds.minZ, bounds.maxZ);
            if (dx * dx + dy * dy + dz * dz <= radiusSqr) {
                return true;
            }
        }
        return false;
    }

    private static double distanceToInterval(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        return value > max ? value - max : 0.0;
    }

    private boolean dealClubDamage(LivingEntity target, float damageMultiplier, float bonusDamage) {
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * damageMultiplier + bonusDamage;
        return target.hurt(damageSources().mobAttack(this), damage);
    }

    private void applyHeavyClubHitEffects(LivingEntity target) {
        CombatEffects.applyDazed(target, Config.DAZED_HEAVY_ATTACK_DURATION_TICKS.get(), this);
    }

    private void applyClubKnockback(LivingEntity target, Vec3 direction,
                                    double strength, double verticalStrength) {
        target.knockback(strength, -direction.x, -direction.z);
        target.push(0.0, verticalStrength, 0.0);
    }

    private void applyVerticalShockwaveLaunch(LivingEntity target, double verticalStrength) {
        Vec3 movement = target.getDeltaMovement();
        target.setDeltaMovement(movement.x, Math.max(movement.y, verticalStrength), movement.z);
        target.hasImpulse = true;
    }

    private void playHeavyClubImpact(Vec3 impact, int blocksDestroyed) {
        float shift = impactPitchShift();
        float volume = impactVolumeScale();
        level().playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6F * volume, 0.45F + shift);
        playSlamLayers(impact, 1.6F, shift);
        scheduleSlamRumble(impact, 1.5F);
        destroyNearbyBlocks(impact, blocksDestroyed);
    }

    private void spawnClubImpactEffects(Vec3 impact, float radius, float shake) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        spawnDenseDustCloud(serverLevel, impact, radius);
        // The ring of cloud at the slam's full reach, as a boss's area attack should show.
        StaticShockwaveParticles.spawn(serverLevel, impact, radius);
        PacketDistributor.sendToPlayersNear(
                serverLevel, null, impact.x, impact.y, impact.z,
                Math.max(24.0, radius * 4.0),
                new ScreenShakePayload(impact.x, impact.y, impact.z, shake));
    }

    /**
     * The club slams' dense dust burst (cloud, poof, boundary ring), reused for the unarmed Phase-1
     * Double Overhead Slam so it reads with the same amount of dust. That attack already gets its
     * own screenshake from the shared hit pipeline, so this excludes it to avoid a duplicate packet.
     */
    private void spawnDenseDustCloud(ServerLevel serverLevel, Vec3 impact, float radius) {
        int particles = Math.max(30, (int) (radius * 9.0F));
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                impact.x, impact.y + 0.25, impact.z,
                particles, radius * 0.35, 0.3, radius * 0.35, 0.05);
        serverLevel.sendParticles(ParticleTypes.POOF,
                impact.x, impact.y + 0.35, impact.z,
                particles / 2, radius * 0.2, 0.2, radius * 0.2, 0.03);

        // Draw the slam's actual AOE boundary, matching the readable ground-ring cue used by the
        // club shockwaves so players can judge the jump dodge consistently.
        int ringPoints = Math.max(32, (int) Math.ceil(radius * 8.0F));
        for (int point = 0; point < ringPoints; point++) {
            double angle = Math.PI * 2.0 * point / ringPoints;
            double ringX = impact.x + Math.cos(angle) * radius;
            double ringZ = impact.z + Math.sin(angle) * radius;
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    ringX, impact.y + 0.15, ringZ,
                    1, 0.035, 0.025, 0.035, 0.005);
        }
    }

    /**
     * Fills the gap between a shockwave's center burst and its edge ring with concentric bands that
     * thin out away from the impact. Radius only controls how far the fill reaches; it doesn't draw
     * or resize the ring.
     */
    private void spawnGraduatedDustFill(ServerLevel serverLevel, Vec3 center, float radius) {
        double y = center.y + 0.12;
        float[] bandFractions = {0.15F, 0.35F, 0.55F, 0.75F, 0.9F};
        int[] bandCounts = {20, 16, 12, 8, 5};
        for (int band = 0; band < bandFractions.length; band++) {
            float bandRadius = radius * bandFractions[band];
            int points = bandCounts[band];
            double angleOffset = band * 0.3;
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0 * i / points + angleOffset;
                double x = center.x + Math.cos(angle) * bandRadius;
                double z = center.z + Math.sin(angle) * bandRadius;
                serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.05, 0.04, 0.05, 0.01);
            }
        }
    }

    private void tickClosePressure() {
        LivingEntity target = getTarget();
        if (target != null
                && target.isAlive()
                && distanceToSqr(target) <= BELLY_BUMP_CLOSE_PRESSURE_RANGE * BELLY_BUMP_CLOSE_PRESSURE_RANGE
                && getSensing().hasLineOfSight(target)) {
            closePressureTicks = Math.min(closePressureTicks + (hurtTime > 0 ? 2 : 1), BELLY_BUMP_CLOSE_PRESSURE_TICKS);
        } else {
            closePressureTicks = Math.max(closePressureTicks - 2, 0);
        }
    }

    private void tickDelayedImpactSounds() {
        tickScheduledSounds();
        heavyAttackRoarDelayTicks = tickDelayedHeavyAttackRoar(heavyAttackRoarDelayTicks);
        oneArmUpswingImpactSoundDelayTicks = tickDelayedImpactSound(oneArmUpswingImpactSoundDelayTicks, 1, this::playLaunchImpactSound);
        doubleStompLeftImpactSoundDelayTicks = tickDelayedImpactSound(doubleStompLeftImpactSoundDelayTicks, 2, () -> playDoubleStompImpactSound(-1.0));
        doubleStompRightImpactSoundDelayTicks = tickDelayedImpactSound(doubleStompRightImpactSoundDelayTicks, 2, () -> playDoubleStompImpactSound(1.0));
        doubleStompLeftGruntDelayTicks = tickDelayedImpactSound(doubleStompLeftGruntDelayTicks, 2, this::playPreAttackGrunt);
        doubleStompRightGruntDelayTicks = tickDelayedImpactSound(doubleStompRightGruntDelayTicks, 2, this::playPreAttackGrunt);
        doubleOverheadSlamImpactSoundDelayTicks = tickDelayedImpactSound(doubleOverheadSlamImpactSoundDelayTicks, 3, this::playDoubleOverheadSlamImpactSound);
        bellyBumpRoarDelayTicks = tickDelayedImpactSound(bellyBumpRoarDelayTicks, 5,
                () -> playVocal(ModSoundEvents.OGRE_LORD_BELLY_BUMP_ROAR.get(), 1.3F, 1.0F));
        bellyBumpImpactSoundDelayTicks = tickDelayedImpactSound(bellyBumpImpactSoundDelayTicks, 5, this::playBellyBumpImpactSound);
        doubleArmUpswingGruntDelayTicks = tickDelayedImpactSound(doubleArmUpswingGruntDelayTicks, 6,
                () -> playVocal(ModSoundEvents.OGRE_LORD_DOUBLE_ARM_UPSWING_GRUNT.get(), 1.35F, 1.0F));
        doubleArmUpswingImpactSoundDelayTicks = tickDelayedImpactSound(doubleArmUpswingImpactSoundDelayTicks, 6, this::playLaunchImpactSound);
        clubHeadThrustImpactSoundDelayTicks = tickDelayedImpactSound(clubHeadThrustImpactSoundDelayTicks, 7, this::playClubHeadThrustWhoosh);
        singleLegStompImpactSoundDelayTicks = tickDelayedImpactSound(singleLegStompImpactSoundDelayTicks, 8, this::playSingleLegStompImpactSound);
    }

    private int tickDelayedHeavyAttackRoar(int ticks) {
        if (ticks < 0) {
            return ticks;
        }
        if (activeLordAttackId != heavyAttackRoarAttackId || attackAnimationTicks <= 0) {
            return -1;
        }
        if (ticks <= 0) {
            float variation = (getRandom().nextFloat() * 2.0F - 1.0F) * HEAVY_ATTACK_ROAR_PITCH_VARIATION;
            SoundEvent roar = heavyAttackUsesOgrebloodRoar
                    ? ModSoundEvents.OGREBLOOD_TOTEM_ACTIVATE.get() : ModSoundEvents.OGRE_LORD_OVERHEAD_GRUNT.get();
            if (heavyAttackUsesOgrebloodRoar) {
                level().playSound(null, getX(), getY(), getZ(), roar, SoundSource.HOSTILE,
                        1.85F, heavyAttackRoarPitch + variation);
            } else {
                playVocal(roar, 1.85F, heavyAttackRoarPitch + variation);
            }
            return -1;
        }
        return ticks - 1;
    }

    private void scheduleHeavyAttackRoar(int attackId, int impactTick, float pitch, boolean useOgrebloodRoar) {
        scheduleHeavyAttackRoar(attackId, impactTick, pitch, useOgrebloodRoar, HEAVY_ATTACK_ROAR_LEAD_TICKS);
    }

    private void scheduleHeavyAttackRoar(int attackId, int impactTick, float pitch, boolean useOgrebloodRoar,
                                         int leadTicks) {
        heavyAttackRoarAttackId = attackId;
        // Use the impact sync offset so the client animation opens the mouth before the grunt.
        heavyAttackRoarDelayTicks = Math.max(0, impactTick - leadTicks + ATTACK_SYNC_DELAY_TICKS);
        heavyAttackRoarPitch = pitch;
        heavyAttackUsesOgrebloodRoar = useOgrebloodRoar;
    }

    private int tickDelayedImpactSound(int ticks, int attackId, Runnable playSound) {
        if (ticks < 0) {
            return ticks;
        }

        if (activeLordAttackId != attackId || attackAnimationTicks <= 0) {
            return -1;
        }

        if (ticks <= 0) {
            playSound.run();
            return -1;
        }

        return ticks - 1;
    }

    private void playLordHuff(float volume) {
        playVocal(ModSoundEvents.OGRE_LORD_HUFF.get(), volume, 1.0F);
    }

    private void playPreAttackGrunt() {
        playVocal(ModSoundEvents.OGRE_LORD_OVERHEAD_GRUNT.get(), 1.35F, 1.0F);
    }

    /**
     * The King's recordings are mastered quiet (peaks near 0.3, where the other ogres reach 0.5 to
     * 0.8) and a game volume above 1 only widens the range, so each vocal is played twice on one
     * seed: the same variation, layered, at twice the level.
     */
    private void playVocal(SoundEvent sound, float volume, float pitch) {
        if (isSilent()) {
            return;
        }
        long seed = getRandom().nextLong();
        for (int layer = 0; layer < VOCAL_LAYERS; layer++) {
            level().playSeededSound(null, getX(), getY(), getZ(), sound, getSoundSource(), volume, pitch, seed);
        }
    }

    private boolean isKingVocal(SoundEvent sound) {
        return sound == ModSoundEvents.OGRE_LORD_HURT.get()
                || sound == ModSoundEvents.OGRE_LORD_DEATH.get()
                || sound == ModSoundEvents.OGRE_LORD_HUFF.get();
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (isKingVocal(sound)) {
            playVocal(sound, volume, pitch);
        } else {
            super.playSound(sound, volume, pitch);
        }
    }

    /** One shared shift per impact, so the layers of a slam stay in tune with each other. */
    private float impactPitchShift() {
        return (getRandom().nextFloat() * 2.0F - 1.0F) * IMPACT_PITCH_VARIATION;
    }

    private float impactVolumeScale() {
        return 1.0F + (getRandom().nextFloat() * 2.0F - 1.0F) * IMPACT_VOLUME_VARIATION;
    }

    private void playDoubleStompImpactSound(double rightOffset) {
        float shift = impactPitchShift();
        float volume = impactVolumeScale();
        Vec3 impact = getImpactCenter(0.4, rightOffset);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.3F * volume, 0.55F + shift);
        playSlamLayers(impact, 1.0F, shift);
        destroyNearbyBlocks(impact, DOUBLE_STOMP_BLOCKS_DESTROYED);
    }

    private void playDoubleOverheadSlamImpactSound() {
        float shift = impactPitchShift();
        float volume = impactVolumeScale();
        Vec3 impact = getImpactCenter(1.6, 0.0);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6F * volume, 0.45F + shift);
        playSlamLayers(impact, 1.5F, shift);
        scheduleSlamRumble(impact, 1.4F);
        destroyNearbyBlocks(impact, DOUBLE_OVERHEAD_SLAM_BLOCKS_DESTROYED);
    }

    /** A bare-handed slam: a deep thud, the belly-slam body hit and the ground giving way. */
    private void playRegularSlamImpact(Vec3 impact) {
        float shift = impactPitchShift();
        float volume = impactVolumeScale();
        level().playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.2F * volume, 0.6F + shift);
        playSlamLayers(impact, 1.1F, shift);
        destroyNearbyBlocks(impact, REGULAR_ATTACK_BLOCKS_DESTROYED);
    }

    private void playSingleLegStompImpactSound() {
        float shift = impactPitchShift();
        float volume = impactVolumeScale();
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.1F * volume, 0.6F + shift);
        playSlamLayers(position(), 1.0F, shift);
    }

    /** A low boom that trails the hit by a few ticks, like the slam rolling away across the ground. */
    private void scheduleSlamRumble(Vec3 at, float power) {
        delayedSounds.add(new DelayedSound(7 + getRandom().nextInt(3), () ->
                level().playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.HOSTILE, 0.8F * power, 0.35F)));
    }

    private static final class DelayedSound {
        private int ticks;
        private final Runnable play;

        private DelayedSound(int ticks, Runnable play) {
            this.ticks = ticks;
            this.play = play;
        }
    }

    private void tickScheduledSounds() {
        for (Iterator<DelayedSound> it = delayedSounds.iterator(); it.hasNext(); ) {
            DelayedSound sound = it.next();
            if (--sound.ticks <= 0) {
                sound.play.run();
                it.remove();
            }
        }
    }

    // The combo variety attacks below reuse OgreBrute's sound palette directly. Both launch-style
    // upswings (one arm and double arm) share this: a wet smack for the grab plus his recorded whoosh.
    private void playBellyBumpImpactSound() {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2F * impactVolumeScale(), 0.6F + impactPitchShift());
    }

    private void playLaunchImpactSound() {
        float shift = impactPitchShift();
        float volume = impactVolumeScale();
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 1.2F * volume, 0.65F + shift);
        level().playSound(null, getX(), getY(), getZ(), ModSoundEvents.OGRE_BRUTE_UPSWING_WHOOSH.get(), SoundSource.HOSTILE, 1.15F * volume, 0.88F + shift);
    }

    /** The club leaving his hands: a low whoosh with a sweep behind it, no metal until it lands. */
    private void playClubHeadThrustWhoosh() {
        float shift = impactPitchShift();
        float volume = impactVolumeScale();
        level().playSound(null, getX(), getY(), getZ(), ModSoundEvents.OGRE_BRUTE_UPSWING_WHOOSH.get(),
                SoundSource.HOSTILE, 1.2F * volume, 0.7F + shift);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.0F * volume, 0.55F + shift);
    }

    /** The club head hitting someone, played where they stand. */
    private void playClubHeadThrustHit(LivingEntity target) {
        float shift = impactPitchShift();
        float volume = impactVolumeScale();
        level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 1.0F * volume, 0.8F + shift);
        level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.IRON_GOLEM_DAMAGE,
                SoundSource.HOSTILE, 1.0F * volume, 0.75F + shift);
        level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.HOSTILE, 1.2F * volume, 0.6F + shift);
    }

    private Vec3 getImpactCenter(double forwardOffset, double rightOffset) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        return position().add(forward.scale(forwardOffset)).add(right.scale(rightOffset));
    }

    /**
     * His slams break up to {@code count} blocks around the impact, any ordinary block in the open.
     * Inside a structure, his own fort included, only soft blocks can go, rarely and only a couple
     * a minute (see OgreGroundBreaker), so the fort takes a little wear and no more.
     */
    private void destroyNearbyBlocks(Vec3 center, int count) {
        if (level() instanceof ServerLevel serverLevel) {
            OgreGroundBreaker.breakNearby(serverLevel, this, center, count, OgreGroundBreaker.Strength.HEAVY);
        }
    }

    private void maybeStartRoar() {
        // Uses activeLordAttackId (cleared server-side by onMeleeAttackPlanFinished()).
        // attackAnimationTicks is never decremented on the server and would block every roar
        // after his first swing.
        if (isSummoning()) {
            if (roarCooldownTicks <= 0) {
                roarCooldownTicks = getSpecialConflictRetryTicks();
            }
            return;
        }
        if (isPhaseTransitioning() || isDyingAnimation()
                || isRoaring() || roarCooldownTicks > 0 || activeLordAttackId != 0 || hasActiveMeleeAttack()) {
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        startRoar();
    }

    private void startRoar() {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            roarLockedYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        } else {
            roarLockedYaw = getYRot();
        }
        roarLockedX = getX();
        roarLockedZ = getZ();
        roarActiveTicks = ROAR_ANIMATION_TICKS;
        roarVariant = chooseRoarVariant();
        lastRoarVariant = roarVariant;
        roarCooldownTicks = isPhaseTwo() ? PHASE_TWO_ROAR_COOLDOWN_TICKS : ROAR_COOLDOWN_TICKS;
        roarEffectsApplied = false;
        roarSoundPlayed = false;
        getNavigation().stop();
        // He turns to the locked facing in tickRoar over the first few ticks rather than snapping here.
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        playAttackAnimation(4);
    }

    private void tickRoar() {
        if (roarActiveTicks <= 0) {
            return;
        }

        getNavigation().stop();
        setXxa(0.0F);
        setZza(0.0F);

        int elapsedTicks = ROAR_ANIMATION_TICKS - roarActiveTicks;
        tickAnchoredNudgeMovement(roarLockedX, roarLockedZ);
        float roarYaw = roarLockedYaw + getRoarYawOffset(elapsedTicks);
        if (elapsedTicks < ROAR_FACING_EASE_TICKS) {
            // Closes most of the turn from wherever he was facing, then the authored sway takes over.
            setYRot(easeFacing(getYRot(), roarYaw, 0.5F));
            setYBodyRot(easeFacing(yBodyRot, roarYaw, 0.5F));
            setYHeadRot(easeFacing(getYHeadRot(), roarYaw, 0.5F));
        } else {
            setYRot(roarYaw);
            setYBodyRot(roarYaw);
            setYHeadRot(roarYaw);
        }

        if (elapsedTicks >= ROAR_SOUND_TICK && !roarSoundPlayed) {
            playVocal(ModSoundEvents.OGRE_LORD_ROAR.get(), 2.2F, 1.0F);
            roarSoundPlayed = true;
            startRoarScreenShake();
        }

        if (elapsedTicks >= ROAR_EFFECT_TICK && !roarEffectsApplied) {
            applyRoarEffects();
            roarEffectsApplied = true;
        }

        tickRoarSlowness(elapsedTicks);

        tickRoarShockwave(elapsedTicks);

        roarActiveTicks--;
        if (roarActiveTicks <= 0) {
            activeLordAttackId = 0;
            attackAnimationTicks = 0;
            clearLordAttackSync();
        }
    }

    private void startRoarScreenShake() {
        roarScreenShakeDelayTicks = ROAR_SCREEN_SHAKE_DELAY_TICKS;
    }

    private void tickRoarScreenShake() {
        if (roarScreenShakeDelayTicks > 0) {
            roarScreenShakeDelayTicks--;
            if (roarScreenShakeDelayTicks == 0) {
                sendContinuousRoarScreenShake();
            }
        }
    }

    private void sendContinuousRoarScreenShake() {
        if (level() instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersNear(
                    serverLevel, null, getX(), getY(), getZ(), ROAR_SCREEN_SHAKE_RANGE,
                    new ScreenShakePayload(getX(), getY(), getZ(), ROAR_SCREEN_SHAKE_INTENSITY,
                            ROAR_SCREEN_SHAKE_ACTIVE_TICKS, ROAR_SCREEN_SHAKE_FREQUENCY_MULTIPLIER));
        }
    }

    private void tickAnchoredNudgeMovement(double anchorX, double anchorZ) {
        double offsetX = getX() - anchorX;
        double offsetZ = getZ() - anchorZ;
        double distanceSqr = offsetX * offsetX + offsetZ * offsetZ;
        if (distanceSqr > ROAR_MAX_NUDGE_DISTANCE * ROAR_MAX_NUDGE_DISTANCE) {
            double scale = ROAR_MAX_NUDGE_DISTANCE / Math.sqrt(distanceSqr);
            setPos(anchorX + offsetX * scale, getY(), anchorZ + offsetZ * scale);
        }

        Vec3 movement = getDeltaMovement();
        double horizontalSpeed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        double speedScale = horizontalSpeed > ROAR_MAX_HORIZONTAL_NUDGE_SPEED
                ? ROAR_MAX_HORIZONTAL_NUDGE_SPEED / horizontalSpeed
                : 1.0;
        setDeltaMovement(
                movement.x * speedScale * ROAR_NUDGE_DAMPING,
                movement.y,
                movement.z * speedScale * ROAR_NUDGE_DAMPING);
    }

    private float getRoarYawOffset(int elapsedTicks) {
        if (elapsedTicks < ROAR_TURN_START_TICK) {
            return 0.0F;
        }
        if (elapsedTicks < ROAR_LEFT_TICK) {
            return smoothRoarYaw(elapsedTicks, ROAR_TURN_START_TICK, ROAR_LEFT_TICK,
                    0.0F, ROAR_LEFT_YAW_OFFSET);
        }
        if (elapsedTicks < ROAR_RIGHT_TICK) {
            return smoothRoarYaw(elapsedTicks, ROAR_LEFT_TICK, ROAR_RIGHT_TICK,
                    ROAR_LEFT_YAW_OFFSET, ROAR_RIGHT_YAW_OFFSET);
        }
        if (elapsedTicks < ROAR_RETURN_START_TICK) {
            return ROAR_RIGHT_YAW_OFFSET;
        }
        if (elapsedTicks < ROAR_FORWARD_TICK) {
            return smoothRoarYaw(elapsedTicks, ROAR_RETURN_START_TICK, ROAR_FORWARD_TICK,
                    ROAR_RIGHT_YAW_OFFSET, 0.0F);
        }
        return 0.0F;
    }

    private static float smoothRoarYaw(int tick, int startTick, int endTick, float startYaw, float endYaw) {
        float progress = Mth.clamp((float) (tick - startTick) / (endTick - startTick), 0.0F, 1.0F);
        float eased = progress * progress * (3.0F - 2.0F * progress);
        return Mth.lerp(eased, startYaw, endYaw);
    }

    private void tickRoarShockwave(int elapsedTicks) {
        if (!(level() instanceof ServerLevel serverLevel)
                || elapsedTicks < ROAR_SHOCKWAVE_START_TICK
                || elapsedTicks > ROAR_SHOCKWAVE_START_TICK + ROAR_SHOCKWAVE_EXPANSION_TICKS
                || (elapsedTicks - ROAR_SHOCKWAVE_START_TICK) % 2 != 0) {
            return;
        }

        float progress = Mth.clamp(
                (float) (elapsedTicks - ROAR_SHOCKWAVE_START_TICK) / ROAR_SHOCKWAVE_EXPANSION_TICKS,
                0.0F, 1.0F);
        float radius = Mth.lerp(progress, ROAR_SHOCKWAVE_START_RADIUS, ROAR_SHOCKWAVE_END_RADIUS);
        int ringPoints = Math.max(14, (int) Math.ceil(radius * 3.0F));
        double y = getY() + 0.15;

        for (int point = 0; point < ringPoints; point++) {
            double angle = Math.TAU * point / ringPoints;
            double x = getX() + Math.cos(angle) * radius;
            double z = getZ() + Math.sin(angle) * radius;
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    x, y, z,
                    1, 0.035, 0.025, 0.035, 0.005);
        }
    }

    private void applyRoarEffects() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB area = getBoundingBox().inflate(ROAR_EFFECT_RADIUS);
        List<LivingEntity> nearby = level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != this && entity.isAlive() && distanceToSqr(entity) <= ROAR_EFFECT_RADIUS * ROAR_EFFECT_RADIUS);

        for (LivingEntity entity : nearby) {
            if (entity instanceof OgreGrunt grunt && !(entity instanceof PlagueOgre) && !grunt.isArmyAlly()) {
                switch (roarVariant) {
                    case BATTLE -> {
                        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ROAR_BUFF_DURATION_TICKS, 1));
                        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ROAR_BUFF_DURATION_TICKS, 0));
                    }
                    // Rallying Roar: a longer, fuller buff for the ogres around him.
                    case RALLY -> {
                        int duration = ROAR_BUFF_DURATION_TICKS * 2;
                        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1));
                        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1));
                        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0));
                    }
                    case SHOCK -> entity.addEffect(new MobEffectInstance(
                            MobEffects.DAMAGE_BOOST, ROAR_BUFF_DURATION_TICKS / 2, 0));
                }
            } else if (isRoarOpponent(entity)) {
                boolean earmuffs = SonicDamage.isWearingPaddedEarmuffs(entity);
                switch (roarVariant) {
                    case BATTLE -> entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                            earmuffs ? ROAR_WEAKNESS_DURATION_TICKS / 2 : ROAR_WEAKNESS_DURATION_TICKS, 0));
                    // Rallying Roar leaves the players' arms alone; the slow still applies.
                    case RALLY -> { }
                    // Shock Roar: blasts everyone nearby back, with a short bout of weakness.
                    case SHOCK -> {
                        double dx = entity.getX() - getX();
                        double dz = entity.getZ() - getZ();
                        double distance = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
                        double push = earmuffs ? ROAR_SHOCK_KNOCKBACK * 0.5 : ROAR_SHOCK_KNOCKBACK;
                        Vec3 motion = entity.getDeltaMovement();
                        entity.setDeltaMovement(motion.x + dx / distance * push, Math.max(motion.y, 0.35),
                                motion.z + dz / distance * push);
                        entity.hurtMarked = true;
                        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                                earmuffs ? ROAR_WEAKNESS_DURATION_TICKS / 4 : ROAR_WEAKNESS_DURATION_TICKS / 2, 0));
                    }
                }
            }
        }

    }

    /**
     * Each Roar is one of three: the standard Battle Roar, a Rallying Roar (only when ogres are
     * around to rally) or a Shock Roar (only when someone is close enough to be blasted). Never the
     * same one twice running.
     */
    private RoarVariant chooseRoarVariant() {
        int ogresNearby = level().getEntitiesOfClass(OgreGrunt.class, getBoundingBox().inflate(ROAR_EFFECT_RADIUS),
                ogre -> ogre != this && ogre.isAlive()).size();
        boolean someoneClose = !level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(ROAR_SHOCK_TRIGGER_RANGE),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative()).isEmpty();
        int[] weights = {3, ogresNearby >= 2 ? 2 : 0, someoneClose ? 4 : 0};
        RoarVariant[] all = RoarVariant.values();
        int total = 0;
        for (int i = 0; i < all.length; i++) {
            if (all[i] != lastRoarVariant) total += weights[i];
        }
        if (total == 0) {
            return RoarVariant.BATTLE;
        }
        int roll = getRandom().nextInt(total);
        for (int i = 0; i < all.length; i++) {
            if (all[i] == lastRoarVariant) continue;
            roll -= weights[i];
            if (roll < 0) return all[i];
        }
        return RoarVariant.BATTLE;
    }

    /**
     * Who the roar hurts: players, plus anything on a player's side or fighting him: tamed boars and other
     * pets, the player's summoned army ogres, and any mob that has him as its target. His own Tribe
     * (including its jockey boars) and Plague Ogres are not affected.
     */
    private boolean isRoarOpponent(LivingEntity entity) {
        if (entity == this || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player) {
            return true;
        }
        if (entity instanceof PlagueOgre) {
            return false;
        }
        if (entity instanceof OgreGrunt grunt) {
            return grunt.isArmyAlly();
        }
        if (entity instanceof MudsnoutBoar boar && boar.isTamed()) {
            return true;
        }
        if (entity instanceof TamableAnimal pet && pet.isTame()) {
            return true;
        }
        return entity instanceof Mob mob && mob.getTarget() == this;
    }

    private void tickRoarSlowness(int elapsedTicks) {
        if (roarVariant == RoarVariant.SHOCK) {
            return;
        }
        if (elapsedTicks < ROAR_SOUND_TICK
                || elapsedTicks % ROAR_SLOWNESS_REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        AABB area = getBoundingBox().inflate(ROAR_EFFECT_RADIUS);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, area,
                candidate -> isRoarOpponent(candidate)
                        && distanceToSqr(candidate) <= ROAR_EFFECT_RADIUS * ROAR_EFFECT_RADIUS)) {
            int amplifier = SonicDamage.isWearingPaddedEarmuffs(victim) ? 0 : 1;
            victim.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    ROAR_SLOWNESS_REFRESH_DURATION_TICKS,
                    amplifier));
        }
    }

    private void performRoyalSummon(ServerLevel serverLevel) {
        LivingEntity target = summonTarget != null && summonTarget.isAlive() ? summonTarget : getTarget();
        commandNearbyOgres(target);

        SummonFormation formation = chooseSummonFormation();
        List<EntityType<? extends OgreGrunt>> ogres = new ArrayList<>();
        switch (formation) {
            case GUARDS -> {
                ogres.add(ModEntities.OGRE_GUARD.get());
                ogres.add(ModEntities.OGRE_GUARD.get());
            }
            case CAPTAIN -> {
                ogres.add(ModEntities.OGRE_GRUNT.get());
                ogres.add(ModEntities.OGRE_GRUNT.get());
                ogres.add(ModEntities.OGRE_GRUNT_CAPTAIN.get());
            }
            case MAGE -> {
                ogres.add(ModEntities.OGRE_MAGE.get());
                ogres.add(ModEntities.OGRE_ARCHER.get());
                ogres.add(ModEntities.OGRE_ARCHER.get());
            }
            case VOLLEY -> {
                for (int i = 0; i < 4; i++) ogres.add(ModEntities.OGRE_ARCHER.get());
            }
            case BRUTE -> {
                ogres.add(ModEntities.OGRE_BRUTE.get());
                ogres.add(ModEntities.OGRE_GRUNT.get());
            }
        }
        // Later call-ups arrive heavier: an extra Grunt from the third, and a Guard on top from the fourth.
        if (summonsPerformed >= 2) {
            ogres.add(ModEntities.OGRE_GRUNT.get());
        }
        if (summonsPerformed >= 3) {
            ogres.add(ModEntities.OGRE_GUARD.get());
        }
        // Never exceed the cap: trim the group to the room left (the extras go first).
        int room = MAX_LIVING_SUMMONS - livingSummonCount(serverLevel);
        if (room <= 0) {
            return;
        }
        while (ogres.size() > room) {
            ogres.remove(ogres.size() - 1);
        }
        lastSummonFormation = formation;
        summonsPerformed++;
        spawnFormation(serverLevel, target, ogres);
    }

    private int livingSummonCount(ServerLevel serverLevel) {
        summonedOgres.removeIf(id -> !(serverLevel.getEntity(id) instanceof OgreGrunt ogre) || !ogre.isAlive());
        return summonedOgres.size();
    }

    /** Weighted pick that never repeats the last group; the Brute only answers later call-ups. */
    private SummonFormation chooseSummonFormation() {
        int[] weights = {3, 3, 2, 2, summonsPerformed >= 2 ? 2 : summonsPerformed >= 1 ? 1 : 0};
        SummonFormation[] all = SummonFormation.values();
        int total = 0;
        for (int i = 0; i < all.length; i++) {
            if (all[i] != lastSummonFormation) total += weights[i];
        }
        int roll = getRandom().nextInt(Math.max(1, total));
        for (int i = 0; i < all.length; i++) {
            if (all[i] == lastSummonFormation) continue;
            roll -= weights[i];
            if (roll < 0) return all[i];
        }
        return SummonFormation.GUARDS;
    }

    private void spawnFormation(ServerLevel serverLevel, LivingEntity target,
                                List<EntityType<? extends OgreGrunt>> formation) {
        for (int i = 0; i < formation.size(); i++) {
            double angle = getSummonSpawnAngle(i, formation.size());
            BlockPos spawnPos = findSummonSpawnPos(serverLevel, angle, formation.get(i));
            if (spawnPos == null) {
                continue;
            }
            OgreGrunt ogre = formation.get(i).spawn(serverLevel, spawnPos, MobSpawnType.MOB_SUMMONED);
            if (ogre == null) {
                continue;
            }

            summonedOgres.add(ogre.getUUID());
            if (ogre instanceof OgreGuard guard) {
                guard.rollEliteSpearEnchantment();
            }
            if (target != null && target.isAlive()) {
                ogre.setRoyalCommandTarget(target);
            }
        }
    }

    private void commandNearbyOgres(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        AABB commandArea = getBoundingBox().inflate(ROYAL_COMMAND_RADIUS);
        for (OgreGrunt ogre : level().getEntitiesOfClass(OgreGrunt.class, commandArea,
                ogre -> ogre != this && ogre.isAlive())) {
            ogre.setRoyalCommandTarget(target);
        }
    }

    @Nullable
    private BlockPos findSummonSpawnPos(ServerLevel serverLevel, double baseAngle,
                                        EntityType<? extends OgreGrunt> entityType) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int direction = attempt % 2 == 0 ? 1 : -1;
            double angleOffset = Math.toRadians(((attempt + 1) / 2) * 18.0 * direction);
            double radius = SUMMON_SPAWN_RADIUS + attempt % 3;
            double angle = baseAngle + angleOffset;
            int x = Mth.floor(getX() + Math.cos(angle) * radius);
            int z = Mth.floor(getZ() + Math.sin(angle) * radius);
            int y = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (serverLevel.getBlockState(pos.below()).getCollisionShape(serverLevel, pos.below()).isEmpty()
                    || !serverLevel.getFluidState(pos).isEmpty()
                    || !serverLevel.noCollision(entityType.getDimensions().makeBoundingBox(
                            x + 0.5, y, z + 0.5))) {
                continue;
            }
            return pos;
        }
        return null;
    }

    private double getSummonSpawnAngle(int index, int formationSize) {
        return Math.toRadians(summonLockedYaw) + Math.PI / 2.0
                + Math.TAU * index / formationSize;
    }

    private enum SummonFormation { GUARDS, CAPTAIN, MAGE, VOLLEY, BRUTE }

    private enum RoarVariant { BATTLE, RALLY, SHOCK }

    // Ogres he has called in that are still alive; the call-up waits or shrinks to stay under the cap.
    private final Set<UUID> summonedOgres = new HashSet<>();
    private SummonFormation lastSummonFormation;
    private int summonsPerformed;
    private RoarVariant roarVariant = RoarVariant.BATTLE;
    private RoarVariant lastRoarVariant;

    private void maybeStartSummonPoint() {
        if (isRoaring()) {
            if (summonCooldownTicks <= 0) {
                summonCooldownTicks = getSpecialConflictRetryTicks();
            }
            return;
        }
        if (isPhaseTransitioning() || isDyingAnimation() || isSummoning()
                || summonCooldownTicks > 0 || activeLordAttackId != 0 || hasActiveMeleeAttack()) {
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        // Too many of his summons still standing: check again shortly instead of crowding the fight.
        if (level() instanceof ServerLevel serverLevel
                && livingSummonCount(serverLevel) > MAX_LIVING_SUMMONS - MIN_SUMMON_ROOM) {
            summonCooldownTicks = SUMMON_FULL_RETRY_TICKS;
            return;
        }

        summonTarget = target;
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        summonLockedYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        summonLockedX = getX();
        summonLockedZ = getZ();
        // Drop any completed attack's lingering recovery plan before reserving the shared
        // controller. The summon itself remains immune to melee selection until it finishes.
        cancelActiveMeleeAttack();
        summonActiveTicks = SUMMON_POINT_ANIMATION_TICKS;
        summonCooldownTicks = SUMMON_COOLDOWN_TICKS;
        summonEventFired = false;
        summonVocalPlayed = false;
        summonPunchSoundPlayed = false;
        getNavigation().stop();
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        playAttackAnimation(13);
    }

    private void tickSummonPoint() {
        if (summonActiveTicks <= 0) {
            return;
        }

        getNavigation().stop();
        setXxa(0.0F);
        setZza(0.0F);
        tickAnchoredNudgeMovement(summonLockedX, summonLockedZ);
        setYRot(summonLockedYaw);
        setYBodyRot(summonLockedYaw);
        setYHeadRot(summonLockedYaw);
        yRotO = summonLockedYaw;
        yBodyRotO = summonLockedYaw;
        yHeadRotO = summonLockedYaw;

        int elapsedTicks = SUMMON_POINT_ANIMATION_TICKS - summonActiveTicks;
        if (!summonPunchSoundPlayed && elapsedTicks >= SUMMON_POINT_PUNCH_SOUND_TICK) {
            summonPunchSoundPlayed = true;
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.25F, 0.8F);
        }
        if (!summonVocalPlayed && elapsedTicks >= SUMMON_POINT_VOCAL_TICK) {
            summonVocalPlayed = true;
            playVocal(ModSoundEvents.OGRE_LORD_SUMMON_GRUNT.get(), 1.3F, 1.0F);
        }
        if (!summonEventFired && elapsedTicks >= SUMMON_POINT_EVENT_TICK) {
            summonEventFired = true;
            if (level() instanceof ServerLevel serverLevel) {
                performRoyalSummon(serverLevel);
            }
        }

        summonActiveTicks--;
        if (summonActiveTicks <= 0) {
            activeLordAttackId = 0;
            attackAnimationTicks = 0;
            summonTarget = null;
            clearLordAttackSync();
        }
    }

    private boolean isSummoning() {
        return summonActiveTicks > 0 || activeLordAttackId == 13 && attackAnimationTicks > 0;
    }

    @Override
    protected boolean isAttackCommitted() {
        // A swing plays out to the end even if the goal would drop it (a lost target, say), so the
        // summon and the roar can't cut in on the second stomp.
        return activeLordAttackId != 0 && !isPhaseTransitioning() && !isDyingAnimation();
    }

    @Override
    protected boolean isPerformingNonMeleeAction() {
        return isSummoning() || isRoaring() || isPhaseTransitioning() || isDyingAnimation();
    }

    private int getSpecialConflictRetryTicks() {
        return SPECIAL_CONFLICT_RETRY_MIN_TICKS
                + getRandom().nextInt(SPECIAL_CONFLICT_RETRY_RANDOM_TICKS);
    }

    public boolean isRoaring() {
        return roarActiveTicks > 0 || activeLordAttackId == 4 && attackAnimationTicks > 0;
    }

    @Override
    protected MeleeAttackPlan startMeleeAttack() {
        if (isPhaseTransitioning()) {
            return new MeleeAttackPlan(10,
                    Math.max(1, PULL_OUT_ANIMATION_TICKS - phaseTransitionElapsedTicks),
                    List.of());
        }
        if (isSummoning() || isRoaring() || isDyingAnimation()) {
            return new MeleeAttackPlan(10, List.of());
        }

        if (isPhaseTwo()) {
            return startPhaseTwoMeleeAttack();
        }

        LivingEntity target = getTarget();
        if (shouldUseButtSmash(target)) {
            return startButtSmash();
        }
        if (canReachForBigAttack(target) && doubleOverheadSlamCooldownTicks <= 0 && getRandom().nextInt(5) == 0) {
            return startDoubleOverheadSlam();
        }

        if (canReachForBigAttack(target) && doubleStompCooldownTicks <= 0 && getRandom().nextInt(5) == 0) {
            return startDoubleStomp();
        }

        if (closePressureTicks >= BELLY_BUMP_CLOSE_PRESSURE_TICKS && bellyBumpCooldownTicks <= 0) {
            return startBellyBump();
        }

        if (doubleArmUpswingCooldownTicks <= 0 && getRandom().nextInt(4) == 0) {
            return startDoubleArmUpswing();
        }

        // The stomp wave is the midrange harass; it is listed twice so it comes up as often as the
        // two regular slams do once he is far enough out to use it.
        int[] pool = isFarEnoughForStompWave(target)
                ? new int[]{ATTACK_ID_REGULAR_ATTACK, ATTACK_ID_REGULAR_ATTACK_2, 1, 8, 8}
                : new int[]{ATTACK_ID_REGULAR_ATTACK, ATTACK_ID_REGULAR_ATTACK_2, 1};
        return startNormalAttack(chooseVariedNormalAttack(pool));
    }

    private MeleeAttackPlan startNormalAttack(int attackId) {
        return switch (attackId) {
            case 1 -> startOneArmUpswing();
            case 7 -> startClubHeadThrust();
            case 8 -> startSingleLegStomp();
            case ATTACK_ID_REGULAR_ATTACK, ATTACK_ID_REGULAR_ATTACK_2 -> startRegularSlam(attackId);
            default -> startSingleOverheadSlam();
        };
    }

    private MeleeAttackPlan startPhaseTwoMeleeAttack() {
        LivingEntity target = getTarget();
        if (shouldUseButtSmash(target)) {
            return startButtSmash();
        }
        // Slightly more club-favoring than before (was 1-in-8), per tuning feedback — still leaves
        // plenty of room for Double Stomp/Belly Bump/the varied pool below, not a takeover.
        if (canReachForBigAttack(target)
                && clubDoubleOverheadCooldownTicks <= 0
                && getRandom().nextInt(6) == 0) {
            return startClubDoubleOverhead();
        }
        if (canReachForBigAttack(target)
                && doubleStompCooldownTicks <= 0
                && getRandom().nextInt(4) == 0) {
            return startDoubleStomp();
        }
        if (closePressureTicks >= BELLY_BUMP_CLOSE_PRESSURE_TICKS
                && bellyBumpCooldownTicks <= 0) {
            if (clubHeadThrustCooldownTicks > 0 || getRandom().nextBoolean()) {
                return startBellyBump();
            }
            // Share Belly Bump's close-pressure window and cooldown so these
            // two punish attacks occur at the same rate instead of the bump
            // repeatedly preempting the normal phase-two pool.
            bellyBumpCooldownTicks = BELLY_BUMP_COOLDOWN_TICKS;
            closePressureTicks = 0;
            return startClubHeadThrust();
        }
        if (shouldUsePhaseTwoSingleLegStomp()) {
            return startSingleLegStomp();
        }
        // The club phase keeps only the second regular slam. Single Overhead Slam (11) is listed
        // twice so it is the most common pick while off cooldown; Club Head Thrust (7) joins when
        // it is ready, besides its appearances via the Belly Bump alternative above.
        boolean overheadReady = singleOverheadCooldownTicks <= 0;
        if (clubHeadThrustCooldownTicks <= 0) {
            return startNormalAttack(overheadReady
                    ? chooseVariedNormalAttack(ATTACK_ID_REGULAR_ATTACK_2, 7, 11, 11)
                    : chooseVariedNormalAttack(ATTACK_ID_REGULAR_ATTACK_2, 7));
        }
        return startNormalAttack(overheadReady
                ? chooseVariedNormalAttack(ATTACK_ID_REGULAR_ATTACK_2, 11, 11)
                : ATTACK_ID_REGULAR_ATTACK_2);
    }

    private boolean shouldUsePhaseTwoSingleLegStomp() {
        return isFarEnoughForStompWave(getTarget())
                && getRandom().nextInt(PHASE_TWO_SINGLE_LEG_STOMP_CHANCE) == 0;
    }

    /** The wave is a midrange attack; up close he swings or bumps instead. */
    private boolean isFarEnoughForStompWave(LivingEntity target) {
        return target != null && distanceToSqr(target) >= STOMP_WAVE_MIN_RANGE * STOMP_WAVE_MIN_RANGE;
    }

    private int chooseVariedNormalAttack(int... attackIds) {
        List<Integer> choices = new ArrayList<>(attackIds.length);
        for (int attackId : attackIds) {
            choices.add(attackId);
        }

        // Usually suppress the immediately previous move, but leave a small repeat chance so
        // the King does not feel like he is stepping through a scripted attack playlist.
        if (choices.size() > 1 && getRandom().nextInt(5) != 0) {
            choices.remove(Integer.valueOf(lastSelectedAttackId));
        }
        // Usually avoid both recent attacks, but occasionally allow the second-last move so the
        // sequence stays organic rather than becoming a perfectly predictable fixed rotation.
        if (choices.size() > 1 && getRandom().nextInt(4) != 0) {
            choices.remove(Integer.valueOf(secondLastSelectedAttackId));
        }

        return choices.get(getRandom().nextInt(choices.size()));
    }

    private boolean shouldUseButtSmash(LivingEntity target) {
        return buttSmashCooldownTicks <= 0
                && canReachForBigAttack(target)
                && onGround()
                && !isInWater()
                && hasButtSmashHeadroom()
                && getRandom().nextInt(BUTT_SMASH_CHANCE) == 0;
    }

    private boolean hasButtSmashHeadroom() {
        AABB body = getBoundingBox();
        AABB above = new AABB(body.minX, body.maxY, body.minZ,
                body.maxX, body.maxY + BUTT_SMASH_MIN_HEADROOM, body.maxZ).deflate(0.1);
        return !level().getBlockCollisions(this, above).iterator().hasNext();
    }

    private MeleeAttackPlan startButtSmash() {
        playAttackAnimation(ATTACK_ID_BUTT_SMASH);
        // The silent wind-up replaces any vocal still queued from the last swing.
        heavyAttackRoarDelayTicks = -1;
        lockSpecialAnimationPose();
        buttSmashElapsedTicks = 0;
        buttSmashAirborne = false;
        buttSmashCooldownTicks = isPhaseTwo() ? PHASE_TWO_BUTT_SMASH_COOLDOWN_TICKS : BUTT_SMASH_COOLDOWN_TICKS;
        warmUpRippleBlockVisual();
        return new MeleeAttackPlan(swingCooldown(BUTT_SMASH_SWING_COOLDOWN_TICKS), BUTT_SMASH_ANIMATION_TICKS,
                List.of());
    }

    private void tickButtSmash() {
        if (activeLordAttackId != ATTACK_ID_BUTT_SMASH) {
            return;
        }
        // Planted for the whole clip.
        freezeSpecialAnimationPose();
        buttSmashElapsedTicks++;

        if (buttSmashElapsedTicks == BUTT_SMASH_TAKEOFF_TICK) {
            buttSmashAirborne = true;
            playButtSmashTakeoff();
        }
        if (buttSmashAirborne && buttSmashElapsedTicks >= BUTT_SMASH_IMPACT_TICK) {
            buttSmashAirborne = false;
            performButtSmashImpact();
        }
        if (buttSmashElapsedTicks == BUTT_SMASH_GRUNT_TICK) {
            playPreAttackGrunt();
        }
        if (buttSmashElapsedTicks == BUTT_SMASH_RECOVER_HUFF_TICK) {
            playLordHuff(1.4F);
        }
    }
    private void playButtSmashTakeoff() {
        float shift = impactPitchShift();
        level().playSound(null, getX(), getY(), getZ(), ModSoundEvents.OGRE_BRUTE_UPSWING_WHOOSH.get(),
                SoundSource.HOSTILE, 1.5F * impactVolumeScale(), 0.6F + shift);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.HOSTILE_BIG_FALL,
                SoundSource.HOSTILE, 1.4F, 0.5F + shift);
        playSlamLayers(position(), 0.8F, shift);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(),
                    30, 1.4, 0.15, 1.4, 0.05);
        }
    }

    /**
     * He comes down on his backside: everyone in the inner circle takes the crushing hit and is thrown
     * clear, and a ripple breaks outward that everyone else can jump.
     */
    private void performButtSmashImpact() {
        Vec3 impact = position();

        directShockwaveWeaponHitTargets.clear();
        for (LivingEntity target : getClubCandidates(BUTT_SMASH_INNER_RADIUS + 1.0, 4.0)) {
            AABB bounds = target.getBoundingBox();
            double distance = Math.hypot(target.getX() - impact.x, target.getZ() - impact.z);
            if (distance > BUTT_SMASH_INNER_RADIUS
                    || bounds.maxY < impact.y - 1.0 || bounds.minY > impact.y + 3.5) {
                continue;
            }
            // Remembered so the ripple can't hit them a second time.
            directShockwaveWeaponHitTargets.add(target.getUUID());
            float share = OgreDamage.falloff(distance, BUTT_SMASH_INNER_RADIUS, BUTT_SMASH_INNER_EDGE_SHARE)
                    * (isTribeAlly(target) ? 0.5F : 1.0F);
            // His whole weight knocks a raised shield down, so it can't block the inner circle.
            if (target instanceof Player player && player.isBlocking()) {
                player.disableShield();
            }
            if (dealClubDamage(target, BUTT_SMASH_CRUSH_DAMAGE_MULTIPLIER * share, 0.0F)) {
                applyHeavyClubHitEffects(target);
                throwClearOfButtSmash(target);
            }
        }

        playButtSmashImpactSound(impact);
        spawnClubImpactEffects(impact, BUTT_SMASH_RIPPLE_RADIUS, 2.6F);
        if (level() instanceof ServerLevel serverLevel) {
            spawnGraduatedDustFill(serverLevel, impact, BUTT_SMASH_RIPPLE_RADIUS);
            // A second ring of cloud marks the edge of the inner circle.
            StaticShockwaveParticles.spawn(serverLevel, impact, BUTT_SMASH_INNER_RADIUS);
        }
        startClubGroundRipple(impact, BUTT_SMASH_RIPPLE_RADIUS, BUTT_SMASH_RIPPLE_LAUNCH,
                GroundRippleKind.KING_BUTT_SMASH, directShockwaveWeaponHitTargets);
        destroyNearbyBlocks(impact, BUTT_SMASH_BLOCKS_DESTROYED);
    }

    /** Full strength at the edge of the inner circle, fading evenly to the weakest at the ripple's end. */
    private float buttSmashRippleDamageMultiplier(LivingEntity target, Vec3 origin) {
        double distance = Math.hypot(target.getX() - origin.x, target.getZ() - origin.z);
        float faded = (float) Mth.clamp((distance - BUTT_SMASH_INNER_RADIUS)
                / (BUTT_SMASH_RIPPLE_RADIUS - BUTT_SMASH_INNER_RADIUS), 0.0, 1.0);
        return Mth.lerp(faded, BUTT_SMASH_RIPPLE_DAMAGE_MULTIPLIER, RIPPLE_EDGE_DAMAGE_MULTIPLIER);
    }

    private void throwClearOfButtSmash(LivingEntity target) {
        Vec3 outward = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        outward = outward.lengthSqr() < 1.0E-4
                ? Vec3.directionFromRotation(0.0F, getYRot())
                : outward.normalize();
        target.setDeltaMovement(outward.x * BUTT_SMASH_CRUSH_THROW, BUTT_SMASH_CRUSH_LIFT,
                outward.z * BUTT_SMASH_CRUSH_THROW);
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private void playButtSmashImpactSound(Vec3 impact) {
        float shift = impactPitchShift();
        float volume = impactVolumeScale();
        level().playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.0F * volume, 0.4F + shift);
        // The body hitting the ground, under the stone-breaking slam layers.
        level().playSound(null, impact.x, impact.y, impact.z, SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.HOSTILE, 1.6F * volume, 0.5F + shift);
        level().playSound(null, impact.x, impact.y, impact.z, SoundEvents.HOSTILE_BIG_FALL,
                SoundSource.HOSTILE, 1.8F * volume, 0.5F + shift);
        playSlamLayers(impact, 2.0F, shift);
        scheduleSlamRumble(impact, 1.9F);
    }

    private MeleeAttackPlan startClubDoubleOverhead() {
        playAttackAnimation(10);
        scheduleHeavyAttackRoar(10, CLUB_DOUBLE_OVERHEAD_IMPACT_TICK, CLUB_DOUBLE_OVERHEAD_ROAR_PITCH, false,
                CLUB_DOUBLE_OVERHEAD_ROAR_LEAD_TICKS);
        lockSpecialAnimationPose();
        clubAttackElapsedTicks = 0;
        clubAttackImpactFired = false;
        clubDoubleOverheadCooldownTicks = CLUB_DOUBLE_OVERHEAD_COOLDOWN_TICKS;
        return new MeleeAttackPlan(
                swingCooldown(DOUBLE_OVERHEAD_SLAM_SWING_COOLDOWN_TICKS),
                CLUB_DOUBLE_OVERHEAD_ANIMATION_TICKS,
                List.of());
    }

    private MeleeAttackPlan startSingleOverheadSlam() {
        playAttackAnimation(11);
        scheduleHeavyAttackRoar(11, SINGLE_OVERHEAD_IMPACT_TICK, SINGLE_OVERHEAD_ROAR_PITCH, false, SINGLE_OVERHEAD_ROAR_LEAD_TICKS);
        clubAttackElapsedTicks = 0;
        clubAttackImpactFired = false;
        singleOverheadCooldownTicks = SINGLE_OVERHEAD_COOLDOWN_TICKS;
        return new MeleeAttackPlan(
                swingCooldown(WALKING_ATTACK_COOLDOWN_TICKS),
                SINGLE_OVERHEAD_SLAM_ANIMATION_TICKS,
                List.of());
    }

    /** The two bread-and-butter slams, thrown while walking; 18 is the right hand, 19 the left. */
    private MeleeAttackPlan startRegularSlam(int attackId) {
        playAttackAnimation(attackId);
        clubAttackElapsedTicks = 0;
        clubAttackImpactFired = false;
        return new MeleeAttackPlan(
                swingCooldown(WALKING_ATTACK_COOLDOWN_TICKS),
                REGULAR_ATTACK_ANIMATION_TICKS,
                List.of());
    }

    private boolean canReachForBigAttack(LivingEntity target) {
        return target != null && target.isAlive() && isWithinRegularAttackStartRange(target) && getSensing().hasLineOfSight(target);
    }

    private MeleeAttackPlan startOneArmUpswing() {
        playAttackAnimation(1);
        oneArmUpswingImpactSoundDelayTicks = ONE_ARM_UPSWING_IMPACT_DELAY_TICKS;
        boolean clubUpswing = isPhaseTwo();
        double horizontalLaunch = clubUpswing ? 0.9 : 1.8;
        double verticalLaunch = clubUpswing ? 2.2 : 1.8;
        float hitRadius = clubUpswing ? 6.5F : 5.0F;
        double forwardOffset = clubUpswing ? 1.5 : 1.0;
        double verticalHitRange = clubUpswing ? 4.5 : 3.5;
        float damageMultiplier = ONE_ARM_UPSWING_DAMAGE_MULTIPLIER;
        if (clubUpswing) {
            double baseDamage = Math.max(0.01, getAttributeValue(Attributes.ATTACK_DAMAGE));
            damageMultiplier += (float) (Config.OGRE_LORD_CLUB_UPSWING_BONUS_DAMAGE.get() / baseDamage);
        }
        // Same launch as the double arm version, redirected: he swings right hand across to the left,
        // so the launch goes ~45 degrees up and out to his left. In phase two the metal club tip adds
        // damage, reach and a stronger launch. The delayed impact sound uses playLaunchImpactSound().
        return new MeleeAttackPlan(swingCooldown(WALKING_ATTACK_COOLDOWN_TICKS), ONE_ARM_UPSWING_ANIMATION_TICKS, List.of(
                MeleeAttackHit.areaSpecial(ONE_ARM_UPSWING_IMPACT_DELAY_TICKS, damageMultiplier, hitRadius, false,
                                0.2F, horizontalLaunch, 120.0F, verticalLaunch, 0, 0, false)
                        .withAreaOffset(forwardOffset, 0.0)
                        .withDirectionalKnockback(0.3, -1.0)
                        .withVerticalHitRange(verticalHitRange)));
    }

    private MeleeAttackPlan startDoubleStomp() {
        playAttackAnimation(2);
        doubleStompCooldownTicks = DOUBLE_STOMP_COOLDOWN_TICKS;
        doubleStompLeftImpactSoundDelayTicks = DOUBLE_STOMP_LEFT_IMPACT_DELAY_TICKS;
        doubleStompRightImpactSoundDelayTicks = DOUBLE_STOMP_RIGHT_IMPACT_DELAY_TICKS;
        doubleStompLeftGruntDelayTicks = DOUBLE_STOMP_LEFT_GRUNT_DELAY_TICKS;
        doubleStompRightGruntDelayTicks = DOUBLE_STOMP_RIGHT_GRUNT_DELAY_TICKS;
        warmUpRippleBlockVisual();
        return new MeleeAttackPlan(swingCooldown(DOUBLE_STOMP_SWING_COOLDOWN_TICKS), DOUBLE_STOMP_ANIMATION_TICKS, List.of(
                MeleeAttackHit.areaSpecial(DOUBLE_STOMP_LEFT_IMPACT_DELAY_TICKS, DOUBLE_STOMP_DAMAGE_MULTIPLIER, DOUBLE_STOMP_AREA_RADIUS, true,
                                0.2F, 2.2, 0.0F, 0.85, 30, 0, true)
                        .withAreaOffset(0.4, -1.0)
                        .withVerticalHitRange(4.0)
                        .withScreenShake(1.3F)
                        .withBlockRippleParticles(),
                MeleeAttackHit.areaSpecial(DOUBLE_STOMP_RIGHT_IMPACT_DELAY_TICKS, DOUBLE_STOMP_DAMAGE_MULTIPLIER, DOUBLE_STOMP_AREA_RADIUS, true,
                                0.2F, 2.2, 0.0F, 0.85, 30, 0, true)
                        .withAreaOffset(0.4, 1.0)
                        .withVerticalHitRange(4.0)
                        .withScreenShake(1.3F)
                        .withBlockRippleParticles()));
    }

    private MeleeAttackPlan startDoubleOverheadSlam() {
        playAttackAnimation(3);
        scheduleHeavyAttackRoar(3, DOUBLE_OVERHEAD_SLAM_IMPACT_DELAY_TICKS,
                DOUBLE_OVERHEAD_ROAR_PITCH, false, DOUBLE_OVERHEAD_ROAR_LEAD_TICKS);
        doubleOverheadSlamCooldownTicks = DOUBLE_OVERHEAD_SLAM_COOLDOWN_TICKS;
        doubleOverheadSlamImpactSoundDelayTicks = DOUBLE_OVERHEAD_SLAM_IMPACT_DELAY_TICKS;
        warmUpRippleBlockVisual();
        return new MeleeAttackPlan(swingCooldown(DOUBLE_OVERHEAD_SLAM_SWING_COOLDOWN_TICKS), DOUBLE_OVERHEAD_SLAM_ANIMATION_TICKS, List.of(
                MeleeAttackHit.areaSpecial(DOUBLE_OVERHEAD_SLAM_IMPACT_DELAY_TICKS, DOUBLE_OVERHEAD_SLAM_DAMAGE_MULTIPLIER, DOUBLE_OVERHEAD_SLAM_AREA_RADIUS, true,
                                0.3F, 3.0, 0.0F, 1.3, 40, 20, true)
                        .withAreaOffset(1.6, 0.0)
                        .withVerticalHitRange(4.5)
                        .withScreenShake(2.0F)
                        .withBlockRippleParticles()));
    }

    private MeleeAttackPlan startBellyBump() {
        playAttackAnimation(5);
        bellyBumpCooldownTicks = BELLY_BUMP_COOLDOWN_TICKS;
        bellyBumpRoarDelayTicks = BELLY_BUMP_ROAR_DELAY_TICKS;
        bellyBumpImpactSoundDelayTicks = BELLY_BUMP_IMPACT_DELAY_TICKS;
        closePressureTicks = 0;
        // Omnidirectional (coneDegrees 0 skips the facing check) — this punishes anyone crowded
        // around him, not just whoever he's currently facing.
        return new MeleeAttackPlan(swingCooldown(BASIC_ATTACK_COOLDOWN_TICKS), BELLY_BUMP_ANIMATION_TICKS, List.of(
                MeleeAttackHit.areaSpecial(BELLY_BUMP_IMPACT_DELAY_TICKS, BELLY_BUMP_DAMAGE_MULTIPLIER, 6.0F, false,
                                0.2F, 4.5, 0.0F, 1.3, 0, 0, false)
                        .withVerticalHitRange(4.0)));
    }

    private MeleeAttackPlan startDoubleArmUpswing() {
        playAttackAnimation(6);
        doubleArmUpswingCooldownTicks = DOUBLE_ARM_UPSWING_COOLDOWN_TICKS;
        doubleArmUpswingGruntDelayTicks = DOUBLE_ARM_UPSWING_GRUNT_DELAY_TICKS;
        doubleArmUpswingImpactSoundDelayTicks = DOUBLE_ARM_UPSWING_IMPACT_DELAY_TICKS;
        return new MeleeAttackPlan(swingCooldown(WALKING_ATTACK_COOLDOWN_TICKS), DOUBLE_ARM_UPSWING_ANIMATION_TICKS, List.of(
                MeleeAttackHit.areaSpecial(DOUBLE_ARM_UPSWING_IMPACT_DELAY_TICKS, DOUBLE_ARM_UPSWING_DAMAGE_MULTIPLIER, 5.5F, false,
                                0.2F, 1.5, 120.0F, 2.2, 0, 0, false)
                        .withAreaOffset(1.3, 0.0)
                        .withVerticalHitRange(3.5)));
    }

    private MeleeAttackPlan startClubHeadThrust() {
        playAttackAnimation(7);
        clubHeadThrustCooldownTicks = CLUB_HEAD_THRUST_COOLDOWN_TICKS;
        // The quick thrust intentionally has no pre-attack vocal.
        heavyAttackRoarDelayTicks = -1;
        clubHeadThrustImpactSoundDelayTicks = CLUB_HEAD_THRUST_WHOOSH_DELAY_TICKS;
        clubHeadThrustElapsedTicks = 0;
        clubHeadThrustOriginX = getX();
        clubHeadThrustOriginZ = getZ();
        clubHeadThrustYaw = getYRot();
        previousClubHeadThrustPosition = null;
        clubHeadThrustHitTargets.clear();
        clubHeadThrustImpactSoundPlayed = false;
        return new MeleeAttackPlan(swingCooldown(WALKING_ATTACK_COOLDOWN_TICKS), CLUB_HEAD_THRUST_ANIMATION_TICKS, List.of());
    }

    private void tickClubHeadThrustHitbox() {
        if (activeLordAttackId != 7 || attackAnimationTicks <= 0) {
            previousClubHeadThrustPosition = null;
            return;
        }

        clubHeadThrustElapsedTicks++;
        // He keeps walking through the thrust, so the club head is measured from where he is now.
        clubHeadThrustOriginX = getX();
        clubHeadThrustOriginZ = getZ();
        clubHeadThrustYaw = getYRot();
        if (clubHeadThrustElapsedTicks < CLUB_HEAD_THRUST_ACTIVE_START_TICK
                || clubHeadThrustElapsedTicks > CLUB_HEAD_THRUST_ACTIVE_END_TICK) {
            return;
        }

        Vec3 forward = Vec3.directionFromRotation(0.0F, clubHeadThrustYaw).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        float progress = Mth.clamp(
                (float) (clubHeadThrustElapsedTicks - CLUB_HEAD_THRUST_ACTIVE_START_TICK + 1)
                        / (CLUB_HEAD_THRUST_ACTIVE_END_TICK - CLUB_HEAD_THRUST_ACTIVE_START_TICK + 1),
                0.0F, 1.0F);
        double reach = Mth.lerp(progress, CLUB_HEAD_THRUST_START_REACH, CLUB_HEAD_THRUST_END_REACH);
        Vec3 currentClubHead = new Vec3(clubHeadThrustOriginX, getY() + 2.25, clubHeadThrustOriginZ)
                .add(forward.scale(reach))
                .add(right.scale(CLUB_HEAD_THRUST_RIGHT_OFFSET));
        Vec3 previousClubHead = previousClubHeadThrustPosition == null
                ? new Vec3(clubHeadThrustOriginX, getY() + 2.25, clubHeadThrustOriginZ)
                    .add(forward.scale(CLUB_HEAD_THRUST_START_REACH))
                    .add(right.scale(CLUB_HEAD_THRUST_RIGHT_OFFSET))
                : previousClubHeadThrustPosition;
        previousClubHeadThrustPosition = currentClubHead;

        AABB sweptVolume = new AABB(previousClubHead, currentClubHead)
                .inflate(CLUB_HEAD_THRUST_HEAD_RADIUS, 2.25, CLUB_HEAD_THRUST_HEAD_RADIUS);
        MeleeAttackHit hit = MeleeAttackHit.areaSpecial(0,
                        CLUB_HEAD_THRUST_DAMAGE_MULTIPLIER, 0.0F, false,
                        0.2F, 4.32, 65.0F, 0.40, 0, 0, false)
                .withDirectionalKnockback(1.0, -0.38);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, sweptVolume,
                candidate -> candidate != this
                        && candidate.isAlive()
                        && (!(candidate instanceof OgreGrunt) || isPlagueEnemy(candidate))
                        && candidate.getBoundingBox().intersects(sweptVolume)
                        && canStrikeReach(candidate))) {
            if (!clubHeadThrustHitTargets.add(target.getUUID())) {
                continue;
            }
            if (doScaledHurtTarget(target, hit.damageMultiplier(), hit.armorPierceFraction())) {
                applyAttackHitExtras(target, hit);
                if (!clubHeadThrustImpactSoundPlayed) {
                    clubHeadThrustImpactSoundPlayed = true;
                    playClubHeadThrustHit(target);
                }
            }
        }
    }

    private MeleeAttackPlan startSingleLegStomp() {
        playAttackAnimation(8);
        singleLegStompImpactSoundDelayTicks = SINGLE_LEG_STOMP_IMPACT_DELAY_TICKS;
        warmUpRippleBlockVisual();
        // The stomp itself deals no area damage; the wave it starts at the moment of impact does.
        stompWaveElapsedTicks = 0;
        stompWaveStartTime = level().getGameTime();
        stompWaveHitTargets.clear();
        return new MeleeAttackPlan(swingCooldown(BASIC_ATTACK_COOLDOWN_TICKS), SINGLE_LEG_STOMP_ANIMATION_TICKS, List.of());
    }

    private int rollFrustrationThreshold() {
        return 100 + getRandom().nextInt(81);
    }

    /**
     * Counts how long a nearby target has been out of reach (up a tree, behind a wall, in a pit) and
     * then plows through whatever is in the way. Fort walls and every other structure are left alone.
     */
    private void tickFrustration() {
        plowCooldownTicks = Math.max(plowCooldownTicks - 1, 0);
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity target = getTarget();
        boolean free = isFortEncounterDormant() || isSummoning() || isRoaring() || isDyingAnimation()
                || isPhaseTransitioning() || target == null || !target.isAlive();
        if (free) {
            frustrationTicks = 0;
            plowTicksRemaining = 0;
            return;
        }

        if (plowTicksRemaining > 0) {
            plowTicksRemaining--;
            if (plowTicksRemaining % 5 == 0) {
                clearPlowPath(serverLevel, target);
            }
            if (plowTicksRemaining == 0) {
                plowCooldownTicks = 400 + getRandom().nextInt(301);
            }
            return;
        }

        if (tickCount % 10 != 0) {
            return;
        }
        double distanceSqr = distanceToSqr(target);
        boolean nearby = distanceSqr > REGULAR_ATTACK_START_RANGE * REGULAR_ATTACK_START_RANGE
                && distanceSqr < PLOW_TRIGGER_RANGE * PLOW_TRIGGER_RANGE;
        Path path = nearby ? getNavigation().createPath(target, 0) : null;
        boolean blocked = nearby && (path == null || !path.canReach());
        frustrationTicks = blocked ? frustrationTicks + 10 : Math.max(0, frustrationTicks - 20);
        if (frustrationTicks >= frustrationThresholdTicks && plowCooldownTicks <= 0 && activeLordAttackId == 0) {
            startPlow(serverLevel);
        }
    }

    private void startPlow(ServerLevel serverLevel) {
        frustrationTicks = 0;
        frustrationThresholdTicks = rollFrustrationThreshold();
        plowTicksRemaining = 60 + getRandom().nextInt(61);
        plowHalfWidth = 1.2 + getRandom().nextDouble() * 1.0;
        plowHeight = 5 + getRandom().nextInt(3);
        plowReach = 4.0 + getRandom().nextDouble() * 3.0;
        plowDropChance = getRandom().nextFloat() * 0.3F;
        playLordHuff(1.3F);
        serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 0.8F, 0.55F + getRandom().nextFloat() * 0.2F);
    }

    /** Breaks a corridor ahead of him toward the target, and a pocket around them once he's close. */
    private void clearPlowPath(ServerLevel serverLevel, LivingEntity target) {
        Vec3 toTarget = new Vec3(target.getX() - getX(), 0.0, target.getZ() - getZ());
        if (toTarget.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 forward = toTarget.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double ahead = Math.min(plowReach, toTarget.length());
        int broken = 0;
        for (double along = 0.5; along <= ahead && broken < PLOW_BLOCKS_PER_PULSE; along += 1.0) {
            for (double side = -plowHalfWidth; side <= plowHalfWidth && broken < PLOW_BLOCKS_PER_PULSE; side += 1.0) {
                for (int up = 0; up < plowHeight && broken < PLOW_BLOCKS_PER_PULSE; up++) {
                    BlockPos pos = BlockPos.containing(getX() + forward.x * along + right.x * side,
                            getY() + up, getZ() + forward.z * along + right.z * side);
                    if (breakPlowBlock(serverLevel, pos)) broken++;
                }
            }
        }
        if (toTarget.length() <= plowReach + 2.0) {
            BlockPos center = target.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 3, 2))) {
                if (broken >= PLOW_BLOCKS_PER_PULSE) break;
                // Leave the block the target is standing on so the pocket doesn't just drop them.
                if (pos.getY() == center.getY() - 1 && pos.getX() == center.getX() && pos.getZ() == center.getZ()) continue;
                if (breakPlowBlock(serverLevel, pos.immutable())) broken++;
            }
        }
    }

    private boolean breakPlowBlock(ServerLevel serverLevel, BlockPos pos) {
        BlockState state = serverLevel.getBlockState(pos);
        if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
            return false;
        }
        float hardness = state.getDestroySpeed(serverLevel, pos);
        if (hardness < 0.0F || hardness > MAX_DESTRUCTIBLE_HARDNESS
                || serverLevel.structureManager().hasAnyStructureAt(pos)) {
            return false;
        }
        serverLevel.destroyBlock(pos, getRandom().nextFloat() < plowDropChance, this);
        return true;
    }

    /** Swing recovery with a little jitter, so the King's rhythm isn't a fixed metronome. */
    private int swingCooldown(int baseTicks) {
        return baseTicks + getRandom().nextInt(SWING_COOLDOWN_JITTER_TICKS * 2 + 1) - SWING_COOLDOWN_JITTER_TICKS;
    }

    /**
     * Crossing a health gate makes the Roar or Summon ready; whichever it is still waits for him to be
     * idle, so it fires between swings. A King loaded mid-fight skips the gates he's already past
     * instead of firing them all at once.
     */
    private void tickHealthGates() {
        float fraction = getHealth() / Math.max(1.0F, getMaxHealth());
        boolean fire = healthGatesInitialised;
        healthGatesInitialised = true;
        while (nextSummonGate < SUMMON_HEALTH_GATES.length && fraction <= SUMMON_HEALTH_GATES[nextSummonGate]) {
            nextSummonGate++;
            if (fire) summonCooldownTicks = 0;
        }
        while (nextRoarGate < ROAR_HEALTH_GATES.length && fraction <= ROAR_HEALTH_GATES[nextRoarGate]) {
            nextRoarGate++;
            if (fire) roarCooldownTicks = 0;
        }
    }

    private void tickStompWave() {
        if (stompWaveElapsedTicks < 0) {
            return;
        }
        if (!isAlive() || level().getGameTime() - stompWaveStartTime > STOMP_WAVE_MAX_LIFETIME_TICKS) {
            stompWaveElapsedTicks = -1;
            return;
        }

        int sinceImpact = stompWaveElapsedTicks++ - SINGLE_LEG_STOMP_IMPACT_DELAY_TICKS;
        if (sinceImpact < 0) {
            return;
        }
        if (sinceImpact == 0) {
            // He doesn't turn mid-stomp, so the wave runs where he is facing at the moment of impact.
            stompWaveOrigin = position();
            stompWaveDirection = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        }
        int lastPulseTick = (STOMP_WAVE_PULSES - 1) * STOMP_WAVE_PULSE_INTERVAL_TICKS;
        if (sinceImpact > lastPulseTick + 6) {
            // The last pulse's ripple has finished.
            stompWaveElapsedTicks = -1;
        } else if (sinceImpact <= lastPulseTick && sinceImpact % STOMP_WAVE_PULSE_INTERVAL_TICKS == 0
                && !spawnStompWavePulse(sinceImpact / STOMP_WAVE_PULSE_INTERVAL_TICKS)) {
            // The wave ran into a wall.
            stompWaveElapsedTicks = -1;
        }
    }

    private boolean hasCollision(BlockPos pos) {
        return !level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty();
    }

    /** Returns false when terrain blocks the wave (a wall or step more than a block high). */
    private boolean spawnStompWavePulse(int pulse) {
        double distance = STOMP_WAVE_FIRST_DISTANCE + pulse * STOMP_WAVE_SPACING;
        double x = stompWaveOrigin.x + stompWaveDirection.x * distance;
        double z = stompWaveOrigin.z + stompWaveDirection.z * distance;
        int baseY = Mth.floor(stompWaveOrigin.y);

        // Find a standing surface up to a block higher or three lower, with room for a player above
        // it. Ceilings and canopies further up don't matter, only what is at body height.
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Double floorY = null;
        for (int surface = baseY + 1; surface >= baseY - 3; surface--) {
            if (hasCollision(pos.set(Mth.floor(x), surface - 1, Mth.floor(z)))
                    && !hasCollision(pos.set(Mth.floor(x), surface, Mth.floor(z)))
                    && !hasCollision(pos.set(Mth.floor(x), surface + 1, Mth.floor(z)))) {
                floorY = (double) surface;
                break;
            }
        }
        if (floorY == null) {
            // Solid at body height means a wall; otherwise it is a gap, which just skips this pulse.
            return !hasCollision(pos.set(Mth.floor(x), baseY, Mth.floor(z)))
                    && !hasCollision(pos.set(Mth.floor(x), baseY + 1, Mth.floor(z)));
        }

        Vec3 center = new Vec3(x, floorY, z);
        MeleeAttackHit hit = MeleeAttackHit.areaSpecial(0, STOMP_WAVE_DAMAGE_MULTIPLIER, STOMP_WAVE_PULSE_RADIUS,
                        false, 0.2F, 0.0, 0.0F, STOMP_WAVE_LAUNCH, STOMP_WAVE_SLOWNESS_TICKS, 0, false)
                .withVerticalHitRange(STOMP_WAVE_VERTICAL_HIT_RANGE);
        int rings = Math.max(2, (int) Math.ceil(STOMP_WAVE_PULSE_RADIUS / 1.25F));
        float propagationSpeed = STOMP_WAVE_PULSE_RADIUS / (rings * 2.0F);
        startGroundRipple(center, hit, new GroundRippleProfile(STOMP_WAVE_PULSE_RADIUS, propagationSpeed,
                1.125F, 0.44F, 1.0F, STOMP_WAVE_VERTICAL_HIT_RANGE, false, GroundRippleKind.KING_STOMP_WAVE));
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, x, floorY + 0.1, z, 5, 0.6, 0.1, 0.6, 0.02);
            serverLevel.playSound(null, x, floorY, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE,
                    0.45F, 0.75F + pulse * 0.04F);
            // The ground itself cracking up under each pulse, in whatever it is made of.
            BlockPos below = BlockPos.containing(x, floorY - 0.5, z);
            serverLevel.playSound(null, x, floorY, z,
                    serverLevel.getBlockState(below).getSoundType(serverLevel, below, this).getBreakSound(),
                    SoundSource.HOSTILE, 0.8F, 0.6F + pulse * 0.03F);
        }
        return true;
    }

    private boolean isWithinRegularAttackStartRange(LivingEntity target) {
        return target != null
                && target.isAlive()
                && distanceToSqr(target) <= REGULAR_ATTACK_START_RANGE * REGULAR_ATTACK_START_RANGE;
    }

    @Override
    protected double getMeleeAttackReachSqr(LivingEntity target) {
        return REGULAR_ATTACK_START_RANGE * REGULAR_ATTACK_START_RANGE;
    }

    @Override
    public boolean canDisableShield() {
        return activeLordAttackId == 1 || activeLordAttackId == ATTACK_ID_REGULAR_ATTACK
                || activeLordAttackId == ATTACK_ID_REGULAR_ATTACK_2 || super.canDisableShield();
    }

    @Override
    protected boolean shouldFaceMeleeTarget(LivingEntity target) {
        // The attacks he throws while walking keep turning him toward his target.
        return !isRoaring() && !isPhaseTransitioning() && !isDyingAnimation()
                && (!isMidSwing() || isWalkingAttack(activeLordAttackId));
    }

    @Override
    protected boolean shouldStayStationaryDuringMeleeAttack() {
        // Without this the goal re-evaluates movement every tick mid-swing, and a target stepping
        // out of range triggers a repath that turns his body. shouldFaceMeleeTarget() already
        // blocks look-based rotation mid-swing; this blocks the navigation-driven kind too.
        return isRoaring() || isPhaseTransitioning() || isDyingAnimation()
                || isMidSwing() && !isWalkingAttack(activeLordAttackId);
    }

    /**
     * Attacks whose clips leave the legs alone, so they can be thrown on top of the combat walk
     * while he keeps moving: the regular slams, both upswings, the single club slam and the thrust.
     * Every other attack plants him.
     */
    private static boolean isWalkingAttack(int attackId) {
        return switch (attackId) {
            case 1, 6, 7, 11, ATTACK_ID_REGULAR_ATTACK, ATTACK_ID_REGULAR_ATTACK_2 -> true;
            default -> false;
        };
    }

    @Override
    protected boolean canCombatWalkDuringAttack() {
        return isWalkingAttack(activeLordAttackId);
    }

    @Override
    protected boolean keepsMovingThroughAttackRange() {
        return true;
    }

    @Override
    protected boolean shouldDirectlyTrackMeleeTargetWhilePursuing() {
        // Let navigation own the body heading while closing distance. The look controller takes
        // over near attack range, avoiding the two controllers pulling the King side-to-side.
        return false;
    }

    @Override
    protected boolean shouldApproachDuringMeleeRecovery() {
        return true;
    }

    @Override
    protected boolean shouldUseCombatWalk(LivingEntity target, boolean closeEnoughToAttack,
                                          int ticksUntilNextAttack, boolean currentlyCombatWalking) {
        // Once he has a target the combat walk is his only gait, near or far; the planted attacks
        // and the roar are what stop him.
        return target != null && target.isAlive()
                && (!isMidSwing() || isWalkingAttack(activeLordAttackId))
                && !isPhaseTransitioning() && !isRoaring() && !isDyingAnimation();
    }

    @Override
    protected void setUsingCombatWalk(boolean usingCombatWalk) {
        if (!level().isClientSide) {
            int locomotion = usingCombatWalk || getTarget() != null && isAggressive()
                    ? LOCOMOTION_COMBAT_WALK
                    : getNavigation().isDone() ? LOCOMOTION_IDLE : LOCOMOTION_WALK;
            entityData.set(DATA_LORD_LOCOMOTION, locomotion);
        }
    }

    @Override
    protected boolean usesRunningTurnSmoothing() {
        return entityData.get(DATA_LORD_LOCOMOTION) == LOCOMOTION_COMBAT_WALK;
    }

    @Override
    protected double getCombatWalkSpeedModifier() {
        return KING_COMBAT_WALK_SPEED;
    }

    @Override
    protected boolean shouldHoldCombatSpacing(LivingEntity target) {
        double x = target.getX() - getX();
        double z = target.getZ() - getZ();
        double combinedRadius = (getBbWidth() + target.getBbWidth()) * 0.5;
        double preferredCenterDistance = combinedRadius + COMBAT_WALK_TARGET_GAP;
        return x * x + z * z <= preferredCenterDistance * preferredCenterDistance
                && Math.abs(target.getY() - getY()) <= COMBAT_WALK_MAX_VERTICAL_DIFFERENCE;
    }

    @Override
    protected boolean usesPolishedMeleePursuit() {
        return true;
    }

    @Override
    protected int getPathRecalculationTicks() {
        return 8 + getRandom().nextInt(5);
    }

    private boolean isMidSwing() {
        return activeLordAttackId != 0 && attackAnimationTicks > 0;
    }

    /**
     * Only a real jump or fall gets under a ground wave. Being off the ground is not enough on its
     * own: swimming, climbing a ladder or vines, or riding a mount (the rider is never "on the
     * ground", the mount is) would otherwise make a player immune to every ripple.
     */
    private boolean isAirborneShockwaveDodge(LivingEntity target) {
        Entity body = target.getRootVehicle();
        return !body.onGround() && !body.isInWaterOrBubble() && !body.isInLava() && !target.onClimbable();
    }

    @Override
    protected void applyAttackHitExtras(LivingEntity target, MeleeAttackHit hit) {
        if (activeLordAttackId == 5) {
            super.applyAttackHitExtras(target, hit.withoutHorizontalKnockback());
            applyBellyBumpLaunch(target, hit.verticalKnockbackStrength());
        } else {
            // Ground shockwaves launch vertically without replacing the target's horizontal motion.
            // Direct contact with a directionally moving weapon keeps its authored knockback.
            super.applyAttackHitExtras(target,
                    hit.blockRippleParticles() ? hit.withoutHorizontalKnockback() : hit);
        }
        if (dazesOnHit(activeLordAttackId) && (!(target instanceof OgreGrunt) || isPlagueEnemy(target))) {
            applyHeavyClubHitEffects(target);
        }
    }

    private void applyBellyBumpLaunch(LivingEntity target, double verticalStrength) {
        Vec3 outward = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (outward.lengthSqr() < 1.0E-4) {
            outward = Vec3.directionFromRotation(0.0F, getYRot());
        } else {
            outward = outward.normalize();
        }
        Vec3 movement = target.getDeltaMovement();
        target.setDeltaMovement(
                outward.x * BELLY_BUMP_HORIZONTAL_LAUNCH,
                Math.max(movement.y, verticalStrength),
                outward.z * BELLY_BUMP_HORIZONTAL_LAUNCH);
        target.hasImpulse = true;
    }

    /**
     * The attacks that go through the shared hit path and are heavy enough to daze: both upswings,
     * the double stomp, the double overhead slam, the belly bump and the club head thrust. The
     * club strikes daze through their own paths, and the single leg stomp is his repeatable
     * poke, so it does not.
     */
    private static boolean dazesOnHit(int attackId) {
        return switch (attackId) {
            case 1, 2, 3, 5, 6, 7 -> true;
            default -> false;
        };
    }

    @Override
    protected boolean applyGroundRippleHit(LivingEntity target, MeleeAttackHit hit, GroundRippleProfile profile,
                                           Vec3 origin) {
        if (profile.kind() == GroundRippleKind.STANDARD) {
            float share = rippleFadeShare(target, origin, profile.maxRadius(), hit.damageMultiplier());
            return super.applyGroundRippleHit(target,
                    hit.withDamageMultiplier(hit.damageMultiplier() * share), profile, origin);
        }
        if (profile.kind() == GroundRippleKind.KING_STOMP_WAVE) {
            // Faded along the whole line, measured from the stomp rather than from each pulse.
            float share = rippleFadeShare(target, stompWaveOrigin, STOMP_WAVE_REACH, hit.damageMultiplier());
            // Neighbouring pulses overlap, so make sure a target only takes the wave once.
            return stompWaveHitTargets.add(target.getUUID())
                    && super.applyGroundRippleHit(target,
                    hit.withDamageMultiplier(hit.damageMultiplier() * share), profile, origin);
        }

        float damageMultiplier;
        float bonusDamage;
        if (profile.kind() == GroundRippleKind.KING_BUTT_SMASH) {
            damageMultiplier = buttSmashRippleDamageMultiplier(target, origin);
            bonusDamage = 0.0F;
        } else if (profile.kind() == GroundRippleKind.KING_CLUB_DOUBLE) {
            damageMultiplier = CLUB_DOUBLE_OVERHEAD_DAMAGE_MULTIPLIER * 0.5F;
            bonusDamage = Config.OGRE_LORD_CLUB_DOUBLE_OVERHEAD_BONUS_DAMAGE.get().floatValue() * 0.5F;
        } else {
            damageMultiplier = PULL_OUT_DAMAGE_MULTIPLIER * 0.5F;
            bonusDamage = Config.OGRE_LORD_PULL_OUT_BONUS_DAMAGE.get().floatValue() * 0.5F;
        }
        if (profile.kind() != GroundRippleKind.KING_BUTT_SMASH) {
            // The club ripples, whose flat bonus counts toward what fades. The butt smash has faded already.
            float fullMultiplier = damageMultiplier
                    + bonusDamage / (float) Math.max(0.01, getAttributeValue(Attributes.ATTACK_DAMAGE));
            float share = rippleFadeShare(target, origin, profile.maxRadius(), fullMultiplier);
            damageMultiplier *= share;
            bonusDamage *= share;
        }
        if (isTribeAlly(target)) {
            // Tamed army ogres and ogre-ridden boars take the same halved ripple damage other ogres deal them.
            damageMultiplier *= 0.5F;
            bonusDamage *= 0.5F;
        }
        if (dealClubDamage(target, damageMultiplier, bonusDamage)) {
            // The butt smash ripple leaves you able to hit back while he gets up; only its circle dazes.
            if (profile.kind() != GroundRippleKind.KING_BUTT_SMASH) {
                applyHeavyClubHitEffects(target);
            }
            return true;
        }
        return false;
    }

    /**
     * The share of a ripple's damage left where it reaches {@code target}: all of it close to where it
     * started, then fading evenly to the weakest a ripple gets at {@code reach}.
     */
    private float rippleFadeShare(LivingEntity target, Vec3 origin, float reach, float fullMultiplier) {
        double distance = Math.hypot(target.getX() - origin.x, target.getZ() - origin.z);
        float edgeShare = Math.min(1.0F, RIPPLE_EDGE_DAMAGE_MULTIPLIER / fullMultiplier);
        return OgreDamage.falloff(distance, reach, edgeShare);
    }

    @Override
    protected void applyDirectShockwaveWeaponHits(List<LivingEntity> targets, Vec3 areaCenter,
                                                  MeleeAttackHit hit) {
        if (activeLordAttackId != 2 && activeLordAttackId != 3 && activeLordAttackId != 8) {
            return;
        }

        // The unarmed Double Overhead Slam is a big enough hit to want the same dust density as
        // the club slams — its own screenshake still comes from the standard shared hit pipeline,
        // so this only adds the extra particles, not a second shake packet.
        if (activeLordAttackId == 3 && level() instanceof ServerLevel serverLevel) {
            spawnDenseDustCloud(serverLevel, areaCenter, DOUBLE_OVERHEAD_SLAM_AREA_RADIUS);
        }

        // Double Stomp's radius (11.25) leaves a visible gap between the shared shockwave's center
        // burst and edge ring; Single Leg Stomp's smaller radius (6.5) doesn't. This adds fill
        // bands into the gap per impact without touching the edge ring or its radius.
        if (activeLordAttackId == 2 && level() instanceof ServerLevel serverLevel) {
            spawnGraduatedDustFill(serverLevel, areaCenter, DOUBLE_STOMP_AREA_RADIUS);
        }

        // Double Stomp has two separately authored impacts. Its first ripple has completed before
        // the second foot lands, so begin fresh per-impact tracking and allow the second foot to
        // connect independently just as it did before this direct-hit audit.
        directShockwaveWeaponHitTargets.clear();

        // Match the visible attacking surface at the authored ground-impact center. Directly
        // struck targets are remembered so the traveling ripple cannot deal a duplicate hit.
        double directRadius = activeLordAttackId == 3 ? 3.25 : 2.1;
        double directTop = activeLordAttackId == 3 ? getY() + 6.5 : getY() + 3.25;
        AABB directImpactVolume = new AABB(
                areaCenter.x - directRadius, getY() - 1.0, areaCenter.z - directRadius,
                areaCenter.x + directRadius, directTop, areaCenter.z + directRadius);
        for (LivingEntity target : targets) {
            if (!target.getBoundingBox().intersects(directImpactVolume)
                    || !directShockwaveWeaponHitTargets.add(target.getUUID())) {
                continue;
            }

            float damageMultiplier = isTribeAlly(target)
                    ? hit.damageMultiplier() * 0.5F
                    : hit.damageMultiplier();
            if (doScaledHurtTarget(target, damageMultiplier, hit.armorPierceFraction())) {
                applyAttackHitExtras(target, hit.withoutHorizontalKnockback());
            }
        }
    }

    @Override
    protected boolean showsAreaRing() {
        // The one-arm and double-arm upswings are quick swings, not slams: no ring for them.
        return activeLordAttackId != 1 && activeLordAttackId != 6;
    }

    /** His slams already break blocks through destroyNearbyBlocks; the shared shockwave hook adds nothing. */
    @Override
    protected void onGroundShockwave(Vec3 center, float radius) {
    }

    @Override
    protected boolean shouldDodgeRippleHit(LivingEntity target) {
        if (!isPhaseTransitioning() && stompWaveElapsedTicks < 0
                && activeLordAttackId != 2 && activeLordAttackId != 3 && activeLordAttackId != 8
                && activeLordAttackId != 10 && activeLordAttackId != ATTACK_ID_BUTT_SMASH
                && tickCount >= jumpableRippleUntilTick) {
            return false;
        }

        // Stomps and the double overhead slams are ground shockwaves.
        // Jumping at the right moment so the ripple passes underneath is the intended dodge.
        // Either half of the jump counts as long as the target is airborne when the wave arrives.
        return directShockwaveWeaponHitTargets.contains(target.getUUID())
                || isAirborneShockwaveDodge(target)
                || hasRecentIntentionalJump(target, 2);
    }

    @Override
    protected double getMeleePursuitSpeedModifier() {
        return 1.0;
    }

    @Override
    protected double getWanderSpeedModifier() {
        return 0.4;
    }

    @Override
    protected void snapFaceTarget(LivingEntity target) {
        // The default hard-snaps to face the target the instant an attack starts. On a giant model
        // that can whip through a huge arc in one frame if the target has circled around — cap how
        // far a single snap can turn so it reads as a quick turn instead of a spin.
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        // A share of the remaining angle each call, not a fixed step: a fixed step is a hard stop at the end of
        // every turn, and a proportional one eases in and out like the rest of his heading changes.
        float yaw = easeFacing(getYRot(), desiredYaw, FACING_EASE_FRACTION);
        // Old rotations are left alone so the renderer blends across the tick instead of popping.
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    @Override
    protected boolean suppressShockwaveSound() {
        // The generic explosion sound is played explicitly in playDoubleStompImpactSound()/
        // playDoubleOverheadSlamImpactSound() alongside the reused belly-slam impact — suppress the
        // shared inherited one here so it doesn't also fire and double up.
        return true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.OGRE_LORD_HURT.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.OGRE_LORD_HUFF.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.OGRE_LORD_DEATH.get();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isFortEncounterDormant() || isDyingAnimation()) {
            // Striking the King while he is holding court is as good as challenging him.
            if (isSeatedInFort() && !royalDefenceStarted && source.getEntity() instanceof ServerPlayer attacker) {
                challenge(attacker);
            }
            return false;
        }
        if (isPhaseTransitioning()) {
            amount *= 0.5F;
        }
        if (source.getDirectEntity() instanceof AbstractArrow) {
            amount *= 0.5F;
        }
        // A duel is fought to a yield, not to the death: the blow that would drop him this low ends it.
        if (duelistId != null && getHealth() - amount <= getMaxHealth() * DUEL_YIELD_FRACTION) {
            if (source.getEntity() instanceof ServerPlayer duelist && isDuelist(duelist)) {
                yieldDuel(duelist);
            }
            return false;
        }

        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && !isPhaseTwo()
                && getHealth() <= getMaxHealth() * PHASE_TWO_HEALTH_THRESHOLD) {
            enterPhaseTwo();
        }
        return hurt;
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (isFortEncounterDormant() || isPhaseTransitioning() || isDyingAnimation() || buttSmashAirborne) {
            return;
        }
        super.knockback(strength, x, z);
    }

    @Override
    public boolean isPushable() {
        return !isFortEncounterDormant() && !buttSmashAirborne && super.isPushable();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().expandTowards(0.0, BUTT_SMASH_VISUAL_HEIGHT, 0.0);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        clearRoyalTrialBossBarPlayers();
        kingBossBar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void die(DamageSource source) {
        if (isDyingAnimation()) {
            return;
        }

        delayedDeathSource = source;
        entityData.set(DATA_LORD_DYING, true);
        entityData.set(DATA_LORD_TRANSITIONING, false);
        cancelActiveMeleeAttack();
        stopRoarImmediately();
        stopSummonImmediately();
        setTarget(null);
        setAggressive(false);
        lockSpecialAnimationPose();
        setNoAi(true);
        deathTime = 0;
        playAttackAnimation(12);
    }

    @Override
    protected void tickDeath() {
        if (!isDyingAnimation()) {
            super.tickDeath();
            return;
        }

        // Only the server has the locked coordinates. Use its position packets to avoid snapping
        // the client toward the origin when death interrupts the animation.
        if (!level().isClientSide) {
            freezeSpecialAnimationPose();
        }
        deathTime++;
        if (deathTime < DEATH_ANIMATION_TICKS) {
            return;
        }

        DamageSource source = delayedDeathSource != null
                ? delayedDeathSource
                : damageSources().genericKill();
        entityData.set(DATA_LORD_DYING, false);
        delayedDeathSource = null;
        super.die(source);
        deathTime = 19;
        super.tickDeath();
    }

    @Override
    public float getVoicePitch() {
        // The new pools provide natural variation and are timed final performances.
        return 1.0F;
    }

    // ------------------------------------------------------------------ //
    //  Animation controllers                                             //
    // ------------------------------------------------------------------ //

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new SynchronizedAnimationController<>(this, "Lord Movement", 5, this::animateLord)
                .withFootfalls(FOOT_CONTACTS, this::playFootfall));
        // Registered second so it is applied after the walk: the attacks he throws while walking
        // only key his upper body, and this controller blends them in from whatever pose the walk
        // has him in. See OgreLordModel for the matching blend back out.
        // Same 5-tick blend-in as the movement controller, so the hit timings calibrated against it
        // (ATTACK_SYNC_DELAY_TICKS) hold for these attacks too.
        controllers.add(new SynchronizedAnimationController<>(this, "Lord Attack Overlay", 5,
                this::animateAttackOverlay));
    }

    /** True on the client while an attack is playing over the walk. */
    public boolean isAttackOverlayVisible() {
        return attackOverlayVisible;
    }

    @Override
    public double getBoneResetTime() {
        // Bones an animation stops keying (the legs when a planted attack takes over) ease back to
        // rest over a few ticks instead of snapping there.
        return 6.0;
    }

    private PlayState animateAttackOverlay(AnimationState<OgreLord> state) {
        state.setControllerSpeed(1.0F);
        boolean blocked = isDyingAnimation()
                || entityData.get(DATA_LORD_TRANSITIONING)
                || entityData.get(DATA_FORT_ENCOUNTER_STATE) != FORT_ENCOUNTER_NORMAL;
        if (blocked) {
            // Death, the pull-out and the throne scenes own the whole body.
            visualOverlayOneShot.clear();
            attackOverlayVisible = false;
            return PlayState.STOP;
        }

        int syncedAttackId = entityData.get(DATA_LORD_ATTACK_ID);
        boolean overlayAttack = isWalkingAttack(syncedAttackId);
        int visualAttackId = visualOverlayOneShot.update(state.getController(),
                overlayAttack ? syncedAttackId : 0,
                overlayAttack ? entityData.get(DATA_LORD_ATTACK_START_TICK) : 0L,
                this::getAttackAnimationTicks, level().getGameTime());
        attackOverlayVisible = visualAttackId > 0;
        return attackOverlayVisible ? state.setAndContinue(getAttackAnimation(visualAttackId)) : PlayState.STOP;
    }

    private PlayState animateLord(AnimationState<OgreLord> state) {
        int fortEncounterState = entityData.get(DATA_FORT_ENCOUNTER_STATE);
        lastRenderedFortEncounterState = fortEncounterState;
        if (isDyingAnimation()) {
            state.setControllerSpeed(1.0F);
            if (visualDeathStartGameTime == Long.MIN_VALUE) {
                long syncedStart = entityData.get(DATA_LORD_ATTACK_ID) == 12
                        ? entityData.get(DATA_LORD_ATTACK_START_TICK) : 0L;
                visualDeathStartGameTime = syncedStart > 0L
                        ? syncedStart : Math.max(1L, level().getGameTime() - deathTime);
            }
            visualOneShot.updateTerminal(state.getController(), 12, visualDeathStartGameTime,
                    DEATH_ANIMATION_TICKS, level().getGameTime());
            return state.setAndContinue(DEATH);
        }
        if (fortEncounterState == FORT_ENCOUNTER_SLEEPING) {
            state.setControllerSpeed(1.0F);
            return state.setAndContinue(SLEEPING);
        }
        int syncedAttackId = entityData.get(DATA_LORD_ATTACK_ID);
        long syncedAttackStartTick = entityData.get(DATA_LORD_ATTACK_START_TICK);
        int visualAttackId;
        // A catch-up seek after looking away reads the controller's speed, so make sure it is 1.0
        // before the one-shot is reconciled; locomotion in water leaves a scaled value behind.
        state.setControllerSpeed(1.0F);
        if (entityData.get(DATA_LORD_TRANSITIONING) && syncedAttackId == 9) {
            // Pull-Out is a forced phase transition, like death: it has to cut off whatever attack
            // animation is playing instead of waiting for it to finish (update() normally protects
            // attacks from being preempted). Same escape hatch death uses.
            visualAttackId = visualOneShot.updateTerminal(state.getController(), 9, syncedAttackStartTick,
                    getAttackAnimationTicks(9), level().getGameTime());
        } else if (fortEncounterState == FORT_ENCOUNTER_WAKING && syncedAttackId == 14) {
            // The stand-up also has to cut off a startle or seated point that is still playing,
            // otherwise it waits for that clip to finish and can be skipped or start half-way.
            visualAttackId = visualOneShot.updateTerminal(state.getController(), 14, syncedAttackStartTick,
                    getAttackAnimationTicks(14), level().getGameTime());
        } else {
            // The attacks thrown while walking belong to the overlay controller; this one keeps
            // playing the walk underneath them.
            boolean overlayAttack = isWalkingAttack(syncedAttackId);
            visualAttackId = visualOneShot.update(state.getController(),
                    overlayAttack ? 0 : syncedAttackId, overlayAttack ? 0L : syncedAttackStartTick,
                    this::getAttackAnimationTicks, level().getGameTime());
        }
        if (visualAttackId > 0) {
            state.setControllerSpeed(1.0F);
            return state.setAndContinue(getAttackAnimation(visualAttackId));
        }
        if (fortEncounterState == FORT_ENCOUNTER_STARTLED || fortEncounterState == FORT_ENCOUNTER_SEATED) {
            // Between the startle and the commands he sits on the throne, watching.
            state.setControllerSpeed(1.0F);
            return state.setAndContinue(SITTING_IDLE);
        }

        int locomotion = entityData.get(DATA_LORD_LOCOMOTION);
        if (locomotion == LOCOMOTION_COMBAT_WALK) {
            idleAnimationTicks = 0;
            idleVariantTicks = 0;
            if (isMovingForAnimation(state)) {
                // Only scale playback speed in water — on dry land this must stay a flat 1.0F, since
                // the ratio against Attributes.MOVEMENT_SPEED doesn't land cleanly at 1.0 for his
                // normal pace and was quietly slowing down every land chase.
                state.setControllerSpeed(isInWater() ? computeMovementAnimSpeed() : 1.0F);
                return state.setAndContinue(COMBAT_WALK);
            }
            state.setControllerSpeed(1.0F);
            return state.setAndContinue(IDLE);
        }

        if (locomotion == LOCOMOTION_WALK && isMovingForAnimation(state)) {
            idleAnimationTicks = 0;
            idleVariantTicks = 0;
            state.setControllerSpeed(isInWater() ? computeMovementAnimSpeed() : 1.0F);
            return state.setAndContinue(WALK);
        }

        state.setControllerSpeed(1.0F);
        updateIdleAnimationTimers(state);
        if (idleVariantTicks > 0) {
            return state.setAndContinue(activeIdleAnimation);
        }
        return state.setAndContinue(IDLE);
    }

    private boolean isMovingForAnimation(AnimationState<OgreLord> state) {
        boolean moving = state.isMoving() || getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
        // Use game ticks so brief pauses don't drop the walk animation at high frame rates.
        long now = level().getGameTime();
        if (moving) {
            lastMovingAnimationTick = now;
            return true;
        }
        return now - lastMovingAnimationTick <= MOVEMENT_ANIMATION_GRACE_TICKS;
    }

    @Override
    protected void onMeleeAttackPlanFinished() {
        // Covers death and a yielded duel as well: both drop the active attack through here.
        buttSmashAirborne = false;
        if (!isRoaring() && !isPhaseTransitioning() && !isDyingAnimation()) {
            activeLordAttackId = 0;
        }
        super.onMeleeAttackPlanFinished();
        if (!level().isClientSide && !isPhaseTransitioning() && !isDyingAnimation()) {
            clearLordAttackSync();
        }
    }

    private void playAttackAnimation(int attackId) {
        directShockwaveWeaponHitTargets.clear();
        if (!level().isClientSide) {
            if (attackId >= 1 && attackId <= 11
                    || attackId == ATTACK_ID_REGULAR_ATTACK || attackId == ATTACK_ID_REGULAR_ATTACK_2) {
                secondLastSelectedAttackId = lastSelectedAttackId;
                lastSelectedAttackId = attackId;
            }
            entityData.set(DATA_LORD_ATTACK_ID, attackId);
            entityData.set(DATA_LORD_ATTACK_START_TICK, level().getGameTime());
        }

        attackAnimationTicks = getAttackAnimationTicks(attackId);
        activeLordAttackId = attackId;
    }

    private RawAnimation getAttackAnimation(int attackId) {
        return switch (attackId) {
            case 2 -> DOUBLE_STOMP;
            case 3 -> DOUBLE_OVERHEAD_SLAM;
            case 4 -> ROAR;
            case 5 -> BELLY_BUMP;
            case 6 -> DOUBLE_ARM_UPSWING;
            case 7 -> CLUB_HEAD_THRUST;
            case 8 -> SINGLE_LEG_STOMP;
            case 9 -> PULL_OUT;
            case 10 -> CLUB_DOUBLE_OVERHEAD;
            case 11 -> SINGLE_OVERHEAD_SLAM;
            case 12 -> DEATH;
            case 13 -> SUMMON_POINT;
            case 14 -> WAKE_UP;
            case ATTACK_ID_STARTLE -> STARTLE_AWAKE;
            case ATTACK_ID_REGULAR_ATTACK -> REGULAR_ATTACK;
            case ATTACK_ID_REGULAR_ATTACK_2 -> REGULAR_ATTACK_2;
            case ATTACK_ID_BUTT_SMASH -> BUTT_SMASH;
            default -> ONE_ARM_UPSWING;
        };
    }

    private int getAttackAnimationTicks(int attackId) {
        return switch (attackId) {
            case 2 -> DOUBLE_STOMP_ANIMATION_TICKS;
            case 3 -> DOUBLE_OVERHEAD_SLAM_ANIMATION_TICKS;
            case 4 -> ROAR_ANIMATION_TICKS;
            case 5 -> BELLY_BUMP_ANIMATION_TICKS;
            case 6 -> DOUBLE_ARM_UPSWING_ANIMATION_TICKS;
            case 7 -> CLUB_HEAD_THRUST_ANIMATION_TICKS;
            case 8 -> SINGLE_LEG_STOMP_ANIMATION_TICKS;
            case 9 -> PULL_OUT_ANIMATION_TICKS;
            case 10 -> CLUB_DOUBLE_OVERHEAD_ANIMATION_TICKS;
            case 11 -> SINGLE_OVERHEAD_SLAM_ANIMATION_TICKS;
            case 12 -> DEATH_ANIMATION_TICKS;
            case 13 -> SUMMON_POINT_ANIMATION_TICKS;
            case 14 -> WAKE_UP_ANIMATION_TICKS;
            case ATTACK_ID_STARTLE -> STARTLE_ANIMATION_TICKS;
            case ATTACK_ID_REGULAR_ATTACK, ATTACK_ID_REGULAR_ATTACK_2 -> REGULAR_ATTACK_ANIMATION_TICKS;
            case ATTACK_ID_BUTT_SMASH -> BUTT_SMASH_ANIMATION_TICKS;
            default -> ONE_ARM_UPSWING_ANIMATION_TICKS;
        };
    }

    private void updateIdleAnimationTimers(AnimationState<OgreLord> state) {
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
