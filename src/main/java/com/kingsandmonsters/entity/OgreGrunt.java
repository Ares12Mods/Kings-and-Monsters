package com.kingsandmonsters.entity;

import com.kingsandmonsters.advancement.RoyalAdvancementEvents;
import com.kingsandmonsters.api.patrol.PatrolTier;
import com.kingsandmonsters.item.ItemAttributes;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import com.kingsandmonsters.Config;
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
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import com.kingsandmonsters.tribute.QuestManager;
import com.kingsandmonsters.tribute.TributeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
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
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.kingsandmonsters.network.ModNetwork;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
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
    private static final float BONE_CLUB_SPAWN_CHANCE = 0.15F;
    private static final float BONE_CLUB_DROP_CHANCE = 0.085F;
    private static final float BONE_CLUB_DAMAGE_MULTIPLIER = 1.5F;
    public static final String ROYAL_PATROL_SQUAD_TAG = "KingsAndMonstersRoyalPatrolSquad";
    public static final String OUTPOST_REINFORCEMENT_SQUAD_TAG = "KingsAndMonstersOutpostReinforcementSquad";
    public static final String ROYAL_TRIAL_OGRE_TAG = "KingsAndMonstersRoyalTrialOgre";
    // How close an ogre has to be before it asks a coin-holder for the toll, and how many lines it has.
    private static final double TOLL_RANGE = 10.0;
    private static final int TOLL_LINES = 5;
    private static final String ROYAL_PATROL_GRUNTS_SPAWNED_TAG = "KingsAndMonstersRoyalPatrolGruntsSpawned";
    private static final String ROYAL_PATROL_ARCHERS_SPAWNED_TAG = "KingsAndMonstersRoyalPatrolArchersSpawned";
    private static final int ROYAL_PATROL_GRUNT_COUNT = 2;
    private static final int ROYAL_GUARD_PATROL_GRUNT_COUNT = 3;
    private static final int ROYAL_PATROL_ARCHER_COUNT = 1;
    private static final int OUTPOST_REINFORCEMENT_GRUNT_COUNT = 1;
    private static final int OUTPOST_REINFORCEMENT_ARCHER_COUNT = 2;
    private static final String OUTPOST_HOME_TAG = "OutpostHome";
    private static final String OUTPOST_CAPTAIN_TAG = "OutpostCaptain";
    private static final String MERCHANT_OWNER_TAG = "MerchantOwner";
    private static final int OUTPOST_HOME_RADIUS = 32;
    // The quarry is walled and about 56 blocks across, so its crew is called back well before the palisade.
    private static final int QUARRY_HOME_RADIUS = 25;
    private static final int OUTPOST_CAPTAIN_ALERT_RADIUS = 30;
    private static final int OUTPOST_RESIDENT_ALERT_RADIUS = 40;
    private static final int OUTPOST_MAX_CHASE_RADIUS = 64;
    private static final int OUTPOST_MAX_CHASE_TICKS = 30 * 20;
    private static final int OUTPOST_RETREAT_COUNTERATTACK_TICKS = 10 * 20;
    private static final int ROYAL_COMMAND_DURATION_TICKS = 10 * 20;
    private OgreGruntMeleeGoal meleeGoal;

    // --- Summon Army: ogres called up by the Ogre King's Crown fight for its wearer for a while -----

    /** How far from its summoner an army ogre wanders before it follows, and how close it comes before it stops. */
    private static final double ARMY_FOLLOW_START = 11.0;
    private static final double ARMY_FOLLOW_STOP = 6.0;
    private static final double ARMY_HURRY_DISTANCE = 20.0;
    private static final double ARMY_WALK_SPEED = 0.8;
    private static final String ARMY_OWNER_TAG = "KingsAndMonstersArmyOwner";
    private static final String ARMY_TICKS_TAG = "KingsAndMonstersArmyTicks";
    @Nullable
    private UUID armyOwnerId;
    private int armyTicksLeft;
    private int armyOwnerAbsentTicks;

    public boolean isArmyAlly() {
        return armyOwnerId != null;
    }

    @Nullable
    public UUID getArmyOwnerId() {
        return armyOwnerId;
    }

    /** Puts this ogre in the player's army for {@code ticks}: it fights for them and then goes. */
    public void joinArmy(Player owner, int ticks) {
        armyOwnerId = owner.getUUID();
        armyTicksLeft = ticks;
        armyOwnerAbsentTicks = 0;
        setPersistenceRequired();
        setTarget(null);
    }

    @Nullable
    private Player armyOwner() {
        return armyOwnerId == null ? null : level().getPlayerByUUID(armyOwnerId);
    }

    /** Counts the army ogre's time down, and sends it away when that is up or its owner has long gone. */
    private void tickArmy() {
        if (armyOwnerId == null) {
            return;
        }
        armyOwnerAbsentTicks = armyOwner() == null ? armyOwnerAbsentTicks + 1 : 0;
        if (--armyTicksLeft <= 0 || armyOwnerAbsentTicks > 20 * 30) {
            dismissArmy();
        }
    }

    /** The army ogre's time is up: it leaves in a puff of smoke, and its drops and experience with it. */
    protected void dismissArmy() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.5, getZ(),
                    16, getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 0.03);
            serverLevel.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.6F, 0.7F);
        }
        ejectPassengers();
        discard();
    }

    /** Army ogres leave nothing behind when they fall, so the army cannot be farmed. */
    @Override
    protected boolean shouldDropLoot() {
        return armyOwnerId == null && super.shouldDropLoot();
    }

    @Override
    public int getExperienceReward() {
        return armyOwnerId == null ? super.getExperienceReward() : 0;
    }

    /**
     * Whom an army's blows must spare: players, villagers and golems, and anything tamed. Used by every
     * area attack, spell and shove so the army cannot hurt what it was called to protect.
     */
    public static boolean spareByArmy(LivingEntity victim) {
        return victim instanceof Player
                || victim instanceof AbstractVillager
                || victim instanceof IronGolem
                || victim instanceof TamableAnimal pet && pet.isTame()
                || victim instanceof AbstractHorse horse && horse.isTamed()
                || victim instanceof MudsnoutBoar boar && boar.isTamed();
    }

    /**
     * Ogres are one tribe, but a summoned ogre and a wild one that is fighting it (or its summoner) are
     * not: an army can answer a hostile Brute, and the Brute can answer back. The King stays out of it.
     */
    private boolean armyFeud(Entity other) {
        if (!(other instanceof OgreGrunt o) || o.isArmyAlly() == isArmyAlly() || this instanceof OgreLord
                || o instanceof OgreLord || royalTrialOgre || o.royalTrialOgre) {
            return false;
        }
        OgreGrunt army = isArmyAlly() ? this : o;
        OgreGrunt wild = army == this ? o : this;
        // A summoned army fights the tribe's ogres on sight, unless its summoner is in the tribe's good graces and
        // has not picked a fight; the tribe only answers once it has been struck.
        if (army == this) {
            return armyMayFightTribe(wild);
        }
        return wild.getLastHurtByMob() == army || army.getLastHurtByMob() == wild || wild.isFightingArmyOf(army);
    }

    /** A wild ogre struck by a summoned one calls the ogres around it in, so the army meets the tribe at once, not one by one. */
    private void rallyTribeAgainst(OgreGrunt army) {
        if (this instanceof OgreLord || royalTrialOgre) {
            return;
        }
        for (OgreGrunt other : level().getEntitiesOfClass(OgreGrunt.class, getBoundingBox().inflate(24.0, 8.0, 24.0),
                o -> o != this && !o.isArmyAlly() && !(o instanceof OgreLord) && !o.royalTrialOgre && o.isAlive())) {
            if (other.getTarget() instanceof Player) {
                continue;
            }
            other.setLastHurtByMob(army);
            if (other.canAttack(army)) {
                other.setTarget(army);
            }
        }
    }

    /** A well-liked summoner's army keeps the peace until the summoner (or the tribe) starts the fight. */
    private boolean armyMayFightTribe(OgreGrunt wild) {
        Player owner = armyOwner();
        if (owner == null || !(level() instanceof ServerLevel server)) {
            return true;
        }
        if (!TributeManager.isAppeased(server, owner.getUUID())
                || TributeManager.isProvoked(server, owner.getUUID())) {
            return true;
        }
        return wild.getLastHurtByMob() == owner || getLastHurtByMob() == wild || wild.isFightingArmyOf(this);
    }

    private boolean isFightingArmyOf(OgreGrunt army) {
        LivingEntity target = getTarget();
        return target != null && army.armyOwnerId != null && (army.armyOwnerId.equals(target.getUUID())
                || target instanceof OgreGrunt ally && army.armyOwnerId.equals(ally.armyOwnerId));
    }

    /** Registers the goals every ogre type shares for fighting alongside its summoner. */
    protected final void registerArmyGoals() {
        goalSelector.addGoal(2, new ArmyFollowOwnerGoal());
        targetSelector.addGoal(0, new ArmyTargetGoal());
    }

    /** Falls in behind the summoner when it is not fighting, and hurries to catch up when it is left behind. */
    private final class ArmyFollowOwnerGoal extends Goal {
        private int repathTicks;

        private ArmyFollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player owner = armyOwner();
            return owner != null && getTarget() == null && getVehicle() == null
                    && owner.level() == level() && distanceToSqr(owner) > ARMY_FOLLOW_START * ARMY_FOLLOW_START;
        }

        @Override
        public boolean canContinueToUse() {
            Player owner = armyOwner();
            return owner != null && getTarget() == null && getVehicle() == null
                    && owner.level() == level() && distanceToSqr(owner) > ARMY_FOLLOW_STOP * ARMY_FOLLOW_STOP;
        }

        @Override
        public void start() {
            repathTicks = 0;
        }

        @Override
        public void tick() {
            Player owner = armyOwner();
            if (owner == null) {
                return;
            }
            getLookControl().setLookAt(owner, 30.0F, 30.0F);
            if (--repathTicks > 0) {
                return;
            }
            repathTicks = 8;
            if (distanceToSqr(owner) > 28.0 * 28.0) {
                // Left far behind: it turns up beside its summoner rather than wandering lost.
                moveTo(owner.getX() + (random.nextDouble() - 0.5) * 4.0, owner.getY(),
                        owner.getZ() + (random.nextDouble() - 0.5) * 4.0, getYRot(), 0.0F);
                getNavigation().stop();
            } else {
                // An unhurried walk while it is close, and a quicker one only once it has really fallen behind.
                getNavigation().moveTo(owner, distanceToSqr(owner) > ARMY_HURRY_DISTANCE * ARMY_HURRY_DISTANCE
                        ? 1.1 : ARMY_WALK_SPEED);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    /**
     * Picks the army ogre's enemy: whatever last hurt its summoner, or what its summoner last hit, or
     * else the nearest hostile monster. Never a player, and never another ogre (except the Plague).
     */
    private final class ArmyTargetGoal extends TargetGoal {
        @Nullable
        private LivingEntity candidate;

        private ArmyTargetGoal() {
            super(OgreGrunt.this, false);
        }

        @Override
        public boolean canUse() {
            Player owner = armyOwner();
            if (owner == null || tickCount % 5 != 0) {
                return false;
            }
            candidate = pick(owner);
            return candidate != null;
        }

        private LivingEntity pick(Player owner) {
            LivingEntity attacker = owner.getLastHurtByMob();
            if (usable(attacker) && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200) {
                return attacker;
            }
            LivingEntity victim = owner.getLastHurtMob();
            if (usable(victim) && worthFighting(victim) && owner.tickCount - owner.getLastHurtMobTimestamp() < 200) {
                return victim;
            }
            return level().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(16.0, 6.0, 16.0),
                            mob -> worthFighting(mob)).stream()
                    .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(OgreGrunt.this)))
                    .orElse(null);
        }

        private boolean usable(@Nullable LivingEntity target) {
            return target != null && target.isAlive() && target != OgreGrunt.this && OgreGrunt.this.canAttack(target);
        }

        /** What an army ogre picks a fight with unprovoked: real threats, not neutral mobs, livestock or anyone it must spare. */
        private boolean worthFighting(LivingEntity target) {
            if (!usable(target) || spareByArmy(target)) {
                return false;
            }
            if (target instanceof OgreGrunt) {
                return true;
            }
            if (target instanceof NeutralMob neutral) {
                return neutral.isAngry();
            }
            if (target instanceof AbstractPiglin piglin) {
                return piglin.getTarget() != null;
            }
            return target instanceof Enemy || isTerritorialInvaderType(target)
                    || target instanceof MudsnoutBoar boar && boar.getTarget() != null;
        }

        @Override
        public void start() {
            setTarget(candidate);
            super.start();
        }
    }
    private static final UUID BITE_ARMOR_PIERCE_MODIFIER_ID =
            ItemAttributes.uuid("ogre_grunt_bite_armor_pierce");
    private static final UUID BLACKHIDE_ARMOR_MODIFIER_ID =
            ItemAttributes.uuid("ogre_grunt_blackhide_armor");
    private static final UUID BLACKHIDE_KNOCKBACK_MODIFIER_ID =
            ItemAttributes.uuid("ogre_grunt_blackhide_knockback");
    private static final UUID BLACKHIDE_SPEED_MODIFIER_ID =
            ItemAttributes.uuid("ogre_grunt_blackhide_speed");
    private static final EntityDataAccessor<Integer> DATA_GRUNT_ATTACK_ID =
            SynchedEntityData.defineId(OgreGrunt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_GRUNT_ATTACK_START_TICK =
            SynchedEntityData.defineId(OgreGrunt.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_BOOSTED_MOVEMENT =
            SynchedEntityData.defineId(OgreGrunt.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_MOVEMENT_STATE =
            SynchedEntityData.defineId(OgreGrunt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ARMOR_VARIANT =
            SynchedEntityData.defineId(OgreGrunt.class, EntityDataSerializers.INT);
    private static final String ARMOR_VARIANT_TAG = "BlackhideArmorVariant";
    private static final int ARMOR_VARIANT_COUNT = 5;
    // The blackhide armor looks (hat, boots, chestplate, full set) are shelved, not deleted. While
    // this is false every grunt is plain, including grunts loaded from older saves.
    private static final boolean BLACKHIDE_VARIANTS_ENABLED = false;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RARE_IDLE = RawAnimation.begin().then("idle2", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("upward smack");
    private static final RawAnimation ATTACK_2 = RawAnimation.begin().thenPlay("bite");
    // The plain Grunt's own set: idle, walk and combat walk, with every attack thrown over the walk.
    private static final RawAnimation COMBAT_WALK = RawAnimation.begin().thenLoop("combat walk");
    private static final RawAnimation SMACK_1 = RawAnimation.begin().thenPlay("regular attack 1");
    private static final RawAnimation SMACK_2 = RawAnimation.begin().thenPlay("regular attack 2");
    // A bone club swings its own version of smack 1; the timing is the same.
    private static final RawAnimation BONE_SMACK = RawAnimation.begin().thenPlay("bone regular attack 3");
    // Plain Grunt attack ids: 1 and 3 are the two smacks (right arm, left arm), 2 the bite.
    private static final int ATTACK_ID_SMACK = 1;
    private static final int ATTACK_ID_BITE = 2;
    private static final int ATTACK_ID_SMACK_2 = 3;
    private static final int SMACK_ANIMATION_TICKS = 45;
    private static final int PLAIN_BITE_ANIMATION_TICKS = 30;
    // The clip reaches the client after a network hop and a 4-tick blend-in, so a hit thrown on the
    // clip's own tick landed while the arm was still coming down; this much lag puts it on the swing.
    protected static final int CLIP_LAG_TICKS = 6;
    /**
     * Idle sounds for every ogre that has one. Vanilla waits this long and then rolls a rising chance
     * each tick, which adds about two seconds, so 860 ticks comes to roughly 45 seconds between sounds.
     */
    protected static final int IDLE_SOUND_INTERVAL_TICKS = 860;
    // Bite lands 0.67s into its clip, the smacks 1s into theirs.
    private static final int PLAIN_BITE_IMPACT_TICKS = 13 + CLIP_LAG_TICKS;
    private static final int SMACK_IMPACT_TICKS = 20 + CLIP_LAG_TICKS;
    // A swing is thrown while he keeps walking, so the recovery has to outlast the clip.
    private static final int SMACK_RECOVERY_TICKS = 65;
    private static final int PLAIN_BITE_RECOVERY_TICKS = 50;
    /** How long the last pose of a swing takes to ease out once it ends. Read by the model. */
    public static final float ATTACK_OVERLAY_FADE_OUT_TICKS = 4.0F;
    // About 0.22 against the 0.255 attribute, a little above the Brute's 0.19 combat walk.
    private static final double COMBAT_WALK_SPEED = 0.86;
    private static final double COMBAT_WALK_TARGET_GAP = 1.3;
    private static final double COMBAT_WALK_MAX_VERTICAL_DIFFERENCE = 3.0;
    // The smack's fist comes down a short way in front of him, at the arm he swung.
    private static final double SMACK_REACH = 2.4;
    private static final double SMACK_STRIKE_START = 0.8;
    private static final double SMACK_SIDE_OFFSET = 0.4;
    private static final double SMACK_STRIKE_RADIUS = 1.1;
    private static final float SMACK_RING_RADIUS = 2.4F;
    private static final double SMACK_CARRY_PUSH = 0.6;
    private static final double SMACK_CARRY_LIFT = 0.2;
    private static final double SMACK_CLOSE_RANGE = 3.5;
    // Animation ticks where a foot lands, from the leg keyframes.
    private static final Map<String, double[]> FOOT_CONTACTS = Map.of(
            "walk", new double[]{0.0, 20.0},
            "combat walk", new double[]{0.0, 12.74});
    // Diamond armor's reduction curve eats most flat damage increases, so these raw values are
    // calibrated against full diamond armor (20 armor / 8 toughness) rather than picked arbitrarily.
    private static final float UPPER_SMACK_DAMAGE = OgreDamage.raw(OgreDamage.Tier.GRUNT, OgreDamage.Blow.STANDARD);
    private static final float BITE_DAMAGE = OgreDamage.raw(OgreDamage.Tier.GRUNT, OgreDamage.Blow.STANDARD);
    private static final float BITE_ARMOR_PIERCE_FRACTION = 0.15F;
    /** Regeneration II for four seconds: about four health back per bite. */
    private static final int BITE_REGENERATION_TICKS = 80;
    private static final double GRUNT_ATTACK_START_RANGE = 2.5;
    private static final double PLAIN_GRUNT_ATTACK_START_RANGE = 3.0;
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
    private static final double SUPERIOR_GUARD_SEARCH_RANGE = 24.0;
    private static final double SUPERIOR_GUARD_START_RANGE = 12.0;
    private static final double SUPERIOR_GUARD_STOP_RANGE = 9.0;
    private static final double SUPERIOR_GUARD_SPEED = 1.0;
    private static final double GRUNT_WALK_SPEED = 0.4;
    // Far from a superior or from camp they go on the combat walk; once they are close they walk.
    private static final double GRUNT_FOLLOW_WALK_SPEED = 0.45;
    private static final double FOLLOW_FAR_DISTANCE = 14.0;
    private static final double CAMP_FAR_DISTANCE = 16.0;
    // Navigation goals take a multiplier, not a raw movement speed. Against the default
    // 0.255024 movement attribute this produces an effective pursuit speed of about 0.3036 -
    // slightly below the Captain's 0.35, so the Captain reads as the faster pursuer.
    private static final double GRUNT_CHASE_SPEED = 0.3036 / 0.255024;
    private static final int MOVEMENT_IDLE = 0;
    private static final int MOVEMENT_WALKING = 1;
    private static final int MOVEMENT_CHASING = 2;
    private static final int MOVEMENT_ATTACKING = 3;
    // Block-ripple visual (e.g. brute temper tantrum) outward propagation speed and per-block duration.
    private static final int RIPPLE_RING_DELAY_TICKS = 2;
    private static final int RIPPLE_BLOCK_VISIBLE_TICKS = 14;
    // Never speed the walk/run cycle up past its authored pace - only ever slow it down to match
    // actual (water-dragged) movement, and never let it stall out to a dead stop mid-stride.
    private static final float MIN_MOVEMENT_ANIM_SPEED = 0.55F;
    private static final float MAX_MOVEMENT_ANIM_SPEED = 1.0F;
    private static final double SHALLOW_WATER_SPEED_FACTOR = 0.5;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int idleAnimationTicks;
    private int idleVariantTicks;
    private int lastStepSoundTick = -1_000_000;
    private int lastAnimationTick = -1;
    private long lastMovingAnimationTick = Long.MIN_VALUE / 2;
    private static final int MOVEMENT_ANIMATION_GRACE_TICKS = 4;
    private int activeGruntAttackId;
    private final CanonicalOneShotState visualOneShot = new CanonicalOneShotState();
    private final CanonicalOneShotState visualOverlayOneShot = new CanonicalOneShotState();
    private int smackStrikeDelayTicks = -1;
    private int lastSmackId;
    private boolean attackOverlayVisible;
    private final List<ActiveGroundRipple> activeGroundRipples = new ArrayList<>();
    private boolean royalPatrolSquadLeader;
    private boolean outpostReinforcementSquadLeader;
    // Spawned by one of the King's royal trials; those fights are never part of the peace.
    private boolean royalTrialOgre;
    private int royalPatrolGruntsSpawned;
    private int royalPatrolArchersSpawned;
    @Nullable
    private BlockPos outpostHome;
    // A hole this ogre's pathfinding treats as solid (the quarry pit); radius 0 means none.
    private int noGoX;
    private int noGoZ;
    private int noGoRadius;
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
    private int merchantMissingTicks;
    @Nullable
    private UUID merchantThreatId;
    private int merchantThreatTicks;

    public OgreGrunt(EntityType<? extends OgreGrunt> type, Level level) {
        super(type, level);
        if (BLACKHIDE_VARIANTS_ENABLED && !level.isClientSide && getClass() == OgreGrunt.class) {
            int armorRoll = getRandom().nextInt(10);
            setArmorVariant(armorRoll < 6 ? 0 : armorRoll - 5);
        }
        if (usesReferenceGroundMovement()) {
            this.moveControl = new OgreGruntMoveControl(this);
        }
        setPathfindingMalus(BlockPathTypes.LEAVES, 0.0F);
        // Pointed dripstone and similar damaging floor nodes can strand the Ogre's wide collision
        // box and cause repeated path recalculation/turning. Never select them as walk nodes.
        setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, -1.0F);
        // Block paths through fire and penalize nearby tiles so paths give flames room.
        setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0F);
        setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16.0F);
        // Grunts keep vanilla's discouraged-but-crossable water behavior so they can fight in one
        // block of shallow water. Larger and ranged subclasses opt into fully blocked water nodes.
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return usesReferenceGroundMovement()
                ? new OgreGruntNavigation(this, level)
                : new NoGoGroundNavigation(this, level);
    }

    /** Keeps this ogre's paths out of its no-go zone, if it has one. */
    public void setNoGoZone(int x, int z, int radius) {
        noGoX = x;
        noGoZ = z;
        noGoRadius = Math.max(radius, 0);
    }

    boolean isNoGoColumn(int x, int z) {
        if (noGoRadius <= 0) return false;
        long dx = x - noGoX;
        long dz = z - noGoZ;
        return dx * dx + dz * dz <= (long) noGoRadius * noGoRadius;
    }

    private boolean usesReferenceGroundMovement() {
        Class<?> type = getClass();
        return type == OgreGrunt.class
                || type == PlagueOgre.class
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
        setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
    }

    // ------------------------------------------------------------------ //
    //  Attributes                                                        //
    // ------------------------------------------------------------------ //

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.ATTACK_DAMAGE, 10.5)
                .add(Attributes.MOVEMENT_SPEED, 0.255024)
                .add(Attributes.FOLLOW_RANGE, 30.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55)
                .add(Attributes.ATTACK_KNOCKBACK, 0.9);
    }

    @Override
    @SuppressWarnings("deprecation")
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData spawnData, @Nullable CompoundTag spawnTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, spawnTag);
        if (getClass() == OgreGrunt.class && getRandom().nextFloat() < BONE_CLUB_SPAWN_CHANCE) {
            ItemStack club = new ItemStack(ModItems.BONE_CLUB.get());
            club.setDamageValue(Mth.nextInt(getRandom(), club.getMaxDamage() / 2, club.getMaxDamage() * 4 / 5));
            setItemSlot(EquipmentSlot.MAINHAND, club);
            setDropChance(EquipmentSlot.MAINHAND, BONE_CLUB_DROP_CHANCE);
        }
        return result;
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
        setMaxUpStep(Config.OGRE_GRUNT_STEP_HEIGHT.get().floatValue());
        configuredSafeFallDistance = Config.OGRE_GRUNT_SAFE_FALL_DISTANCE.get().intValue();

        if (healToMax) {
            setHealth(getMaxHealth());
        } else if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    protected void setAttributeBaseValue(Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private int configuredSafeFallDistance = 4;

    @Override
    public int getMaxFallDistance() {
        return configuredSafeFallDistance;
    }

    protected void applyConfiguredHealth(boolean healToMax) {
        if (healToMax) {
            setHealth(getMaxHealth());
        } else if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_GRUNT_ATTACK_ID, 0);
        this.entityData.define(DATA_GRUNT_ATTACK_START_TICK, 0L);
        this.entityData.define(DATA_BOOSTED_MOVEMENT, false);
        this.entityData.define(DATA_MOVEMENT_STATE, MOVEMENT_IDLE);
        this.entityData.define(DATA_ARMOR_VARIANT, 0);
    }

    public int getArmorVariant() {
        return entityData.get(DATA_ARMOR_VARIANT);
    }

    public int getBlackhidePieceCount() {
        return switch (getArmorVariant()) {
            case 1, 2 -> 1;
            case 3 -> 2;
            case 4 -> 3;
            default -> 0;
        };
    }

    private void setArmorVariant(int variant) {
        entityData.set(DATA_ARMOR_VARIANT, Mth.clamp(variant, 0, ARMOR_VARIANT_COUNT - 1));
        applyBlackhideBonuses();
    }

    private void applyBlackhideBonuses() {
        if (level().isClientSide || getClass() != OgreGrunt.class) {
            return;
        }

        int pieces = getBlackhidePieceCount();
        replaceBlackhideModifier(Attributes.ARMOR, BLACKHIDE_ARMOR_MODIFIER_ID,
                pieces * 2.0, AttributeModifier.Operation.ADDITION);
        replaceBlackhideModifier(Attributes.KNOCKBACK_RESISTANCE, BLACKHIDE_KNOCKBACK_MODIFIER_ID,
                pieces * 0.0375, AttributeModifier.Operation.ADDITION);
        replaceBlackhideModifier(Attributes.MOVEMENT_SPEED, BLACKHIDE_SPEED_MODIFIER_ID,
                Math.max(0, pieces - 1) * 0.05, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private void replaceBlackhideModifier(Attribute attribute, UUID id, double amount,
                                           AttributeModifier.Operation operation) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, id.toString(), amount, operation));
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            if (merchantThreatTicks > 0 && --merchantThreatTicks == 0) {
                merchantThreatId = null;
                setTarget(null);
            }
            dropAppeasedPlayerTarget();
            tickArmy();
            tickRoyalCommandTarget();
            tickMerchantGuardOrphan();
            tickOutpostPlayerDetection();
            tickOutpostCombatLeash();
            entityData.set(DATA_BOOSTED_MOVEMENT, hasRunSpeedEffect());
            updateIntendedMovementState();
            tickGroundRippleCombat();
            if (isDeadOrDying()) {
                // A swing that has not landed yet never does: no late hit or sound from a dying ogre.
                smackStrikeDelayTicks = -1;
            } else {
                tickSmackStrike();
            }
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
        } else if (getTarget() != null && isAggressive() || runningToSuperior
                || returningToOutpost && (!usesCombatWalkSystem() || isFarFromCamp())) {
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

    // ------------------------------------------------------------------ //
    //  AI goals                                                          //
    // ------------------------------------------------------------------ //

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        registerErrandTalkGoal();
        registerBogToadWarinessGoal();
        meleeGoal = new OgreGruntMeleeGoal(this, getMeleePursuitSpeedModifier());
        goalSelector.addGoal(1, meleeGoal);
        if (!(this instanceof PlagueOgre)) {
            goalSelector.addGoal(3, new FollowAssignedMerchantGoal(this));
            goalSelector.addGoal(4, new GuardSuperiorGoal(this));
        }
        goalSelector.addGoal(5, createWanderGoal());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        if (this instanceof PlagueOgre) {
            // Infection seeks healthy Ogres first, even when a player drew the Plague Ogre
            // toward an outpost. Players remain fallback prey and can still provoke it.
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, OgreGrunt.class, true,
                    target -> !(target instanceof PlagueOgre)
                            // A King asleep or holding court cannot be hurt, so it is not worth chasing.
                            && !(target instanceof OgreLord king && !king.isFortEncounterActive())));
            targetSelector.addGoal(2, new HurtByTargetGoal(this));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        } else {
            targetSelector.addGoal(1, new HurtByTargetGoal(this));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
            registerTerritorialTargetGoal();
        }
        registerArmyGoals();
    }

    /**
     * Rank-and-file ogres are wary of Bog Toads but not smart about it: each encounter they roll
     * their nerve, and plenty stand their ground (or are already hunting the toad), which is how
     * the swamp plague keeps finding new hosts. Call from every registerGoals override.
     */
    protected final void registerBogToadWarinessGoal() {
        if (!(this instanceof PlagueOgre || this instanceof OgreLord || isCaptainRank())) {
            goalSelector.addGoal(0, new BogToadWarinessGoal());
        }
    }

    private final class BogToadWarinessGoal extends AvoidEntityGoal<BogToad> {
        private static final int NERVE_ROLL_INTERVAL_TICKS = 200;
        private long nerveRolledUntil;
        private boolean brave;

        BogToadWarinessGoal() {
            super(OgreGrunt.this, BogToad.class, 12.0F, 1.0, 1.35);
        }

        @Override
        public boolean canUse() {
            // A mounted archer cannot flee independently of its boar. Let the mount's combat goal
            // position the pair and leave the rider's mounted shoot goal free to engage the toad.
            if (isPassenger()) return false;
            // The toad search is an entity query; staggering it keeps a crowd of ogres cheap.
            if ((tickCount + getId()) % 8 != 0 || isMerchantGuard() || !super.canUse()) return false;
            long now = level().getGameTime();
            if (now >= nerveRolledUntil) {
                nerveRolledUntil = now + NERVE_ROLL_INTERVAL_TICKS;
                int allies = level().getEntitiesOfClass(OgreGrunt.class, getBoundingBox().inflate(8.0),
                        other -> other != OgreGrunt.this && other.isAlive() && !(other instanceof PlagueOgre)).size();
                // Only an ogre on its own is afraid: with any other ogre beside it, it stands firm.
                // Wounds still break its nerve.
                double braveChance = allies > 0 ? 1.0 : 0.35;
                if (getHealth() < getMaxHealth() * 0.4F) braveChance = 0.0;
                brave = getRandom().nextDouble() < braveChance;
            }
            return !brave;
        }
    }

    protected final void registerTerritorialTargetGoal() {
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, true, this::isTerritorialInvader));
        if (isCaptainRank()) {
            // Captains always take on the swamp's infected, ahead of anything else on the field.
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                    this, LivingEntity.class, true, this::isSwampInfection));
        } else if (!(this instanceof OgreLord)) {
            targetSelector.addGoal(2, new JoinCaptainFightGoal());
        }
    }

    /** The captains: the Grunt Captain, the Brute and the Mage. */
    protected final boolean isCaptainRank() {
        return this instanceof OgreGruntCaptain || this instanceof OgreBrute || this instanceof OgreMage;
    }

    private boolean isSwampInfection(LivingEntity target) {
        return !isMerchantGuard() && target.isAlive() && (target instanceof PlagueOgre || target instanceof BogToad);
    }

    /** A lesser ogre that sees a captain fighting a Plague Ogre or a Bog Toad joins in against it. */
    private final class JoinCaptainFightGoal extends Goal {
        private static final double JOIN_RANGE = 20.0;
        private LivingEntity enemy;

        private JoinCaptainFightGoal() {
            setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            // An entity query, so look only about twice a second and only while idle.
            if ((tickCount + getId()) % 10 != 0 || getTarget() != null || isMerchantGuard()) {
                return false;
            }
            for (OgreGrunt captain : level().getEntitiesOfClass(OgreGrunt.class,
                    getBoundingBox().inflate(JOIN_RANGE),
                    other -> other != OgreGrunt.this && other.isAlive() && other.isCaptainRank()
                            && other.getTarget() != null && isSwampInfection(other.getTarget()))) {
                LivingEntity foe = captain.getTarget();
                if (canAttack(foe) && (hasLineOfSight(captain) || hasLineOfSight(foe))) {
                    enemy = foe;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            setTarget(enemy);
        }
    }

    private boolean isTerritorialInvader(LivingEntity target) {
        // Every ogre in the camp turns on an infected one, not just the captains.
        return !isMerchantGuard() && (isTerritorialInvaderType(target) || target instanceof PlagueOgre);
    }

    /** What the Tribe treats as an intruder on its land; a boar carrying an ogre jockey hunts the same things. */
    public static boolean isTerritorialInvaderType(LivingEntity target) {
        return !(target instanceof OgreGrunt)
                && (target instanceof Zombie
                || target instanceof AbstractSkeleton
                || target instanceof Spider
                || target instanceof Slime
                || target instanceof Raider
                || target instanceof Warden
                || target instanceof BogToad);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        // An army ogre never turns on players, its summoner's pets, or its own army's boars.
        if (armyOwnerId != null && (spareByArmy(target)
                || target instanceof MudsnoutBoar boar && armyOwnerId.equals(boar.getArmyOwnerId())
                || target instanceof OwnableEntity pet
                        && armyOwnerId.equals(pet.getOwnerUUID()))) {
            return false;
        }
        return (!isMerchantGuard() || target.getUUID().equals(merchantThreatId)
                    || target == getLastHurtByMob())
                && canAttackOgreTarget(target)
                && !tribeIsAppeasedWith(target)
                && super.canAttack(target);
    }

    private int trapHeldUntil;

    /** A Bogjaw Trap has clamped this ogre: for the hold it cannot walk, lunge or be pushed along the ground. */
    public void holdInTrap(int ticks) {
        trapHeldUntil = tickCount + ticks;
    }

    public boolean isHeldInTrap() {
        return tickCount < trapHeldUntil;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isHeldInTrap()) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(0.0, Math.min(motion.y, 0.0), 0.0);
            getNavigation().stop();
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    private static final float STRAY_FORGIVEN_FRACTION = 0.25F;
    private static final long STRAY_MEMORY_TICKS = 20L * 30;
    private UUID strayHitterId;
    private float strayDamage;
    private long strayDamageTime;
    private boolean fightsToTheDeath;

    /** Counts what an appeased player has done to this ogre by accident, and turns it on them past a quarter of its health. */
    private void noteStrayHit(ServerLevel server, ServerPlayer hitter, float amount) {
        long now = server.getGameTime();
        if (!hitter.getUUID().equals(strayHitterId) || now - strayDamageTime > STRAY_MEMORY_TICKS) {
            strayHitterId = hitter.getUUID();
            strayDamage = 0.0F;
        }
        strayDamage += amount;
        strayDamageTime = now;
        if (strayDamage >= getMaxHealth() * STRAY_FORGIVEN_FRACTION) {
            fightsToTheDeath = true;
        }
    }

    /**
     * Clear appeased players here because target goals only recheck targets they picked.
     * A mount, boar, or alert can also supply a target.
     */
    private void dropAppeasedPlayerTarget() {
        if (tickCount % 10 == 0 && getTarget() instanceof Player player && tribeIsAppeasedWith(player)) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    /**
     * An appeased tribe leaves players alone, and so does one that sees a Gold Coin held out
     * while its anger is still low. Whoever just struck this ogre is the exception. The Plague
     * Ogres and the awake King are not part of the peace.
     */
    private boolean tribeIsAppeasedWith(LivingEntity target) {
        if (!(target instanceof Player player)
                || this instanceof PlagueOgre || this instanceof OgreLord || royalTrialOgre
                || !(level() instanceof ServerLevel server)
                || TributeManager.isProvoked(server, player.getUUID())) {
            return false;
        }
        // Someone the tribe likes who hits an ogre by accident is forgiven until that ogre has lost a
        // quarter of its health to them; after that it fights them to the death, and never drops the
        // fight and picks it up again.
        if (player.getUUID().equals(strayHitterId) && fightsToTheDeath) {
            return false;
        }
        if (target == getLastHurtByMob() && !TributeManager.isAppeased(server, player.getUUID())) {
            return false;
        }
        if (TributeManager.isAppeased(server, player.getUUID())) {
            return true;
        }
        if (PatrolTier.fromAnger(TributeManager.getFactionAngerLevel(server))
                != PatrolTier.SMALL) {
            return false;
        }
        if (TributeManager.hasTollPass(server, player.getUUID())) {
            return true;
        }
        ItemStack coin = player.getMainHandItem().is(ModItems.GOLD_COIN.get())
                ? player.getMainHandItem() : player.getOffhandItem();
        if (!coin.is(ModItems.GOLD_COIN.get())) {
            return false;
        }
        // From afar a held coin is only respected; the toll is taken once an ogre is face to face.
        if (player instanceof ServerPlayer payer
                && distanceToSqr(player) <= TOLL_RANGE * TOLL_RANGE && hasLineOfSight(player)) {
            takeToll(payer, coin);
        }
        return true;
    }

    /** The first ogre to meet a player holding a coin takes it, and the tribe stands aside for a while. */
    private void takeToll(ServerPlayer payer, ItemStack coin) {
        if (!payer.getAbilities().instabuild) {
            coin.shrink(1);
        }
        TributeManager.grantTollPass(payer.serverLevel(), payer.getUUID());
        RoyalAdvancementEvents.awardPath(payer, "tribute/toll_paid");
        playAmbientSound();
        payer.sendSystemMessage(Component.empty()
                .append(getDisplayName().copy().withStyle(ChatFormatting.GOLD))
                .append(Component.literal(": ")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.translatable(
                                "dialogue.kingsandmonsters.ogre_toll." + (1 + getRandom().nextInt(TOLL_LINES)))
                        .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC)));
    }

    /** For captains summoned to the King's side: they fight whatever the tribe thinks of the player. */
    public void markRoyalTrialOgre() {
        royalTrialOgre = true;
    }

    /** Royal Trial ogres are always hostile, so fighting them never counts against a player's peace. */
    public boolean isRoyalTrialOgre() {
        return royalTrialOgre;
    }

    private static final long ERRAND_TALK_MAX_TICKS = 20L * 60;
    private static final double ERRAND_TALK_HOLD_RANGE = 10.0;
    @Nullable private UUID errandTalkPlayer;
    private long errandTalkUntil;

    /** A captain stops where he stands and faces whoever is talking to him. */
    public void beginErrandTalk(ServerPlayer player) {
        errandTalkPlayer = player.getUUID();
        errandTalkUntil = level().getGameTime() + ERRAND_TALK_MAX_TICKS;
        // He turns to the player through ErrandTalkGoal's look control rather than snapping round.
        getNavigation().stop();
    }

    public void endErrandTalk(ServerPlayer player) {
        if (player.getUUID().equals(errandTalkPlayer)) {
            errandTalkPlayer = null;
        }
    }

    @Nullable
    private Player errandTalkPartner() {
        if (errandTalkPlayer == null) {
            return null;
        }
        Player partner = level().getPlayerByUUID(errandTalkPlayer);
        if (partner == null || level().getGameTime() >= errandTalkUntil || getTarget() != null
                || partner.distanceToSqr(this) > ERRAND_TALK_HOLD_RANGE * ERRAND_TALK_HOLD_RANGE) {
            errandTalkPlayer = null;
            return null;
        }
        return partner;
    }

    protected final void registerErrandTalkGoal() {
        goalSelector.addGoal(0, new ErrandTalkGoal());
    }

    /** Holds him still and looking at the player for as long as the conversation lasts. */
    private final class ErrandTalkGoal extends Goal {
        private ErrandTalkGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return errandTalkPartner() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return errandTalkPartner() != null;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            Player partner = errandTalkPartner();
            if (partner != null) {
                getNavigation().stop();
                getLookControl().setLookAt(partner, 60.0F, 60.0F);
            }
        }
    }

    private static final double ERRAND_TALK_RANGE = 6.0;

    /** A captain who is not fighting and is at peace with the player will talk about errands. */
    public boolean canGiveErrandsTo(ServerPlayer player) {
        return isCaptainRank() && !(this instanceof PlagueOgre) && !royalTrialOgre && isAlive()
                && getTarget() == null && distanceToSqr(player) <= ERRAND_TALK_RANGE * ERRAND_TALK_RANGE
                && tribeIsAppeasedWith(player);
    }

    /** A captain takes the coins held out to him as tribute, or, empty-handed, offers an errand. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (isCaptainRank() && !(this instanceof PlagueOgre) && !held.is(ModItems.GOLD_COIN.get())
                && !player.isSecondaryUseActive() && player instanceof ServerPlayer talker
                && canGiveErrandsTo(talker)) {
            QuestManager.open(talker, this);
            return InteractionResult.SUCCESS;
        }
        if (!isCaptainRank() || this instanceof PlagueOgre || !held.is(ModItems.GOLD_COIN.get())) {
            return super.mobInteract(player, hand);
        }
        if (player instanceof ServerPlayer giver) {
            TributeManager.offerCoins(giver, held);
            playSound(SoundEvents.ARMOR_EQUIP_GOLD, 1.0F, 0.8F);
        }
        return InteractionResult.SUCCESS;
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
        // Limit pursuit so players can't kite a resident indefinitely.
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

        int radius = outpostHomeRadius();
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

    /** A camp with a pit is the quarry; its crew keeps tighter to the walls than other camps do. */
    private int outpostHomeRadius() {
        return noGoRadius > 0 ? QUARRY_HOME_RADIUS : OUTPOST_HOME_RADIUS;
    }

    private boolean isFarFromCamp() {
        return outpostHome != null
                && horizontalDistanceSqr(outpostHome, blockPosition()) > (long) (CAMP_FAR_DISTANCE * CAMP_FAR_DISTANCE);
    }

    private void navigateBackToOutpost() {
        getNavigation().moveTo(
                outpostHome.getX() + 0.5,
                outpostHome.getY(),
                outpostHome.getZ() + 0.5,
                // Grunts and Captains head home on the combat walk while they are far, then walk.
                usesCombatWalkSystem()
                        ? (isFarFromCamp() ? getCombatWalkSpeedModifier() : GRUNT_FOLLOW_WALK_SPEED)
                        : getCombatWalkSpeedModifier());
    }

    private void tickOutpostPlayerDetection() {
        if (outpostHome == null || returningToOutpost || getTarget() != null || tickCount % 10 != 0
                || horizontalDistanceSqr(outpostHome, blockPosition())
                > (long) outpostHomeRadius() * outpostHomeRadius()) {
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
        super.setTarget(target == null || !canAttack(target) ? null : target);
    }

    protected boolean canAttackOgreTarget(LivingEntity target) {
        return target instanceof PlagueOgre || !(target instanceof OgreGrunt) || armyFeud(target);
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

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Its summoner cannot hurt it by accident.
        if (armyOwnerId != null && source.getEntity() instanceof Player attacker
                && attacker.getUUID().equals(armyOwnerId)) {
            return false;
        }
        if (getClass() == OgreGrunt.class
                && source.getDirectEntity() instanceof AbstractArrow
                && getRandom().nextFloat() < 0.075F * getBlackhidePieceCount()) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && amount > 0.0F && armyOwnerId == null && source.getEntity() instanceof ServerPlayer hitter
                && level() instanceof ServerLevel hurtLevel && !(this instanceof PlagueOgre)
                && !(this instanceof OgreLord) && !royalTrialOgre
                && TributeManager.isAppeased(hurtLevel, hitter.getUUID())
                && !TributeManager.isProvoked(hurtLevel, hitter.getUUID())) {
            noteStrayHit(hurtLevel, hitter, amount);
        }
        if (hurt && armyOwnerId == null && source.getEntity() instanceof OgreGrunt striker && striker.isArmyAlly()) {
            rallyTribeAgainst(striker);
        }
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
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        // Only ordinary Grunts can become Plague Ogres. Converting instead of running the
        // death/loot flow prevents one infected Grunt from producing both its drops and a mob.
        MobEffectInstance bogrot = getEffect(ModMobEffects.BOGROT.get());
        if (getClass() == OgreGrunt.class
                && level() instanceof ServerLevel server
                && bogrot != null
                && bogrot.getAmplifier() >= 1) {
            PlagueOgre plague = ModEntities.PLAGUE_OGRE.get().create(server);
            if (plague != null) {
                plague.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                plague.setCustomName(getCustomName());
                plague.setCustomNameVisible(isCustomNameVisible());
                if (server.addFreshEntity(plague)) {
                    double burstY = getY() + getBbHeight() * 0.55;
                    server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                            getX(), burstY, getZ(), 28, 0.65, 0.9, 0.65, 0.02);
                    server.sendParticles(ParticleTypes.SMOKE,
                            getX(), burstY, getZ(), 16, 0.45, 0.7, 0.45, 0.025);
                    server.playSound(null, getX(), getY(), getZ(),
                            SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 1.0F, 0.72F);
                    discard();
                    return;
                }
            }
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(ROYAL_PATROL_SQUAD_TAG, royalPatrolSquadLeader);
        tag.putBoolean(OUTPOST_REINFORCEMENT_SQUAD_TAG, outpostReinforcementSquadLeader);
        tag.putBoolean(ROYAL_TRIAL_OGRE_TAG, royalTrialOgre);
        if (fightsToTheDeath && strayHitterId != null) {
            tag.putUUID("StrayHitter", strayHitterId);
        }
        tag.putInt(ROYAL_PATROL_GRUNTS_SPAWNED_TAG, royalPatrolGruntsSpawned);
        tag.putInt(ROYAL_PATROL_ARCHERS_SPAWNED_TAG, royalPatrolArchersSpawned);
        if (outpostHome != null) {
            tag.putLong(OUTPOST_HOME_TAG, outpostHome.asLong());
            tag.putBoolean(OUTPOST_CAPTAIN_TAG, outpostCaptain);
        }
        if (noGoRadius > 0) {
            tag.putIntArray("KingsAndMonstersNoGoZone", new int[]{noGoX, noGoZ, noGoRadius});
        }
        if (merchantOwnerId != null) tag.putUUID(MERCHANT_OWNER_TAG, merchantOwnerId);
        tag.putInt(ARMOR_VARIANT_TAG, getArmorVariant());
        if (armyOwnerId != null) {
            tag.putUUID(ARMY_OWNER_TAG, armyOwnerId);
            tag.putInt(ARMY_TICKS_TAG, armyTicksLeft);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        royalPatrolSquadLeader = tag.getBoolean(ROYAL_PATROL_SQUAD_TAG);
        outpostReinforcementSquadLeader = tag.getBoolean(OUTPOST_REINFORCEMENT_SQUAD_TAG);
        royalTrialOgre = tag.getBoolean(ROYAL_TRIAL_OGRE_TAG);
        strayHitterId = tag.hasUUID("StrayHitter") ? tag.getUUID("StrayHitter") : null;
        fightsToTheDeath = strayHitterId != null;
        armyOwnerId = tag.hasUUID(ARMY_OWNER_TAG) ? tag.getUUID(ARMY_OWNER_TAG) : null;
        armyTicksLeft = tag.getInt(ARMY_TICKS_TAG);
        royalPatrolGruntsSpawned = Mth.clamp(tag.getInt(ROYAL_PATROL_GRUNTS_SPAWNED_TAG),
                0, getRoyalPatrolGruntCount());
        royalPatrolArchersSpawned = Mth.clamp(tag.getInt(ROYAL_PATROL_ARCHERS_SPAWNED_TAG),
                0, getRoyalPatrolArcherCount());
        if (tag.contains(OUTPOST_HOME_TAG)) {
            assignOutpostResident(BlockPos.of(tag.getLong(OUTPOST_HOME_TAG)),
                    tag.getBoolean(OUTPOST_CAPTAIN_TAG));
        }
        int[] noGo = tag.getIntArray("KingsAndMonstersNoGoZone");
        if (noGo.length == 3) {
            setNoGoZone(noGo[0], noGo[1], noGo[2]);
        }
        merchantOwnerId = tag.hasUUID(MERCHANT_OWNER_TAG) ? tag.getUUID(MERCHANT_OWNER_TAG) : null;
        if (BLACKHIDE_VARIANTS_ENABLED && tag.contains(ARMOR_VARIANT_TAG, Tag.TAG_INT)) {
            setArmorVariant(tag.getInt(ARMOR_VARIANT_TAG));
        }
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
        setCustomName(Component.literal("Merchant Guard"));
        setCustomNameVisible(false);
        setPersistenceRequired();
        setTarget(null);
    }

    /**
     * The merchant clears his own guards when he leaves or dies, but only the ones that are loaded.
     * A guard whose merchant has been gone for two minutes was left behind, so he leaves too.
     */
    private void tickMerchantGuardOrphan() {
        if (merchantOwnerId == null || tickCount % 20 != 0 || !(level() instanceof ServerLevel server)) {
            return;
        }
        if (server.getEntity(merchantOwnerId) instanceof OgreMerchant merchant && merchant.isAlive()) {
            merchantMissingTicks = 0;
        } else if ((merchantMissingTicks += 20) >= 20 * 120) {
            discard();
        }
    }

    public boolean isMerchantGuard() { return merchantOwnerId != null; }

    public boolean isMerchantGuardFor(UUID merchantId) {
        return merchantOwnerId != null && merchantOwnerId.equals(merchantId);
    }

    public void defendMerchantFrom(LivingEntity attacker) {
        if (!isMerchantGuard() || attacker instanceof OgreGrunt || !attacker.isAlive()) return;
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

        // The spawner search walks about 1,100 blocks, so look once a second rather than every tick.
        if ((tickCount + getId()) % 20 != 0) {
            return;
        }
        BlockPos owner = findOwningRoyalPatrolSpawner(serverLevel);
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

    /** The spawner block this squad leader came out of: any spawner within a few blocks of where it stands. */
    private BlockPos findOwningRoyalPatrolSpawner(ServerLevel serverLevel) {
        BlockPos center = blockPosition();
        for (BlockPos candidate : BlockPos.betweenClosed(
                center.offset(-6, -3, -6), center.offset(6, 3, 6))) {
            if (serverLevel.getBlockEntity(candidate) instanceof SpawnerBlockEntity) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private boolean spawnTrackedRoyalPatrolMember(ServerLevel serverLevel,
                                                   BlockPos spawnerPos,
                                                   EntityType<? extends OgreGrunt> entityType) {
        OgreGrunt member = entityType.create(serverLevel);
        if (member == null) {
            return false;
        }

        for (int attempt = 0; attempt < 20; attempt++) {
            double x = spawnerPos.getX() + 0.5 + (getRandom().nextDouble() - getRandom().nextDouble()) * 4.5;
            double y = spawnerPos.getY() + getRandom().nextInt(3) - 1;
            double z = spawnerPos.getZ() + 0.5 + (getRandom().nextDouble() - getRandom().nextDouble()) * 4.5;
            member.moveTo(x, y, z, getRandom().nextFloat() * 360.0F, 0.0F);
            if (!serverLevel.noCollision(member) || !member.checkSpawnObstruction(serverLevel)) {
                continue;
            }

            member.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(member.blockPosition()),
                    MobSpawnType.SPAWNER, null, null);
            member.setPersistenceRequired();
            return serverLevel.addFreshEntity(member);
        }
        return false;
    }

    /** Called when it joins a level (world launch, chunk reload). */
    public void onJoinedLevel() {
        // A freshly loaded mob (world launch, chunk reload) shouldn't be stuck mid-attack or aggro'd
        // on whatever it was doing at save time. Force a clean idle state and let targeting start over.
        resetTransientCombatState();
    }

    /**
     * Clears any in-progress attack/animation state left over from before this entity was
     * (re)loaded. Subclasses with their own pending-attack timers should override and call super.
     */
    public void resetTransientCombatState() {
        setTarget(null);
        setAggressive(false);
        activeGruntAttackId = 0;
        clearSyncedAttackAnimation();
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof OgreGrunt && !(entity instanceof PlagueOgre) && !armyFeud(entity)
                || armyOwnerId != null && armyOwnerId.equals(entity.getUUID())
                || super.isAlliedTo(entity);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return super.doHurtTarget(target);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.OGRE_GRUNT_HURT.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getClass() == OgreGrunt.class ? ModSoundEvents.OGRE_GRUNT_IDLE.get() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        // Grunts gather in groups, so each one should leave plenty of room for the others.
        return IDLE_SOUND_INTERVAL_TICKS;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return getClass() == OgreGrunt.class ? ModSoundEvents.OGRE_GRUNT_DEATH.get() : null;
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

    /**
     * The sound of a heavy blow landing on the ground: a mace-smash crack and the break sound of
     * whatever he hit, so a slam on stone, mud or wood each reads differently.
     */
    protected void playSlamLayers(Vec3 at, float power, float pitchShift) {
        BlockPos ground = BlockPos.containing(at.x, at.y - 0.5, at.z);
        var groundSound = level().getBlockState(ground).getSoundType(level(), ground, this);
        level().playSound(null, at.x, at.y, at.z,
                power >= 1.2F ? SoundEvents.DEEPSLATE_BRICKS_BREAK : SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.HOSTILE, 0.9F * power, 0.75F + pitchShift);
        level().playSound(null, at.x, at.y, at.z, groundSound.getBreakSound(),
                SoundSource.HOSTILE, 1.3F * power, 0.5F + pitchShift);
    }

    /** True when the ogre's footsteps are played from its walk animation instead of by distance moved. */
    protected boolean usesAnimatedFootsteps() {
        return usesCombatWalkSystem();
    }

    /**
     * The plain Grunt goes by idle, walk and combat walk, with every attack thrown over the walk,
     * like the Brute and the King. The Captain and the other Grunt-based mobs keep their own sets.
     */
    protected boolean usesCombatWalkSystem() {
        return getClass() == OgreGrunt.class || this instanceof OgreGruntCaptain;
    }

    /** Animation ticks at which a foot lands in each walk clip; a subclass with its own clips overrides this. */
    protected Map<String, double[]> getFootContacts() {
        return FOOT_CONTACTS;
    }

    /** Client-side: a dull thud each time a foot visibly lands, a lighter take on the Brute's. */
    private void playGruntFootfall() {
        if (!onGround() || isInWater() || isSilent()) {
            return;
        }
        playFootfallSounds();
    }

    /** The sound of one foot landing; a subclass with its own gait overrides this. */
    protected void playFootfallSounds() {
        float variation = getRandom().nextFloat() * 0.08F;
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.WARDEN_STEP, SoundSource.HOSTILE,
                0.45F * getFootfallVolumeScale(), (0.9F + variation) * getFootfallPitchScale(), false);
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.HOSTILE_BIG_FALL, SoundSource.HOSTILE,
                0.15F * getFootfallVolumeScale(), (0.95F + variation) * getFootfallPitchScale(), false);
        BlockPos ground = getOnPos();
        SoundType surface = level().getBlockState(ground).getSoundType(level(), ground, this);
        level().playLocalSound(getX(), getY(), getZ(), surface.getStepSound(), SoundSource.HOSTILE,
                0.45F * getFootfallVolumeScale(), (0.8F + variation) * getFootfallPitchScale(), false);
    }

    /** Louder footsteps for a bigger ogre. */
    protected float getFootfallVolumeScale() {
        return 1.0F;
    }

    /** Lower than 1 for a deeper step. */
    protected float getFootfallPitchScale() {
        return 1.0F;
    }

    /**
     * How much of an area attack's damage reaches something this far from its centre: all of it
     * through the inner part, fading down to {@code edgeShare} at the rim.
     */
    protected static float areaFalloff(double distance, double radius, float edgeShare) {
        return OgreDamage.falloff(distance, radius, edgeShare);
    }

    /** Whether an attack is thrown over the combat walk. The Captain's sword salute takes the whole body. */
    protected boolean playsOverWalk(int attackId) {
        return true;
    }

    /** True on the client while a swing is playing over the walk. */
    public boolean isAttackOverlayVisible() {
        return attackOverlayVisible;
    }

    @Override
    public double getBoneResetTime() {
        // Bones a clip stops keying ease back to rest over a few ticks instead of snapping there.
        return 6.0;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (usesAnimatedFootsteps()) {
            return;
        }
        if (consumeStepSoundInterval()) {
            playSurfaceStepSound(state, pos, 0.15F, 1.0F);
        }
    }

    @Override
    protected void playCombinationStepSounds(BlockState primaryState, BlockState secondaryState,
                                             BlockPos primaryPos, BlockPos secondaryPos) {
        if (usesAnimatedFootsteps() || !consumeStepSoundInterval()) {
            return;
        }
        playSurfaceStepSound(primaryState, primaryPos, 0.15F, 1.0F);
        playSurfaceStepSound(secondaryState, secondaryPos, 0.05F, 0.8F);
    }

    @Override
    protected void playMuffledStepSound(BlockState state, BlockPos pos) {
        if (!usesAnimatedFootsteps() && consumeStepSoundInterval()) {
            playSurfaceStepSound(state, pos, 0.05F, 0.8F);
        }
    }

    @Override
    public void animateHurt(float yaw) {
        // GeckoLib drives the ogre body animations; the vanilla hurt pose can snap one-shot attacks back to frame 0.
    }

    // ------------------------------------------------------------------ //
    //  Animation controllers                                             //
    // ------------------------------------------------------------------ //

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Short crossfade, mostly to soften the run/swim snap entering and leaving water. It was
        // 1 tick back when state.isMoving() lagged navigation; isMovingForAnimation() fixed that.
        // Shared by every transition on this controller, so keep it short for idle/walk.
        SynchronizedAnimationController<OgreGrunt> movement =
                new SynchronizedAnimationController<>(this, "Grunt Movement", 4, this::animateGrunt);
        if (!usesCombatWalkSystem()) {
            controllers.add(movement);
            return;
        }
        controllers.add(movement.withFootfalls(getFootContacts(), this::playGruntFootfall));
        // Registered second so it is applied after the walk: the swings only key the upper body and
        // blend in from whatever pose the walk has. See OgreGruntModel for the blend out.
        controllers.add(new SynchronizedAnimationController<>(this, "Grunt Attack Overlay", 4,
                this::animateAttackOverlay));
    }

    private PlayState animateAttackOverlay(AnimationState<OgreGrunt> state) {
        state.setControllerSpeed(1.0F);
        if (isDeadOrDying()) {
            // A swing in progress is cancelled the moment he dies, not played out while he falls.
            visualOverlayOneShot.clear();
            attackOverlayVisible = false;
            return PlayState.STOP;
        }
        int syncedAttackId = entityData.get(DATA_GRUNT_ATTACK_ID);
        int attackId = playsOverWalk(syncedAttackId) ? syncedAttackId : 0;
        int visualAttackId = visualOverlayOneShot.update(state.getController(), attackId,
                attackId > 0 ? entityData.get(DATA_GRUNT_ATTACK_START_TICK) : 0L,
                this::getGruntAttackAnimationTicks, level().getGameTime());
        attackOverlayVisible = visualAttackId > 0;
        return attackOverlayVisible ? state.setAndContinue(getGruntAttackAnimation(visualAttackId)) : PlayState.STOP;
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
        if (usesCombatWalkSystem()) {
            return startPlainGruntAttack(baseAttack, target);
        }
        if (usesOnlyBiteAttack() || (canStartBiteAttack(target) && getRandom().nextInt(4) == 0)) {
            return startGruntAttack(2, BITE_ATTACK_COOLDOWN_TICKS, new MeleeAttackHit(BITE_IMPACT_TICKS + GRUNT_ATTACK_SYNC_DELAY_TICKS, BITE_DAMAGE / baseAttack, 0.0F, false,
                    BITE_ARMOR_PIERCE_FRACTION, 0.0, 0.0F, 0.0, 0, 0, false,
                    0.0F, false, 0.0, 0.0, 2.5, 0.0, 0.0));
        }

        return startGruntAttack(1, UPPER_SMACK_ATTACK_COOLDOWN_TICKS, new MeleeAttackHit(UPPER_SMACK_IMPACT_TICKS + GRUNT_ATTACK_SYNC_DELAY_TICKS, UPPER_SMACK_DAMAGE / baseAttack, 0.0F, false,
                0.0F, 0.0, 0.0F, 0.0, 0, 0, false, 0.0F, false, 0.0, 0.0, 2.5, 0.0, 0.0));
    }

    protected MeleeAttackPlan startBitePlan(float baseAttack) {
        startGruntAttack(ATTACK_ID_BITE, 0);
        // The plan runs as long as the clip, not just to the bite, so the clip isn't cut short.
        return new MeleeAttackPlan(PLAIN_BITE_RECOVERY_TICKS, PLAIN_BITE_ANIMATION_TICKS, List.of(
                new MeleeAttackHit(getBiteImpactTicks(), getBiteDamage() / baseAttack, 0.0F, false,
                        BITE_ARMOR_PIERCE_FRACTION, 0.0, 0.0F, 0.0, 0, 0, false,
                        0.0F, false, 0.0, 0.0, 2.5, 0.0, 0.0)));
    }

    /** The tick of the plan the bite lands on. */
    protected int getBiteImpactTicks() {
        return PLAIN_BITE_IMPACT_TICKS;
    }

    /** Raw damage of the bite. */
    protected float getBiteDamage() {
        return BITE_DAMAGE;
    }

    private MeleeAttackPlan startPlainGruntAttack(float baseAttack, LivingEntity target) {
        if (canStartBiteAttack(target) && getRandom().nextInt(4) == 0) {
            return startBitePlan(baseAttack);
        }

        int pick;
        if (getMainHandItem().is(ModItems.BONE_CLUB.get())) {
            // This id plays "bone regular attack 3", the swing that actually uses the club.
            pick = getRandom().nextInt(4) != 0 ? ATTACK_ID_SMACK : ATTACK_ID_SMACK_2;
        } else {
            // Unarmed smacks alternate arms most of the time.
            pick = lastSmackId == 0 || getRandom().nextInt(4) == 0
                    ? (getRandom().nextBoolean() ? ATTACK_ID_SMACK : ATTACK_ID_SMACK_2)
                    : (lastSmackId == ATTACK_ID_SMACK ? ATTACK_ID_SMACK_2 : ATTACK_ID_SMACK);
        }
        lastSmackId = pick;
        startGruntAttack(pick, 0);
        scheduleStrike(SMACK_IMPACT_TICKS);
        // The smack itself is thrown from aiStep at the clip's impact (see tickStrike).
        return new MeleeAttackPlan(SMACK_RECOVERY_TICKS, SMACK_ANIMATION_TICKS, List.of());
    }

    /** Throws the impact of a swing at the clip's impact tick. */
    protected void scheduleStrike(int impactTicks) {
        smackStrikeDelayTicks = impactTicks;
    }

    /** Attacks whose impact is thrown from aiStep at the clip's impact tick, not by the attack plan. */
    protected boolean isStrikeAttack(int attackId) {
        return attackId == ATTACK_ID_SMACK || attackId == ATTACK_ID_SMACK_2;
    }

    /** The impact of a strike attack. Entity-right is the model's right arm: smack 1 swings it, smack 2 the left. */
    protected void performStrike(int attackId) {
        performSmack(attackId == ATTACK_ID_SMACK ? 1.0 : -1.0);
    }

    private void tickSmackStrike() {
        if (smackStrikeDelayTicks < 0) {
            return;
        }
        if (!isStrikeAttack(activeGruntAttackId)) {
            smackStrikeDelayTicks = -1;
            return;
        }
        if (smackStrikeDelayTicks-- > 0) {
            return;
        }
        smackStrikeDelayTicks = -1;
        performStrike(activeGruntAttackId);
    }

    /**
     * The fist driving through whatever is in front of him and into the ground: whoever is in its
     * path takes the full hit, the ring around the impact a share. Always lands on the ground,
     * hit or miss, and asks for no line of sight so trees can't make it whiff. A lighter take on
     * the Brute's slam.
     */
    protected void performSmack(double rightSign) {
        double scale = getSmackScale();
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 side = right.scale(SMACK_SIDE_OFFSET * rightSign * scale);
        Vec3 strikeStart = position().add(forward.scale(SMACK_STRIKE_START * scale)).add(side);
        Vec3 impact = position().add(forward.scale(getSmackReach())).add(side);
        AABB strikePath = new AABB(
                Math.min(strikeStart.x, impact.x) - SMACK_STRIKE_RADIUS * scale, getY() - 1.0,
                Math.min(strikeStart.z, impact.z) - SMACK_STRIKE_RADIUS * scale,
                Math.max(strikeStart.x, impact.x) + SMACK_STRIKE_RADIUS * scale, getY() + 3.0,
                Math.max(strikeStart.z, impact.z) + SMACK_STRIKE_RADIUS * scale);
        double ringRadius = SMACK_RING_RADIUS * scale;
        double ringSqr = ringRadius * ringRadius;
        double search = getSmackReach() + ringRadius + 1.0;
        float damageMultiplier = getSmackDamage() / (float) getAttributeValue(Attributes.ATTACK_DAMAGE);

        List<LivingEntity> candidates = level().getEntitiesOfClass(
                LivingEntity.class, getBoundingBox().inflate(search, 3.0, search),
                target -> target != this && target.isAlive() && canAttackOgreTarget(target));
        for (LivingEntity target : candidates) {
            AABB bounds = target.getBoundingBox();
            double dx = target.getX() - impact.x;
            double dz = target.getZ() - impact.z;
            boolean struck = bounds.intersects(strikePath);
            boolean inRing = dx * dx + dz * dz <= ringSqr
                    && bounds.maxY >= getY() - 1.0 && bounds.minY <= getY() + 3.0;
            if (!struck && !inRing) {
                continue;
            }

            float share = struck ? 1.0F : areaFalloff(Math.sqrt(dx * dx + dz * dz), ringRadius, OgreDamage.BLAST_EDGE_SHARE);
            float armorPierce = getMainHandItem().is(ModItems.BONE_CLUB.get()) ? 0.20F : 0.0F;
            if (!doScaledHurtTarget(target, damageMultiplier * share, armorPierce)) {
                continue;
            }
            double carried = 1.0 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            Vec3 motion = target.getDeltaMovement();
            if (struck) {
                // Carried through along the smack and driven back down, not flung into the air.
                // A bone club shoves a little harder than a bare fist.
                double shove = getMainHandItem().is(ModItems.BONE_CLUB.get()) ? 1.25 : 1.0;
                target.setDeltaMovement(forward.x * SMACK_CARRY_PUSH * scale * carried * shove,
                        Math.max(motion.y, SMACK_CARRY_LIFT * carried), forward.z * SMACK_CARRY_PUSH * scale * carried * shove);
            } else {
                target.setDeltaMovement(motion.x, Math.max(motion.y, SMACK_CARRY_LIFT * 0.5 * carried), motion.z);
            }
            target.hasImpulse = true;
        }

        playSmackImpact(impact);
        spawnSmackGroundImpact(serverLevel, impact);
    }

    /** How far in front of him the blow lands. */
    protected double getSmackReach() {
        return SMACK_REACH * getSmackScale();
    }

    /** How much bigger than a Grunt's smack this one reaches. */
    protected double getSmackScale() {
        return 1.0;
    }

    /** Raw damage of the unarmed smack. */
    protected float getSmackDamage() {
        return getMainHandItem().is(ModItems.BONE_CLUB.get())
                ? UPPER_SMACK_DAMAGE * BONE_CLUB_DAMAGE_MULTIPLIER
                : UPPER_SMACK_DAMAGE;
    }

    /** How hard the smack lands: 1 for a Grunt, more for something bigger. */
    protected float getSmackPower() {
        return getMainHandItem().is(ModItems.BONE_CLUB.get()) ? 1.45F : 1.0F;
    }

    /** Debris off the block he hit, a puff of dust and a little screen shake. */
    protected void spawnSmackGroundImpact(ServerLevel serverLevel, Vec3 impact) {
        float power = getSmackPower();
        BlockPos ground = BlockPos.containing(impact.x, impact.y - 0.5, impact.z);
        BlockState state = serverLevel.getBlockState(ground);
        // Now and then the fist breaks the block it lands on, if it is a soft one.
        boolean broke = OgreGroundBreaker.breakBlock(serverLevel, this, ground, getSmackBlockBreakChance());
        if (state.isAir()) {
            ground = getOnPos();
            state = serverLevel.getBlockState(ground);
        }
        if (!state.isAir()) {
            // Otherwise the vanilla block-break effect on the block the fist lands on: its break
            // sound and shards. The block itself stays.
            if (!broke) {
                serverLevel.levelEvent(2001, ground, Block.getId(state));
            }
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    impact.x, impact.y + 0.1, impact.z, (int) (16 * power), 0.6 * power, 0.12, 0.6 * power, 0.1);
        }
        serverLevel.sendParticles(ParticleTypes.POOF, impact.x, impact.y + 0.2, impact.z,
                (int) (7 * power), 0.55 * power, 0.1, 0.55 * power, 0.02);
        ModNetwork.sendToPlayersNear(serverLevel, null, impact.x, impact.y, impact.z, 18.0,
                new ScreenShakePayload(impact.x, impact.y, impact.z, 0.3F * power));
    }

    /**
     * A big blow shakes the ground at {@code center}: a few soft blocks nearby may give way. The
     * number and spread grow with the blow's radius but stay small, and OgreGroundBreaker keeps
     * forts and camps from being torn up.
     */
    protected void onGroundShockwave(Vec3 center, float radius) {
        if (level() instanceof ServerLevel serverLevel) {
            OgreGroundBreaker.breakAround(serverLevel, this, center,
                    Math.min(radius * 0.3, 3.0), radius >= 8.0F ? 3 : 2, 0.35F);
        }
    }

    /** How often a smack breaks the soft block it lands on. */
    protected float getSmackBlockBreakChance() {
        return getMainHandItem().is(ModItems.BONE_CLUB.get()) ? 0.30F : 0.12F;
    }

    /** A thump and the ground's own crack: lighter than the Brute's iron slam, but still an ogre's. */
    private void playSmackImpact(Vec3 at) {
        float power = getSmackPower();
        level().playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.HOSTILE, 0.8F * power, 0.75F / power);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.MUD_HIT,
                SoundSource.HOSTILE, 0.5F * power, 0.7F);
        playSlamLayers(at, 0.55F * power, 0.1F - 0.05F * (power - 1.0F));
        if (getMainHandItem().is(ModItems.BONE_CLUB.get())) {
            // Bone on impact: a hard crack and a clatter of splinters, over a quiet low boom for weight.
            level().playSound(null, at.x, at.y, at.z, SoundEvents.BONE_BLOCK_BREAK,
                    SoundSource.HOSTILE, 1.0F, 0.85F);
            level().playSound(null, at.x, at.y, at.z, SoundEvents.BONE_BLOCK_HIT,
                    SoundSource.HOSTILE, 0.9F, 0.75F);
            level().playSound(null, at.x, at.y, at.z, SoundEvents.STONE_HIT,
                    SoundSource.HOSTILE, 0.2F, 0.9F);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(new ItemParticleOption(
                                ParticleTypes.ITEM, new ItemStack(Items.BONE)),
                        at.x, at.y + 0.2, at.z, 10, 0.35, 0.15, 0.35, 0.12);
            }
        }
    }

    /** Allows specialized Grunts to retain the bite while replacing the standard upswing. */
    protected boolean usesOnlyBiteAttack() {
        return false;
    }

    protected MeleeAttackPlan defaultMeleeAttackPlan() {
        return new MeleeAttackPlan(24, List.of(MeleeAttackHit.fullDamage(10)));
    }

    /** Whether an area hit that has already covered this target may actually reach it. */
    protected boolean canAreaHitReach(LivingEntity target) {
        return getSensing().hasLineOfSight(target)
                || usesCombatWalkSystem() && distanceToSqr(target) <= SMACK_CLOSE_RANGE * SMACK_CLOSE_RANGE;
    }

    /** Ogres, and boars carrying an ogre jockey, are the Tribe's own: ripples and area hits deal them half damage. */
    protected boolean isTribeAlly(LivingEntity target) {
        return target instanceof OgreGrunt && !isPlagueEnemy(target)
                || target instanceof MudsnoutBoar boar && boar.hasOgreRider();
    }

    /** Plague Ogres are the Tribe's enemies, so every ogre hits them as it would any other foe. */
    protected boolean isPlagueEnemy(LivingEntity target) {
        return target instanceof PlagueOgre && !(this instanceof PlagueOgre);
    }

    protected boolean doScaledHurtTarget(LivingEntity target, float damageMultiplier) {
        return doScaledHurtTarget(target, damageMultiplier, 0.0F);
    }

    protected boolean doScaledHurtTarget(LivingEntity target, float damageMultiplier, float armorPierceFraction) {
        float damage = damageMultiplier == 1.0F
                ? (float) getAttributeValue(Attributes.ATTACK_DAMAGE)
                : (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * damageMultiplier;
        boolean hurt = hurtTargetWithTemporaryArmorPierce(target, damage, armorPierceFraction);
        if (hurt && activeGruntAttackId == ATTACK_ID_BITE && healsOnBite()) {
            addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION, BITE_REGENERATION_TICKS, 1));
        }
        return hurt;
    }

    /** Grunts and Grunt Captains heal a little from every bite that lands. */
    private boolean healsOnBite() {
        return getClass() == OgreGrunt.class || this instanceof OgreGruntCaptain;
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
        armor.addTransientModifier(new AttributeModifier(BITE_ARMOR_PIERCE_MODIFIER_ID, BITE_ARMOR_PIERCE_MODIFIER_ID.toString(), -armorPierceFraction,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
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
        if (isDeadOrDying()) {
            // Cancel whatever swing was playing the moment he dies and settle into the idle pose.
            visualOneShot.clear();
            state.setControllerSpeed(1.0F);
            return state.setAndContinue(getIdleAnimation());
        }
        // The plain Grunt throws every attack from the overlay controller, over the walk.
        boolean combatWalkSystem = usesCombatWalkSystem();
        int syncedAttackId = entityData.get(DATA_GRUNT_ATTACK_ID);
        boolean overlayAttack = combatWalkSystem && playsOverWalk(syncedAttackId);
        int visualAttackId = visualOneShot.update(state.getController(),
                overlayAttack ? 0 : syncedAttackId, overlayAttack ? 0L : entityData.get(DATA_GRUNT_ATTACK_START_TICK),
                this::getGruntAttackAnimationTicks, level().getGameTime());
        if (visualAttackId > 0) {
            state.setControllerSpeed(1.0F);
            return state.setAndContinue(getGruntAttackAnimation(visualAttackId));
        }

        int movementState = entityData.get(DATA_MOVEMENT_STATE);
        if (combatWalkSystem && movementState == MOVEMENT_ATTACKING) {
            movementState = MOVEMENT_CHASING;
        }
        boolean moving = isMovingForGruntAnimation(state);
        if (movementState == MOVEMENT_CHASING) {
            idleAnimationTicks = 0;
            idleVariantTicks = 0;
            if (moving) {
                state.setControllerSpeed(isInWater() ? computeMovementAnimSpeed() : 1.0F);
                return state.setAndContinue(getChaseAnimation());
            }
            state.setControllerSpeed(1.0F);
            return state.setAndContinue(getIdleAnimation());
        }

        if (movementState == MOVEMENT_WALKING && moving) {
            idleAnimationTicks = 0;
            idleVariantTicks = 0;
            state.setControllerSpeed(isInWater() ? computeMovementAnimSpeed() : 1.0F);
            return state.setAndContinue(getWalkAnimation());
        }

        state.setControllerSpeed(1.0F);
        updateIdleAnimationTimers(state);
        if (hasRareIdleAnimation() && (idleVariantTicks > 0 || state.isCurrentAnimation(RARE_IDLE) && !state.getController().hasAnimationFinished())) {
            return state.setAndContinue(RARE_IDLE);
        }
        return state.setAndContinue(getIdleAnimation());
    }

    protected RawAnimation getIdleAnimation() { return IDLE; }
    protected RawAnimation getWalkAnimation() { return WALK; }
    protected RawAnimation getChaseAnimation() { return usesCombatWalkSystem() ? COMBAT_WALK : RUN; }
    protected boolean hasRareIdleAnimation() { return true; }

    protected RawAnimation getGruntAttackAnimation(int attackId) {
        if (usesCombatWalkSystem()) {
            return switch (attackId) {
                case ATTACK_ID_BITE -> ATTACK_2;
                case ATTACK_ID_SMACK_2 -> SMACK_2;
                default -> getMainHandItem().is(ModItems.BONE_CLUB.get()) ? BONE_SMACK : SMACK_1;
            };
        }
        return attackId == 2 ? ATTACK_2 : ATTACK;
    }

    protected int getGruntAttackAnimationTicks(int attackId) {
        if (usesCombatWalkSystem()) {
            return attackId == ATTACK_ID_BITE ? PLAIN_BITE_ANIMATION_TICKS : SMACK_ANIMATION_TICKS;
        }
        return attackId == 2 ? BITE_ANIMATION_TICKS : UPPER_SMACK_ANIMATION_TICKS;
    }

    protected boolean isMovingForGruntAnimation(AnimationState<OgreGrunt> state) {
        boolean moving = state.isMoving() || getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
        // Use game ticks so brief pauses don't drop the walk animation at high frame rates.
        long now = level().getGameTime();
        if (moving) {
            lastMovingAnimationTick = now;
            return true;
        }
        return now - lastMovingAnimationTick <= MOVEMENT_ANIMATION_GRACE_TICKS;
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
        if (hasRareIdleAnimation() && idleAnimationTicks >= 160 && getRandom().nextInt(240) == 0) {
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

        protected MeleeAttackHit withDamageMultiplier(float damageMultiplier) {
            return new MeleeAttackHit(delayTicks, damageMultiplier, areaRadius, hitsOgreAllies, armorPierceFraction,
                    knockbackStrength, coneDegrees, verticalKnockbackStrength, slownessTicks, nauseaTicks,
                    shockwaveParticles, screenShakeIntensity, blockRippleParticles, forwardOffset, rightOffset, verticalHitRange,
                    knockbackForward, knockbackRight);
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
     * Halves the requested pursuit speed while wading through (shallow) water - shared by every
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
        private int nextSuperiorSearch;

        protected GuardSuperiorGoal(OgreGrunt guard) {
            this.guard = guard;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!guard.canGuardSuperiors() || guard.getTarget() != null
                    || guard.tickCount < nextSuperiorSearch) {
                return false;
            }
            // Two entity queries per look, so look about twice a second instead of every goal tick.
            nextSuperiorSearch = guard.tickCount + 10 + guard.getRandom().nextInt(10);

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
            // Grunts and Captains use the combat walk while they are far from their superior and
            // walk once they are close (see tick).
            guard.runningToSuperior = guard.usesCombatWalkSystem()
                    ? isFarFromSuperior()
                    : guard instanceof OgreArcher
                    || guard instanceof OgreGuard
                    || guard instanceof OgreBrute;
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
            if (guard.usesCombatWalkSystem()) {
                guard.runningToSuperior = isFarFromSuperior();
            }
            repathTicks = Math.max(repathTicks - 1, 0);
            if (repathTicks <= 0 || guard.getNavigation().isDone()) {
                // Regular Grunts and Guards use the same catch-up pace as Archers and play their
                // run animation until they close back to the superior's nine-block stopping range.
                // Their unrelated random stroll and combat speeds remain unchanged.
                double followSpeed = guard.usesCombatWalkSystem()
                        ? (isFarFromSuperior() ? guard.getCombatWalkSpeedModifier() : GRUNT_FOLLOW_WALK_SPEED)
                        : guard.isRegularGruntRole() ? GRUNT_WALK_SPEED : guard.getCombatWalkSpeedModifier();
                guard.getNavigation().moveTo(superior, followSpeed);
                repathTicks = adjustedTickDelay(10);
            }
        }

        private boolean isFarFromSuperior() {
            return superior != null && guard.distanceToSqr(superior) > FOLLOW_FAR_DISTANCE * FOLLOW_FAR_DISTANCE;
        }

        private OgreGrunt findNearestSuperior() {
            // The lord outranks the mage - guard him first if he's around, otherwise fall back to
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
        // Hysteresis for stop-and-swing vs keep-approaching. A single threshold flickered every
        // tick from position drift; once stopped, stay stopped until the target passes the wider
        // hit-landing range.
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
            if (activeAttackPlan != null && ogre.isAttackCommitted()) {
                // A leap or a tantrum plays out even if the target is lost or turns invulnerable.
                return true;
            }
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
                if (activeAttackPlan != null && ogre.isAttackCommitted()) {
                    tickActiveAttack();
                }
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
            boolean usesFinishedMeleeFlow = ogre.isRegularGruntRole() || ogre instanceof OgreBrute
                    || ogre instanceof OgreTrapper;
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
                // Count recovery from attack commitment so the animation lock can't freeze it.
                ticksUntilNextAttack = Math.max(ticksUntilNextAttack - 1, 0);
            }

            boolean useCombatWalk = (activeAttackPlan == null || ogre.canCombatWalkDuringAttack())
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
                    || combatWalking && !(ogre instanceof OgreLord) && !(ogre instanceof OgreBrute) && !(ogre instanceof OgreTrapper)
                    && !ogre.usesCombatWalkSystem()
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
                    && (ogre.wantsImmediateAttack()
                    || ticksUntilNextAttack <= 0
                    && closeEnoughToAttack
                    && ogre.getSensing().hasLineOfSight(target));
            boolean shouldStopForAttackRange = closeEnoughToAttack
                    && !shouldApproachDuringRecovery
                    && !(combatWalking && ogre.keepsMovingThroughAttackRange())
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
                // Snap the body/head yaw to face the target exactly as the swing starts - the gradual
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

        private int directChaseRecheckTicks;
        private boolean directChaseActive;

        /**
         * On open, flat ground a wide ogre walks straight at its target instead of following path nodes. Node paths
         * run in 8-way segments, so a big body shows the diagonal jog as an angled, zigzagging approach, and the
         * look control then fights that heading near attack range. Anything in the way falls back to pathing.
         */
        private boolean tickDirectChase(LivingEntity target) {
            if (--directChaseRecheckTicks <= 0) {
                directChaseRecheckTicks = 4;
                boolean open = ogre.onGround() && ogre.hasOpenGroundTo(target);
                if (directChaseActive && !open) {
                    ticksUntilNextPathRecalculation = 0;
                }
                directChaseActive = open;
            }
            if (!directChaseActive) {
                return false;
            }
            ogre.getNavigation().stop();
            ogre.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(),
                    ogre.applyShallowWaterSpeedFactor(getActivePursuitSpeed()));
            return true;
        }

        private void tickGroundPursuit(LivingEntity target) {
            if (ogre.usesDirectOpenGroundChase() && ogre.getPursuitGoalOverride(target) == null
                    && tickDirectChase(target)) {
                return;
            }
            // Respect the repath timer even when a short path completes. Rebuilding a path every
            // tick makes navigation and look control repeatedly fight over the grunt's heading.
            if (ticksUntilNextPathRecalculation <= 0) {
                double effectiveSpeed = ogre.applyShallowWaterSpeedFactor(getActivePursuitSpeed());
                Vec3 goal = ogre.getPursuitGoalOverride(target);
                boolean pathing = goal != null
                        ? ogre.getNavigation().moveTo(goal.x, goal.y, goal.z, effectiveSpeed)
                        : ogre.getNavigation().moveTo(target, effectiveSpeed);
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
            if (hit.areaRadius() > 0.0F || activeAttackTicks < Math.max(0, hit.delayTicks() - ogre.getAttackImpactSoundLeadTicks())) {
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
                    && ogre.canAttackOgreTarget(pendingAttackTarget)
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
                    && ogre.canAttackOgreTarget(pendingAttackTarget)
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
                if (!hit.blockRippleParticles()) {
                    ogre.onGroundShockwave(areaCenter, hit.areaRadius());
                }
                spawnShockwaveParticles(areaCenter, hit.areaRadius());
                if (!ogre.suppressShockwaveSound()) {
                    playShockwaveSound(areaCenter);
                    ogre.playShockwaveLayers(areaCenter, hit);
                }
                sendShockwaveScreenShake(areaCenter, hit.areaRadius(), hit.screenShakeIntensity());
            } else if (hit.areaRadius() > 0.0F && ogre.showsAreaRing()) {
                // A boss's area attacks show how far they reach even without the full shockwave.
                spawnShockwaveParticles(areaCenter, hit.areaRadius());
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
                            && (hit.hitsOgreAllies() && !(target instanceof OgreLord) || !(target instanceof OgreGrunt) || ogre.isPlagueEnemy(target))
                            && horizontalDistanceToSqr(areaCenter, target) <= hit.areaRadius() * hit.areaRadius()
                            && Math.abs(areaCenter.y - target.getY()) <= hit.verticalHitRange()
                            && ogre.canAreaHitReach(target));

            if (hit.blockRippleParticles()) {
                ogre.applyDirectShockwaveWeaponHits(targets, areaCenter, hit);
                spawnBlockRipple(areaCenter, hit, ogre.consumeDirectShockwaveRippleExclusions());
            } else {
                for (LivingEntity target : targets) {
                    if (hit.coneDegrees() > 0.0F && !isInsideAttackCone(target, hit.coneDegrees())) {
                        continue;
                    }

                    float damageMultiplier = ogre.isTribeAlly(target) ? hit.damageMultiplier() * 0.5F : hit.damageMultiplier();
                    // Weaker toward the edge of the area.
                    damageMultiplier *= areaFalloff(Math.sqrt(horizontalDistanceToSqr(areaCenter, target)),
                            hit.areaRadius(),
                            hit.shockwaveParticles() ? OgreDamage.BLAST_EDGE_SHARE : OgreDamage.ARC_EDGE_SHARE);
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

            // No block cap: ring and point counts are already bounded by radius and 24 points per
            // ring. A flat cap cut the outer rings short and left the ripple smaller than the
            // shockwave particles.
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
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE,
                    1.25F,
                    0.55F);
        }

        private void sendShockwaveScreenShake(Vec3 center, float radius, float intensity) {
            if (intensity > 0.0F && ogre.level() instanceof ServerLevel serverLevel) {
                ModNetwork.sendToPlayersNear(
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
        // Keep previous rotations so the renderer interpolates the turn across the tick.
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    /**
     * True when this ogre has to start an attack at once, whatever its cooldown, range or sight line:
     * the Trapper's knife draw, which comes as soon as his last animation is over.
     */
    protected boolean wantsImmediateAttack() {
        return false;
    }

    protected boolean shouldStayStationaryDuringMeleeAttack() {
        // The grunt's short windup is its dodge window. Committing its feet until contact keeps
        // the visible attack and server hit volume together instead of letting it slide after a target.
        return isRegularGruntRole() && !usesCombatWalkSystem();
    }

    protected boolean shouldSuspendNavigationDuringMeleeAttack() {
        return false;
    }

    protected boolean shouldApproachDuringMeleeRecovery() {
        return usesCombatWalkSystem();
    }

    /** True for attacks that are played over the combat walk instead of stopping the mob. */
    protected boolean canCombatWalkDuringAttack() {
        return usesCombatWalkSystem();
    }

    /** True if the mob keeps walking once it is in range, leaving the stop to the spacing check. */
    protected boolean keepsMovingThroughAttackRange() {
        return usesCombatWalkSystem();
    }

    protected boolean shouldUseCombatWalk(LivingEntity target, boolean closeEnoughToAttack,
                                          int ticksUntilNextAttack, boolean currentlyCombatWalking) {
        return usesCombatWalkSystem() && target != null && target.isAlive();
    }

    protected void setUsingCombatWalk(boolean usingCombatWalk) {
    }

    protected double getCombatWalkSpeedModifier() {
        return usesCombatWalkSystem() ? COMBAT_WALK_SPEED : getMeleePursuitSpeedModifier();
    }

    protected boolean shouldHoldCombatSpacing(LivingEntity target) {
        if (!usesCombatWalkSystem()) {
            return false;
        }
        // Closes in to a body's width of the target and stays there, instead of shoving into them.
        double x = target.getX() - getX();
        double z = target.getZ() - getZ();
        double preferred = (getBbWidth() + target.getBbWidth()) * 0.5 + COMBAT_WALK_TARGET_GAP;
        return x * x + z * z <= preferred * preferred
                && Math.abs(target.getY() - getY()) <= COMBAT_WALK_MAX_VERTICAL_DIFFERENCE;
    }

    protected double getMeleePursuitSpeedModifier() {
        // Navigation modifiers, not attribute mutations, own the Grunt's movement modes.
        return isRegularGruntRole() ? GRUNT_CHASE_SPEED : 1.0;
    }

    protected double getWanderSpeedModifier() {
        return isRegularGruntRole() ? GRUNT_WALK_SPEED : 0.8;
    }

    protected Goal createWanderGoal() {
        return new WaterAvoidingRandomStrollGoal(this, getWanderSpeedModifier());
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
        // No ogre lunges forward through an attack any more; the swings land where he stands.
        return 0.0;
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

    /** Whether this ogre's area attacks always draw the ring of cloud showing their reach. */
    protected boolean showsAreaRing() {
        return false;
    }

    /** Somewhere to walk to instead of straight at the target while pursuing it, or null to go at the target. */
    protected Vec3 getPursuitGoalOverride(LivingEntity target) {
        return null;
    }

    /** True for an attack that must play out to the end once started, whatever happens to the target. */
    protected boolean isAttackCommitted() {
        return usesCombatWalkSystem() && activeGruntAttackId != 0;
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

    protected int getAttackImpactSoundLeadTicks() {
        return ATTACK_IMPACT_SOUND_LEAD_TICKS;
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
        onGroundShockwave(origin, profile.maxRadius());
        activeGroundRipples.add(new ActiveGroundRipple(origin, hit, profile, alreadyHit));
        if (!showsGroundRippleVisual(hit, profile)) {
            return;
        }
        ModNetwork.sendToPlayersNear(serverLevel, null, origin.x, origin.y, origin.z,
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
                        && (ripple.hit.hitsOgreAllies() && !(target instanceof OgreLord) || !(target instanceof OgreGrunt) || isPlagueEnemy(target))
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
            if (applyGroundRippleHit(target, ripple.hit, ripple.profile, ripple.origin)) {
                shoveWithRipple(target, ripple);
            }
        }
    }

    /** Carries a struck target up and away from the wave, like surf breaking over them. */
    private void shoveWithRipple(LivingEntity target, ActiveGroundRipple ripple) {
        double lift = ripple.hit.verticalKnockbackStrength();
        if (lift <= 0.0 || target instanceof OgreGrunt && !isPlagueEnemy(target)) {
            return;
        }
        Vec3 from = ripple.profile.kind() == GroundRippleKind.KING_STOMP_WAVE ? position() : ripple.origin;
        double dx = target.getX() - from.x;
        double dz = target.getZ() - from.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-3) {
            Vec3 facing = Vec3.directionFromRotation(0.0F, getYRot());
            dx = facing.x;
            dz = facing.z;
            length = 1.0;
        }
        double up = Math.max(0.3, Math.min(0.6, lift * RIPPLE_SHOVE_LIFT_SCALE));
        double out = up * RIPPLE_SHOVE_OUTWARD_RATIO;
        target.setDeltaMovement(dx / length * out, up, dz / length * out);
        target.hasImpulse = true;
        target.hurtMarked = true;
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

    /** Returns true if the target was hurt, which is when the wave also shoves it. */
    protected boolean applyGroundRippleHit(LivingEntity target, MeleeAttackHit hit, GroundRippleProfile profile,
                                           Vec3 origin) {
        boolean ogreAlly = isTribeAlly(target);
        float adjustedDamageMultiplier = ogreAlly ? hit.damageMultiplier() * 0.5F : hit.damageMultiplier();
        if (doScaledHurtTarget(target, adjustedDamageMultiplier, hit.armorPierceFraction())) {
            applyAttackHitExtras(target, hit);
            return true;
        }
        return false;
    }

    /** The ripple always deals its damage; this only decides whether the block wave is drawn. */
    protected boolean showsGroundRippleVisual(MeleeAttackHit hit, GroundRippleProfile profile) {
        return false;
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

    /** Extra sound a subclass layers on top of the shared shockwave boom. */
    protected void playShockwaveLayers(Vec3 center, MeleeAttackHit hit) {
    }

    protected boolean suppressShockwaveSound() {
        return false;
    }

    protected int getPathRecalculationTicks() {
        return isRegularGruntRole()
                ? 8 + getRandom().nextInt(5)
                : 4 + getRandom().nextInt(4);
    }

    /** Every ogre walks straight at its target across open ground; see {@link OpenGroundChase}. */
    protected boolean usesDirectOpenGroundChase() {
        return true;
    }

    protected boolean hasOpenGroundTo(LivingEntity target) {
        return noGoRadius <= 0 && OpenGroundChase.isClear(this, target);
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

    // A ripple lifts a target to a third to half of its launch strength, capped, and pushes it out
    // at 1.4x that, so the hit travels with the wave instead of flinging targets skyward.
    private static final double RIPPLE_SHOVE_LIFT_SCALE = 0.45;
    private static final double RIPPLE_SHOVE_OUTWARD_RATIO = 1.4;

    protected enum GroundRippleKind { STANDARD, KING_CLUB_DOUBLE, KING_CLUB_PULL_OUT, KING_STOMP_WAVE, KING_BUTT_SMASH }

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
