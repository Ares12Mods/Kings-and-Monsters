package com.kingsandmonsters.effect;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.entity.BogToad;
import com.kingsandmonsters.entity.BogPole;
import com.kingsandmonsters.entity.OgreGrunt;
import com.kingsandmonsters.item.BoghideSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Central effect-immunity rules for naturally resistant K&M creatures. */
public final class EffectImmunityRules {
    /** Elemental, amorphous and swamp creatures that cannot rot; extensible by datapacks and mods. */
    public static final TagKey<EntityType<?>> BOGROT_IMMUNE = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(KingsAndMonsters.MODID, "bogrot_immune"));

    private EffectImmunityRules() {
    }

    public static boolean rejects(LivingEntity entity, MobEffect effect) {
        if (effect.equals(ModMobEffects.BOGROT.get()) && entity.getType().is(BOGROT_IMMUNE)) {
            return true;
        }
        // Toadskin: a full Boghide set keeps the rot off its wearer.
        if (effect.equals(ModMobEffects.BOGROT.get()) && BoghideSet.hasFullSet(entity)) {
            return true;
        }
        if ((entity instanceof BogToad || entity instanceof BogPole) && isToxin(effect)) {
            return true;
        }
        return entity instanceof OgreGrunt && isLegacyOgreDebuff(effect);
    }

    public static boolean rejectsBogrot(LivingEntity entity) {
        return rejects(entity, ModMobEffects.BOGROT.get());
    }

    private static boolean isToxin(MobEffect effect) {
        return effect.equals(MobEffects.POISON) || effect.equals(ModMobEffects.BOGROT.get());
    }

    private static boolean isLegacyOgreDebuff(MobEffect effect) {
        return effect.equals(MobEffects.POISON)
                || effect.equals(MobEffects.MOVEMENT_SLOWDOWN)
                || effect.equals(MobEffects.WEAKNESS);
    }
}
