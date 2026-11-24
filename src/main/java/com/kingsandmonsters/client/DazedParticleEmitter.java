package com.kingsandmonsters.client;

import com.kingsandmonsters.ClientConfig;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.client.particle.DazedParticle;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import com.kingsandmonsters.event.ClientTickEvent;

/** Emits the animated Dazed stars above every nearby afflicted entity. */
public final class DazedParticleEmitter {
    private static final double RANGE = 40.0;
    private static final Int2LongOpenHashMap NEXT_EMISSION = new Int2LongOpenHashMap();
    private static final Int2ObjectOpenHashMap<DazedParticle> ACTIVE_SWIRLS = new Int2ObjectOpenHashMap<>();
    private static ClientLevel lastLevel;

    static {
        NEXT_EMISSION.defaultReturnValue(Long.MIN_VALUE);
    }

    private DazedParticleEmitter() {}

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            NEXT_EMISSION.clear();
            ACTIVE_SWIRLS.clear();
            lastLevel = null;
            return;
        }
        if (level != lastLevel) {
            NEXT_EMISSION.clear();
            ACTIVE_SWIRLS.clear();
            lastLevel = level;
        }
        if (minecraft.isPaused()) return;

        emit(level, minecraft.player);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                minecraft.player.getBoundingBox().inflate(RANGE),
                entity -> entity != minecraft.player && entity.isAlive() && entity.hasEffect(ModMobEffects.DAZED.get()))) {
            emit(level, entity);
        }
        if (level.getGameTime() % 100 == 0) {
            NEXT_EMISSION.keySet().removeIf(id -> !(level.getEntity(id) instanceof LivingEntity living)
                    || !living.hasEffect(ModMobEffects.DAZED.get()));
            ACTIVE_SWIRLS.int2ObjectEntrySet().removeIf(entry -> !entry.getValue().isAlive());
        }
    }

    private static void emit(ClientLevel level, LivingEntity entity) {
        if (!entity.isAlive() || !entity.hasEffect(ModMobEffects.DAZED.get())) return;
        double size = Math.sqrt(Math.max(0.01,
                entity.getBbWidth() / 0.6 * entity.getBbHeight() / 1.8));
        float scale = Mth.clamp((float) size, 0.65F, 1.8F);
        if (ClientConfig.useCustomEffectParticles()) {
            DazedParticle current = ACTIVE_SWIRLS.get(entity.getId());
            if (current == null || !current.isAlive()) {
                DazedParticle spawned = DazedParticle.spawn(level, entity, scale);
                if (spawned != null) ACTIVE_SWIRLS.put(entity.getId(), spawned);
            }
            return;
        }

        DazedParticle custom = ACTIVE_SWIRLS.remove(entity.getId());
        if (custom != null) custom.remove();
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        int interval = status == ParticleStatus.MINIMAL ? 52 : status == ParticleStatus.DECREASED ? 36 : 24;
        long now = level.getGameTime();
        if (now < NEXT_EMISSION.get(entity.getId())) return;
        NEXT_EMISSION.put(entity.getId(), now + interval);

        double radius = Math.max(0.18, entity.getBbWidth() * 0.35);
        double angle = entity.getRandom().nextDouble() * (Math.PI * 2.0);
        level.addParticle(ParticleTypes.CRIT,
                entity.getX() + Math.cos(angle) * radius,
                entity.getY() + entity.getBbHeight() * 0.9,
                entity.getZ() + Math.sin(angle) * radius,
                0.0, 0.04, 0.0);
    }
}
