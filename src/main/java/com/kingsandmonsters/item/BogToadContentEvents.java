package com.kingsandmonsters.item;

import java.util.UUID;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.effect.BogrotMechanics;
import com.kingsandmonsters.effect.EffectImmunityRules;
import com.kingsandmonsters.enchantment.ModEnchantmentEffects;
import com.kingsandmonsters.enchantment.ModEnchantments;
import com.kingsandmonsters.advancement.RoyalAdvancementEvents;
import net.minecraft.core.particles.ParticleTypes;
import com.kingsandmonsters.entity.BogToadLandingImpact;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import com.kingsandmonsters.event.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import com.kingsandmonsters.event.PlayerTickEvent;

public final class BogToadContentEvents {
    private static final UUID SWIM_BONUS_ID =
            ItemAttributes.uuid("boghide_boots_swim_speed");
    private static final AttributeModifier SWIM_BONUS = new AttributeModifier(SWIM_BONUS_ID, SWIM_BONUS_ID.toString(), 0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final String AMBUSH_MARK_UNTIL = "kingsandmonsters.ambush_mark_until";
    private static final String AMBUSH_MARK_OWNER = "kingsandmonsters.ambush_mark_owner";
    private static final String LEAP_UNTIL = "kingsandmonsters.bog_toad_leap_until";
    private static final String IMPACT_UNTIL = "kingsandmonsters.bog_toad_impact_until";
    private static final String LEAP_AIRBORNE = "kingsandmonsters.bog_toad_leap_airborne";
    private static final String LEAP_DIMENSION = "kingsandmonsters.bog_toad_leap_dimension";
    private static final float PLAYER_LANDING_DAMAGE = 5.0F;
    private static final double BOGHIDE_LEAP_HORIZONTAL_SPEED = 0.90;
    private static final double BOGHIDE_LEAP_VERTICAL_SPEED = 0.72;
    private static final String RUPTURE_COOLDOWN = "kingsandmonsters.bogfang_rupture_cooldown";
    private static final String RUPTURE_GUARD = "kingsandmonsters.bogfang_rupture_guard";
    private static final String CRIT_TARGET = "kingsandmonsters.bogfang_crit_target";
    private static final String CRIT_TICK = "kingsandmonsters.bogfang_crit_tick";
    // Keep the HUD timer comfortably above vanilla's expiring-effect flash window.
    private static final int BOGSTRIDE_DISPLAY_DURATION_TICKS = 13 * 20;
    private static final int BOGSTRIDE_REFRESH_THRESHOLD_TICKS = 11 * 20;
    private static final TagKey<EntityType<?>> OGRES = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(KingsAndMonsters.MODID, "ogres"));

    private BogToadContentEvents() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        var swimSpeed = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        if (swimSpeed == null) return;
        boolean wearing = player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BOGHIDE_BOOTS.get());
        if (wearing && swimSpeed.getModifier(SWIM_BONUS_ID) == null) {
            swimSpeed.addTransientModifier(SWIM_BONUS);
        } else if (!wearing && swimSpeed.getModifier(SWIM_BONUS_ID) != null) {
            swimSpeed.removeModifier(SWIM_BONUS_ID);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (wearing && player.isInWater()) {
            // Refresh infrequently so the icon remains steady without spamming effect packets.
            MobEffectInstance stride = player.getEffect(ModMobEffects.BOGSTRIDE.get());
            if (stride == null || stride.getDuration() <= BOGSTRIDE_REFRESH_THRESHOLD_TICKS) {
                player.addEffect(new MobEffectInstance(ModMobEffects.BOGSTRIDE.get(),
                        BOGSTRIDE_DISPLAY_DURATION_TICKS, 0, false, true, true));
            }
            // Every other tick, give back a bit of air: doubles the breath without water breathing.
            if (player.getAirSupply() < player.getMaxAirSupply() && player.tickCount % 2 == 0) {
                player.setAirSupply(player.getAirSupply() + 1);
            }
        } else if (player.hasEffect(ModMobEffects.BOGSTRIDE.get())) {
            // The longer display duration is only visual bookkeeping; remove it as soon as it is inactive.
            player.removeEffect(ModMobEffects.BOGSTRIDE.get());
        }
        tickPendingLanding(serverPlayer, wearing);
    }

    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isSprinting()) return;
        var boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ModItems.BOGHIDE_BOOTS.get()) || player.getCooldowns().isOnCooldown(boots.getItem())) return;
        var forward = player.getLookAngle().multiply(BOGHIDE_LEAP_HORIZONTAL_SPEED, 0.0,
                BOGHIDE_LEAP_HORIZONTAL_SPEED);
        player.setDeltaMovement(forward.x, BOGHIDE_LEAP_VERTICAL_SPEED, forward.z);
        player.hurtMarked = true;
        player.getCooldowns().addCooldown(boots.getItem(), BoghideSet.leapCooldownTicks(player));
        ItemUtil.hurt(boots, 1, player, EquipmentSlot.FEET);
        player.getPersistentData().putLong(LEAP_UNTIL, player.level().getGameTime() + 12);
        player.getPersistentData().putLong(IMPACT_UNTIL, player.level().getGameTime() + 10 * 20);
        player.getPersistentData().putBoolean(LEAP_AIRBORNE, false);
        player.getPersistentData().putString(LEAP_DIMENSION, player.level().dimension().location().toString());
    }

    public static boolean isPerformingBogToadLeap(Player player) {
        return player.getPersistentData().getLong(LEAP_UNTIL) >= player.level().getGameTime();
    }

    private static void tickPendingLanding(ServerPlayer player, boolean wearing) {
        var data = player.getPersistentData();
        long deadline = data.getLong(IMPACT_UNTIL);
        if (deadline == 0L) return;
        // Shallow-water contact does not cancel a leap that is still waiting to land.
        boolean invalid = !player.isAlive() || !wearing
                || player.level().getGameTime() > deadline
                || !data.getString(LEAP_DIMENSION).equals(player.level().dimension().location().toString());
        if (invalid) {
            clearPendingLanding(player);
            return;
        }
        if (!player.onGround()) {
            data.putBoolean(LEAP_AIRBORNE, true);
        } else if (data.getBoolean(LEAP_AIRBORNE)) {
            applyPlayerLandingImpact(player);
            clearPendingLanding(player);
        }
    }

    private static void applyPlayerLandingImpact(ServerPlayer player) {
        BogToadLandingImpact.spawnVisuals(player.serverLevel(), player, true);
        double radius = BogToadLandingImpact.RADIUS;
        boolean ambush = BoghideSet.hasFullSet(player);
        if (ambush) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BoghideSet.AMBUSH_EFFECT_TICKS, 0));
        }
        AABB area = player.getBoundingBox().inflate(radius, BogToadLandingImpact.VERTICAL_TOLERANCE, radius);
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
                target -> target != player && target.isAlive() && target instanceof Enemy
                        && !player.isAlliedTo(target))) {
            double dx = Math.max(0.0, Math.max(target.getBoundingBox().minX - player.getX(),
                    player.getX() - target.getBoundingBox().maxX));
            double dz = Math.max(0.0, Math.max(target.getBoundingBox().minZ - player.getZ(),
                    player.getZ() - target.getBoundingBox().maxZ));
            if (dx * dx + dz * dz <= radius * radius
                    && Math.abs(target.getY() - player.getY()) <= BogToadLandingImpact.VERTICAL_TOLERANCE) {
                target.hurt(player.damageSources().playerAttack(player), PLAYER_LANDING_DAMAGE);
                if (ambush) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            BoghideSet.AMBUSH_EFFECT_TICKS, 1), player);
                    // Marked after the landing hit, so only the follow-up blows are amplified.
                    var mark = target.getPersistentData();
                    mark.putLong(AMBUSH_MARK_UNTIL, player.level().getGameTime() + BoghideSet.AMBUSH_MARK_TICKS);
                    mark.putUUID(AMBUSH_MARK_OWNER, player.getUUID());
                    player.serverLevel().sendParticles(ParticleTypes.CRIT, target.getX(),
                            target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.3, 0.3, 0.3, 0.2);
                }
            }
        }
    }

    private static void clearPendingLanding(ServerPlayer player) {
        var data = player.getPersistentData();
        data.remove(IMPACT_UNTIL);
        data.remove(LEAP_AIRBORNE);
        data.remove(LEAP_DIMENSION);
    }

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        // Marsh Hide: the full Boghide set shrugs off a tenth of the damage in water, mud or swamp.
        LivingEntity victim = event.getEntity();
        if (BoghideSet.hasFullSet(victim) && BoghideSet.inMarsh(victim)) {
            event.setNewDamage(event.getNewDamage() * BoghideSet.MARSH_DAMAGE_MULTIPLIER);
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        float damage = event.getNewDamage();
        // Ambush Landing: a marked target takes extra damage from the player who landed on it.
        var mark = event.getEntity().getPersistentData();
        if (mark.getLong(AMBUSH_MARK_UNTIL) > attacker.level().getGameTime()
                && mark.hasUUID(AMBUSH_MARK_OWNER) && mark.getUUID(AMBUSH_MARK_OWNER).equals(attacker.getUUID())) {
            damage *= BoghideSet.AMBUSH_DAMAGE_MULTIPLIER;
        }
        if (attacker.hasEffect(ModMobEffects.OGRES_BANE.get())
                && event.getEntity().getType().is(OGRES)) {
            damage *= 1.15F;
        }
        event.setNewDamage(damage);
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || event.getSource().getDirectEntity() != attacker
                || !attacker.getMainHandItem().is(ModItems.BOGFANG_BLADE.get())
                || attacker.getPersistentData().getBoolean(RUPTURE_GUARD)) return;

        LivingEntity target = event.getEntity();
        if (EffectImmunityRules.rejectsBogrot(target)) return;
        var existing = target.getEffect(ModMobEffects.BOGROT.get());
        boolean critical = attacker.getPersistentData().getInt(CRIT_TARGET) == target.getId()
                && attacker.getPersistentData().getLong(CRIT_TICK) == attacker.level().getGameTime();
        long now = attacker.level().getGameTime();
        String progressKey = "kingsandmonsters.bogfang_progress_" + attacker.getUUID();
        if (existing == null) target.getPersistentData().remove(progressKey);

        boolean cooldown = attacker.getPersistentData().getLong(RUPTURE_COOLDOWN) > now;
        int progress = critical && !cooldown ? target.getPersistentData().getInt(progressKey) + 1 : 0;
        if (progress >= 3) {
            float ruptureDamage = BogrotMechanics.remainingScheduledDamage(existing, target);
            target.removeEffect(ModMobEffects.BOGROT.get());
            target.getPersistentData().remove(progressKey);
            attacker.getPersistentData().putLong(RUPTURE_COOLDOWN, now + 8 * 20);
            spawnRuptureParticles(attacker, target, true);
            if (ruptureDamage > 0.0F) {
                attacker.getPersistentData().putBoolean(RUPTURE_GUARD, true);
                try {
                    target.hurt(attacker.damageSources().playerAttack(attacker), ruptureDamage);
                } finally {
                    attacker.getPersistentData().remove(RUPTURE_GUARD);
                }
            }
            spreadBogburst(attacker, target);
            if (target.getType().is(OGRES)) RoyalAdvancementEvents.awardBogfangRupture(attacker);
            return;
        }
        if (progress > 0) target.getPersistentData().putInt(progressKey, progress);
        target.addEffect(BogrotMechanics.standard(45 * 20), attacker);
    }

    public static void onCriticalHit(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.isVanillaCritical()
                || !player.getMainHandItem().is(ModItems.BOGFANG_BLADE.get())) return;
        player.getPersistentData().putInt(CRIT_TARGET, event.getTarget().getId());
        player.getPersistentData().putLong(CRIT_TICK, player.level().getGameTime());
    }

    private static void spreadBogburst(ServerPlayer attacker, LivingEntity origin) {
        int level = ModEnchantmentEffects.level(attacker.level(), attacker.getMainHandItem(), ModEnchantments.BOGBURST);
        if (level <= 0) return;
        double radius = level >= 2 ? 7.0 : 5.0;
        int duration = (level >= 2 ? 25 : 15) * 20;
        int ogresAffected = 0;
        for (LivingEntity target : attacker.serverLevel().getEntitiesOfClass(LivingEntity.class,
                origin.getBoundingBox().inflate(radius), target -> target != attacker && target != origin
                        && target.isAlive() && target instanceof Enemy && !attacker.isAlliedTo(target)
                        && !EffectImmunityRules.rejectsBogrot(target))) {
            if (target.distanceToSqr(origin) <= radius * radius) {
                if (target.addEffect(BogrotMechanics.standard(duration), attacker)) {
                    spawnRuptureParticles(attacker, target, false);
                    if (target.getType().is(OGRES)) ogresAffected++;
                }
            }
        }
        if (ogresAffected >= 3) RoyalAdvancementEvents.awardBogburstChain(attacker);
    }

    private static void spawnRuptureParticles(ServerPlayer attacker, LivingEntity target, boolean full) {
        int count = full ? 34 : 10;
        attacker.serverLevel().sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), count,
                target.getBbWidth() * 0.65, target.getBbHeight() * 0.35,
                target.getBbWidth() * 0.65, full ? 0.08 : 0.025);
        if (full) attacker.serverLevel().sendParticles(ParticleTypes.POOF,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 12,
                0.35, 0.35, 0.35, 0.04);
    }
}
