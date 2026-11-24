package com.kingsandmonsters.enchantment;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModItems;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Predicate;

/** The enchantments of the mod. Costs and rarity follow what the 1.21 data files declared. */
public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, KingsAndMonsters.MODID);

    public static final RegistryObject<Enchantment> BARBED = ENCHANTMENTS.register("barbed",
            () -> new ModEnchantment(Enchantment.Rarity.COMMON, EquipmentSlot.MAINHAND, 2,
                    8, 10, 35, 10, ModEnchantments::isHuntersSpear));

    public static final RegistryObject<Enchantment> HEAVY_THROW = ENCHANTMENTS.register("heavy_throw",
            () -> new ModEnchantment(Enchantment.Rarity.UNCOMMON, EquipmentSlot.MAINHAND, 3,
                    10, 10, 45, 12, ModEnchantments::isThrowable));

    public static final RegistryObject<Enchantment> TYRANT = ENCHANTMENTS.register("tyrant",
            () -> new ModEnchantment(Enchantment.Rarity.RARE, EquipmentSlot.MAINHAND, 3,
                    15, 12, 50, 15, ModEnchantments::isTyrantWeapon));

    public static final RegistryObject<Enchantment> BOGBURST = ENCHANTMENTS.register("bogburst",
            () -> new ModEnchantment(Enchantment.Rarity.UNCOMMON, EquipmentSlot.MAINHAND, 2,
                    12, 15, 35, 15, stack -> stack.is(ModItems.BOGFANG_BLADE.get())));

    public static final RegistryObject<Enchantment> BRIGHTWICK = ENCHANTMENTS.register("brightwick",
            () -> new ModEnchantment(Enchantment.Rarity.UNCOMMON, EquipmentSlot.HEAD, 3,
                    5, 10, 30, 10, stack -> stack.is(ModItems.CANDLE_HELMET.get())));

    public static final RegistryObject<Enchantment> TIMBER = ENCHANTMENTS.register("timber",
            () -> new ModEnchantment(Enchantment.Rarity.VERY_RARE, EquipmentSlot.MAINHAND, 1,
                    50, 0, 50, 0, stack -> stack.is(ModItems.BOG_IRON_AXE.get())));

    private ModEnchantments() {}

    private static boolean isHuntersSpear(ItemStack stack) {
        return stack.is(ModItems.HUNTERS_SPEAR.get());
    }

    private static boolean isThrowable(ItemStack stack) {
        return isHuntersSpear(stack) || stack.is(Items.TRIDENT);
    }

    private static boolean isTyrantWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)
                || item == ModItems.KINGS_CLEAVER.get() || item == ModItems.OGRE_KINGS_CLUB.get();
    }

    /** Tyrant and Heavy Throw are the ogre signature pair: neither sits with a vanilla damage enchantment. */
    private static boolean excludedBySignature(Enchantment other) {
        return other == Enchantments.SHARPNESS || other == Enchantments.SMITE
                || other == Enchantments.BANE_OF_ARTHROPODS || other == Enchantments.IMPALING
                || other == Enchantments.RIPTIDE;
    }

    /** One of the enchantments, described by its costs and what it can go on. */
    private static final class ModEnchantment extends Enchantment {
        private final int maxLevel;
        private final int minBase;
        private final int minPerLevel;
        private final int maxBase;
        private final int maxPerLevel;
        private final Predicate<ItemStack> supported;

        ModEnchantment(Rarity rarity, EquipmentSlot slot, int maxLevel, int minBase, int minPerLevel,
                       int maxBase, int maxPerLevel, Predicate<ItemStack> supported) {
            super(rarity, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{slot});
            this.maxLevel = maxLevel;
            this.minBase = minBase;
            this.minPerLevel = minPerLevel;
            this.maxBase = maxBase;
            this.maxPerLevel = maxPerLevel;
            this.supported = supported;
        }

        @Override
        public int getMaxLevel() {
            return maxLevel;
        }

        @Override
        public int getMinCost(int level) {
            return minBase + (level - 1) * minPerLevel;
        }

        @Override
        public int getMaxCost(int level) {
            return maxBase + (level - 1) * maxPerLevel;
        }

        @Override
        public boolean canEnchant(ItemStack stack) {
            return supported.test(stack);
        }

        @Override
        public boolean canApplyAtEnchantingTable(ItemStack stack) {
            return supported.test(stack);
        }

        @Override
        protected boolean checkCompatibility(Enchantment other) {
            if (other == this) {
                return false;
            }
            boolean signature = this == TYRANT.get() || this == HEAVY_THROW.get();
            if (signature && (other == TYRANT.get() || other == HEAVY_THROW.get() || excludedBySignature(other))) {
                return false;
            }
            if (this == BOGBURST.get()) {
                return other != Enchantments.SHARPNESS && other != Enchantments.SMITE
                        && other != Enchantments.BANE_OF_ARTHROPODS;
            }
            return true;
        }
    }
}
