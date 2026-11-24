package com.kingsandmonsters.effect;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModMobEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** Shared application helpers keep durations at the call site/config and effect flags consistent. */
public final class CombatEffects {
    private CombatEffects() {}

    /** Persistent-data key holding who opened a bleeding wound, so its damage can be credited to them. */
    public static final String BLEED_SOURCE = "KingsAndMonstersBleedSource";

    /** Mobs without blood (slimes, skeletons, golems...) are immune to every Bleeding tier. */
    public static final TagKey<EntityType<?>> BLEED_IMMUNE = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(KingsAndMonsters.MODID, "bleed_immune"));

    public static boolean isBleedImmune(LivingEntity target) {
        return target.getType().is(BLEED_IMMUNE);
    }

    public static void applyDazed(LivingEntity target, int durationTicks, LivingEntity source) {
        // The persistent custom head swirl replaces vanilla's ambient status particles.
        target.addEffect(new MobEffectInstance(ModMobEffects.DAZED.get(), durationTicks, 0, false, false, true), source);
    }

    public static void applyBogrot(LivingEntity target, int durationTicks, int amplifier, LivingEntity source) {
        target.addEffect(BogrotMechanics.instance(durationTicks, amplifier), source);
    }

    public static void applyCrippled(LivingEntity target, int durationTicks, LivingEntity source) {
        // Crippled has a fixed commitment window: repeated bites or spear procs cannot reset
        // its timer. Once the active instance expires, a later hit may apply a fresh one.
        if (target.hasEffect(ModMobEffects.CRIPPLED.get())) {
            return;
        }
        target.addEffect(new MobEffectInstance(ModMobEffects.CRIPPLED.get(), durationTicks, 0, false, true, true), source);
    }

    public static void applyBleeding(LivingEntity target, BleedingEffect.Severity severity, LivingEntity source) {
        if (isBleedImmune(target)) {
            return;
        }
        int activeSeverity = activeBleedingSeverity(target);
        int requestedSeverity = severity.ordinal();
        // A lesser wound never replaces or refreshes a more serious one. Equal wounds refresh;
        // stronger ones upgrade immediately and remove the old severity.
        if (activeSeverity > requestedSeverity) {
            return;
        }
        if (activeSeverity >= 0) {
            target.removeEffect(bleedingEffect(activeSeverity));
        }
        int duration = severity.durationTicks;
        target.getPersistentData().putUUID(BLEED_SOURCE, source.getUUID());
        // Particles hidden: the custom blood drips replace the vanilla swirl; the icon stays.
        target.addEffect(new MobEffectInstance(bleedingEffect(requestedSeverity), duration, 0,
                false, false, true), source);
        // Vanilla Hunger supplies both the animated food-bar overlay and the exhaustion drain.
        // Hidden particles/icon keep it from reading as a second combat debuff.
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration, severity.hungerAmplifier,
                false, false, false), source);
    }

    /** What is left of a heal for someone bleeding: the worse the wound, the less of it lands. */
    public static float healingMultiplier(LivingEntity target) {
        int severity = activeBleedingSeverity(target);
        return severity < 0 ? 1.0F : 1.0F - BleedingEffect.Severity.values()[severity].healingReduction;
    }

    public static boolean hasBleeding(LivingEntity target) {
        return activeBleedingSeverity(target) >= 0;
    }

    private static int activeBleedingSeverity(LivingEntity target) {
        if (target.hasEffect(ModMobEffects.GREATER_BLEEDING.get())) return 2;
        if (target.hasEffect(ModMobEffects.BLEEDING.get())) return 1;
        if (target.hasEffect(ModMobEffects.LESSER_BLEEDING.get())) return 0;
        return -1;
    }

    private static MobEffect bleedingEffect(int severity) {
        return severity >= 2 ? ModMobEffects.GREATER_BLEEDING.get()
                : severity == 1 ? ModMobEffects.BLEEDING.get() : ModMobEffects.LESSER_BLEEDING.get();
    }

}
