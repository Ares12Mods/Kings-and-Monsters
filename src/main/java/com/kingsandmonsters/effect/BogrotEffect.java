package com.kingsandmonsters.effect;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class BogrotEffect extends MobEffect {
    private static final String NEXT_DAMAGE_TICK = "kingsandmonsters.bogrot_next_damage_tick";
    public BogrotEffect() {
        super(MobEffectCategory.HARMFUL, 0x536B2F);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, id("bogrot_movement"), -0.10,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_SPEED, id("bogrot_attack"), -0.10,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Damage timing lives in applyEffectTick; keying it off remaining duration made the first
        // hit land at inconsistent times.
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;
        long now = entity.level().getGameTime();
        long next = entity.getPersistentData().getLong(NEXT_DAMAGE_TICK);
        if (next == 0L) {
            entity.getPersistentData().putLong(NEXT_DAMAGE_TICK,
                    now + BogrotMechanics.damageIntervalTicks(amplifier));
        } else if (now >= next) {
            entity.hurt(entity.damageSources().magic(), BogrotMechanics.tickDamage(entity));
            entity.getPersistentData().putLong(NEXT_DAMAGE_TICK,
                    now + BogrotMechanics.damageIntervalTicks(amplifier));
        }
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(entity, attributes, amplifier);
        entity.getPersistentData().putLong(NEXT_DAMAGE_TICK,
                entity.level().getGameTime() + BogrotMechanics.damageIntervalTicks(amplifier));
    }

    private static String id(String path) {
        return com.kingsandmonsters.item.ItemAttributes.uuid(path).toString();
    }
}
