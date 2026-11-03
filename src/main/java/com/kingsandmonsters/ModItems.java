package com.kingsandmonsters;

import com.google.common.collect.ImmutableMultimap;
import com.kingsandmonsters.item.BogIronArmorItem;
import com.kingsandmonsters.item.OgreKingsCrownItem;
import com.kingsandmonsters.item.OgreKingsClubItem;
import com.kingsandmonsters.item.KingsCleaverItem;
import com.kingsandmonsters.item.CurioItem;
import com.kingsandmonsters.item.IronbellyStewItem;
import com.kingsandmonsters.item.BogEyeCharmItem;
import com.kingsandmonsters.item.GoldenOgreToothItem;
import com.kingsandmonsters.item.CreativeDestroyerItem;
import com.kingsandmonsters.item.OgreHookbladeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.kingsandmonsters.item.HunterSpearItem;
import com.kingsandmonsters.item.OgrebloodTotemItem;
import com.kingsandmonsters.item.OgreMerchantBackpackItem;
import com.kingsandmonsters.item.KingsTributeBagItem;
import net.minecraft.world.item.component.BundleContents;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KingsAndMonsters.MODID);

    public static final DeferredItem<Item> BOG_IRON_INGOT = ITEMS.register("bog_iron",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RAW_BOG_IRON = ITEMS.register("raw_bog_iron",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<GoldenOgreToothItem> GOLDEN_OGRE_TOOTH = ITEMS.register("golden_ogre_tooth",
            () -> new GoldenOgreToothItem(new Item.Properties()));

    public static final DeferredItem<OgrebloodTotemItem> OGREBLOOD_TOTEM = ITEMS.register("ogreblood_totem",
            () -> new OgrebloodTotemItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<OgreMerchantBackpackItem> OGRE_MERCHANT_BACKPACK = ITEMS.register("ogre_merchant_backpack",
            () -> new OgreMerchantBackpackItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<KingsTributeBagItem> KINGS_TRIBUTE_BUNDLE = ITEMS.register("kings_tribute_bundle",
            () -> new KingsTributeBagItem(new Item.Properties().stacksTo(1)
                    .component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)));

    public static final DeferredItem<Item> IRONBELLY_STEW = ITEMS.register("ironbelly_stew",
            () -> new IronbellyStewItem(new Item.Properties()
                    .stacksTo(1)
                    .food(new FoodProperties.Builder()
                            .nutrition(8)
                            .saturationModifier(0.9F)
                            .alwaysEdible()
                            .build())));

    public static final DeferredItem<BogEyeCharmItem> BOG_EYE_CHARM = ITEMS.register("bog_eye_charm",
            () -> new BogEyeCharmItem(new Item.Properties().durability(64)));

    public static final DeferredItem<Item> WARCALLER_BELL = ITEMS.register("warcaller_bell",
            () -> new CurioItem(new Item.Properties().stacksTo(1), ImmutableMultimap.of(), curioTooltip("Charm",
                    Component.literal("Draws the attention of nearby hostile creatures").withStyle(ChatFormatting.BLUE),
                    Component.literal("While targeted by 3+ hostiles within 16 blocks:").withStyle(ChatFormatting.GRAY),
                    Component.literal("+1 Max Heart").withStyle(ChatFormatting.BLUE),
                    Component.literal("+4 Armor").withStyle(ChatFormatting.BLUE),
                    Component.literal("Bonuses linger for 2 seconds").withStyle(ChatFormatting.DARK_GRAY)
            )));

    public static final DeferredItem<Item> RATTLEBONE_RING = ITEMS.register("rattlebone_ring",
            () -> new CurioItem(new Item.Properties().stacksTo(1), ImmutableMultimap.of(), curioTooltip("Ring",
                    Component.literal("Hostile kills release a 4 damage Death Rattle").withStyle(ChatFormatting.BLUE),
                    Component.literal("Hits enemies within 5.5 blocks").withStyle(ChatFormatting.BLUE),
                    Component.literal("Applies Slowness I for 4 seconds").withStyle(ChatFormatting.BLUE),
                    Component.literal("6 second cooldown").withStyle(ChatFormatting.DARK_GRAY)
            )));

    public static final DeferredItem<BannerItem> OGRE_BRUTE_BANNER = ITEMS.register("ogre_brute_banner",
            () -> fixedBanner(Blocks.BLACK_BANNER, Blocks.BLACK_WALL_BANNER, ModBannerPatterns.OGRE_BRUTE));

    public static final DeferredItem<BannerItem> GRUNT_CAPTAIN_BANNER = ITEMS.register("grunt_captain_banner",
            () -> fixedBanner(Blocks.RED_BANNER, Blocks.RED_WALL_BANNER, ModBannerPatterns.GRUNT_CAPTAIN));

    public static final DeferredItem<BannerItem> OGRE_KINGS_BANNER = ITEMS.register("ogre_kings_banner",
            () -> fixedBanner(Blocks.PURPLE_BANNER, Blocks.PURPLE_WALL_BANNER, ModBannerPatterns.OGRE_KING));

    public static final DeferredItem<BannerItem> OGRE_MAGE_BANNER = ITEMS.register("ogre_mage_banner",
            () -> fixedBanner(Blocks.BROWN_BANNER, Blocks.BROWN_WALL_BANNER, ModBannerPatterns.OGRE_MAGE));

    private static BannerItem fixedBanner(Block standing, Block wall, Holder<net.minecraft.world.level.block.entity.BannerPattern> pattern) {
        return new BannerItem(standing, wall,
                new Item.Properties().stacksTo(16).component(
                        DataComponents.BANNER_PATTERNS,
                        new BannerPatternLayers.Builder().add(pattern, DyeColor.WHITE).build()));
    }

    public static final DeferredItem<Item> OGRE_TOOTH_NECKLACE = ITEMS.register("ogre_tooth_necklace",
            () -> new CurioItem(new Item.Properties().stacksTo(1),
                    ImmutableMultimap.of(
                            Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(
                                    ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "ogre_tooth_necklace_damage"),
                                    1.0, AttributeModifier.Operation.ADD_VALUE)),
                    curioTooltip("Necklace",
                            Component.literal("+1 Attack Damage").withStyle(ChatFormatting.BLUE),
                            Component.literal("Attacks deal +10% knockback").withStyle(ChatFormatting.BLUE)
                    )));

    public static final DeferredItem<Item> BRUTE_HEART = ITEMS.register("brute_heart", () -> {
        ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.MAX_HEALTH, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "brute_heart_health"),
                4.0, AttributeModifier.Operation.ADD_VALUE));
        builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "brute_heart_knockback_res"),
                0.2, AttributeModifier.Operation.ADD_VALUE));
        return new CurioItem(new Item.Properties().stacksTo(1), builder.build(), curioTooltip("Charm",
                Component.literal("+2 Max Hearts").withStyle(ChatFormatting.BLUE),
                Component.literal("+20% Knockback Resistance").withStyle(ChatFormatting.BLUE)
        ));
    });

    public static final DeferredItem<Item> BOG_IRON_BAND = ITEMS.register("bog_iron_band",
            () -> new CurioItem(new Item.Properties().stacksTo(1),
                    ImmutableMultimap.of(
                            Attributes.ARMOR,
                            new AttributeModifier(
                                    ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "bog_iron_band_armor"),
                                    2.0, AttributeModifier.Operation.ADD_VALUE)),
                    curioTooltip("Ring",
                            Component.literal("+2 Armor").withStyle(ChatFormatting.BLUE),
                            Component.literal("20% chance to prevent incoming Poison").withStyle(ChatFormatting.BLUE),
                            Component.literal("Stacks with Bog Iron armor").withStyle(ChatFormatting.DARK_GRAY)
                    )));

    public static final DeferredItem<Item> BOGFUME_CHARM = ITEMS.register("bogfume_charm",
            () -> new CurioItem(new Item.Properties().stacksTo(1), ImmutableMultimap.of(), curioTooltip("Charm",
                    Component.literal("30% chance to inflict Poison II on melee attackers for 5s").withStyle(ChatFormatting.BLUE),
                    Component.literal("All poison you inflict deals +20% damage").withStyle(ChatFormatting.BLUE)
            )));

    public static final DeferredItem<Item> BUCKLER = ITEMS.register("buckler",
            () -> new CurioItem(new Item.Properties().stacksTo(1), ImmutableMultimap.of(), curioTooltip("Hands",
                    Component.literal("25% chance to block arrows").withStyle(ChatFormatting.BLUE),
                    Component.literal("Player-fired arrows deal +10% damage").withStyle(ChatFormatting.BLUE),
                    Component.literal("Does not affect thrown spears").withStyle(ChatFormatting.DARK_GRAY)
            )));

    private static List<Component> curioTooltip(String slot, Component... effects) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.kingsandmonsters.slot", slot)
                .withStyle(ChatFormatting.YELLOW));
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.kingsandmonsters.when_worn")
                .withStyle(ChatFormatting.GRAY));
        lines.addAll(Arrays.asList(effects));
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.kingsandmonsters.not_stackable")
                .withStyle(ChatFormatting.BLUE));
        return List.copyOf(lines);
    }

    /** Throwable player weapon and the Ogre Guard's signature armament. */
    public static final DeferredItem<HunterSpearItem> HUNTERS_SPEAR = ITEMS.register("hunters_spear",
            () -> new HunterSpearItem(new Item.Properties()
                    .durability(384)
                    .attributes(HunterSpearItem.createAttributes())));

    // -------------------------------------------------------------------------
    // Dev tools — test jigsaw structure spawners
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Armor Materials
    // -------------------------------------------------------------------------

    /**
     * Bog Iron - a swamp-forged endgame material that sits between diamond
     * and netherite. Vanilla diamond and netherite share armor point values,
     * so bog iron matches that defense while landing between them on
     * durability and toughness.
     *
     * Defense:    Helmet 3 | Chestplate 8 | Leggings 6 | Boots 3
     * Durability: multiplier 35  (diamond uses 33, netherite uses 37)
     */
    public static final Holder<ArmorMaterial> BOG_IRON = Holder.direct(new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.HELMET,     3);
                map.put(ArmorItem.Type.CHESTPLATE, 8);
                map.put(ArmorItem.Type.LEGGINGS,   6);
                map.put(ArmorItem.Type.BOOTS,      3);
                map.put(ArmorItem.Type.BODY,       8);
            }),
            12,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(BOG_IRON_INGOT.get()),
            List.of(new ArmorMaterial.Layer(
                    ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "bog_iron"))),
            2.5f,
            0.05f
    ));

    /** Gold-level protection, 1 armor toughness, and the durability of a Bog Iron helmet. */
    public static final Holder<ArmorMaterial> OGRE_KINGS_CROWN_MATERIAL = Holder.direct(new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.HELMET, 2);
                map.put(ArmorItem.Type.CHESTPLATE, 5);
                map.put(ArmorItem.Type.LEGGINGS, 3);
                map.put(ArmorItem.Type.BOOTS, 1);
                map.put(ArmorItem.Type.BODY, 5);
            }),
            25,
            SoundEvents.ARMOR_EQUIP_GOLD,
            () -> Ingredient.of(net.minecraft.world.item.Items.GOLD_INGOT),
            List.of(new ArmorMaterial.Layer(
                    ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "ogre_kings_crown"))),
            1.0f,
            0.0f
    ));

    // -------------------------------------------------------------------------
    // Tool Materials
    // -------------------------------------------------------------------------

    public static final Tier BOG_IRON_TOOL = new SimpleTier(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            1750,
            8.5F,
            3.25F,
            12,
            () -> Ingredient.of(BOG_IRON_INGOT.get()));

    // -------------------------------------------------------------------------
    // Bog Iron Tools
    // -------------------------------------------------------------------------

    public static final DeferredItem<SwordItem> BOG_IRON_SWORD = ITEMS.register("bog_iron_sword",
            () -> new SwordItem(BOG_IRON_TOOL,
                    new Item.Properties().attributes(SwordItem.createAttributes(BOG_IRON_TOOL, 3, -2.4F))));

    public static final DeferredItem<OgreHookbladeItem> OGRE_HOOKBLADE = ITEMS.register("ogre_hookblade",
            () -> new OgreHookbladeItem(BOG_IRON_TOOL,
                    new Item.Properties()
                            .durability(650)
                            .attributes(ItemAttributeModifiers.builder()
                                    // Total damage: player base 1 + this 6.5-point modifier.
                                    .add(Attributes.ATTACK_DAMAGE,
                                            new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.5,
                                                    AttributeModifier.Operation.ADD_VALUE),
                                            EquipmentSlotGroup.MAINHAND)
                                    .add(Attributes.ATTACK_SPEED,
                                            new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.6,
                                                    AttributeModifier.Operation.ADD_VALUE),
                                            EquipmentSlotGroup.MAINHAND)
                                    .add(Attributes.ENTITY_INTERACTION_RANGE,
                                            new AttributeModifier(
                                                    ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "ogre_hookblade_reach"),
                                                    0.75, AttributeModifier.Operation.ADD_VALUE),
                                            EquipmentSlotGroup.MAINHAND)
                                    .build())));

    /** Creative-only iron sword with exactly 1000 total attack damage. */
    public static final DeferredItem<CreativeDestroyerItem> CREATIVE_DESTROYER = ITEMS.register("creative_destroyer",
            () -> new CreativeDestroyerItem(Tiers.IRON, new Item.Properties().attributes(
                    ItemAttributeModifiers.builder()
                            .add(Attributes.ATTACK_DAMAGE,
                                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 999.0,
                                            AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .add(Attributes.ATTACK_SPEED,
                                    new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4,
                                            AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .build())));

    /** Creative-only diamond sword with exactly 100 total attack damage. */

    public static final DeferredItem<PickaxeItem> BOG_IRON_PICKAXE = ITEMS.register("bog_iron_pickaxe",
            () -> new PickaxeItem(BOG_IRON_TOOL,
                    new Item.Properties().attributes(DiggerItem.createAttributes(BOG_IRON_TOOL, 1.0F, -2.8F))));

    public static final DeferredItem<AxeItem> BOG_IRON_AXE = ITEMS.register("bog_iron_axe",
            () -> new AxeItem(BOG_IRON_TOOL,
                    new Item.Properties().attributes(DiggerItem.createAttributes(BOG_IRON_TOOL, 6.0F, -3.1F))));

    public static final DeferredItem<ShovelItem> BOG_IRON_SHOVEL = ITEMS.register("bog_iron_shovel",
            () -> new ShovelItem(BOG_IRON_TOOL,
                    new Item.Properties().attributes(DiggerItem.createAttributes(BOG_IRON_TOOL, 1.5F, -3.0F))));

    public static final DeferredItem<HoeItem> BOG_IRON_HOE = ITEMS.register("bog_iron_hoe",
            () -> new HoeItem(BOG_IRON_TOOL,
                    new Item.Properties().attributes(DiggerItem.createAttributes(BOG_IRON_TOOL, -2.0F, -1.0F))));

    // -------------------------------------------------------------------------
    // Bog Iron Armor
    // -------------------------------------------------------------------------

    public static final DeferredItem<ArmorItem> BOG_IRON_HELMET = ITEMS.register("bog_iron_helmet",
            () -> new BogIronArmorItem(BOG_IRON, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(35))));

    public static final DeferredItem<ArmorItem> BOG_IRON_CHESTPLATE = ITEMS.register("bog_iron_chestplate",
            () -> new BogIronArmorItem(BOG_IRON, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(35))));

    public static final DeferredItem<ArmorItem> BOG_IRON_LEGGINGS = ITEMS.register("bog_iron_leggings",
            () -> new BogIronArmorItem(BOG_IRON, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(35))));

    public static final DeferredItem<ArmorItem> BOG_IRON_BOOTS = ITEMS.register("bog_iron_boots",
            () -> new BogIronArmorItem(BOG_IRON, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(35))));

    public static final DeferredItem<ArmorItem> OGRE_KINGS_CROWN = ITEMS.register("ogre_kings_crown",
            () -> new OgreKingsCrownItem(OGRE_KINGS_CROWN_MATERIAL,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(35))));

    public static final DeferredItem<OgreKingsClubItem> OGRE_KINGS_CLUB = ITEMS.register("ogre_kings_club",
            () -> new OgreKingsClubItem(new Item.Properties()
                    .durability(1600)
                    .attributes(ItemAttributeModifiers.builder()
                            // Total held damage 12: player base 1 plus this 11-point modifier.
                            .add(Attributes.ATTACK_DAMAGE,
                                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 11.0,
                                            AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            // Total vanilla attack speed 0.9 from the player's base 4.0.
                            .add(Attributes.ATTACK_SPEED,
                                    new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.1,
                                            AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .build())));

    // -------------------------------------------------------------------------
    // Spawn Eggs
    // -------------------------------------------------------------------------

    /** Dark mossy-green body, muddy brown spots. */
    public static final DeferredItem<DeferredSpawnEggItem> OGRE_GRUNT_SPAWN_EGG = ITEMS.register(
            "ogre_grunt_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OGRE_GRUNT, 0x3d5a1e, 0x8b6914,
                    new Item.Properties()));

    /** Mossy-green hide with an iron-gray accent for the captain's armor. */
    public static final DeferredItem<DeferredSpawnEggItem> OGRE_GRUNT_CAPTAIN_SPAWN_EGG = ITEMS.register(
            "ogre_grunt_captain_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OGRE_GRUNT_CAPTAIN, 0x3d5a1e, 0xa8adb0,
                    new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> OGRE_MAGE_SPAWN_EGG = ITEMS.register(
            "ogre_mage_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OGRE_MAGE, 0x5e3b2c, 0x6f8d5a,
                    new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> OGRE_BRUTE_SPAWN_EGG = ITEMS.register(
            "ogre_brute_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OGRE_BRUTE, 0x425a2b, 0xb3b7aa,
                    new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> OGRE_ARCHER_SPAWN_EGG = ITEMS.register(
            "ogre_archer_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OGRE_ARCHER, 0x4f642d, 0x2b2118,
                    new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> OGRE_GUARD_SPAWN_EGG = ITEMS.register(
            "ogre_guard_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OGRE_GUARD, 0x44552b, 0x8b793f,
                    new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> OGRE_MERCHANT_SPAWN_EGG = ITEMS.register(
            "ogre_merchant_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OGRE_MERCHANT, 0x58723c, 0xd6a51c,
                    new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> OGRE_LORD_SPAWN_EGG = ITEMS.register(
            "ogre_lord_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OGRE_LORD, 0x2b3a1e, 0xc9a227,
                    new Item.Properties()));

    // The Ogre Lord's weapon — a two-handed greatsword, clearly the strongest weapon in the mod
    // and noticeably better than the Bog Iron tier across the board.
    public static final Tier KINGS_CLEAVER_TOOL = new SimpleTier(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            2200,
            9.0F,
            6.0F,
            15,
            () -> Ingredient.of(BOG_IRON_INGOT.get()));

    private static final ResourceLocation KINGS_CLEAVER_BLOCK_REACH_ID =
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "kings_cleaver_block_reach");

    public static final DeferredItem<KingsCleaverItem> KINGS_CLEAVER = ITEMS.register("kings_cleaver",
            () -> new KingsCleaverItem(KINGS_CLEAVER_TOOL, new Item.Properties().attributes(
                    ItemAttributeModifiers.builder()
                            // Total held damage is 10: player base 1 plus this 9-point modifier.
                            .add(Attributes.ATTACK_DAMAGE,
                                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 9.0, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            // Heavy and slow — a greatsword swing, not a quick poke.
                            .add(Attributes.ATTACK_SPEED,
                                    new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.0, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .add(Attributes.BLOCK_INTERACTION_RANGE,
                                    new AttributeModifier(KINGS_CLEAVER_BLOCK_REACH_ID, 1.0, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .build())));

}
