package com.kingsandmonsters.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * One of the three bleeding severities hunting weapons open. Gameplay only: the blood drip visuals are derived client-side
 * from the synchronized effect (see BleedingParticleEmitter).
 */
public final class BleedingEffect extends MobEffect {
    public static final int DAMAGE_INTERVAL_TICKS = 40;

    private final Severity severity;

    public BleedingEffect(Severity severity) {
        super(MobEffectCategory.HARMFUL, 0xB31217);
        this.severity = severity;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % DAMAGE_INTERVAL_TICKS == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            DamageSource wound = entity.damageSources().wither();
            // Credit the one who opened the wound, so a kill it finishes still counts for them: quests,
            // experience and player-only drops all depend on the killer being known.
            if (entity.getPersistentData().hasUUID(CombatEffects.BLEED_SOURCE)
                    && level.getEntity(entity.getPersistentData().getUUID(CombatEffects.BLEED_SOURCE))
                    instanceof LivingEntity cause && cause.isAlive() && cause != entity) {
                wound = new DamageSource(wound.typeHolder(), null, cause);
            }
            entity.hurt(wound, severity.damagePerTick);
        }
        return true;
    }

    /** The three fixed severities. A spear starts at Lesser and Barbed raises it. */
    public enum Severity {
        LESSER(0.5F, 320, 0, 0.0F),
        NORMAL(1.0F, 320, 1, 0.25F),
        GREATER(1.2F, 400, 2, 0.5F);

        public final float damagePerTick;
        public final int durationTicks;
        public final int hungerAmplifier;
        /** The share of every heal the wounded one loses while the wound is open. */
        public final float healingReduction;

        Severity(float damagePerTick, int durationTicks, int hungerAmplifier, float healingReduction) {
            this.damagePerTick = damagePerTick;
            this.durationTicks = durationTicks;
            this.hungerAmplifier = hungerAmplifier;
            this.healingReduction = healingReduction;
        }
    }

}
