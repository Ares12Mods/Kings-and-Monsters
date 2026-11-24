package com.kingsandmonsters.item;

import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

/**
 * The Boghide set bonus, Toadskin: with all four pieces on, the wearer can't be afflicted with
 * Bogrot, the boots' leap recovers a little faster, a leap landing slows what it hits and speeds the
 * wearer up, and the hide takes less damage in the wet. The boots' own gifts (swimming, breath, leap)
 * work without the rest of the set.
 */
public final class BoghideSet {
    public static final int LEAP_COOLDOWN_TICKS = 10 * 20;
    public static final int FULL_SET_LEAP_COOLDOWN_TICKS = 8 * 20;
    public static final int AMBUSH_EFFECT_TICKS = 3 * 20;
    public static final float MARSH_DAMAGE_MULTIPLIER = 0.9F;
    /** How long a leap landing leaves its targets marked, and how much extra they take from the wearer. */
    public static final int AMBUSH_MARK_TICKS = 4 * 20;
    public static final float AMBUSH_DAMAGE_MULTIPLIER = 1.25F;

    private static final TagKey<Item> BOGHIDE_ARMOR = TagKey.create(Registries.ITEM,
            new ResourceLocation(KingsAndMonsters.MODID, "boghide_armor"));

    private BoghideSet() {
    }

    public static boolean hasFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(BOGHIDE_ARMOR)
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(BOGHIDE_ARMOR)
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(BOGHIDE_ARMOR)
                && entity.getItemBySlot(EquipmentSlot.FEET).is(BOGHIDE_ARMOR);
    }

    /** Standing in water, on mud, or anywhere in a swamp. */
    public static boolean inMarsh(LivingEntity entity) {
        if (entity.isInWater() || entity.getBlockStateOn().is(Blocks.MUD)) return true;
        var biome = entity.level().getBiome(entity.blockPosition());
        return biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
    }

    public static int leapCooldownTicks(LivingEntity entity) {
        return hasFullSet(entity) ? FULL_SET_LEAP_COOLDOWN_TICKS : LEAP_COOLDOWN_TICKS;
    }
}
