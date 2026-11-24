package com.kingsandmonsters;

import com.google.common.collect.ImmutableMultimap;
import com.kingsandmonsters.item.BogIronArmorItem;
import com.kingsandmonsters.item.BogjawTrapItem;
import com.kingsandmonsters.item.CandleHelmetItem;
import com.kingsandmonsters.item.OgreKingsCrownItem;
import com.kingsandmonsters.item.OgreKingsClubItem;
import com.kingsandmonsters.item.KingsCleaverItem;
import com.kingsandmonsters.item.CurioItem;
import com.kingsandmonsters.item.IronbellyStewItem;
import com.kingsandmonsters.item.BogEyeCharmItem;
import com.kingsandmonsters.item.GoldenOgreToothItem;
import com.kingsandmonsters.item.CreativeDestroyerItem;
import com.kingsandmonsters.item.OgreHookbladeItem;
import com.kingsandmonsters.item.BlackhideArmorItem;
import com.kingsandmonsters.item.BoghideArmorItem;
import com.kingsandmonsters.item.ToadStewItem;
import com.kingsandmonsters.item.RoastedMudsnoutHaunchItem;
import com.kingsandmonsters.item.BoarCallingHornItem;
import com.kingsandmonsters.item.BoneBrothItem;
import com.kingsandmonsters.item.SkinningKnifeItem;
import com.kingsandmonsters.item.BruteGauntletsItem;
import com.kingsandmonsters.item.OgreHelmetItem;
import com.kingsandmonsters.item.OgreLoinclothItem;
import com.kingsandmonsters.item.BogsteelChestplateItem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.ForgeMod;
import net.minecraft.world.level.block.entity.BannerPattern;
import com.kingsandmonsters.item.ItemAttributes;
import com.kingsandmonsters.item.ModArmorMaterial;
import com.kingsandmonsters.item.ModTier;
import com.kingsandmonsters.item.FixedBannerItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.Ingredient;
import com.kingsandmonsters.item.BoarJockeySpawnEggItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraftforge.registries.DeferredRegister;
import com.kingsandmonsters.item.HunterSpearItem;
import com.kingsandmonsters.item.OgrebloodTotemItem;
import com.kingsandmonsters.item.OgreMerchantBackpackItem;
import com.kingsandmonsters.item.KingsTributeBagItem;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, KingsAndMonsters.MODID);

    public static final RegistryObject<Item> BOG_IRON_INGOT = ITEMS.register("bog_iron",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RAW_BOG_IRON = ITEMS.register("raw_bog_iron",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BOG_TOAD_SKIN = ITEMS.register("bog_toad_skin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BOGHIDE = ITEMS.register("boghide", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BOG_TOAD_VENOM = ITEMS.register("bog_toad_venom", () -> new Item(new Item.Properties()));
    // Technically edible, in the same regrettable sense as a spider eye.
    public static final RegistryObject<Item> TOAD_EYE = ITEMS.register("toad_eye", () -> new Item(
            new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(2).saturationMod(0.8F)
                    .effect(new MobEffectInstance(MobEffects.POISON, 5 * 20), 1.0F)
                    .build())));
    public static final RegistryObject<Item> BOGFANG = ITEMS.register("bogfang", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_TOAD_LEG = ITEMS.register("raw_toad_leg", () -> new Item(
            new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_TOAD_LEG = ITEMS.register("cooked_toad_leg", () -> new Item(
            new Item.Properties().food(new FoodProperties.Builder().nutrition(9).saturationMod(0.8F).build())));
    public static final RegistryObject<ToadStewItem> TOAD_STEW = ITEMS.register("toad_stew", () -> new ToadStewItem(
            new Item.Properties().stacksTo(1).food(new FoodProperties.Builder()
                    .nutrition(10).saturationMod(1.0F).alwaysEat().build())));
    public static final RegistryObject<Item> RAW_MUDSNOUT_HAUNCH = ITEMS.register("raw_mudsnout_haunch", () -> new Item(
            new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(6).saturationMod(0.4F).build())));
    public static final RegistryObject<RoastedMudsnoutHaunchItem> ROASTED_MUDSNOUT_HAUNCH = ITEMS.register(
            "roasted_mudsnout_haunch", () -> new RoastedMudsnoutHaunchItem(
                    new Item.Properties().food(new FoodProperties.Builder()
                            .nutrition(12).saturationMod(0.8F).build())));
    public static final RegistryObject<com.kingsandmonsters.item.HaunchPlatterServingItem> HAUNCH_PLATTER_SERVING =
            ITEMS.register("haunch_platter_serving", () -> new com.kingsandmonsters.item.HaunchPlatterServingItem(
                    new Item.Properties().stacksTo(1).food(new FoodProperties.Builder()
                            .nutrition(10).saturationMod(0.9F)
                            .effect(new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.REGENERATION, 160), 1.0F)
                            .effect(new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 900), 1.0F)
                            .build())));
    public static final RegistryObject<Item> MUDSNOUT_BONE = ITEMS.register("mudsnout_bone", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MUDSNOUT_TUSK = ITEMS.register("mudsnout_tusk", () -> new Item(new Item.Properties()));
    public static final RegistryObject<BoneBrothItem> BONE_BROTH = ITEMS.register("bone_broth",
            () -> new BoneBrothItem(new Item.Properties().stacksTo(1)
                    .food(new FoodProperties.Builder().nutrition(8).saturationMod(0.8F).alwaysEat().build())));
    public static final RegistryObject<BoarCallingHornItem> BOAR_CALLING_HORN = ITEMS.register("boar_calling_horn",
            () -> new BoarCallingHornItem(new Item.Properties().durability(64)));
    public static final RegistryObject<SkinningKnifeItem> SKINNING_KNIFE = ITEMS.register("skinning_knife",
            () -> new SkinningKnifeItem(Tiers.IRON, new Item.Properties().durability(250)));
    static {
        ItemAttributes.mainHand(SKINNING_KNIFE, 5.0, -1.8);
    }
    // Placeable under oak leaves, where it grows into the acorns the Mudsnout Boars eat.
    public static final RegistryObject<Item> ACORN = ITEMS.register("acorn",
            () -> new net.minecraft.world.item.ItemNameBlockItem(ModBlocks.ACORN.get(), new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(1).saturationMod(0.1F).build())));
    public static final RegistryObject<Item> BLACKHIDE_LEATHER = ITEMS.register("blackhide_leather", () -> new Item(new Item.Properties()));

    public static final Tier BONE_CLUB_TIER = new ModTier(2, 384, 4.0F, 2.0F, 8,
            () -> Ingredient.of(MUDSNOUT_BONE.get()));
    public static final RegistryObject<SwordItem> BONE_CLUB = ITEMS.register("bone_club",
            () -> new SwordItem(BONE_CLUB_TIER, 5, -3.0F, new Item.Properties()));
    static {
        ItemAttributes.mainHand(BONE_CLUB, 7.0, -3.0);
    }

    public static final RegistryObject<GoldenOgreToothItem> GOLDEN_OGRE_TOOTH = ITEMS.register("golden_ogre_tooth",
            () -> new GoldenOgreToothItem(new Item.Properties()));

    public static final RegistryObject<com.kingsandmonsters.item.TributeCoinItem> GOLD_COIN = ITEMS.register("gold_coin",
            () -> new com.kingsandmonsters.item.TributeCoinItem(new Item.Properties()));

    public static final RegistryObject<OgrebloodTotemItem> OGREBLOOD_TOTEM = ITEMS.register("ogreblood_totem",
            () -> new OgrebloodTotemItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<OgreMerchantBackpackItem> OGRE_MERCHANT_BACKPACK = ITEMS.register("ogre_merchant_backpack",
            () -> new OgreMerchantBackpackItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<KingsTributeBagItem> KINGS_TRIBUTE_BUNDLE = ITEMS.register("kings_tribute_bundle",
            () -> new KingsTributeBagItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> IRONBELLY_STEW = ITEMS.register("ironbelly_stew",
            () -> new IronbellyStewItem(new Item.Properties()
                    .stacksTo(1)
                    .food(new FoodProperties.Builder()
                            .nutrition(8)
                            .saturationMod(0.9F)
                            .alwaysEat()
                            .build())));

    public static final RegistryObject<BogEyeCharmItem> BOG_EYE_CHARM = ITEMS.register("bog_eye_charm",
            () -> new BogEyeCharmItem(new Item.Properties().durability(64)));

    public static final RegistryObject<Item> WARCALLER_BELL = ITEMS.register("warcaller_bell",
            () -> new CurioItem(new Item.Properties().stacksTo(1), ImmutableMultimap.of(), curioTooltip("Charm",
                    Component.literal("Draws the attention of nearby hostile creatures").withStyle(ChatFormatting.BLUE),
                    Component.literal("While targeted by 3+ hostiles within 16 blocks:").withStyle(ChatFormatting.GRAY),
                    Component.literal("+1 Max Heart").withStyle(ChatFormatting.BLUE),
                    Component.literal("+4 Armor").withStyle(ChatFormatting.BLUE),
                    Component.literal("Bonuses linger for 2 seconds").withStyle(ChatFormatting.DARK_GRAY)
            )));

    public static final RegistryObject<Item> RATTLEBONE_RING = ITEMS.register("rattlebone_ring",
            () -> new CurioItem(new Item.Properties().stacksTo(1), ImmutableMultimap.of(), curioTooltip("Ring",
                    Component.literal("Hostile kills release a 4 damage Death Rattle").withStyle(ChatFormatting.BLUE),
                    Component.literal("Hits enemies within 5.5 blocks").withStyle(ChatFormatting.BLUE),
                    Component.literal("Applies Slowness I for 4 seconds").withStyle(ChatFormatting.BLUE),
                    Component.literal("6 second cooldown").withStyle(ChatFormatting.DARK_GRAY)
            )));

    public static final RegistryObject<BannerItem> OGRE_BRUTE_BANNER = ITEMS.register("ogre_brute_banner",
            () -> fixedBanner(Blocks.BLACK_BANNER, Blocks.BLACK_WALL_BANNER, ModBannerPatterns.OGRE_BRUTE));

    public static final RegistryObject<BannerItem> GRUNT_CAPTAIN_BANNER = ITEMS.register("grunt_captain_banner",
            () -> fixedBanner(Blocks.RED_BANNER, Blocks.RED_WALL_BANNER, ModBannerPatterns.GRUNT_CAPTAIN));

    public static final RegistryObject<BannerItem> OGRE_KINGS_BANNER = ITEMS.register("ogre_kings_banner",
            () -> fixedBanner(Blocks.PURPLE_BANNER, Blocks.PURPLE_WALL_BANNER, ModBannerPatterns.OGRE_KING));

    public static final RegistryObject<BannerItem> OGRE_MAGE_BANNER = ITEMS.register("ogre_mage_banner",
            () -> fixedBanner(Blocks.BROWN_BANNER, Blocks.BROWN_WALL_BANNER, ModBannerPatterns.OGRE_MAGE));

    private static BannerItem fixedBanner(Block standing, Block wall, RegistryObject<BannerPattern> pattern) {
        return new FixedBannerItem(standing, wall, new Item.Properties().stacksTo(16), pattern);
    }

    public static final RegistryObject<Item> OGRE_TOOTH_NECKLACE = ITEMS.register("ogre_tooth_necklace",
            () -> new CurioItem(new Item.Properties().stacksTo(1),
                    ImmutableMultimap.of(
                            Attributes.ATTACK_DAMAGE,
                            modifier("ogre_tooth_necklace_damage", 1.0, AttributeModifier.Operation.ADDITION)),
                    curioTooltip("Necklace",
                            Component.literal("+1 Attack Damage").withStyle(ChatFormatting.BLUE),
                            Component.literal("Attacks deal +10% knockback").withStyle(ChatFormatting.BLUE)
                    )));

    public static final RegistryObject<Item> BRUTE_HEART = ITEMS.register("brute_heart", () -> {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.MAX_HEALTH, modifier("brute_heart_health", 4.0, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.KNOCKBACK_RESISTANCE,
                modifier("brute_heart_knockback_res", 0.2, AttributeModifier.Operation.ADDITION));
        return new CurioItem(new Item.Properties().stacksTo(1), builder.build(), curioTooltip("Charm",
                Component.literal("+2 Max Hearts").withStyle(ChatFormatting.BLUE),
                Component.literal("+20% Knockback Resistance").withStyle(ChatFormatting.BLUE)
        ));
    });

    public static final RegistryObject<Item> BOG_IRON_BAND = ITEMS.register("bog_iron_band",
            () -> new CurioItem(new Item.Properties().stacksTo(1),
                    ImmutableMultimap.of(
                            Attributes.ARMOR,
                            modifier("bog_iron_band_armor", 2.0, AttributeModifier.Operation.ADDITION)),
                    curioTooltip("Ring",
                            Component.literal("+2 Armor").withStyle(ChatFormatting.BLUE),
                            Component.literal("Poison and Bogrot deal 20% less damage").withStyle(ChatFormatting.BLUE)
                    )));

    public static final RegistryObject<Item> BOGFUME_CHARM = ITEMS.register("bogfume_charm",
            () -> new CurioItem(new Item.Properties().stacksTo(1), ImmutableMultimap.of(), curioTooltip("Charm",
                    Component.literal("30% chance to inflict Poison II on melee attackers for 5s").withStyle(ChatFormatting.BLUE),
                    Component.literal("All poison you inflict deals +20% damage").withStyle(ChatFormatting.BLUE)
            )));

    public static final RegistryObject<Item> BUCKLER = ITEMS.register("buckler",
            () -> new CurioItem(new Item.Properties().stacksTo(1), ImmutableMultimap.of(), curioTooltip("Hands",
                    Component.literal("25% chance to block arrows").withStyle(ChatFormatting.BLUE),
                    Component.literal("Player-fired arrows deal +10% damage").withStyle(ChatFormatting.BLUE),
                    Component.literal("Does not affect thrown spears").withStyle(ChatFormatting.DARK_GRAY)
            )));

    public static final RegistryObject<Item> PADDED_EARMUFFS = ITEMS.register("padded_earmuffs",
            () -> new CurioItem(new Item.Properties().stacksTo(1), ImmutableMultimap.of(),
                    curioTooltip("Head",
                            Component.literal("Softens the Ogre King's roars").withStyle(ChatFormatting.BLUE),
                            Component.literal("Reduces roar debuffs and knockback").withStyle(ChatFormatting.BLUE))));

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
    public static final RegistryObject<HunterSpearItem> HUNTERS_SPEAR = ITEMS.register("hunters_spear",
            () -> new HunterSpearItem(new Item.Properties().durability(384)));
    static {
        ItemAttributes.mainHand(HUNTERS_SPEAR, 7.0, -2.9);
    }

    // -------------------------------------------------------------------------
    // Armor Materials
    // -------------------------------------------------------------------------

    /**
     * Bog Iron: a swamp-forged material between diamond and netherite. Diamond and netherite share
     * armor point values, so bog iron matches that defense and lands between them on durability and
     * toughness.
     *
     * Defense:    Helmet 3 | Chestplate 8 | Leggings 6 | Boots 3
     * Durability: multiplier 35  (diamond uses 33, netherite uses 37)
     */
    public static final ArmorMaterial BOG_IRON = new ModArmorMaterial("bog_iron", 3, 8, 6, 3, 12,
            SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(BOG_IRON_INGOT.get()), 2.0f, 0.05f);

    /** Gold-level protection, 1 armor toughness, and the durability of a Bog Iron helmet. */
    public static final ArmorMaterial OGRE_KINGS_CROWN_MATERIAL = new ModArmorMaterial("ogre_kings_crown",
            2, 5, 3, 1, 25, SoundEvents.ARMOR_EQUIP_GOLD,
            () -> Ingredient.of(net.minecraft.world.item.Items.GOLD_INGOT), 1.0f, 0.0f);

    /**
     * A step above iron: iron's 2/6/5/2 defense plus a touch of toughness (iron has none) and more
     * durability. Bog Iron stays the stronger fighter; Boghide's edge is its set bonus.
     */
    public static final ArmorMaterial BOGHIDE_ARMOR = new ModArmorMaterial("boghide", 2, 6, 5, 2, 15,
            SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(BOGHIDE.get()), 0.5F, 0.0F);

    /**
     * Between iron (2/6/5/2, no toughness) and diamond (3/8/6/3, 2 toughness): a little more defense
     * than iron and some toughness, with iron's weak durability lifted partway. Its edge is Charge.
     */
    public static final ArmorMaterial BLACKHIDE_ARMOR = new ModArmorMaterial("blackhide", 3, 7, 5, 3, 12,
            SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(BLACKHIDE_LEATHER.get()), 1.0F, 0.0F);

    /** Bogsteel beats Netherite's raw helmet defense, but yields slightly more against heavy hits. */
    public static final ArmorMaterial BRUTES_HELM_MATERIAL = bogsteelGear("brutes_helm", 4,
            SoundEvents.ARMOR_EQUIP_IRON);

    /** Loincloth: scant cover that mostly leaves the muscle to speak. */
    public static final ArmorMaterial OGRE_LOINCLOTH_MATERIAL = ogreGear("ogre_loincloth", 1, 0.5f, 0.15f,
            SoundEvents.ARMOR_EQUIP_LEATHER);

    /** Bogsteel beats Netherite's raw chest defense, but has slightly less toughness. */
    public static final ArmorMaterial BOGSTEEL_CHESTPLATE_MATERIAL = bogsteelGear(
            "bogsteel_chestplate", 10, SoundEvents.ARMOR_EQUIP_IRON);

    /**
     * Ogre-purified Bogsteel is unmistakably above player-forged Bog Iron: more defense and twice
     * its knockback resistance. Its 3 toughness matches Netherite and clearly exceeds Bog Iron's
     * 2 toughness. Item durability is assigned at registration.
     */
    private static ArmorMaterial bogsteelGear(String name, int defense, SoundEvent sound) {
        return ModArmorMaterial.uniform(name, defense, 12, sound, () -> Ingredient.of(BOG_IRON_INGOT.get()),
                3.0f, 0.1f);
    }

    private static ArmorMaterial ogreGear(String name, int defense, float toughness, float knockbackResistance,
                                          SoundEvent sound) {
        return ModArmorMaterial.uniform(name, defense, 10, sound,
                () -> Ingredient.of(net.minecraft.world.item.Items.LEATHER), toughness, knockbackResistance);
    }

    // -------------------------------------------------------------------------
    // Tool Materials
    // -------------------------------------------------------------------------

    public static final Tier BOG_IRON_TOOL = new ModTier(3, 1750, 8.5F, 3.25F, 12,
            () -> Ingredient.of(BOG_IRON_INGOT.get()));

    // -------------------------------------------------------------------------
    // Bog Iron Tools
    // -------------------------------------------------------------------------

    public static final RegistryObject<SwordItem> BOG_IRON_SWORD = ITEMS.register("bog_iron_sword",
            () -> new SwordItem(BOG_IRON_TOOL, 3, -2.4F, new Item.Properties()));
    static {
        ItemAttributes.mainHand(BOG_IRON_SWORD, 6.25, -2.4);
    }

    public static final RegistryObject<OgreHookbladeItem> OGRE_HOOKBLADE = ITEMS.register("ogre_hookblade",
            () -> new OgreHookbladeItem(BOG_IRON_TOOL, new Item.Properties().durability(650)));
    static {
        // Total damage: player base 1 + this 6.5-point modifier.
        ItemAttributes.mainHand(OGRE_HOOKBLADE, 6.5, -2.6)
                .add(ForgeMod.ENTITY_REACH,
                        modifier("ogre_hookblade_reach", 0.75, AttributeModifier.Operation.ADDITION));
    }

    /** Creative-only iron sword with exactly 1000 total attack damage. */
    public static final RegistryObject<CreativeDestroyerItem> CREATIVE_DESTROYER = ITEMS.register("creative_destroyer",
            () -> new CreativeDestroyerItem(Tiers.IRON, new Item.Properties()));
    static {
        ItemAttributes.mainHand(CREATIVE_DESTROYER, 999.0, -2.4);
    }

    public static final RegistryObject<PickaxeItem> BOG_IRON_PICKAXE = ITEMS.register("bog_iron_pickaxe",
            () -> new PickaxeItem(BOG_IRON_TOOL, 1, -2.8F, new Item.Properties()));
    static {
        ItemAttributes.mainHand(BOG_IRON_PICKAXE, 4.25, -2.8);
    }

    public static final RegistryObject<AxeItem> BOG_IRON_AXE = ITEMS.register("bog_iron_axe",
            () -> new AxeItem(BOG_IRON_TOOL, 5.0F, -3.0F, new Item.Properties()));
    static {
        ItemAttributes.mainHand(BOG_IRON_AXE, 8.25, -3.0);
    }

    public static final RegistryObject<ShovelItem> BOG_IRON_SHOVEL = ITEMS.register("bog_iron_shovel",
            () -> new ShovelItem(BOG_IRON_TOOL, 1.5F, -3.0F, new Item.Properties()));
    static {
        ItemAttributes.mainHand(BOG_IRON_SHOVEL, 4.75, -3.0);
    }

    public static final RegistryObject<HoeItem> BOG_IRON_HOE = ITEMS.register("bog_iron_hoe",
            () -> new HoeItem(BOG_IRON_TOOL, -3, 0.0F, new Item.Properties()));
    static {
        ItemAttributes.mainHand(BOG_IRON_HOE, 0.0, 0.0);
    }

    // -------------------------------------------------------------------------
    // Bog Iron Armor
    // -------------------------------------------------------------------------

    public static final RegistryObject<ArmorItem> BOG_IRON_HELMET = ITEMS.register("bog_iron_helmet",
            () -> new BogIronArmorItem(BOG_IRON, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.HELMET, 35))));

    public static final RegistryObject<ArmorItem> BOG_IRON_CHESTPLATE = ITEMS.register("bog_iron_chestplate",
            () -> new BogIronArmorItem(BOG_IRON, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.CHESTPLATE, 35))));

    public static final RegistryObject<ArmorItem> BOG_IRON_LEGGINGS = ITEMS.register("bog_iron_leggings",
            () -> new BogIronArmorItem(BOG_IRON, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.LEGGINGS, 35))));

    public static final RegistryObject<ArmorItem> BOG_IRON_BOOTS = ITEMS.register("bog_iron_boots",
            () -> new BogIronArmorItem(BOG_IRON, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.BOOTS, 35))));

    public static final RegistryObject<BoghideArmorItem> BOGHIDE_HELMET = ITEMS.register("boghide_helmet",
            () -> new BoghideArmorItem(BOGHIDE_ARMOR, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.HELMET, 20))));

    public static final RegistryObject<BoghideArmorItem> BOGHIDE_CHESTPLATE = ITEMS.register("boghide_chestplate",
            () -> new BoghideArmorItem(BOGHIDE_ARMOR, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.CHESTPLATE, 20))));

    public static final RegistryObject<BoghideArmorItem> BOGHIDE_LEGGINGS = ITEMS.register("boghide_leggings",
            () -> new BoghideArmorItem(BOGHIDE_ARMOR, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.LEGGINGS, 20))));

    public static final RegistryObject<BoghideArmorItem> BOGHIDE_BOOTS = ITEMS.register("boghide_boots",
            () -> new BoghideArmorItem(BOGHIDE_ARMOR, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.BOOTS, 20))));

    public static final RegistryObject<BlackhideArmorItem> BLACKHIDE_HELMET = ITEMS.register("blackhide_helmet",
            () -> new BlackhideArmorItem(BLACKHIDE_ARMOR, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.HELMET, 24))));

    public static final RegistryObject<BlackhideArmorItem> BLACKHIDE_CHESTPLATE = ITEMS.register("blackhide_chestplate",
            () -> new BlackhideArmorItem(BLACKHIDE_ARMOR, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.CHESTPLATE, 24))));

    public static final RegistryObject<BlackhideArmorItem> BLACKHIDE_LEGGINGS = ITEMS.register("blackhide_leggings",
            () -> new BlackhideArmorItem(BLACKHIDE_ARMOR, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.LEGGINGS, 24))));

    public static final RegistryObject<BlackhideArmorItem> BLACKHIDE_BOOTS = ITEMS.register("blackhide_boots",
            () -> new BlackhideArmorItem(BLACKHIDE_ARMOR, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.BOOTS, 24))));

    public static final RegistryObject<CandleHelmetItem> CANDLE_HELMET = ITEMS.register("candle_helmet",
            () -> new CandleHelmetItem(net.minecraft.world.item.ArmorMaterials.IRON,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.HELMET, 15))));

    public static final Tier BOGFANG_TOOL = new ModTier(2, 400, 6.0F, 3.0F, 15,
            () -> Ingredient.of(BOGFANG.get()));

    public static final RegistryObject<SwordItem> BOGFANG_BLADE = ITEMS.register("bogfang_blade",
            () -> new SwordItem(BOGFANG_TOOL, 1, -2.4F, new Item.Properties().durability(400)));
    static {
        ItemAttributes.mainHand(BOGFANG_BLADE, 4.5, -2.4);
    }

    public static final RegistryObject<ArmorItem> OGRE_KINGS_CROWN = ITEMS.register("ogre_kings_crown",
            () -> new OgreKingsCrownItem(OGRE_KINGS_CROWN_MATERIAL,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.HELMET, 35))));

    public static final RegistryObject<OgreHelmetItem> BRUTES_HELM = ITEMS.register("brutes_helm",
            () -> new OgreHelmetItem(BRUTES_HELM_MATERIAL,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.HELMET, 40))));

    public static final RegistryObject<OgreLoinclothItem> OGRE_LOINCLOTH = ITEMS.register("ogre_loincloth",
            () -> new OgreLoinclothItem(OGRE_LOINCLOTH_MATERIAL,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.LEGGINGS, 18))));

    public static final RegistryObject<BogsteelChestplateItem> BOGSTEEL_CHESTPLATE = ITEMS.register(
            "bogsteel_chestplate", () -> new BogsteelChestplateItem(BOGSTEEL_CHESTPLATE_MATERIAL,
                    new Item.Properties().durability(ModArmorMaterial.durability(ArmorItem.Type.CHESTPLATE, 40))));

    public static final RegistryObject<Item> BRUTE_GAUNTLETS = ITEMS.register("brute_gauntlets", () -> {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ARMOR, modifier("brute_gauntlets_armor", 2.0, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, modifier("brute_gauntlets_toughness", 1.0, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_DAMAGE, modifier("brute_gauntlets_damage", 2.0, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_KNOCKBACK, modifier("brute_gauntlets_knockback", 0.5, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.KNOCKBACK_RESISTANCE, modifier("brute_gauntlets_knockback_res", 0.15, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, modifier("brute_gauntlets_speed", -0.05, AttributeModifier.Operation.MULTIPLY_TOTAL));
        return new BruteGauntletsItem(new Item.Properties().stacksTo(1), builder.build(), curioTooltip("Hands",
                Component.literal("+2 Armor").withStyle(ChatFormatting.BLUE),
                Component.literal("+1 Armor Toughness").withStyle(ChatFormatting.BLUE),
                Component.literal("+2 Attack Damage").withStyle(ChatFormatting.BLUE),
                Component.literal("+0.5 Attack Knockback").withStyle(ChatFormatting.BLUE),
                Component.literal("+15% Knockback Resistance").withStyle(ChatFormatting.BLUE),
                Component.literal("-5% Attack Speed").withStyle(ChatFormatting.RED)
        ));
    });

    public static final RegistryObject<Item> OGRE_STOMACH = ITEMS.register("ogre_stomach",
            () -> new CurioItem(new Item.Properties().stacksTo(1),
                    ImmutableMultimap.of(Attributes.MAX_HEALTH,
                            modifier("ogre_stomach_health", 4.0, AttributeModifier.Operation.ADDITION)),
                    curioTooltip("Charm",
                            Component.literal("+4 Max Health").withStyle(ChatFormatting.BLUE),
                            Component.literal("Food is eaten instantly").withStyle(ChatFormatting.BLUE),
                            Component.literal("Absorption foods are eaten at normal speed").withStyle(ChatFormatting.GOLD),
                            Component.literal("Can eat even while completely full").withStyle(ChatFormatting.BLUE),
                            Component.literal("Saturated natural healing is 25% faster").withStyle(ChatFormatting.BLUE),
                            Component.literal("Excess hunger and saturation are not stored").withStyle(ChatFormatting.DARK_GRAY)
                    )));

    private static AttributeModifier modifier(String name, double amount, AttributeModifier.Operation operation) {
        return ItemAttributes.modifier(name, amount, operation);
    }

    public static final RegistryObject<OgreKingsClubItem> OGRE_KINGS_CLUB = ITEMS.register("ogre_kings_club",
            () -> new OgreKingsClubItem(new Item.Properties().durability(1600)));
    static {
        // Total held damage 12: player base 1 plus this 11-point modifier, and a vanilla attack speed
        // of 0.9 from the player's base 4.0.
        ItemAttributes.mainHand(OGRE_KINGS_CLUB, 11.0, -3.1);
    }

    // -------------------------------------------------------------------------
    // Spawn Eggs
    // -------------------------------------------------------------------------

    /** Dark mossy-green body, muddy brown spots. */
    public static final RegistryObject<ForgeSpawnEggItem> OGRE_GRUNT_SPAWN_EGG = ITEMS.register(
            "ogre_grunt_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OGRE_GRUNT, 0x3d5a1e, 0x8b6914,
                    new Item.Properties()));

    /** Mossy-green hide with an iron-gray accent for the captain's armor. */
    public static final RegistryObject<ForgeSpawnEggItem> OGRE_GRUNT_CAPTAIN_SPAWN_EGG = ITEMS.register(
            "ogre_grunt_captain_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OGRE_GRUNT_CAPTAIN, 0x3d5a1e, 0xa8adb0,
                    new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> OGRE_MAGE_SPAWN_EGG = ITEMS.register(
            "ogre_mage_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OGRE_MAGE, 0x5e3b2c, 0x6f8d5a,
                    new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> OGRE_BRUTE_SPAWN_EGG = ITEMS.register(
            "ogre_brute_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OGRE_BRUTE, 0x425a2b, 0xb3b7aa,
                    new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> OGRE_TRAPPER_SPAWN_EGG = ITEMS.register(
            "ogre_trapper_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OGRE_TRAPPER, 0x3f4a2a, 0x7a5b3a,
                    new Item.Properties()));

    public static final RegistryObject<BogjawTrapItem> BOGJAW_TRAP = ITEMS.register("bogjaw_trap",
            () -> new BogjawTrapItem(new Item.Properties().stacksTo(16)));

    // The knife the Trapper draws when hurt. No recipe, no tab: the only way to get one is from him, and it comes worn.
    public static final RegistryObject<Item> TRAPPER_KNIFE = ITEMS.register("trapper_knife",
            () -> new Item(new Item.Properties().durability(180)));
    static {
        ItemAttributes.mainHand(TRAPPER_KNIFE, 5.0, -1.8);
    }

    public static final RegistryObject<ForgeSpawnEggItem> OGRE_ARCHER_SPAWN_EGG = ITEMS.register(
            "ogre_archer_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OGRE_ARCHER, 0x4f642d, 0x2b2118,
                    new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> OGRE_GUARD_SPAWN_EGG = ITEMS.register(
            "ogre_guard_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OGRE_GUARD, 0x44552b, 0x8b793f,
                    new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> OGRE_MERCHANT_SPAWN_EGG = ITEMS.register(
            "ogre_merchant_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OGRE_MERCHANT, 0x58723c, 0xd6a51c,
                    new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> OGRE_LORD_SPAWN_EGG = ITEMS.register(
            "ogre_lord_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OGRE_LORD, 0x2b3a1e, 0xc9a227,
                    new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> BOG_TOAD_SPAWN_EGG = ITEMS.register(
            "bog_toad_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BOG_TOAD, 0x52683A, 0xB88954,
                    new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> PLAGUE_OGRE_SPAWN_EGG = ITEMS.register(
            "plague_ogre_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PLAGUE_OGRE, 0x33382E, 0xA6453D,
                    new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> MUDSNOUT_BOAR_SPAWN_EGG = ITEMS.register(
            "mudsnout_boar_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MUDSNOUT_BOAR, 0x0E0E10, 0x3A3A3F,
                    new Item.Properties()));

    public static final RegistryObject<BoarJockeySpawnEggItem> BOAR_JOCKEY_SPAWN_EGG = ITEMS.register(
            "boar_jockey_spawn_egg",
            () -> new BoarJockeySpawnEggItem(0x0E0E10, 0x3d5a1e, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> BOG_POLE_SPAWN_EGG = ITEMS.register(
            "bog_pole_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BOG_POLE, 0x52683A, 0x8FAF62,
                    new Item.Properties()));

    // The Ogre Lord's weapon — a two-handed greatsword, clearly the strongest weapon in the mod
    // and noticeably better than the Bog Iron tier across the board.
    public static final Tier KINGS_CLEAVER_TOOL = new ModTier(3, 2200, 9.0F, 6.0F, 15,
            () -> Ingredient.of(BOG_IRON_INGOT.get()));

    public static final RegistryObject<KingsCleaverItem> KINGS_CLEAVER = ITEMS.register("kings_cleaver",
            () -> new KingsCleaverItem(KINGS_CLEAVER_TOOL, new Item.Properties()));
    static {
        // Total held damage is 10: player base 1 plus this 9-point modifier. Heavy and slow, a
        // greatsword swing and not a quick poke.
        ItemAttributes.mainHand(KINGS_CLEAVER, 9.0, -3.0)
                .add(ForgeMod.BLOCK_REACH,
                        modifier("kings_cleaver_block_reach", 1.0, AttributeModifier.Operation.ADDITION));
    }

}
