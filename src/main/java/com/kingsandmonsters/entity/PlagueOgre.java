package com.kingsandmonsters.entity;

import java.util.Map;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.effect.BogrotMechanics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.List;

/** An infected ogre, separate from the Grunt's randomized armor appearances. */
public class PlagueOgre extends OgreGrunt {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("Zombie_idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("Zombie_walk");
    private static final RawAnimation CHASE = RawAnimation.begin().thenLoop("Zombie_aggro walk");
    private static final RawAnimation OVERHEAD = RawAnimation.begin().thenPlay("overhead_slam");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("BITE");
    // Both clips run 2.25s. Attack ids: 1 is the overhead slam, 2 the bite.
    private static final int ATTACK_ANIMATION_TICKS = 45;
    private static final int OVERHEAD_ID = 1;
    private static final int BITE_ID = 2;
    // The bite lands 1.25s into its clip and the slam, hands on the ground, 1.13s into its own.
    private static final int BITE_IMPACT_TICKS = 25 + CLIP_LAG_TICKS;
    private static final int OVERHEAD_IMPACT_TICKS = 23 + CLIP_LAG_TICKS;
    // Swings are thrown over the walk, so recovery has to outlast the clip.
    private static final int BITE_RECOVERY_TICKS = 62;
    private static final int OVERHEAD_RECOVERY_TICKS = 70;
    private static final float BITE_DAMAGE = 11.0F;
    private static final float OVERHEAD_DAMAGE = 13.0F;
    private static final int NATURAL_SPAWN_RADIUS = 64;
    private static final String NATURAL_SPAWN_TAG = "NaturallySpawnedPlagueOgre";
    private boolean naturallySpawned;
    private boolean slamming;

    public PlagueOgre(EntityType<? extends PlagueOgre> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return OgreGrunt.createAttributes()
                .add(Attributes.MAX_HEALTH, 55.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35);
    }

    public static boolean checkPlagueOgreSpawnRules(EntityType<? extends PlagueOgre> type,
                                                    ServerLevelAccessor level, MobSpawnType spawnType,
                                                    BlockPos pos, RandomSource random) {
        // Plague Ogres are living creatures and may spawn in daylight. Structure spawners (the
        // abandoned camp's graves) sit on plains, so only natural spawns need the swamp.
        boolean fromSpawner = spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.STRUCTURE;
        if (level.getDifficulty() == Difficulty.PEACEFUL
                || (!fromSpawner && !isSwampBiome(level, pos))
                || !Mob.checkMobSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        // Reserve one quarter of natural spawn attempts for nighttime.
        if (spawnType == MobSpawnType.NATURAL
                && level.getLevel().isDay()
                && random.nextInt(4) == 0) {
            return false;
        }
        if (spawnType == MobSpawnType.NATURAL && level instanceof ServerLevel server) {
            AABB area = new AABB(pos).inflate(NATURAL_SPAWN_RADIUS, 24, NATURAL_SPAWN_RADIUS);
            int nearbyNatural = server.getEntitiesOfClass(PlagueOgre.class, area,
                    plague -> plague.isAlive() && plague.isNaturallySpawned()).size();
            return nearbyNatural < naturalSpawnLimit(server, pos);
        }
        return true;
    }

    public static boolean isSwampBiome(ServerLevelAccessor level, BlockPos pos) {
        return level.getBiome(pos).is(Biomes.SWAMP) || level.getBiome(pos).is(Biomes.MANGROVE_SWAMP);
    }

    private static int naturalSpawnLimit(ServerLevel level, BlockPos pos) {
        // Scale the local cap with loaded swamp habitat without generating chunks.
        if (swampDirections(level, pos, 64) < 3) {
            return 2;
        }
        return swampDirections(level, pos, 128) >= 3 ? 5 : 3;
    }

    private static int swampDirections(ServerLevel level, BlockPos pos, int distance) {
        int swampDirections = 0;
        for (int[] offset : new int[][]{{distance, 0}, {-distance, 0}, {0, distance}, {0, -distance}}) {
            int x = pos.getX() + offset[0];
            int z = pos.getZ() + offset[1];
            if (level.hasChunk(x >> 4, z >> 4)
                    && isSwampBiome(level, new BlockPos(x, pos.getY(), z))) {
                swampDirections++;
            }
        }
        return swampDirections;
    }

    public boolean isNaturallySpawned() {
        return naturallySpawned;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag spawnTag) {
        naturallySpawned = spawnType == MobSpawnType.NATURAL;
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData, spawnTag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(NATURAL_SPAWN_TAG, naturallySpawned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        naturallySpawned = tag.getBoolean(NATURAL_SPAWN_TAG);
    }

    @Override
    public void applyConfiguredCombatAttributes(boolean healToMax) {
        // Infected Ogres use their own attributes instead of Grunt equipment bonuses.
    }

    @Override
    protected boolean isRegularGruntRole() {
        return true;
    }

    @Override
    protected double getMeleePursuitSpeedModifier() {
        // Retain Grunt navigation with a slower, heavier pursuit speed.
        return 0.8;
    }

    @Override
    protected Goal createWanderGoal() {
        return new FarWanderGoal();
    }

    private final class FarWanderGoal extends WaterAvoidingRandomStrollGoal {
        private static final int WANDER_RANGE = 28;
        private static final double MIN_WANDER_DISTANCE_SQR = 12.0 * 12.0;

        private FarWanderGoal() {
            super(PlagueOgre.this, getWanderSpeedModifier());
        }

        @Override
        protected Vec3 getPosition() {
            // Prefer long land routes, then fall back to vanilla water-avoiding wandering.
            for (int attempt = 0; attempt < 6; attempt++) {
                Vec3 destination = LandRandomPos.getPos(PlagueOgre.this, WANDER_RANGE, 7);
                if (destination != null && destination.distanceToSqr(position()) >= MIN_WANDER_DISTANCE_SQR) {
                    return destination;
                }
            }
            return super.getPosition();
        }
    }

    @Override
    protected boolean canAttackOgreTarget(LivingEntity target) {
        return !(target instanceof PlagueOgre) && !(target instanceof BogToad);
    }

    /** Idle, walk and combat walk, with both attacks thrown over the walk like the Grunt's. */
    @Override
    protected boolean usesCombatWalkSystem() {
        return true;
    }

    @Override
    protected boolean shouldHoldCombatSpacing(LivingEntity target) {
        // Once a swing has started he keeps walking in instead of planting his feet for it.
        return !hasActiveMeleeAttack() && super.shouldHoldCombatSpacing(target);
    }

    @Override
    protected boolean isMovingForGruntAnimation(AnimationState<OgreGrunt> state) {
        // The combat walk keeps playing under the whole swing, even if he is pressed up to the target.
        return isAttackOverlayVisible() || super.isMovingForGruntAnimation(state);
    }

    // Animation ticks where a foot lands: the leg keys put the forward-most foot at 0 and half a cycle on.
    private static final Map<String, double[]> PLAGUE_FOOT_CONTACTS = Map.of(
            "Zombie_walk", new double[]{0.0, 25.2},
            "Zombie_aggro walk", new double[]{0.0, 17.03});

    @Override
    protected Map<String, double[]> getFootContacts() {
        return PLAGUE_FOOT_CONTACTS;
    }

    @Override
    protected void playFootfallSounds() {
        // A heavy, wet zombie shuffle on each visible footfall.
        float variation = getRandom().nextFloat() * 0.1F;
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.ZOMBIE_STEP, SoundSource.HOSTILE,
                0.45F, 0.7F + variation, false);
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.MUD_STEP, SoundSource.HOSTILE,
                0.3F, 0.8F + variation, false);
    }

    @Override
    protected double getCombatWalkSpeedModifier() {
        return getMeleePursuitSpeedModifier();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof BogToad) && super.canAttack(target);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof PlagueOgre || entity instanceof BogToad
                || super.isAlliedTo(entity) && !(entity instanceof OgreGrunt);
    }

    @Override
    protected RawAnimation getIdleAnimation() { return IDLE; }

    @Override
    protected RawAnimation getWalkAnimation() { return WALK; }

    @Override
    protected RawAnimation getChaseAnimation() { return CHASE; }

    @Override
    protected boolean hasRareIdleAnimation() { return false; }

    @Override
    protected RawAnimation getGruntAttackAnimation(int attackId) {
        return attackId == BITE_ID ? BITE : OVERHEAD;
    }

    @Override
    protected int getGruntAttackAnimationTicks(int attackId) {
        return ATTACK_ANIMATION_TICKS;
    }

    @Override
    protected MeleeAttackPlan startMeleeAttack() {
        if (getRandom().nextInt(3) != 0) {
            // The slam is thrown from aiStep at the clip's impact, like the Grunt's smack.
            startGruntAttack(OVERHEAD_ID, 0);
            scheduleStrike(OVERHEAD_IMPACT_TICKS);
            return new MeleeAttackPlan(OVERHEAD_RECOVERY_TICKS, ATTACK_ANIMATION_TICKS, List.of());
        }
        MeleeAttackHit hit = new MeleeAttackHit(BITE_IMPACT_TICKS,
                BITE_DAMAGE / (float) getAttributeValue(Attributes.ATTACK_DAMAGE), 0.0F, false,
                0.0F, 0.0, 0.0F, 0.0, 0, 0, false,
                0.0F, false, 0.0, 0.0, 2.5, 0.0, 0.0);
        startGruntAttack(BITE_ID, 0);
        return new MeleeAttackPlan(BITE_RECOVERY_TICKS, ATTACK_ANIMATION_TICKS, List.of(hit));
    }

    @Override
    protected void performStrike(int attackId) {
        // Both hands come down together in front of him, so the blow is centred on him, not an arm.
        slamming = true;
        try {
            performSmack(0.0);
        } finally {
            slamming = false;
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_BLOCK_HIT,
                SoundSource.HOSTILE, 0.35F, 0.6F);
    }

    // A two-handed slam: wider and heavier than the Grunt's one-armed smack.
    @Override
    protected double getSmackScale() {
        return 1.2;
    }

    // Where his hands meet the ground in the slam clip, about a block and a half out.
    @Override
    protected double getSmackReach() {
        return 1.6;
    }

    @Override
    protected float getSmackDamage() {
        return OVERHEAD_DAMAGE;
    }

    @Override
    protected float getSmackPower() {
        return 1.4F;
    }

    @Override
    protected boolean doScaledHurtTarget(LivingEntity target, float damageMultiplier, float armorPierceFraction) {
        boolean hurt = super.doScaledHurtTarget(target, damageMultiplier, armorPierceFraction);
        // The slam spreads the rot like the bite; bite hits get theirs from applyAttackHitExtras.
        if (hurt && slamming && target.isAlive()) {
            target.addEffect(BogrotMechanics.naturalBite(), this);
        }
        return hurt;
    }

    @Override
    protected void applyAttackHitExtras(LivingEntity target, MeleeAttackHit hit) {
        if (target.isAlive()) {
            target.addEffect(BogrotMechanics.naturalBite(), this);
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != ModMobEffects.BOGROT.get() && super.canBeAffected(effect);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.PLAGUE_OGRE_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.PLAGUE_OGRE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.PLAGUE_OGRE_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return 1.0F;
    }
}
