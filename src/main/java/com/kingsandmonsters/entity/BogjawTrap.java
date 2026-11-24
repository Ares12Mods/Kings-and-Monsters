package com.kingsandmonsters.entity;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.effect.BleedingEffect;
import com.kingsandmonsters.effect.CombatEffects;
import com.kingsandmonsters.tribute.TributeManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Trapper's iron jaws. Thrown, it tumbles through the air closed and snaps open where it lands;
 * placed, it opens in front of him. Armed, it lies there until something that is not an ogre walks
 * onto it, then clamps shut: a heavy bite, a deep wound and a few seconds pinned in place.
 */
public class BogjawTrap extends Entity implements GeoEntity {
    public static final int PHASE_FLYING = 0;
    public static final int PHASE_DEPLOYING = 1;
    public static final int PHASE_ARMED = 2;
    public static final int PHASE_CLAMPED = 3;

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(BogjawTrap.class, EntityDataSerializers.INT);
    private static final TagKey<EntityType<?>> OGRES = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(KingsAndMonsters.MODID, "ogres"));

    private static final RawAnimation CLAMPED = RawAnimation.begin().thenLoop("clamped");
    private static final RawAnimation DEPLOYED = RawAnimation.begin().thenPlay("deployed").thenLoop("idle");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CLAMP = RawAnimation.begin().thenPlay("clamp").thenLoop("clamped");

    /** Downward pull each tick while it flies. The throw is aimed against this exact value. */
    public static final double GRAVITY = 0.08;
    // The deploy clip is 0.25s; the trap is live once it has finished opening.
    private static final int DEPLOY_TICKS = 6;
    private static final int MAX_FLIGHT_TICKS = 120;
    // An untouched trap rusts away after five minutes.
    private static final int ARMED_LIFETIME_TICKS = 6000;
    private static final int CLAMPED_LIFETIME_TICKS = 100;
    /** How long a clamped victim is held: 2.5 seconds. */
    private static final int HOLD_TICKS = 50;
    private static final float CLAMP_DAMAGE = 14.0F;
    /** What it does simply by landing on someone, before it opens. */
    private static final float IMPACT_DAMAGE = 8.0F;
    // The clamp clip closes at 0.13s, which is about three ticks in.
    private static final int CLAMP_IMPACT_TICK = 3;
    /** How far from its centre a standing trap springs: wider than the jaws, so it is hard to step around. */
    private static final double TRIGGER_RADIUS = 1.5;
    /** The ring of dust that marks that reach, so the trap can be seen and avoided. */
    private static final DustParticleOptions TRIGGER_MARK = new DustParticleOptions(new Vector3f(0.85F, 0.15F, 0.1F), 0.6F);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<UUID> held = new ArrayList<>();
    private UUID ownerId;
    private int phaseTicks;
    private boolean hitSomeone;
    // A trap a player set down spares players and catches only enemies; the Trapper's catches players too.
    private boolean playerOwned;

    public BogjawTrap(EntityType<? extends BogjawTrap> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    /** A trap in the air, closed, on its way to wherever it lands. */
    public static BogjawTrap thrown(Level level, LivingEntity owner, Vec3 at, Vec3 velocity) {
        BogjawTrap trap = new BogjawTrap(ModEntities.BOGJAW_TRAP.get(), level);
        trap.ownerId = owner.getUUID();
        trap.playerOwned = owner instanceof Player;
        trap.setPos(at);
        trap.setDeltaMovement(velocity);
        trap.setYRot(owner.getYRot());
        return trap;
    }

    /** A trap set down on the ground, opening at once. */
    public static BogjawTrap placed(Level level, LivingEntity owner, Vec3 at) {
        BogjawTrap trap = new BogjawTrap(ModEntities.BOGJAW_TRAP.get(), level);
        trap.ownerId = owner.getUUID();
        trap.playerOwned = owner instanceof Player;
        trap.setPos(at);
        trap.setYRot(owner.getYRot());
        trap.entityData.set(DATA_PHASE, PHASE_DEPLOYING);
        return trap;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public int getPhase() {
        return entityData.get(DATA_PHASE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_PHASE, PHASE_FLYING);
    }

    @Override
    public void tick() {
        super.tick();
        phaseTicks++;
        switch (getPhase()) {
            case PHASE_FLYING -> tickFlight();
            case PHASE_DEPLOYING -> {
                if (!level().isClientSide && phaseTicks >= DEPLOY_TICKS) {
                    setPhase(PHASE_ARMED);
                }
            }
            case PHASE_ARMED -> {
                if (level().isClientSide) {
                    markTriggerRing();
                } else if (phaseTicks > ARMED_LIFETIME_TICKS) {
                    rustAway();
                } else if (tickCount % 2 == 0) {
                    clampOn(findTriggered());
                }
            }
            default -> tickClamped();
        }
    }

    private void tickFlight() {
        Vec3 motion = getDeltaMovement();
        move(MoverType.SELF, motion);
        if (!level().isClientSide) {
            // Hitting someone does not set it off in the air: it stops dead and drops at their feet, and
            // snaps open on the ground like any other landing.
            List<LivingEntity> struck = findVictims();
            if (!struck.isEmpty()) {
                motion = new Vec3(0.0, Math.min(motion.y, 0.0), 0.0);
                setDeltaMovement(motion);
                if (!hitSomeone) {
                    hitSomeone = true;
                    slamInto(struck);
                }
            }
            if (onGround() || phaseTicks > MAX_FLIGHT_TICKS) {
                land();
                return;
            }
        }
        // A wall knocks it back a little instead of letting it slide along it.
        if (horizontalCollision) {
            motion = motion.multiply(-0.15, 1.0, -0.15);
        }
        setDeltaMovement(motion.x, motion.y - GRAVITY, motion.z);
    }

    /** A heavy lump of iron landing on someone: it hurts before it ever opens. Only the first contact counts. */
    private void slamInto(List<LivingEntity> victims) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Entity owner = server.getEntity(ownerId);
        DamageSource damage = owner instanceof LivingEntity living
                ? damageSources().mobAttack(living) : damageSources().generic();
        for (LivingEntity victim : victims) {
            victim.hurt(damage, IMPACT_DAMAGE);
            server.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6,
                    victim.getZ(), 10, 0.25, 0.25, 0.25, 0.3);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.9F, 1.1F);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    /** It hit the ground: stop dead, then snap open. */
    private void land() {
        setDeltaMovement(Vec3.ZERO);
        hasImpulse = true;
        setPhase(PHASE_DEPLOYING);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6F, 1.3F);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.HOSTILE, 1.0F, 0.7F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.1, getZ(), 6, 0.4, 0.05, 0.4, 0.02);
        }
    }

    private void setPhase(int phase) {
        entityData.set(DATA_PHASE, phase);
        phaseTicks = 0;
    }

    private void tickClamped() {
        if (level().isClientSide) {
            return;
        }
        if (phaseTicks == CLAMP_IMPACT_TICK && level() instanceof ServerLevel server) {
            snapShut(server);
        }
        if (level() instanceof ServerLevel server) {
            for (UUID id : held) {
                if (server.getEntity(id) instanceof LivingEntity victim && victim.isAlive()
                        && phaseTicks <= HOLD_TICKS) {
                    Vec3 motion = victim.getDeltaMovement();
                    victim.setDeltaMovement(0.0, Math.min(motion.y, 0.0), 0.0);
                    if (victim instanceof Mob mob) {
                        mob.getNavigation().stop();
                    }
                }
            }
        }
        if (phaseTicks > CLAMPED_LIFETIME_TICKS) {
            rustAway();
        }
    }

    private void rustAway() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 8, 0.4, 0.1, 0.4, 0.02);
        }
        discard();
    }

    /** Everything standing within the trigger radius of an armed trap, at about its height. */
    private List<LivingEntity> findTriggered() {
        AABB zone = new AABB(getX() - TRIGGER_RADIUS, getY() - 0.3, getZ() - TRIGGER_RADIUS,
                getX() + TRIGGER_RADIUS, getY() + 1.2, getZ() + TRIGGER_RADIUS);
        return level().getEntitiesOfClass(LivingEntity.class, zone, entity ->
                entity.distanceToSqr(getX(), entity.getY(), getZ()) <= TRIGGER_RADIUS * TRIGGER_RADIUS
                        && canClamp(entity));
    }

    /** Two dots chase each other round the trigger edge, which reads as a ring on the ground. */
    private void markTriggerRing() {
        if (tickCount % 2 != 0) {
            return;
        }
        double angle = tickCount * 0.3;
        for (int i = 0; i < 2; i++) {
            double a = angle + i * Math.PI;
            level().addParticle(TRIGGER_MARK, getX() + Math.cos(a) * TRIGGER_RADIUS, getY() + 0.08,
                    getZ() + Math.sin(a) * TRIGGER_RADIUS, 0.0, 0.0, 0.0);
        }
    }

    private List<LivingEntity> findVictims() {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.05, 0.1, 0.05),
                this::canClamp);
    }

    /**
     * A player's trap catches nearly anything but the owner's friends. The tribe's own traps only catch
     * enemies of the tribe: not ogres, and players only when they have not made peace.
     */
    private boolean canClamp(LivingEntity entity) {
        if (entity instanceof OgreLord king && (!playerOwned || !king.canBeTrapped())) {
            return false;
        }
        if (playerOwned) {
            // A player's trap catches the hostile and the aggressive, and players and the tribe, but not
            // the owner's friends and not passive animals or villagers.
            return entity.isAlive() && !entity.isSpectator() && !isFriendOfOwner(entity) && isThreat(entity);
        }
        if (!entity.isAlive() || entity.isSpectator() || entity.getType().is(OGRES)) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !(level() instanceof ServerLevel server && isAtPeaceWith(server, player));
        }
        return entity instanceof Enemy || OgreGrunt.isTerritorialInvaderType(entity);
    }

    /** Hostile mobs, the tribe, other players, and anything neutral that has picked a fight. Passive mobs are not. */
    private boolean isThreat(LivingEntity entity) {
        // An ogre that has not picked a fight is left alone: springing on it would only anger the tribe.
        if (entity.getType().is(OGRES)) {
            return entity instanceof Mob ogre && (ogre.isAggressive() || ogre.getTarget() != null);
        }
        if (entity instanceof Enemy || entity instanceof Player) {
            return true;
        }
        return entity instanceof Mob mob && (mob.isAggressive() || mob.getTarget() != null);
    }

    /**
     * Who a player's trap lets pass: the owner, tamed pets, ogres fighting in the owner's army, anyone
     * on the owner's team or otherwise allied to them, players the owner cannot harm, and the owner's
     * golems. {@link #isThreat} then decides whether what is left is worth catching.
     */
    private boolean isFriendOfOwner(LivingEntity entity) {
        if (entity instanceof ArmorStand || entity.getUUID().equals(ownerId)) {
            return true;
        }
        if (entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null) {
            return true;
        }
        if (entity instanceof MudsnoutBoar boar && boar.isTamed()) {
            return true;
        }
        if (entity instanceof OgreGrunt grunt && grunt.isArmyAlly() && ownerId != null
                && ownerId.equals(grunt.getArmyOwnerId())) {
            return true;
        }
        if (entity instanceof IronGolem golem && golem.isPlayerCreated()) {
            return true;
        }
        if (entity instanceof Player other && other.isCreative()) {
            return true;
        }
        if (!(level() instanceof ServerLevel server) || ownerId == null
                || !(server.getEntity(ownerId) instanceof Player owner)) {
            return false;
        }
        return entity.isAlliedTo(owner) || owner.isAlliedTo(entity)
                || entity instanceof Player other && !owner.canHarmPlayer(other);
    }
    private static boolean isAtPeaceWith(ServerLevel server, Player player) {
        return TributeManager.isAppeased(server, player.getUUID())
                && !TributeManager.isProvoked(server, player.getUUID());
    }

    /** Snaps shut on everything standing on it. Returns false when there was nothing to catch. */
    private boolean clampOn(List<LivingEntity> victims) {
        if (victims.isEmpty() || !(level() instanceof ServerLevel server)) {
            return false;
        }
        setDeltaMovement(Vec3.ZERO);
        setPhase(PHASE_CLAMPED);
        for (LivingEntity victim : victims) {
            // Slowness at this level leaves no speed at all, and a cut tendon takes the jump.
            int hold = HOLD_TICKS;
            if (victim instanceof OgreLord king) {
                king.onTrapped();
            }
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, hold, 6, false, true));
            victim.addEffect(new MobEffectInstance(ModMobEffects.HAMSTRUNG.get(), hold, 0, false, true));
            held.add(victim.getUUID());
        }
        return true;
    }

    /** The jaws meet, a moment into the clamp animation. */
    private void snapShut(ServerLevel server) {
        Entity owner = server.getEntity(ownerId);
        for (UUID id : held) {
            if (!(server.getEntity(id) instanceof LivingEntity victim) || !victim.isAlive()) {
                continue;
            }
            LivingEntity source = owner instanceof LivingEntity living ? living : victim;
            DamageSource damage = owner instanceof LivingEntity living
                    ? damageSources().mobAttack(living) : damageSources().generic();
            victim.hurt(damage, CLAMP_DAMAGE);
            CombatEffects.applyBleeding(victim, BleedingEffect.Severity.GREATER, source);
            server.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 0.4, victim.getZ(),
                    14, 0.3, 0.2, 0.3, 0.3);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.HOSTILE, 1.5F, 0.55F);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.5F, 1.6F);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    /** Only an armed trap can be hit, so it can be picked up or set off by hand. */
    @Override
    public boolean isPickable() {
        return getPhase() == PHASE_ARMED;
    }

    /**
     * A punch on an armed trap. With a stick in hand it is prised up safely and comes back as the item;
     * with anything else, or bare-handed, it goes off on whoever hit it. Arrows and the like do nothing.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || getPhase() != PHASE_ARMED || !(source.getDirectEntity() instanceof Player player)) {
            return false;
        }
        if (player.isHolding(Items.STICK)) {
            ItemStack stack = new ItemStack(ModItems.BOGJAW_TRAP.get());
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.6F, 1.4F);
            discard();
        } else if (!player.isCreative() && !player.isSpectator()) {
            clampOn(List.of(player));
        }
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("Phase", getPhase());
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putBoolean("PlayerOwned", playerOwned);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        entityData.set(DATA_PHASE, tag.getInt("Phase"));
        phaseTicks = tag.getInt("PhaseTicks");
        playerOwned = tag.getBoolean("PlayerOwned");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "Trap", 0, this::animate));
    }

    private PlayState animate(AnimationState<BogjawTrap> state) {
        return state.setAndContinue(switch (getPhase()) {
            case PHASE_DEPLOYING -> DEPLOYED;
            case PHASE_ARMED -> IDLE;
            case PHASE_CLAMPED -> CLAMP;
            default -> CLAMPED;
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
