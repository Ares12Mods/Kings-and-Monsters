package com.kingsandmonsters.effect;

import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.OgreLord;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class BogrotMechanics {
    public static final float DAMAGE_PER_TICK = 2.0F;
    public static final float OGRE_DAMAGE_MULTIPLIER = 1.15F;
    /** Players take half damage so a toad bite hurts without being a death sentence. */
    public static final float PLAYER_DAMAGE_MULTIPLIER = 0.5F;
    /** The Ogre King is vulnerable to the rot despite being a boss. */
    public static final float OGRE_KING_DAMAGE_MULTIPLIER = 1.50F;
    private static final TagKey<EntityType<?>> OGRES = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(KingsAndMonsters.MODID, "ogres"));
    private BogrotMechanics() {}
    /** Entity types that always show Bogrot visuals (the Plague Ogre) despite being immune to the effect. */
    private static final TagKey<EntityType<?>> AMBIENT = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(KingsAndMonsters.MODID, "bogrot_ambient"));
    public static boolean isAffected(LivingEntity entity) { return entity.hasEffect(ModMobEffects.BOGROT.get()); }
    /** True while the synchronized effect is live, or the entity type naturally carries Bogrot. */
    public static boolean showsVisuals(LivingEntity entity) {
        MobEffectInstance effect = entity.getEffect(ModMobEffects.BOGROT.get());
        return (effect != null && effect.getDuration() > 0) || entity.getType().is(AMBIENT);
    }
    public static MobEffectInstance standard(int durationTicks) {
        return instance(durationTicks, 0);
    }
    public static MobEffectInstance naturalBite() {
        return instance(45 * 20, 1);
    }
    public static MobEffectInstance instance(int durationTicks, int amplifier) {
        return new MobEffectInstance(ModMobEffects.BOGROT.get(), durationTicks, amplifier,
                false, true, true);
    }
    public static int damageIntervalTicks(int amplifier) { return amplifier >= 1 ? 80 : 100; }
    public static float tickDamage(LivingEntity entity) {
        if (entity instanceof OgreLord) {
            return DAMAGE_PER_TICK * OGRE_KING_DAMAGE_MULTIPLIER;
        }
        if (entity instanceof Player) {
            return DAMAGE_PER_TICK * PLAYER_DAMAGE_MULTIPLIER;
        }
        return DAMAGE_PER_TICK * (entity.getType().is(OGRES) ? OGRE_DAMAGE_MULTIPLIER : 1.0F);
    }
    public static float remainingScheduledDamage(MobEffectInstance effect, LivingEntity entity) {
        return (effect.getDuration() / damageIntervalTicks(effect.getAmplifier())) * tickDamage(entity);
    }
}
