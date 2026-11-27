package com.kingsandmonsters.entity;

import java.util.Map;
import com.kingsandmonsters.Config;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.effect.CombatEffects;
import com.kingsandmonsters.item.BogIronEquipmentEvents;
import com.kingsandmonsters.network.ScreenShakePayload;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animation.RawAnimation;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The battle-hardened captain of a Grunt-controlled outpost.
 *
 * <p>He moves and fights like the Grunt: idle, walk and combat walk, with every attack thrown over
 * the walk except the sword salute. He is bigger and stronger, so his swings reach further, hit
 * harder and land heavier, and his steps are deeper.</p>
 */
public class OgreGruntCaptain extends OgreGrunt {
    private static final float BOG_IRON_SWORD_SPAWN_CHANCE = 0.25F;
    // Captains use 120% of the Grunt's default movement attribute (0.33264), so this
    // multiplier yields an effective pursuit speed of about 0.35 — the Grunt's own
    // pre-nerf speed, making the Captain the faster, more threatening pursuer.
    private static final double CAPTAIN_PURSUIT_SPEED_MODIFIER = 0.35 / (0.2772 * 1.20);
    private static final RawAnimation SWORD_SLASH = RawAnimation.begin().thenPlay("sword_slash");
    private static final RawAnimation SWORD_BUTT = RawAnimation.begin().thenPlay("sword_butt");
    private static final RawAnimation SMACK = RawAnimation.begin().thenPlay("regular attack 1");
    private static final RawAnimation SWORD_SALUTE = RawAnimation.begin().thenPlay("sword_salute");
    private static final int SWORD_SLASH_ID = 3;
    private static final int SWORD_BUTT_ID = 4;
    private static final int SMACK_ID = 5;
    private static final int SWORD_SALUTE_ID = 6;
    // Clip lengths, the tick each one lands on (the clip's impact plus the sync delay) and how long
    // he waits before the next swing. Each recovery leaves about a second after the clip.
    private static final int SWORD_SLASH_ANIMATION_TICKS = 35;
    private static final int SWORD_BUTT_ANIMATION_TICKS = 40;
    private static final int CAPTAIN_SMACK_ANIMATION_TICKS = 45;
    // The blade crosses the front of him partway through the sweep (clip ticks 13 to 23), not at its end, so the
    // hit and its sound land earlier than the other swings do relative to their clips.
    private static final int SWORD_SLASH_IMPACT_TICKS = 10 + CLIP_LAG_TICKS;
    private static final int SWORD_BUTT_IMPACT_TICKS = 18 + CLIP_LAG_TICKS;
    private static final int CAPTAIN_SMACK_IMPACT_TICKS = 20 + CLIP_LAG_TICKS;
    private static final int SWORD_SLASH_RECOVERY_TICKS = 55;
    private static final int SWORD_BUTT_RECOVERY_TICKS = 60;
    private static final int CAPTAIN_SMACK_RECOVERY_TICKS = 65;
    // He hits harder than a Grunt: raw damage before armor.
    private static final float SWORD_SLASH_DAMAGE = OgreDamage.raw(OgreDamage.Tier.CAPTAIN, OgreDamage.Blow.STANDARD);
    private static final float SWORD_BUTT_DAMAGE = OgreDamage.raw(OgreDamage.Tier.CAPTAIN, OgreDamage.Blow.LIGHT);
    private static final float BITE_DAMAGE = OgreDamage.raw(OgreDamage.Tier.CAPTAIN, OgreDamage.Blow.STANDARD);
    private static final float SMACK_DAMAGE = OgreDamage.raw(OgreDamage.Tier.CAPTAIN, OgreDamage.Blow.HEAVY);
    // The slash sweeps an arc in front of him, the pommel strike drives down a narrow lane.
    private static final double SLASH_REACH = 3.3;
    private static final double SLASH_HALF_WIDTH = 2.5;
    private static final double SLASH_RADIUS = Math.hypot(SLASH_REACH, SLASH_HALF_WIDTH);
    private static final double BUTT_REACH = 3.2;
    private static final double BUTT_HALF_WIDTH = 1.2;
    private static final double ATTACK_START_RANGE = 2.9;
    // The ticks after the swing starts that the sweep streak is drawn on.
    private static final int SLASH_TRAIL_START_TICK = 11;
    private static final int SLASH_TRAIL_END_TICK = 17;
    private static final double COMBAT_WALK_SPEED = 0.8;
    private static final int SWORD_SALUTE_ANIMATION_TICKS = 50;
    private static final int SWORD_SALUTE_BUFF_DELAY_TICKS = 28;
    private static final int SWORD_SALUTE_BUFF_TICKS = 30 * 20;
    private static final double SWORD_SALUTE_BUFF_RADIUS = 16.0;
    private static final DustParticleOptions SWORD_SALUTE_RING =
            new DustParticleOptions(new Vector3f(0.14F, 0.68F, 0.18F), 1.05F);
    private int activeCaptainAttackId;
    private int swordSaluteTicks;
    private boolean swordSaluteUsed;
    private boolean swordSaluteBuffApplied;
    private boolean swordSalutePending;
    private int swordSaluteRingTicks;
    private int slashTrailTicks = -1;
    private int lastCaptainAttackId;

    public OgreGruntCaptain(EntityType<? extends OgreGruntCaptain> type, Level level) {
        super(type, level);
        ensureCaptainTitle();
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        // The Captain's dedicated loot table controls rewards; prevent a second equipment roll.
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    @SuppressWarnings("deprecation") // Required 1.21.1 spawn initialization hook.
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        // Roll once when the captain enters the world. Equipment serialization preserves the
        // variant on save/load; the existing renderer uses this same hand slot and transform.
        if (getRandom().nextFloat() < BOG_IRON_SWORD_SPAWN_CHANCE) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BOG_IRON_SWORD.get()));
        } else {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return result;
    }

    private void ensureCaptainTitle() {
        if (!hasCustomName()) {
            setCustomName(Component.translatable("title.kingsandmonsters.grunt_captain"));
        }
        setCustomNameVisible(Config.SHOW_CAPTAIN_NAMETAGS.get());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return OgreGrunt.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ARMOR, 15.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.66);
    }

    @Override
    public void applyConfiguredCombatAttributes(boolean healToMax) {
        float healthBeforeGruntConfiguration = getHealth();
        super.applyConfiguredCombatAttributes(healToMax);
        if (!Config.isLoaded()) {
            return;
        }
        AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(Config.OGRE_GRUNT_CAPTAIN_MAX_HEALTH.get());
            if (healToMax) {
                setHealth(getMaxHealth());
            } else {
                // The shared Grunt configuration temporarily lowers the maximum to the Grunt's
                // value. Restore the Captain's saved health after installing his own maximum.
                setHealth(Math.min(healthBeforeGruntConfiguration, getMaxHealth()));
            }
        }
        AttributeInstance armor = getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(Config.OGRE_GRUNT_CAPTAIN_ARMOR.get());
        }
        AttributeInstance movementSpeed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(Config.OGRE_GRUNT_MOVEMENT_SPEED.get() * 1.20);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        if (isDeadOrDying()) {
            // Dying mid-swing, mid-salute or mid-trail ends all of it.
            swordSaluteTicks = 0;
            swordSalutePending = false;
            swordSaluteRingTicks = 0;
            slashTrailTicks = -1;
            return;
        }

        if (!swordSaluteUsed && getHealth() <= getMaxHealth() * 0.5F) {
            swordSalutePending = true;
        }

        if (swordSalutePending && !hasActiveMeleeAttack()) {
            beginSwordSalute();
        }

        if (swordSaluteTicks > 0) {
            int elapsedTicks = SWORD_SALUTE_ANIMATION_TICKS - swordSaluteTicks;
            if (!swordSaluteBuffApplied && elapsedTicks >= SWORD_SALUTE_BUFF_DELAY_TICKS) {
                applySwordSaluteBuff();
            }
            swordSaluteTicks--;
            getNavigation().stop();
            setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
            if (swordSaluteTicks == 0) {
                onMeleeAttackPlanFinished();
            }
        }

        if (swordSaluteRingTicks > 0) {
            spawnSwordSaluteRingStep();
            swordSaluteRingTicks--;
        }

        tickSlashTrail();
    }

    private void beginSwordSalute() {
        swordSaluteUsed = true;
        swordSalutePending = false;
        swordSaluteBuffApplied = false;
        swordSaluteTicks = SWORD_SALUTE_ANIMATION_TICKS;
        activeCaptainAttackId = SWORD_SALUTE_ID;
        startGruntAttack(SWORD_SALUTE_ID, SWORD_SALUTE_ANIMATION_TICKS);

    }

    private void applySwordSaluteBuff() {
        swordSaluteBuffApplied = true;
        level().playSound(null, getX(), getY(), getZ(),
                ModSoundEvents.OGRE_GRUNT_CAPTAIN_SALUTE.get(), SoundSource.HOSTILE, 1.1F, 1.0F);
        for (OgreGrunt ogre : level().getEntitiesOfClass(OgreGrunt.class,
                getBoundingBox().inflate(SWORD_SALUTE_BUFF_RADIUS), OgreGrunt::isAlive)) {
            ogre.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                    SWORD_SALUTE_BUFF_TICKS, 0, false, true, true), this);
            ogre.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    SWORD_SALUTE_BUFF_TICKS, 0, false, true, true), this);
        }

        if (level() instanceof ServerLevel serverLevel) {
            swordSaluteRingTicks = 8;
        }
    }

    private void spawnSwordSaluteRingStep() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int completedSteps = 8 - swordSaluteRingTicks + 1;
        double radius = SWORD_SALUTE_BUFF_RADIUS * completedSteps / 8.0;
        int particleCount = 16;
        for (int index = 0; index < particleCount; index++) {
            double angle = Math.PI * 2.0 * index / particleCount;
            double x = getX() + Math.cos(angle) * radius;
            double z = getZ() + Math.sin(angle) * radius;
            double y = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) Math.floor(x), (int) Math.floor(z)) + 0.18;
            serverLevel.sendParticles(SWORD_SALUTE_RING, x, y, z,
                    1, 0.0, 0.025, 0.0, 0.0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("SwordSaluteUsed", swordSaluteUsed);
        tag.putBoolean("SwordSaluteBuffApplied", swordSaluteBuffApplied);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ensureCaptainTitle();
        swordSaluteUsed = tag.getBoolean("SwordSaluteUsed");
        swordSaluteBuffApplied = tag.getBoolean("SwordSaluteBuffApplied");
    }

    @Override
    public float getVoicePitch() {
        return 0.98F + getRandom().nextFloat() * 0.04F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.OGRE_GRUNT_CAPTAIN_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.OGRE_GRUNT_CAPTAIN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.OGRE_GRUNT_CAPTAIN_DEATH.get();
    }

    @Override
    protected boolean usesOnlyBiteAttack() {
        return true;
    }

    @Override
    protected double getMeleePursuitSpeedModifier() {
        return CAPTAIN_PURSUIT_SPEED_MODIFIER;
    }

    @Override
    protected double getCombatWalkSpeedModifier() {
        // About 0.245 against his 0.306 attribute: quicker than a Grunt's combat walk.
        return COMBAT_WALK_SPEED;
    }

    // His own clips: the combat walk is 1.2741s, so the feet land at 0.637s.
    private static final Map<String, double[]> CAPTAIN_FOOT_CONTACTS = Map.of(
            "walk", new double[]{0.0, 20.0},
            "combat walk", new double[]{0.0, 12.74});

    @Override
    protected Map<String, double[]> getFootContacts() {
        return CAPTAIN_FOOT_CONTACTS;
    }

    @Override
    protected double getWanderSpeedModifier() {
        // Captains use a 0.33264 default movement attribute after configuration;
        // convert the requested effective wandering speed into a goal multiplier.
        return 0.12 / (0.2772 * 1.20);
    }

    @Override
    protected double getMeleeAttackReachSqr(LivingEntity target) {
        return ATTACK_START_RANGE * ATTACK_START_RANGE;
    }

    @Override
    protected MeleeAttackPlan startMeleeAttack() {
        // The regular attack (smack) and the slash are his bread and butter, then the sword butt and
        // the bite; never the same attack twice in a row.
        List<Integer> choices = new ArrayList<>(List.of(
                SMACK_ID, SMACK_ID, SMACK_ID, SMACK_ID,
                SWORD_SLASH_ID, SWORD_SLASH_ID, SWORD_SLASH_ID, SWORD_SLASH_ID,
                SWORD_BUTT_ID, SWORD_BUTT_ID,
                2, 2));
        choices.removeIf(id -> id == lastCaptainAttackId);
        int pick = choices.get(getRandom().nextInt(choices.size()));
        lastCaptainAttackId = pick;
        return switch (pick) {
            case 2 -> startCaptainBite();
            case SWORD_SLASH_ID -> startCaptainSwing(SWORD_SLASH_ID, SWORD_SLASH_ANIMATION_TICKS,
                    SWORD_SLASH_IMPACT_TICKS, SWORD_SLASH_RECOVERY_TICKS);
            case SWORD_BUTT_ID -> startCaptainSwing(SWORD_BUTT_ID, SWORD_BUTT_ANIMATION_TICKS,
                    SWORD_BUTT_IMPACT_TICKS, SWORD_BUTT_RECOVERY_TICKS);
            default -> startCaptainSwing(SMACK_ID, CAPTAIN_SMACK_ANIMATION_TICKS,
                    CAPTAIN_SMACK_IMPACT_TICKS, CAPTAIN_SMACK_RECOVERY_TICKS);
        };
    }

    @Override
    protected boolean isPerformingNonMeleeAction() {
        return swordSaluteTicks > 0;
    }

    private MeleeAttackPlan startCaptainBite() {
        activeCaptainAttackId = 2;
        return startBitePlan((float) getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    /** A swing thrown over the combat walk: its impact comes from aiStep at the clip's impact tick. */
    private MeleeAttackPlan startCaptainSwing(int attackId, int animationTicks, int impactTicks, int recoveryTicks) {
        activeCaptainAttackId = attackId;
        startGruntAttack(attackId, 0);
        slashTrailTicks = attackId == SWORD_SLASH_ID ? 0 : -1;
        scheduleStrike(impactTicks);
        return new MeleeAttackPlan(recoveryTicks, animationTicks, List.of());
    }

    @Override
    protected boolean isStrikeAttack(int attackId) {
        return attackId == SWORD_SLASH_ID || attackId == SWORD_BUTT_ID || attackId == SMACK_ID;
    }

    @Override
    protected void performStrike(int attackId) {
        switch (attackId) {
            case SWORD_SLASH_ID -> performSwordSlash();
            case SWORD_BUTT_ID -> performSwordButt();
            // His sword is in his right hand, so the unarmed smack is thrown with the left.
            default -> performSmack(-1.0);
        }
    }

    @Override
    protected boolean playsOverWalk(int attackId) {
        // Everything is thrown while he walks except the sword salute, which takes his whole body.
        return attackId != SWORD_SALUTE_ID;
    }

    /**
     * The vanilla sweep-attack streak, following the blade from the raised sword down across the
     * front of him while the slash swings, so it reads even when it lands on nothing.
     */
    private void tickSlashTrail() {
        if (slashTrailTicks < 0) {
            return;
        }
        if (activeCaptainAttackId != SWORD_SLASH_ID || !(level() instanceof ServerLevel serverLevel)) {
            slashTrailTicks = -1;
            return;
        }
        int tick = slashTrailTicks++;
        if (tick < SLASH_TRAIL_START_TICK || tick > SLASH_TRAIL_END_TICK) {
            return;
        }
        double progress = (tick - SLASH_TRAIL_START_TICK) / (double) (SLASH_TRAIL_END_TICK - SLASH_TRAIL_START_TICK);
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        // From over his right shoulder, down and across to his left.
        Vec3 at = position()
                .add(forward.scale(1.7 + 0.5 * Math.sin(progress * Math.PI)))
                .add(right.scale(Mth.lerp(progress, 1.3, -1.3)))
                .add(0.0, Mth.lerp(progress, 2.1, 0.9), 0.0);
        // Count 0 sends the offset as the particle's velocity; the sweep reads it as 1 - x / 2 for its
        // size, so -2.4 gives a streak about 2.2 blocks across (1.0 was a speck).
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 0, -2.4, 0.0, 0.0, 1.0);
    }

    /**
     * The blade sweeping through the whole arc in front of him, whatever height the target is at
     * and with no line of sight needed, so trees and jumping can't dodge it. Weaker toward the far
     * end of the arc. Anyone it cuts can be poisoned by a Bog Iron blade.
     */
    private void performSwordSlash() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double search = SLASH_REACH + SLASH_HALF_WIDTH;
        float damageMultiplier = SWORD_SLASH_DAMAGE / (float) getAttributeValue(Attributes.ATTACK_DAMAGE);

        for (LivingEntity target : level().getEntitiesOfClass(
                LivingEntity.class, getBoundingBox().inflate(search, 3.0, search),
                candidate -> candidate != this && candidate.isAlive() && canAttackOgreTarget(candidate))) {
            AABB bounds = target.getBoundingBox();
            Vec3 relative = bounds.getCenter().subtract(position());
            double halfWidth = target.getBbWidth() * 0.5;
            double ahead = relative.dot(forward);
            double across = relative.dot(right);
            boolean inArc = ahead >= -0.3 - halfWidth && ahead <= SLASH_REACH + halfWidth
                    && Math.abs(across) <= SLASH_HALF_WIDTH + halfWidth;
            boolean reachable = bounds.maxY >= getY() - 0.5 && bounds.minY <= getY() + 3.2;
            if (!inArc || !reachable) {
                continue;
            }
            float falloff = areaFalloff(Math.hypot(ahead, across), SLASH_RADIUS, OgreDamage.ARC_EDGE_SHARE);
            if (doScaledHurtTarget(target, damageMultiplier * falloff)) {
                BogIronEquipmentEvents.tryApplySwordPoison(this, target);
                target.knockback(0.6 * falloff, getX() - target.getX(), getZ() - target.getZ());
            }
        }

        Vec3 at = position().add(forward.scale(SLASH_REACH * 0.8));
        level().playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.1F, 0.75F);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.IRON_GOLEM_ATTACK,
                SoundSource.HOSTILE, 0.6F, 1.1F);
    }

    /**
     * The pommel driven down a narrow lane in front of him: short and heavy. Whoever it lands on
     * is dazed and shoved back.
     */
    private void performSwordButt() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double search = BUTT_REACH + BUTT_HALF_WIDTH;
        float damageMultiplier = SWORD_BUTT_DAMAGE / (float) getAttributeValue(Attributes.ATTACK_DAMAGE);

        for (LivingEntity target : level().getEntitiesOfClass(
                LivingEntity.class, getBoundingBox().inflate(search, 3.0, search),
                candidate -> candidate != this && candidate.isAlive() && canAttackOgreTarget(candidate))) {
            AABB bounds = target.getBoundingBox();
            Vec3 relative = bounds.getCenter().subtract(position());
            double halfWidth = target.getBbWidth() * 0.5;
            double ahead = relative.dot(forward);
            double across = relative.dot(right);
            boolean inLane = ahead >= -0.3 - halfWidth && ahead <= BUTT_REACH + halfWidth
                    && Math.abs(across) <= BUTT_HALF_WIDTH + halfWidth;
            boolean reachable = bounds.maxY >= getY() - 0.5 && bounds.minY <= getY() + 3.2;
            if (!inLane || !reachable || !doScaledHurtTarget(target, damageMultiplier)) {
                continue;
            }
            if (!(target instanceof OgreGrunt)) {
                CombatEffects.applyDazed(target, Config.DAZED_HEAVY_ATTACK_DURATION_TICKS.get(), this);
            }
            target.knockback(1.0, getX() - target.getX(), getZ() - target.getZ());
        }

        Vec3 at = position().add(forward.scale(BUTT_REACH * 0.8));
        level().playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.HOSTILE, 1.1F, 0.7F);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 0.4F, 0.9F);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.MUD_HIT,
                SoundSource.HOSTILE, 0.5F, 0.65F);
        shake(at, 0.35F);
    }

    private void shake(Vec3 at, float intensity) {
        if (level() instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersNear(serverLevel, null, at.x, at.y, at.z, 20.0,
                    new ScreenShakePayload(at.x, at.y, at.z, intensity));
        }
    }

    // The unarmed smack is a bigger, harder take on the Grunt's.
    @Override
    protected double getSmackScale() {
        return 1.3;
    }

    @Override
    protected float getSmackDamage() {
        return SMACK_DAMAGE;
    }

    @Override
    protected float getSmackPower() {
        return 1.5F;
    }

    @Override
    protected int getBiteImpactTicks() {
        // The bite lands 0.67s (tick 13) into its clip, plus the same lag as the swings.
        return 13 + CLIP_LAG_TICKS;
    }

    @Override
    protected float getBiteDamage() {
        return BITE_DAMAGE;
    }

    @Override
    protected void onMeleeAttackPlanFinished() {
        super.onMeleeAttackPlanFinished();
        activeCaptainAttackId = 0;
    }

    @Override
    protected RawAnimation getGruntAttackAnimation(int attackId) {
        return switch (attackId) {
            case SWORD_SLASH_ID -> SWORD_SLASH;
            case SWORD_BUTT_ID -> SWORD_BUTT;
            case SMACK_ID -> SMACK;
            case SWORD_SALUTE_ID -> SWORD_SALUTE;
            default -> super.getGruntAttackAnimation(attackId);
        };
    }

    @Override
    protected int getGruntAttackAnimationTicks(int attackId) {
        return switch (attackId) {
            case SWORD_SLASH_ID -> SWORD_SLASH_ANIMATION_TICKS;
            case SWORD_BUTT_ID -> SWORD_BUTT_ANIMATION_TICKS;
            case SMACK_ID -> CAPTAIN_SMACK_ANIMATION_TICKS;
            case SWORD_SALUTE_ID -> SWORD_SALUTE_ANIMATION_TICKS;
            default -> super.getGruntAttackAnimationTicks(attackId);
        };
    }

    // A bigger ogre: heavier, deeper steps than a Grunt's, a touch under the Brute's.
    @Override
    protected float getFootfallVolumeScale() {
        return 1.4F;
    }

    @Override
    protected float getFootfallPitchScale() {
        return 0.85F;
    }
}
