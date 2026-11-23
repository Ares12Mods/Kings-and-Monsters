package com.kingsandmonsters.compat.spell_power;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModEntities;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Map;
import java.util.Optional;

/**
 * Optional, registry-only compatibility with the Spell Power library behind the RPG Series mods
 * (Wizards, Paladins &amp; Priests, Rogues &amp; Warriors, Archers). Spell Power has one defensive
 * attribute, generic magic resistance, which works like armor against every spell school; each
 * ogre gets a base value of it that suits their role, as Iron's Spells resistance is handled.
 */
public final class SpellPowerCompat {
    private static final String MOD_ID = "spell_power";
    private static final ResourceLocation RESISTANCE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "resistance.generic");

    private static Map<EntityType<? extends LivingEntity>, Double> resistances;
    private static Holder<Attribute> attribute;
    private static boolean attributeLookedUp;

    private SpellPowerCompat() {
    }

    public static void registerIfPresent() {
        if (ModList.get().isLoaded(MOD_ID)) {
            NeoForge.EVENT_BUS.addListener(SpellPowerCompat::onEntityJoinLevel);
            KingsAndMonsters.LOGGER.info("Enabling optional Spell Power magic resistance compatibility");
        }
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }
        Double value = resistances().get(livingEntity.getType());
        Holder<Attribute> resistance = attribute();
        if (value == null || resistance == null) {
            return;
        }
        AttributeInstance instance = livingEntity.getAttribute(resistance);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /** Points of generic resistance, which scale like armor points: tougher or more arcane ogres have more. */
    private static Map<EntityType<? extends LivingEntity>, Double> resistances() {
        if (resistances == null) {
            resistances = Map.of(
                    ModEntities.BOG_TOAD.get(), 3.0,
                    ModEntities.OGRE_GRUNT.get(), 4.0,
                    ModEntities.OGRE_ARCHER.get(), 3.0,
                    ModEntities.OGRE_MERCHANT.get(), 3.0,
                    ModEntities.OGRE_GUARD.get(), 6.0,
                    ModEntities.OGRE_GRUNT_CAPTAIN.get(), 8.0,
                    ModEntities.OGRE_BRUTE.get(), 8.0,
                    ModEntities.OGRE_MAGE.get(), 14.0,
                    ModEntities.OGRE_LORD.get(), 16.0);
        }
        return resistances;
    }

    private static Holder<Attribute> attribute() {
        if (!attributeLookedUp) {
            attributeLookedUp = true;
            Optional<Holder.Reference<Attribute>> found = BuiltInRegistries.ATTRIBUTE.getHolder(
                    ResourceKey.create(Registries.ATTRIBUTE, RESISTANCE));
            if (found.isPresent()) {
                attribute = found.get();
            } else {
                KingsAndMonsters.LOGGER.warn("Spell Power attribute {} is unavailable; skipping it", RESISTANCE);
            }
        }
        return attribute;
    }
}
