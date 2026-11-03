package com.kingsandmonsters.compat.irons_spellbooks;

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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Optional, registry-only compatibility with Iron's Spells 'n Spellbooks. */
public final class IronsSpellbooksCompat {
    private static final String MOD_ID = "irons_spellbooks";

    private static final ResourceLocation SPELL_RESIST = id("spell_resist");
    private static final ResourceLocation FIRE_RESIST = id("fire_magic_resist");
    private static final ResourceLocation ENDER_RESIST = id("ender_magic_resist");
    private static final ResourceLocation BLOOD_RESIST = id("blood_magic_resist");
    private static final ResourceLocation EVOCATION_RESIST = id("evocation_magic_resist");
    private static final ResourceLocation NATURE_RESIST = id("nature_magic_resist");
    private static final ResourceLocation ELDRITCH_RESIST = id("eldritch_magic_resist");

    private static Map<EntityType<? extends LivingEntity>, Map<ResourceLocation, Double>> profiles;
    private static Map<ResourceLocation, Holder<Attribute>> attributes;

    private IronsSpellbooksCompat() {
    }

    public static void registerIfPresent() {
        if (ModList.get().isLoaded(MOD_ID)) {
            NeoForge.EVENT_BUS.addListener(IronsSpellbooksCompat::onEntityJoinLevel);
            KingsAndMonsters.LOGGER.info("Enabling optional Iron's Spells 'n Spellbooks resistance compatibility");
        }
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }

        Map<ResourceLocation, Double> profile = profiles().get(livingEntity.getType());
        if (profile == null) {
            return;
        }

        Map<ResourceLocation, Holder<Attribute>> resolvedAttributes = attributes();
        profile.forEach((attributeId, value) -> {
            Holder<Attribute> attribute = resolvedAttributes.get(attributeId);
            if (attribute == null) {
                return;
            }

            AttributeInstance instance = livingEntity.getAttribute(attribute);
            if (instance != null) {
                instance.setBaseValue(value);
            }
        });
    }

    private static Map<EntityType<? extends LivingEntity>, Map<ResourceLocation, Double>> profiles() {
        if (profiles == null) {
            Map<EntityType<? extends LivingEntity>, Map<ResourceLocation, Double>> values = new LinkedHashMap<>();
            values.put(ModEntities.OGRE_GRUNT.get(), profile(NATURE_RESIST, 1.15));
            values.put(ModEntities.OGRE_ARCHER.get(), profile(NATURE_RESIST, 1.10));
            values.put(ModEntities.OGRE_GUARD.get(), profile(
                    NATURE_RESIST, 1.15,
                    EVOCATION_RESIST, 1.10));
            values.put(ModEntities.OGRE_GRUNT_CAPTAIN.get(), profile(
                    NATURE_RESIST, 1.20,
                    EVOCATION_RESIST, 1.15,
                    BLOOD_RESIST, 1.10));
            values.put(ModEntities.OGRE_BRUTE.get(), profile(
                    SPELL_RESIST, 1.10,
                    NATURE_RESIST, 1.15,
                    BLOOD_RESIST, 1.10));
            values.put(ModEntities.OGRE_MAGE.get(), profile(
                    NATURE_RESIST, 1.40,
                    ELDRITCH_RESIST, 1.25,
                    EVOCATION_RESIST, 1.20,
                    BLOOD_RESIST, 1.15,
                    FIRE_RESIST, 0.90));
            values.put(ModEntities.OGRE_MERCHANT.get(), profile(NATURE_RESIST, 1.10));
            values.put(ModEntities.OGRE_LORD.get(), profile(
                    SPELL_RESIST, 1.15,
                    NATURE_RESIST, 1.30,
                    ELDRITCH_RESIST, 1.25,
                    EVOCATION_RESIST, 1.20,
                    BLOOD_RESIST, 1.15,
                    ENDER_RESIST, 1.10));
            profiles = Map.copyOf(values);
        }
        return profiles;
    }

    private static Map<ResourceLocation, Holder<Attribute>> attributes() {
        if (attributes == null) {
            Map<ResourceLocation, Holder<Attribute>> resolved = new LinkedHashMap<>();
            for (Map<ResourceLocation, Double> profile : profiles().values()) {
                for (ResourceLocation attributeId : profile.keySet()) {
                    if (resolved.containsKey(attributeId)) {
                        continue;
                    }

                    Optional<Holder.Reference<Attribute>> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(
                            ResourceKey.create(Registries.ATTRIBUTE, attributeId));
                    if (attribute.isPresent()) {
                        resolved.put(attributeId, attribute.get());
                    } else {
                        KingsAndMonsters.LOGGER.warn("Iron's Spells attribute {} is unavailable; skipping it", attributeId);
                    }
                }
            }
            attributes = Map.copyOf(resolved);
        }
        return attributes;
    }

    private static Map<ResourceLocation, Double> profile(Object... entries) {
        Map<ResourceLocation, Double> profile = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            profile.put((ResourceLocation) entries[index], (Double) entries[index + 1]);
        }
        return Map.copyOf(profile);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
