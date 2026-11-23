package com.kingsandmonsters.client;

import com.kingsandmonsters.ClientConfig;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.client.particle.BloodDropletParticle;
import com.kingsandmonsters.effect.CombatEffects;
import com.kingsandmonsters.entity.BogToad;
import com.kingsandmonsters.entity.MudsnoutBoar;
import com.kingsandmonsters.entity.OgreBrute;
import com.kingsandmonsters.entity.OgreLord;
import com.kingsandmonsters.entity.OgreMage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

/**
 * Client-only blood drips for Lesser Bleeding / Bleeding / Greater Bleeding. Derives everything from the
 * already-synchronized MobEffect state of nearby rendered entities: no packets, no server particles, and
 * no gameplay system reads the particles. Each bleeding entity releases one drop at a randomized interval.
 */
public final class BleedingParticleEmitter {
    // Drip intervals in ticks, indexed by severity (0 = Lesser, 1 = Bleeding, 2 = Greater).
    // One restrained drip at a time, but frequent enough that each tier reads clearly in motion.
    private static final int[] MIN_INTERVAL_TICKS = {42, 32, 22};
    private static final int[] MAX_INTERVAL_TICKS = {62, 48, 34};
    // Extra drops are emitted over time, never on the same tick: One drop per interval for every tier.
    private static final int[] DROPS_PER_SEQUENCE = {1, 1, 1};
    private static final int[] SEQUENCE_SPACING_TICKS = {0, 5, 4};
    private static final float DECREASED_INTERVAL_SCALE = 2.0F;
    private static final float MINIMAL_INTERVAL_SCALE = 4.0F;

    // Distance / view culling.
    private static final double MAX_DISTANCE_SQR = 32.0 * 32.0;
    private static final double ALWAYS_EMIT_DISTANCE_SQR = 6.0 * 6.0;
    private static final double MIN_VIEW_DOT = 0.1;

    // Droplet spawn and motion.
    private static final double DROP_MIN_HEIGHT = 0.05;
    private static final double DROP_MAX_HEIGHT = 0.30;
    private static final double ANGLED_MIN_HEIGHT = 0.38;
    private static final double ANGLED_MAX_HEIGHT = 0.72;
    private static final float ANGLED_DROP_CHANCE = 0.25F;
    // First person: drops start at hip/thigh height, smaller, and never close to the camera, so they fall past
    // the bottom of the screen instead of cutting across it.
    private static final double FIRST_PERSON_MIN_HEIGHT = 0.2;
    private static final double FIRST_PERSON_MAX_HEIGHT = 0.45;
    private static final float FIRST_PERSON_SIZE_MULTIPLIER = 0.6F;
    private static final double MIN_CAMERA_DISTANCE_SQR = 1.0;
    // Drops form just under the body's surface, so the model hides their first moment and they
    // read as seeping out of the wound rather than appearing beside it.
    private static final double BODY_SURFACE_CLEARANCE = -0.15;
    private static final double LARGE_MODEL_SURFACE_CLEARANCE = -0.07;
    private static final double BODY_SURFACE_JITTER = 0.025;
    private static final double SILHOUETTE_ANGLE_JITTER = 28.0 * Mth.DEG_TO_RAD;
    private static final double MIN_OUTWARD_SPEED = 0.028;
    private static final double MAX_OUTWARD_SPEED = 0.040;
    private static final double MIN_DRIP_OUTWARD_SPEED = 0.001;
    private static final double MAX_DRIP_OUTWARD_SPEED = 0.004;
    private static final double MIN_DRIP_FALL_SPEED = 0.018;
    private static final double MAX_DRIP_FALL_SPEED = 0.028;
    private static final double MAX_LAUNCH_ANGLE_JITTER = 4.0 * Mth.DEG_TO_RAD;
    private static final double MIN_LAUNCH_RISE_RATIO = 0.38;
    private static final double MAX_LAUNCH_RISE_RATIO = 0.52;
    private static final float MIN_ENTITY_SIZE_SCALE = 0.75F;
    private static final float MAX_ENTITY_SIZE_SCALE = 1.35F;
    private static final double INHERITED_MOTION_FACTOR = 0.10;
    private static final double MAX_INHERITED_SPEED = 0.018;

    private static final int PRUNE_INTERVAL_TICKS = 100;
    private static final int STALE_AFTER_TICKS = 40;

    private static final class Drip {
        int severity;
        long nextTick;
        long lastSeenTick;
        int queuedDrops;
        long nextQueuedDropTick;
    }

    private static final Int2ObjectOpenHashMap<Drip> DRIPS = new Int2ObjectOpenHashMap<>();
    private static ClientLevel lastLevel;

    private BleedingParticleEmitter() {}

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        DRIPS.clear();
        BloodParticleBudget.reset();
        lastLevel = null;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            if (lastLevel != null) clear();
            return;
        }
        if (level != lastLevel) {
            clear();
            lastLevel = level;
        }
        if (minecraft.isPaused()) return;

        long now = level.getGameTime();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vector3f look = minecraft.gameRenderer.getMainCamera().getLookVector();
        RandomSource random = level.random;

        // The camera player is not guaranteed to be returned by entitiesForRendering in first person.
        // Process it explicitly so the player can always see their own bleeding trail.
        processEntity(minecraft, minecraft.player, now, camera, look, random);
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && living.getId() != minecraft.player.getId()) {
                processEntity(minecraft, living, now, camera, look, random);
            }
        }

        if (now % PRUNE_INTERVAL_TICKS == 0) {
            DRIPS.values().removeIf(drip -> now - drip.lastSeenTick > STALE_AFTER_TICKS);
        }
    }

    private static void processEntity(Minecraft minecraft, LivingEntity living, long now,
                                      Vec3 camera, Vector3f look, RandomSource random) {
        if (!living.isAlive() || living.isSpectator() || living.getType().is(CombatEffects.BLEED_IMMUNE)) return;
        int severity = severityOf(living);
        if (severity < 0) return;

        Drip drip = DRIPS.get(living.getId());
        if (drip == null) {
            drip = new Drip();
            drip.severity = severity;
            // Make a newly observed wound visible immediately; later drops use the restrained cadence.
            drip.nextTick = now;
            DRIPS.put(living.getId(), drip);
        } else if (severity > drip.severity) {
            drip.severity = severity;
            drip.nextTick = Math.min(drip.nextTick, now + nextInterval(severity, random));
        } else {
            drip.severity = severity;
        }
        drip.lastSeenTick = now;

        // Continue an existing sequence one drop at a time. Each drop gets fresh random motion.
        if (drip.queuedDrops > 0 && now >= drip.nextQueuedDropTick) {
            if (shouldEmit(minecraft, living, camera, look)) {
                emit(minecraft, living, random);
            }
            drip.queuedDrops--;
            drip.nextQueuedDropTick = now + SEQUENCE_SPACING_TICKS[severity];
        }
        if (now < drip.nextTick) return;
        // Start the next sequence after this one has finished so two drops never share a tick.
        drip.nextTick = now + nextInterval(severity, random)
                + (long) SEQUENCE_SPACING_TICKS[severity] * (DROPS_PER_SEQUENCE[severity] - 1);

        if (shouldEmit(minecraft, living, camera, look)) {
            emit(minecraft, living, random);
        }
        drip.queuedDrops = DROPS_PER_SEQUENCE[severity] - 1;
        drip.nextQueuedDropTick = now + SEQUENCE_SPACING_TICKS[severity];
    }

    /** Highest active bleeding tier (2 Greater, 1 Bleeding, 0 Lesser) or -1. */
    private static int severityOf(LivingEntity entity) {
        if (entity.hasEffect(ModMobEffects.GREATER_BLEEDING)) return 2;
        if (entity.hasEffect(ModMobEffects.BLEEDING)) return 1;
        if (entity.hasEffect(ModMobEffects.LESSER_BLEEDING)) return 0;
        return -1;
    }

    private static int maxInterval(int severity) {
        return Math.round(MAX_INTERVAL_TICKS[severity] * intervalScale());
    }

    private static int nextInterval(int severity, RandomSource random) {
        float scale = intervalScale();
        int min = Math.round(MIN_INTERVAL_TICKS[severity] * scale);
        int max = Math.round(MAX_INTERVAL_TICKS[severity] * scale);
        return min + random.nextInt(max - min + 1);
    }

    private static float intervalScale() {
        ParticleStatus status = BloodParticleBudget.status();
        return switch (status) {
            case ALL -> 1.0F;
            case DECREASED -> DECREASED_INTERVAL_SCALE;
            case MINIMAL -> MINIMAL_INTERVAL_SCALE;
        };
    }

    private static boolean shouldEmit(Minecraft minecraft, LivingEntity entity, Vec3 camera, Vector3f look) {
        if (entity.isInWaterOrBubble() || entity.isInLava()) return false;
        if (!BloodParticleBudget.canSpawnDroplet()) return false;
        double dx = entity.getX() - camera.x;
        double dy = entity.getY() + entity.getBbHeight() * 0.5 - camera.y;
        double dz = entity.getZ() - camera.z;
        double distSqr = dx * dx + dy * dy + dz * dz;
        if (distSqr > MAX_DISTANCE_SQR) return false;
        if (entity == minecraft.player || distSqr <= ALWAYS_EMIT_DISTANCE_SQR) return true;
        double dot = (dx * look.x() + dy * look.y() + dz * look.z()) / Math.sqrt(distSqr);
        return dot > MIN_VIEW_DOT;
    }

    private static void emit(Minecraft minecraft, LivingEntity entity, RandomSource random) {
        boolean firstPerson = entity == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
        boolean angled = firstPerson || entity instanceof OgreMage || entity instanceof BogToad
                || random.nextFloat() < ANGLED_DROP_CHANCE;
        Vector3f cameraLeft = minecraft.gameRenderer.getMainCamera().getLeftVector();
        double angle = Math.atan2(cameraLeft.z(), cameraLeft.x());
        if (random.nextBoolean()) angle += Math.PI;
        angle += Mth.lerp(random.nextDouble(), -SILHOUETTE_ANGLE_JITTER, SILHOUETTE_ANGLE_JITTER);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        // Pick either camera-visible silhouette edge. A full 360-degree choice lets front/back
        // origins project through the middle of wide models even when their world position is valid.
        double radius = bodySurfaceRadius(entity, angle)
                + bodySurfaceClearance(entity) + random.nextDouble() * BODY_SURFACE_JITTER;
        double x = entity.getX() + cos * radius;
        double minHeight = firstPerson ? FIRST_PERSON_MIN_HEIGHT : angled ? ANGLED_MIN_HEIGHT : DROP_MIN_HEIGHT;
        double maxHeight = firstPerson ? FIRST_PERSON_MAX_HEIGHT : angled ? ANGLED_MAX_HEIGHT : DROP_MAX_HEIGHT;
        double y = entity.getY() + entity.getBbHeight() * Mth.lerp(random.nextDouble(), minHeight, maxHeight);
        double z = entity.getZ() + sin * radius;
        if (firstPerson) {
            Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();
            double dx = x - eye.x;
            double dy = y - eye.y;
            double dz = z - eye.z;
            if (dx * dx + dy * dy + dz * dz < MIN_CAMERA_DISTANCE_SQR) return;
        }

        double outward = angled
                ? Mth.lerp(random.nextDouble(), MIN_OUTWARD_SPEED, MAX_OUTWARD_SPEED)
                : Mth.lerp(random.nextDouble(), MIN_DRIP_OUTWARD_SPEED, MAX_DRIP_OUTWARD_SPEED);
        double launchAngle = angle + Mth.lerp(random.nextDouble(),
                -MAX_LAUNCH_ANGLE_JITTER, MAX_LAUNCH_ANGLE_JITTER);
        double xd = Math.cos(launchAngle) * outward;
        double zd = Math.sin(launchAngle) * outward;
        // Most blood simply lets go below the waist. A few higher drops peel gently away from the body.
        double yd = angled
                ? -outward * Mth.lerp(random.nextDouble(), MIN_LAUNCH_RISE_RATIO, MAX_LAUNCH_RISE_RATIO)
                : -Mth.lerp(random.nextDouble(), MIN_DRIP_FALL_SPEED, MAX_DRIP_FALL_SPEED);

        Vec3 motion = entity.getDeltaMovement();
        double inheritedX = motion.x * INHERITED_MOTION_FACTOR;
        double inheritedZ = motion.z * INHERITED_MOTION_FACTOR;
        double inherited = Math.sqrt(inheritedX * inheritedX + inheritedZ * inheritedZ);
        if (inherited > MAX_INHERITED_SPEED) {
            double shrink = MAX_INHERITED_SPEED / inherited;
            inheritedX *= shrink;
            inheritedZ *= shrink;
        }
        xd += inheritedX;
        zd += inheritedZ;

        float sizeMultiplier = particleScale(entity)
                * (firstPerson ? FIRST_PERSON_SIZE_MULTIPLIER : 1.0F);
        if (ClientConfig.useCustomEffectParticles()) {
            BloodDropletParticle.spawn(minecraft.level, x, y, z, xd, yd, zd,
                    entity.getId(), severityOf(entity), sizeMultiplier, angled);
        } else {
            minecraft.level.addParticle(ParticleTypes.DAMAGE_INDICATOR, x, y, z, xd, yd, zd);
        }
    }

    /** Mage robes and shoulders are much wider than the collision box used for pathing. */
    private static double bodySurfaceRadius(LivingEntity entity, double worldAngle) {
        // The toad's broad pathing box includes its projecting face and legs, not just its torso.
        if (entity instanceof BogToad) return 0.58;
        if (!(entity instanceof OgreMage)) {
            return Math.max(0.12, entity.getBbWidth() * 0.5);
        }
        double localAngle = worldAngle - entity.getYRot() * Mth.DEG_TO_RAD;
        double right = Math.abs(Math.cos(localAngle));
        double forward = Math.abs(Math.sin(localAngle));
        double halfVisualWidth = 0.90;
        double halfVisualDepth = 0.68;
        return 1.0 / Math.max(right / halfVisualWidth, forward / halfVisualDepth);
    }

    private static double bodySurfaceClearance(LivingEntity entity) {
        return entity instanceof OgreLord || entity instanceof MudsnoutBoar || entity instanceof OgreBrute
                ? LARGE_MODEL_SURFACE_CLEARANCE : BODY_SURFACE_CLEARANCE;
    }

    /** Mildly adapts droplets to body size without letting Ogre blood obscure the screen. */
    private static float particleScale(LivingEntity entity) {
        double widthRatio = entity.getBbWidth() / 0.6;
        double heightRatio = entity.getBbHeight() / 1.8;
        float scale = (float) Math.pow(Math.max(0.01, widthRatio * heightRatio), 0.25);
        return Mth.clamp(scale, MIN_ENTITY_SIZE_SCALE, MAX_ENTITY_SIZE_SCALE);
    }
}
