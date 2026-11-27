package com.kingsandmonsters;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Shared, player-facing item order for the Creative inventory and JEI ingredient list. */
public final class ContentDisplayOrder {
    private ContentDisplayOrder() {}

    public static List<ItemStack> mainCreativeItems() {
        List<ItemStack> items = new ArrayList<>();

        // Raw resources and creature materials.
        add(items,
                ModBlocks.BOG_IRON_ORE_ITEM.get(),
                ModBlocks.DEEPSLATE_BOG_IRON_ORE_ITEM.get(),
                ModItems.RAW_BOG_IRON.get(),
                ModBlocks.RAW_BOG_IRON_BLOCK_ITEM.get(),
                ModItems.BOG_IRON_INGOT.get(),
                ModBlocks.BOG_IRON_BLOCK_ITEM.get(),
                ModItems.BOG_TOAD_SKIN.get(),
                ModItems.BOGHIDE.get(),
                ModItems.BOG_TOAD_VENOM.get(),
                ModItems.TOAD_EYE.get(),
                ModItems.BOGFANG.get(),
                ModItems.ACORN.get(),
                ModItems.BLACKHIDE_LEATHER.get(),
                ModItems.MUDSNOUT_BONE.get(),
                ModItems.MUDSNOUT_TUSK.get());

        // Food.
        add(items,
                ModItems.RAW_TOAD_LEG.get(),
                ModItems.COOKED_TOAD_LEG.get(),
                ModItems.TOAD_STEW.get(),
                ModBlocks.HAUNCH_PLATTER_ITEM.get(),
                ModItems.HAUNCH_PLATTER_SERVING.get(),
                ModItems.RAW_MUDSNOUT_HAUNCH.get(),
                ModItems.ROASTED_MUDSNOUT_HAUNCH.get(),
                ModItems.BONE_BROTH.get(),
                ModItems.IRONBELLY_STEW.get());

        // Weapons, tools, traps, and field utility.
        add(items,
                ModItems.BOG_IRON_SWORD.get(),
                ModItems.BOG_IRON_PICKAXE.get(),
                ModItems.BOG_IRON_AXE.get(),
                ModItems.BOG_IRON_SHOVEL.get(),
                ModItems.BOG_IRON_HOE.get(),
                ModItems.BOGFANG_BLADE.get(),
                ModItems.BONE_CLUB.get(),
                ModItems.SKINNING_KNIFE.get(),
                ModItems.BOGJAW_TRAP.get(),
                ModItems.BOAR_CALLING_HORN.get());

        // Armor sets: Bog Iron, Boghide, then Blackhide.
        add(items,
                ModItems.BOG_IRON_HELMET.get(),
                ModItems.BOG_IRON_CHESTPLATE.get(),
                ModItems.BOG_IRON_LEGGINGS.get(),
                ModItems.BOG_IRON_BOOTS.get(),
                ModItems.BOGHIDE_HELMET.get(),
                ModItems.BOGHIDE_CHESTPLATE.get(),
                ModItems.BOGHIDE_LEGGINGS.get(),
                ModItems.BOGHIDE_BOOTS.get(),
                ModItems.BLACKHIDE_HELMET.get(),
                ModItems.BLACKHIDE_CHESTPLATE.get(),
                ModItems.BLACKHIDE_LEGGINGS.get(),
                ModItems.BLACKHIDE_BOOTS.get());

        // Currency, tribute, and merchant utility. Charms live in the Artifacts tab.
        add(items,
                ModItems.GOLDEN_OGRE_TOOTH.get(),
                ModItems.GOLD_COIN.get(),
                ModItems.OGRE_HOOKBLADE.get(),
                ModItems.KINGS_TRIBUTE_BUNDLE.get(),
                ModItems.OGRE_MERCHANT_BACKPACK.get());

        // Signature Ogre equipment belongs in the main Kings and Monsters tab.
        add(items,
                ModItems.HUNTERS_SPEAR.get(),
                ModItems.KINGS_CLEAVER.get(),
                ModItems.BRUTES_HELM.get(),
                ModItems.BOGSTEEL_CHESTPLATE.get(),
                ModItems.OGRE_LOINCLOTH.get(),
                ModItems.BRUTE_GAUNTLETS.get(),
                ModItems.OGRE_STOMACH.get(),
                ModItems.OGRE_KINGS_CROWN.get(),
                ModItems.OGRE_KINGS_CLUB.get());

        // Faction banners, followed by the Goblin-update teaser.
        add(items,
                ModItems.OGRE_BRUTE_BANNER.get(),
                ModItems.GRUNT_CAPTAIN_BANNER.get(),
                ModItems.OGRE_MAGE_BANNER.get(),
                ModItems.OGRE_KINGS_BANNER.get(),
                ModItems.CANDLE_HELMET.get());

        // Creatures: Bog wildlife first, then the Ogre ranks from common to boss.
        add(items,
                ModItems.BOG_TOAD_SPAWN_EGG.get(),
                ModItems.BOG_POLE_SPAWN_EGG.get(),
                ModItems.MUDSNOUT_BOAR_SPAWN_EGG.get(),
                ModItems.BOAR_JOCKEY_SPAWN_EGG.get(),
                ModItems.OGRE_GRUNT_SPAWN_EGG.get(),
                ModItems.OGRE_GRUNT_CAPTAIN_SPAWN_EGG.get(),
                ModItems.OGRE_MAGE_SPAWN_EGG.get(),
                ModItems.OGRE_BRUTE_SPAWN_EGG.get(),
                ModItems.OGRE_TRAPPER_SPAWN_EGG.get(),
                ModItems.OGRE_ARCHER_SPAWN_EGG.get(),
                ModItems.OGRE_GUARD_SPAWN_EGG.get(),
                ModItems.PLAGUE_OGRE_SPAWN_EGG.get(),
                ModItems.OGRE_MERCHANT_SPAWN_EGG.get(),
                ModItems.OGRE_LORD_SPAWN_EGG.get());

        // Explicitly last: destructive testing utility.
        items.add(new ItemStack(ModItems.CREATIVE_DESTROYER.get()));
        return List.copyOf(items);
    }

    public static List<ItemStack> artifactCreativeItems() {
        List<ItemStack> items = new ArrayList<>();
        // Wearable curios, charms, wards, and compact Ogre relics.
        add(items,
                ModItems.BOG_EYE_CHARM.get(),
                ModItems.OGREBLOOD_TOTEM.get(),
                ModItems.WARCALLER_BELL.get(),
                ModItems.RATTLEBONE_RING.get(),
                ModItems.OGRE_TOOTH_NECKLACE.get(),
                ModItems.BRUTE_HEART.get(),
                ModItems.BOG_IRON_BAND.get(),
                ModItems.BOGFUME_CHARM.get(),
                ModItems.BUCKLER.get(),
                ModItems.PADDED_EARMUFFS.get());
        return List.copyOf(items);
    }

    public static List<ItemStack> jeiItems() {
        List<ItemStack> items = new ArrayList<>(mainCreativeItems());
        items.removeIf(stack -> stack.is(ModItems.CREATIVE_DESTROYER.get()));

        // Keep relics ahead of banners and spawn eggs in JEI's single continuous list.
        int firstBanner = indexOf(items, ModItems.OGRE_BRUTE_BANNER.get());
        items.addAll(firstBanner, artifactCreativeItems());
        return List.copyOf(items);
    }

    private static int indexOf(List<ItemStack> items, ItemLike item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).is(item.asItem())) return i;
        }
        return items.size();
    }

    private static void add(List<ItemStack> output, ItemLike... items) {
        for (var item : items) output.add(new ItemStack(item));
    }
}
