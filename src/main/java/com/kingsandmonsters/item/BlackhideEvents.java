package com.kingsandmonsters.item;

import java.util.UUID;
import net.minecraft.world.entity.EquipmentSlot;
import com.kingsandmonsters.ModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.kingsandmonsters.event.LivingDamageEvent;
import com.kingsandmonsters.event.LivingIncomingDamageEvent;
import com.kingsandmonsters.event.PlayerTickEvent;
import com.kingsandmonsters.event.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/** Runs the Blackhide set: arrow deflection, and Charge (building momentum, the effect, and its impacts). */
public final class BlackhideEvents {
    private static final String MOMENTUM = "kingsandmonsters.blackhide_momentum";
    private static final String CHARGED_TICKS = "kingsandmonsters.blackhide_charged_ticks";
    private static final String CHARGE_GRACE_UNTIL = "kingsandmonsters.blackhide_charge_grace_until";
    private static final String WINDED_UNTIL = "kingsandmonsters.blackhide_winded_until";
    private static final String LAST_X = "kingsandmonsters.blackhide_last_x";
    private static final String LAST_Z = "kingsandmonsters.blackhide_last_z";
    private static final String RAMS = "kingsandmonsters.blackhide_rams";
    private static final String RAMMED_UNTIL = "kingsandmonsters.blackhide_rammed_until";
    private static final int RAM_REHIT_TICKS = 40;
    // Keep the client-side icon comfortably outside vanilla's expiring-effect flash window. The
    // actual half-second grace period is tracked separately and still controls when Charge ends.
    private static final int CHARGE_DISPLAY_DURATION_TICKS = BlackhideSet.CHARGE_MAX_TICKS + 20;
    private static final int CHARGE_DISPLAY_REFRESH_THRESHOLD_TICKS = BlackhideSet.CHARGE_MAX_TICKS - 20;
    private static final UUID MOMENTUM_SPEED_ID =
            ItemAttributes.uuid("blackhide_momentum");
    private static final double MIN_RUN_SPEED_SQR = 0.06 * 0.06;
    private static final double MAX_RUN_STEP_SQR = 2.0 * 2.0;

    /** Launches wait until the end of the tick, so vanilla's own knockback from the same hit cannot halve them. */
    private static final List<Launch> PENDING = new ArrayList<>();
    /** Set while a ram's own damage is dealt, so it is not mistaken for a weapon hit. */
    private static boolean ramming;

    private record Launch(LivingEntity target, Vec3 direction, double strength, double lift) {
    }

    private BlackhideEvents() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData();
        if (!BlackhideSet.hasFullSet(player)) {
            if (data.getInt(MOMENTUM) != 0) {
                reset(player);
            }
            if (player.hasEffect(ModMobEffects.CHARGE.get())) {
                player.removeEffect(ModMobEffects.CHARGE.get());
            }
            data.remove(LAST_X);
            return;
        }

        // The server's own xo/zo already equal the new position by now (the client's move packet was
        // applied before the tick), so the distance moved is measured from where the last tick left off.
        boolean tracked = data.contains(LAST_X);
        double dx = tracked ? player.getX() - data.getDouble(LAST_X) : 0.0;
        double dz = tracked ? player.getZ() - data.getDouble(LAST_Z) : 0.0;
        if (dx * dx + dz * dz > MAX_RUN_STEP_SQR) {
            // A teleport or respawn, not a step: it neither builds momentum nor aims a ram.
            dx = 0.0;
            dz = 0.0;
        }
        data.putDouble(LAST_X, player.getX());
        data.putDouble(LAST_Z, player.getZ());

        int momentum = data.getInt(MOMENTUM);
        boolean winded = player.level().getGameTime() < data.getLong(WINDED_UNTIL);
        // A jump keeps the run going even if the sprint flag drops mid-air (clipping a block, say).
        boolean sprinting = player.isSprinting() || (!player.onGround() && momentum > 0);
        boolean running = !winded && !player.isSpectator() && sprinting && dx * dx + dz * dz > MIN_RUN_SPEED_SQR
                && !player.isInWater() && !player.isFallFlying() && !player.isPassenger()
                && !player.getAbilities().flying;
        momentum = running
                ? Math.min(momentum + 1, BlackhideSet.BUILD_TICKS)
                : Math.max(momentum - BlackhideSet.DECAY_PER_TICK, 0);
        data.putInt(MOMENTUM, momentum);
        applyMomentumSpeed(player, momentum);

        long gameTime = player.level().getGameTime();
        if (running && momentum >= BlackhideSet.BUILD_TICKS) {
            MobEffectInstance charge = player.getEffect(ModMobEffects.CHARGE.get());
            data.putLong(CHARGE_GRACE_UNTIL, gameTime + BlackhideSet.CHARGE_GRACE_TICKS);
            // Refresh occasionally, while its displayed duration is still above the HUD's flashing range.
            if (charge == null || charge.getDuration() <= CHARGE_DISPLAY_REFRESH_THRESHOLD_TICKS) {
                player.addEffect(new MobEffectInstance(ModMobEffects.CHARGE.get(), CHARGE_DISPLAY_DURATION_TICKS, 0,
                        false, true, true));
            }
            if (charge == null) {
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.HOGLIN_ANGRY, SoundSource.PLAYERS, 0.8F, 1.3F);
            }
        } else if (player.hasEffect(ModMobEffects.CHARGE.get())
                && gameTime >= data.getLong(CHARGE_GRACE_UNTIL)) {
            player.removeEffect(ModMobEffects.CHARGE.get());
        }

        if (player.hasEffect(ModMobEffects.CHARGE.get())) {
            int held = data.getInt(CHARGED_TICKS) + 1;
            if (held >= BlackhideSet.CHARGE_MAX_TICKS) {
                windPlayer(player);
                return;
            }
            data.putInt(CHARGED_TICKS, held);
            if (player.tickCount % 2 == 0) {
                player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1,
                        player.getZ(), 1, 0.2, 0.0, 0.2, 0.01);
            }
            ramNearby(player, dx, dz);
        } else {
            // Once the Charge has lapsed, the ram it had already made is forgotten: the next Charge gets
            // its own free ram before the second one winds the wearer.
            data.putInt(RAMS, 0);
            if (momentum == 0) {
                // A pause long enough to lose all momentum is what resets the 12 seconds, so a brief lift
                // off the sprint key cannot be used to dodge being winded.
                data.putInt(CHARGED_TICKS, 0);
            }
        }
    }

    /** A Charge held too long runs out: the wearer is winded and cannot build momentum for a few seconds. */
    private static void windPlayer(ServerPlayer player) {
        spend(player);
        player.getPersistentData().putLong(WINDED_UNTIL,
                player.level().getGameTime() + BlackhideSet.WINDED_TICKS);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
                10, 0.4, 0.3, 0.4, 0.04);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.HORSE_BREATHE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** Running into something while charged is an impact on its own, without a swing. */
    private static void ramNearby(ServerPlayer player, double dx, double dz) {
        Vec3 direction = runDirection(player, dx, dz);
        AABB reach = player.getBoundingBox().inflate(0.3, 0.0, 0.3).move(direction.scale(0.4));
        List<LivingEntity> hits = player.level().getEntitiesOfClass(LivingEntity.class, reach,
                target -> canRam(player, target));
        if (hits.isEmpty()) return;

        long now = player.level().getGameTime();
        for (LivingEntity target : hits) {
            // The same body is not rammed again as the wearer charges through it.
            target.getPersistentData().putLong(RAMMED_UNTIL, now + RAM_REHIT_TICKS);
            ramming = true;
            try {
                target.hurt(player.damageSources().playerAttack(player), BlackhideSet.RAM_DAMAGE);
            } finally {
                ramming = false;
            }
            PENDING.add(new Launch(target, direction, BlackhideSet.RAM_KNOCKBACK, BlackhideSet.RAM_LIFT));
            impactEffects(player.serverLevel(), target, false);
        }
        // The first ram is charged through, with the Charge intact; the second spends it and winds the wearer.
        int rams = player.getPersistentData().getInt(RAMS) + 1;
        if (rams >= BlackhideSet.RAMS_PER_CHARGE) {
            windPlayer(player);
        } else {
            player.getPersistentData().putInt(RAMS, rams);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (deflectsMissile(event)) {
            // Cancelled, the arrow glances off, the way it does from a shield.
            event.setCanceled(true);
            return;
        }
        if (isChargedMeleeHit(event.getSource())
                && holdsWeapon((ServerPlayer) event.getSource().getEntity())) {
            event.setAmount(event.getAmount() * BlackhideSet.WEAPON_DAMAGE_MULTIPLIER);
        }
    }

    private static boolean deflectsMissile(LivingIncomingDamageEvent event) {
        if (!BlackhideSet.isDeflectable(event.getSource().getDirectEntity())) return false;
        LivingEntity wearer = event.getEntity();
        int pieces = BlackhideSet.piecesWorn(wearer);
        if (pieces == 0 || wearer.getRandom().nextFloat() >= pieces * BlackhideSet.MISSILE_DEFLECT_PER_PIECE) {
            return false;
        }
        if (wearer.level() instanceof ServerLevel level) {
            level.playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.4F);
            level.sendParticles(ParticleTypes.CRIT, wearer.getX(), wearer.getY() + wearer.getBbHeight() * 0.6,
                    wearer.getZ(), 6, 0.3, 0.3, 0.3, 0.2);
        }
        return true;
    }

    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!isChargedMeleeHit(event.getSource())) return;
        ServerPlayer player = (ServerPlayer) event.getSource().getEntity();
        LivingEntity target = event.getEntity();

        Vec3 away = target.position().subtract(player.position());
        Vec3 direction = new Vec3(away.x, 0.0, away.z);
        direction = direction.lengthSqr() < 1.0E-4 ? player.getLookAngle() : direction.normalize();
        boolean weapon = holdsWeapon(player);
        PENDING.add(weapon
                ? new Launch(target, direction, BlackhideSet.WEAPON_KNOCKBACK, BlackhideSet.WEAPON_LIFT)
                : new Launch(target, direction, BlackhideSet.RAM_KNOCKBACK, BlackhideSet.RAM_LIFT));
        impactEffects(player.serverLevel(), target, weapon);
        spend(player);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        for (Launch launch : PENDING) {
            LivingEntity target = launch.target();
            if (!target.isAlive()) continue;
            double passes = 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            if (passes <= 0.0) continue;
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(launch.direction().x * launch.strength() * passes,
                    Math.max(motion.y, launch.lift() * passes),
                    launch.direction().z * launch.strength() * passes);
            target.hurtMarked = true;
            target.setOnGround(false);
        }
        PENDING.clear();
    }

    /** A bare melee blow from the wearer while Charged. Thorns, reflected damage and the like do not count. */
    private static boolean isChargedMeleeHit(DamageSource source) {
        return !ramming
                && source.is(DamageTypes.PLAYER_ATTACK)
                && source.getEntity() instanceof ServerPlayer player
                && source.getDirectEntity() == player
                && player.hasEffect(ModMobEffects.CHARGE.get())
                && BlackhideSet.hasFullSet(player);
    }

    /** A tool or sword in the main hand, as opposed to a bare fist or a held block. */
    private static boolean holdsWeapon(ServerPlayer player) {
        return player.getMainHandItem().getAttributeModifiers(EquipmentSlot.MAINHAND)
                .containsKey(Attributes.ATTACK_DAMAGE);
    }

    private static boolean canRam(ServerPlayer player, LivingEntity target) {
        long now = target.level().getGameTime();
        if (target.getPersistentData().getLong(RAMMED_UNTIL) > now) {
            return false;
        }
        if (target == player || !target.isAlive() || target.isSpectator() || target instanceof ArmorStand
                || player.isPassengerOfSameVehicle(target) || target instanceof EnderDragon
                || target instanceof Player creative && creative.getAbilities().invulnerable) {
            return false;
        }
        if (target instanceof OwnableEntity pet && player.getUUID().equals(pet.getOwnerUUID())) return false;
        return !(target instanceof Player other) || player.canHarmPlayer(other);
    }

    private static Vec3 runDirection(ServerPlayer player, double dx, double dz) {
        Vec3 moved = new Vec3(dx, 0.0, dz);
        if (moved.lengthSqr() > MIN_RUN_SPEED_SQR) return moved.normalize();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static void impactEffects(ServerLevel level, LivingEntity target, boolean weapon) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, weapon ? 2 : 1, 0.3, 0.3, 0.3, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, weapon ? 24 : 14, 0.35, 0.3, 0.35, 0.18);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, weapon ? 28 : 16, 0.4, 0.4, 0.4, 0.5);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.3F, 0.7F);
        if (weapon) {
            level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** An impact uses the Charge up: it has to be built from a standing start again. */
    private static void spend(ServerPlayer player) {
        player.removeEffect(ModMobEffects.CHARGE.get());
        reset(player);
    }

    private static void reset(ServerPlayer player) {
        player.getPersistentData().putInt(MOMENTUM, 0);
        player.getPersistentData().putInt(CHARGED_TICKS, 0);
        player.getPersistentData().putInt(RAMS, 0);
        player.getPersistentData().remove(CHARGE_GRACE_UNTIL);
        applyMomentumSpeed(player, 0);
    }

    private static void applyMomentumSpeed(ServerPlayer player, int momentum) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double amount = BlackhideSet.MAX_MOMENTUM_SPEED * momentum / BlackhideSet.BUILD_TICKS;
        AttributeModifier current = speed.getModifier(MOMENTUM_SPEED_ID);
        if (amount <= 0.0) {
            if (current != null) speed.removeModifier(MOMENTUM_SPEED_ID);
        } else if (current == null || Math.abs(current.getAmount() - amount) > 0.004) {
            ItemAttributes.addOrUpdate(speed, new AttributeModifier(MOMENTUM_SPEED_ID, MOMENTUM_SPEED_ID.toString(), amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
}
