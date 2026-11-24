package com.kingsandmonsters.tribute;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.effect.EffectImmunityRules;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.kingsandmonsters.event.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import com.kingsandmonsters.event.ServerTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Full-set Bog Iron ability and shared effect-immunity handling. */
public final class BogIronArmorEffects {
    public static final int UNYIELDING_DURATION_TICKS = 10 * 20;
    public static final int UNYIELDING_COOLDOWN_TICKS = 30 * 20;
    public static final float UNYIELDING_MINIMUM_DAMAGE = 2.0F;
    public static final double UNYIELDING_ARMOR_BONUS = 4.0;
    public static final double UNYIELDING_TOUGHNESS_BONUS = 2.0;
    public static final double UNYIELDING_KNOCKBACK_RESISTANCE = 0.30;
    public static final double SHOCKWAVE_RADIUS = 4.0;
    public static final double SHOCKWAVE_KNOCKBACK = 1.35;

    private static final int SHOCKWAVE_EXPANSION_TICKS = 10;
    private static final double SHOCKWAVE_START_RADIUS = 0.45;
    private static final String COOLDOWN_UNTIL_TAG = "KingsAndMonstersUnyieldingCooldownUntil";
    private static final TagKey<Item> BOG_IRON_ARMOR = TagKey.create(Registries.ITEM, id("bog_iron_armor"));
    private static final DustParticleOptions BOG_IRON_GREEN =
            new DustParticleOptions(new Vector3f(0.31F, 0.40F, 0.24F), 1.15F);
    private static final List<ActiveShockwave> ACTIVE_SHOCKWAVES = new ArrayList<>();

    private BogIronArmorEffects() {
    }

    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (EffectImmunityRules.rejects(event.getEntity(), event.getEffectInstance().getEffect())) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide() || event.getNewDamage() < UNYIELDING_MINIMUM_DAMAGE
                || !hasFullSet(wearer)) return;

        long now = wearer.level().getGameTime();
        if (wearer.hasEffect(ModMobEffects.UNYIELDING.get())) return;
        if (wearer.getPersistentData().getLong(COOLDOWN_UNTIL_TAG) > now) return;

        wearer.getPersistentData().putLong(COOLDOWN_UNTIL_TAG, now + UNYIELDING_COOLDOWN_TICKS);
        wearer.addEffect(new MobEffectInstance(ModMobEffects.UNYIELDING.get(),
                UNYIELDING_DURATION_TICKS, 0, false, true, true));

        if (wearer.level() instanceof ServerLevel level) {
            LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
            ACTIVE_SHOCKWAVES.add(new ActiveShockwave(level, wearer, attacker, wearer.position()));
            level.playSound(null, wearer.blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE,
                    SoundSource.PLAYERS, 1.15F, 0.72F);
            level.sendParticles(ParticleTypes.CLOUD, wearer.getX(), wearer.getY() + 0.2, wearer.getZ(),
                    18, 0.35, 0.1, 0.35, 0.04);
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<ActiveShockwave> waves = ACTIVE_SHOCKWAVES.iterator();
        while (waves.hasNext()) if (!waves.next().tick()) waves.remove();
    }

    public static void reset() {
        ACTIVE_SHOCKWAVES.clear();
    }

    private static boolean hasFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(BOG_IRON_ARMOR)
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(BOG_IRON_ARMOR)
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(BOG_IRON_ARMOR)
                && entity.getItemBySlot(EquipmentSlot.FEET).is(BOG_IRON_ARMOR);
    }

    private static boolean isValidTarget(LivingEntity wearer, LivingEntity target, LivingEntity attacker) {
        if (target == wearer || !target.isAlive() || wearer.isAlliedTo(target)
                || target instanceof TamableAnimal tameable && tameable.isTame()) return false;
        if (target instanceof Player) return target == attacker;
        return target instanceof Enemy || target instanceof Mob mob && mob.getTarget() == wearer;
    }

    private static void spawnRing(ServerLevel level, Vec3 center, double radius) {
        int points = Math.max(18, (int) Math.ceil(radius * 7.0));
        for (int point = 0; point < points; point++) {
            double angle = (Math.PI * 2.0) * point / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.CLOUD, x, center.y + 0.12, z,
                    1, 0.02, 0.015, 0.02, 0.002);
            if (point % 2 == 0) level.sendParticles(BOG_IRON_GREEN, x, center.y + 0.16, z,
                    1, 0.012, 0.012, 0.012, 0.0);
        }
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(KingsAndMonsters.MODID, path);
    }

    private static final class ActiveShockwave {
        private final ServerLevel level;
        private final LivingEntity wearer;
        private final LivingEntity attacker;
        private final Vec3 origin;
        private final Set<UUID> hitTargets = new HashSet<>();
        private int age;

        private ActiveShockwave(ServerLevel level, LivingEntity wearer, LivingEntity attacker, Vec3 origin) {
            this.level = level;
            this.wearer = wearer;
            this.attacker = attacker;
            this.origin = origin;
        }

        private boolean tick() {
            if (!wearer.isAlive() || age > SHOCKWAVE_EXPANSION_TICKS) return false;
            double previousProgress = Math.max(0.0, (age - 1.0) / SHOCKWAVE_EXPANSION_TICKS);
            double progress = Math.min(1.0, (double) age / SHOCKWAVE_EXPANSION_TICKS);
            double previousRadius = SHOCKWAVE_START_RADIUS
                    + (SHOCKWAVE_RADIUS - SHOCKWAVE_START_RADIUS) * previousProgress;
            double radius = SHOCKWAVE_START_RADIUS
                    + (SHOCKWAVE_RADIUS - SHOCKWAVE_START_RADIUS) * progress;
            spawnRing(level, origin, radius);
            AABB area = new AABB(origin, origin).inflate(radius + 1.0, 3.0, radius + 1.0);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                    target -> isValidTarget(wearer, target, attacker))) {
                if (hitTargets.contains(target.getUUID())) continue;
                double dx = target.getX() - origin.x;
                double dz = target.getZ() - origin.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius + 0.65 || distance < Math.max(0.0, previousRadius - 0.65)) continue;
                hitTargets.add(target.getUUID());
                if (distance < 0.01) {
                    double angle = wearer.getRandom().nextDouble() * (Math.PI * 2.0);
                    dx = Math.cos(angle);
                    dz = Math.sin(angle);
                    distance = 1.0;
                }
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x * 0.5 + dx / distance * SHOCKWAVE_KNOCKBACK,
                        Math.max(motion.y, 0.18), motion.z * 0.5 + dz / distance * SHOCKWAVE_KNOCKBACK);
                target.hurtMarked = true;
            }
            age++;
            return true;
        }
    }

    /** Drops state tied to a world that has closed, so it cannot leak into the next one opened in this session. */
    public static void resetState() {
        ACTIVE_SHOCKWAVES.clear();
    }
}
