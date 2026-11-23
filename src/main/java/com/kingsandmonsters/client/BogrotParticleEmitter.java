package com.kingsandmonsters.client;

import com.kingsandmonsters.ClientConfig;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.effect.BogrotMechanics;
import com.kingsandmonsters.client.particle.BogrotBubbleParticle;
import com.kingsandmonsters.client.particle.BogrotFlyParticle;
import com.kingsandmonsters.client.particle.BogrotParticleBudget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client-only ambient particles for entities currently afflicted by Bogrot. */
public final class BogrotParticleEmitter {
    private static final double MAX_DISTANCE_SQR = 32.0 * 32.0;
    private static ClientLevel lastLevel;

    private BogrotParticleEmitter() {}

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        BogrotParticleBudget.reset();
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

        process(level, minecraft, minecraft.player);
        // Filter inside the query so only afflicted mobs ever land in the list.
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                minecraft.player.getBoundingBox().inflate(32.0),
                living -> living != minecraft.player && BogrotMechanics.showsVisuals(living))) {
            process(level, minecraft, living);
        }
    }

    private static void process(ClientLevel level, Minecraft minecraft, LivingEntity living) {
        if (!living.isAlive()
                || !BogrotMechanics.showsVisuals(living)
                || living.distanceToSqr(minecraft.player) > MAX_DISTANCE_SQR) {
            return;
        }

        ParticleStatus status = minecraft.options.particles().get();
        int amplifier = living.hasEffect(ModMobEffects.BOGROT)
                ? living.getEffect(ModMobEffects.BOGROT).getAmplifier() : 1;
        int desiredFlies = status == ParticleStatus.MINIMAL ? 1 : (amplifier >= 1 ? 2 : 1);
        int cadence = living.tickCount + living.getId();
        if (!ClientConfig.useCustomEffectParticles()) {
            emitVanilla(level, living, cadence, status);
            return;
        }
        if (cadence % 8 == 0) {
            BogrotFlyParticle.spawn(level, living, desiredFlies,
                    BogrotParticleBudget.fliesFor(living.getId()));
        }
        // Bubbles live a while, so they can spawn less often than flies.
        int bubbleInterval = amplifier >= 1 ? 10 : 16;
        if (status == ParticleStatus.DECREASED) bubbleInterval *= 2;
        else if (status == ParticleStatus.MINIMAL) bubbleInterval *= 4;
        if (cadence % bubbleInterval == 0) spawnBubble(level, living);
    }

    private static void emitVanilla(ClientLevel level, LivingEntity entity, int cadence, ParticleStatus status) {
        int interval = status == ParticleStatus.MINIMAL ? 24 : status == ParticleStatus.DECREASED ? 16 : 8;
        if (cadence % interval != 0) return;
        double width = Math.max(0.35, entity.getBbWidth());
        double angle = entity.getRandom().nextDouble() * Math.TAU;
        double radius = width * Mth.lerp(entity.getRandom().nextDouble(), 0.2, 0.55);
        double x = entity.getX() + Math.cos(angle) * radius;
        double y = entity.getY() + entity.getBbHeight() * Mth.lerp(entity.getRandom().nextDouble(), 0.25, 0.9);
        double z = entity.getZ() + Math.sin(angle) * radius;
        level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 0.0, 0.015, 0.0);
        if (cadence % (interval * 2) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0.0, 0.025, 0.0);
        }
    }

    private static void spawnBubble(ClientLevel level, LivingEntity entity) {
        double width = Math.max(0.35, entity.getBbWidth());
        float scale = particleScale(entity);
        double angle = entity.getRandom().nextDouble() * Math.TAU;
        // Spawn just outside the hitbox so bubbles don't start inside the model.
        double modelEdge = width * 0.5;
        double visualClearance = 0.18 * scale;
        double radius = modelEdge + visualClearance
                + width * Mth.lerp(entity.getRandom().nextDouble(), 0.08, 0.32);
        double x = entity.getX() + Math.cos(angle) * radius;
        // Upper body only, so they drift up past the head before popping.
        double y = entity.getY() + entity.getBbHeight()
                * Mth.lerp(entity.getRandom().nextDouble(), 0.52, 0.98);
        double z = entity.getZ() + Math.sin(angle) * radius;
        BogrotBubbleParticle.spawn(level, x, y, z, scale);
    }

    /** Sprite size follows the entity's box, clamped so tiny and huge mobs both stay readable. */
    private static float particleScale(LivingEntity entity) {
        double widthRatio = entity.getBbWidth() / 0.6;
        double heightRatio = entity.getBbHeight() / 1.8;
        return Mth.clamp((float) Math.sqrt(Math.max(0.01, widthRatio * heightRatio)), 0.7F, 2.25F);
    }
}
