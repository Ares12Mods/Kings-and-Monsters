package com.kingsandmonsters.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * An arrow from the Ogre King's seated arrow storm. It falls straight down, ignores every entity it
 * passes, and when it lands it splashes whichever players are close to the impact. That keeps the
 * storm from hurting ogres and makes the damage dependable; a falling arrow rarely hits a player's
 * hitbox directly.
 */
public final class StormArrow extends Arrow {
    private static final double SPLASH_RADIUS = 1.6;
    private static final int MAX_LIFETIME_TICKS = 100;
    // Landed arrows stay stuck in the ground this long, a few seconds of the aftermath without
    // piling up hundreds of entities when storms come often.
    private static final int LANDED_LIFETIME_TICKS = 20 * 30;

    private final float splashDamage;
    private int landedAtTick = -1;

    public StormArrow(Level level, double x, double y, double z, LivingEntity owner, float splashDamage) {
        super(level, x, y, z, new ItemStack(Items.ARROW), null);
        this.splashDamage = splashDamage;
        setOwner(owner);
        pickup = Pickup.DISALLOWED;
        // The vanilla landing sound is full volume per arrow; the storm plays its own quieter one.
        setSilent(true);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        // Let vanilla stick the arrow into the block, then splash once.
        super.onHitBlock(hit);
        if (!(level() instanceof ServerLevel serverLevel) || landedAtTick >= 0) {
            return;
        }
        landedAtTick = tickCount;
        Vec3 impact = hit.getLocation();
        AABB area = new AABB(impact, impact).inflate(SPLASH_RADIUS);
        for (Player player : serverLevel.getEntitiesOfClass(Player.class, area,
                candidate -> candidate.isAlive() && !candidate.isSpectator() && !candidate.isCreative()
                        && candidate.distanceToSqr(impact) <= SPLASH_RADIUS * SPLASH_RADIUS)) {
            player.hurt(damageSources().arrow(this, getOwner()), splashDamage);
        }
        serverLevel.sendParticles(ParticleTypes.CRIT, impact.x, impact.y + 0.1, impact.z, 3, 0.15, 0.05, 0.15, 0.1);
        serverLevel.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ARROW_HIT,
                SoundSource.HOSTILE, 0.5F, 0.9F + random.nextFloat() * 0.4F);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        // Arrows that never land (an open shaft, the void) shouldn't linger, and landed ones
        // are cleared after a while.
        boolean expired = landedAtTick < 0
                ? tickCount > MAX_LIFETIME_TICKS
                : tickCount - landedAtTick > LANDED_LIFETIME_TICKS;
        if (expired) {
            discard();
        }
    }
}
