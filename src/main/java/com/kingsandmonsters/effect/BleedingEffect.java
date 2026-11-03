package com.kingsandmonsters.effect;

import com.kingsandmonsters.Config;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/** Short physical damage-over-time effect applied by the Barbed spear enchantment. */
public final class BleedingEffect extends MobEffect {
    public static final int DAMAGE_INTERVAL_TICKS = 20;
    private static final DustParticleOptions BLOOD_DUST =
            new DustParticleOptions(new Vector3f(0.72F, 0.02F, 0.02F), 1.0F);

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0xB31217);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % DAMAGE_INTERVAL_TICKS == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    BLOOD_DUST,
                    entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.55,
                    entity.getZ(),
                    6,
                    entity.getBbWidth() * 0.3,
                    entity.getBbHeight() * 0.3,
                    entity.getBbWidth() * 0.3,
                    0.015);
            float damage = (float) (Config.BARBED_BLEED_DAMAGE.get() * (amplifier + 1));
            entity.hurt(entity.damageSources().wither(), damage);
        }
        return true;
    }
}
