package com.kingsandmonsters.client;

import com.kingsandmonsters.enchantment.ModEnchantments;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.registries.ForgeRegistries;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ClientConfig;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

/** Adds concise mechanics and a distinct bog-iron presentation to ogre relic tooltips. */
public final class ClientTooltipEvents {
    private static final ResourceLocation OGRE_FONT =
            new ResourceLocation(KingsAndMonsters.MODID, "ogre");

    // Bog Iron equipment palette: dark bog + moss + mud + tarnished iron, deliberately less
    // saturated and less "magical" than the Ogre artifact palette below. Reuses the same
    // renderer/architecture as every other tier — only these color constants are new.
    private static final int BOG_IRON_EQUIPMENT_NAME_COLOR = 0x9C8A56; // tarnished brass / muted gold
    private static final int BOG_IRON_EQUIPMENT_GLYPH_COLOR = 0x5C6E43; // dull moss
    // Same dark green used elsewhere for weapon/tool stat lines (e.g. the vanilla "total attack"
    // color already seen on the Iron Maw/Ogre King's Club) — kept uniform with other items rather
    // than a Bog-Iron-specific teal, per explicit feedback that the teal read as washed-out gray.
    private static final int BOG_IRON_EQUIPMENT_STAT_COLOR = 0x00AA00;
    private static final int BOG_IRON_EQUIPMENT_FLAVOR_COLOR = 0x3A5F35; // moss/fern green, darker than relic flavor
    private static final int BOG_TOAD_NAME_COLOR = 0x9AAA78;
    private static final int BOG_TOAD_GLYPH_COLOR = 0x61734A;
    private static final int BOG_TOAD_ABILITY_COLOR = 0x91A66D;
    private static final int BOG_TOAD_DETAIL_COLOR = 0x78866A;
    // Warm campfire and broth colors shared by every food item, regardless of its source faction.
    private static final int FOOD_NAME_COLOR = 0xE2B86F;
    private static final int FOOD_GLYPH_COLOR = 0xA85F2A;
    private static final int FOOD_ABILITY_COLOR = 0xD5A85C;
    private static final int FOOD_DETAIL_COLOR = 0xC7B79B;
    // Kept in sync with the v2.0 Goblin palette so this item reads as a deliberate teaser.
    private static final int GOBLIN_NAME_COLOR = 0xA7A174;
    private static final int GOBLIN_GLYPH_COLOR = 0x657044;
    private static final int GOBLIN_ABILITY_COLOR = 0x8F9860;
    private static final int GOBLIN_DETAIL_COLOR = 0xAAA58B;
    private static final int BLACKHIDE_NAME_COLOR = 0xD8C7C7;
    private static final int BLACKHIDE_GLYPH_COLOR = 0x73131A;
    private static final int BLACKHIDE_ABILITY_COLOR = 0xC7474F;
    private static final int BLACKHIDE_DETAIL_COLOR = 0xB9A2A2;
    private static final int BLACKHIDE_FLAVOR_COLOR = 0x7F3035;

    private ClientTooltipEvents() {}

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!ClientConfig.showCustomTooltips()) return;
        boolean ogreTooltip = isOgreRelic(event.getItemStack());
        boolean bogIronEquipment = isBogIronEquipment(event.getItemStack()) || isBogfangBlade(event.getItemStack());
        boolean boghideArmor = isBoghideArmor(event.getItemStack());
        boolean blackhideArmor = isBlackhideArmor(event.getItemStack());
        boolean goblinContent = isGoblinContent(event.getItemStack());
        boolean food = isFood(event.getItemStack());
        boolean addedHeader = false;

        addBogIronMaterialLore(event);
        addMerchantLore(event);
        addBogWeaponAbilities(event);
        addNewOgreItemAbilities(event);
        addFoodAbilities(event);
        addSurvivalItemLore(event);
        addBlackhideArmorStat(event);

        if (event.getItemStack().is(ModItems.BONE_CLUB.get())) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bone_club.splintering")
                    .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bone_club.pierce")
                    .withStyle(ChatFormatting.BLUE));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bone_club.daze")
                    .withStyle(ChatFormatting.BLUE));
        }

        if (event.getItemStack().is(ModItems.HUNTERS_SPEAR.get())) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.when_used")
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(CombatDescriptionComponents.huntersSpearBleed()
                    .copy().withStyle(ChatFormatting.BLUE));
            event.getToolTip().add(CombatDescriptionComponents.huntersSpearCripple()
                    .copy().withStyle(ChatFormatting.BLUE));
            addedHeader = true;
        }

        var enchantments = EnchantmentHelper.getEnchantments(event.getItemStack());
        for (var enchantment : enchantments.keySet()) {
            Component description = CombatDescriptionComponents.enchantment(enchantment, enchantments.get(enchantment));
            if (description != null) {
                if (!addedHeader) {
                    event.getToolTip().add(Component.empty());
                    event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.when_used")
                            .withStyle(ChatFormatting.GRAY));
                    addedHeader = true;
                }
                event.getToolTip().add(description.copy().withStyle(ChatFormatting.GRAY));
                event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.not_stackable")
                        .withStyle(ChatFormatting.BLUE));
            }
        }

        if (event.getItemStack().getItem() instanceof PotionItem
                || event.getItemStack().getItem() instanceof TippedArrowItem) {
            for (var effect : PotionUtils.getMobEffects(event.getItemStack())) {
                Component description = CombatDescriptionComponents.effect(effect.getEffect());
                if (description != null) {
                    if (!addedHeader) {
                        event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.header")
                                .withStyle(ChatFormatting.DARK_GREEN));
                        addedHeader = true;
                    }
                    event.getToolTip().add(description.copy().withStyle(ChatFormatting.GRAY));
                }
            }
        }

        if (blackhideArmor) {
            removeRedundantAdvancedDetails(event);
            styleBlackhideTooltip(event);
        } else if (goblinContent) {
            removeRedundantAdvancedDetails(event);
            styleGoblinTooltip(event);
        } else if (food) {
            removeRedundantAdvancedDetails(event);
            styleFoodTooltip(event);
        } else if (isMerchantWare(event.getItemStack())) {
            removeRedundantAdvancedDetails(event);
            styleMerchantTooltip(event);
        } else if (bogIronEquipment) {
            removeRedundantAdvancedDetails(event);
            styleBogIronEquipmentTooltip(event);
        } else if (boghideArmor) {
            removeRedundantAdvancedDetails(event);
            styleBogToadTooltip(event);
        } else if (ogreTooltip) {
            removeRedundantAdvancedDetails(event);
            styleOgreTooltip(event);
        }
    }

    private static void addMerchantLore(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItems.OGRE_MERCHANT_BACKPACK.get())) {
            event.getToolTip().add(Component.literal("A merchant's hoard, carried on your back.")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }

    /**
     * What the swamp blades do on a hit, in the same layout as the Bog Iron armor's set bonus: a
     * gold title, the effect, then the cooldown. The numbers match BogIronEquipmentEvents and
     * BogToadContentEvents.
     */
    private static void addBogWeaponAbilities(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItems.BOG_IRON_SWORD.get())) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bog_iron_sword.venom")
                    .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bog_iron_sword.venom.effect")
                    .withStyle(ChatFormatting.BLUE));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bog_iron_sword.venom.cooldown")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else if (stack.is(ModItems.BOGFANG_BLADE.get())) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bogfang_blade.rot")
                    .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bogfang_blade.rot.effect")
                    .withStyle(ChatFormatting.BLUE));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bogfang_blade.rupture")
                    .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bogfang_blade.rupture.effect")
                    .withStyle(ChatFormatting.BLUE));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bogfang_blade.rupture.cooldown")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** A bog iron axe the King has enchanted carries Timber; any item with the enchantment counts. */
    private static boolean hasTimber(ItemStack stack) {
        return EnchantmentHelper.getEnchantments(stack).keySet().stream()
                .anyMatch(enchantment -> enchantment == ModEnchantments.TIMBER.get());
    }

    private static void addNewOgreItemAbilities(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItems.BOGSTEEL_CHESTPLATE.get())) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bogsteel_chestplate.glancing")
                    .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bogsteel_chestplate.glancing.effect")
                    .withStyle(ChatFormatting.BLUE));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.bogsteel_chestplate.glancing.cooldown")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else if (stack.is(ModItems.OGRE_LOINCLOTH.get())) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.ogre_loincloth.might")
                    .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.ogre_loincloth.effect")
                    .withStyle(ChatFormatting.BLUE));
        } else if (hasTimber(stack)) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.timber.title")
                    .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.timber.effect")
                    .withStyle(ChatFormatting.BLUE));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.timber.sneak")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else if (stack.is(ModItems.SKINNING_KNIFE.get())) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.skinning_knife.hamstring")
                    .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.skinning_knife.effect")
                    .withStyle(ChatFormatting.BLUE));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.skinning_knife.cooldown")
                    .withStyle(ChatFormatting.DARK_GRAY));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.skinning_knife.fresh")
                    .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.skinning_knife.fresh.effect")
                    .withStyle(ChatFormatting.BLUE));
        } else if (stack.is(ModItems.BOAR_CALLING_HORN.get())) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.when_used")
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.boar_calling_horn.recall")
                    .withStyle(ChatFormatting.BLUE));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.boar_calling_horn.cooldown")
                    .withStyle(ChatFormatting.DARK_GRAY));
            event.getToolTip().add(Component.translatable("tooltip.kingsandmonsters.boar_calling_horn.repair")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void addFoodAbilities(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItems.TOAD_STEW.get())) {
            addFoodEffects(event, "tooltip.kingsandmonsters.toad_stew.effects",
                    "tooltip.kingsandmonsters.toad_stew.regeneration",
                    "tooltip.kingsandmonsters.toad_stew.resistance",
                    "tooltip.kingsandmonsters.toad_stew.ogres_bane");
        } else if (stack.is(ModItems.IRONBELLY_STEW.get())) {
            addFoodEffects(event, "tooltip.kingsandmonsters.ironbelly_stew.effects",
                    "tooltip.kingsandmonsters.ironbelly_stew.cleanse",
                    "tooltip.kingsandmonsters.ironbelly_stew.poison_resistance",
                    "tooltip.kingsandmonsters.ironbelly_stew.resistance",
                    "tooltip.kingsandmonsters.ironbelly_stew.absorption");
        } else if (stack.is(ModBlocks.HAUNCH_PLATTER_ITEM.get())) {
            addFoodEffects(event, "tooltip.kingsandmonsters.haunch_platter.feast",
                    "tooltip.kingsandmonsters.haunch_platter.servings",
                    "tooltip.kingsandmonsters.haunch_platter.serve");
        } else if (stack.is(ModItems.HAUNCH_PLATTER_SERVING.get())) {
            addFoodEffects(event, "tooltip.kingsandmonsters.haunch_platter_serving.effects",
                    "tooltip.kingsandmonsters.haunch_platter_serving.regeneration",
                    "tooltip.kingsandmonsters.haunch_platter_serving.resistance");
        } else if (stack.is(ModItems.BONE_BROTH.get())) {
            addFoodEffects(event, "tooltip.kingsandmonsters.bone_broth.hearty",
                    "tooltip.kingsandmonsters.bone_broth.effect");
        }
    }

    private static void addFoodEffects(ItemTooltipEvent event, String heading, String... effects) {
        event.getToolTip().add(Component.empty());
        event.getToolTip().add(Component.translatable(heading).withStyle(ChatFormatting.GOLD));
        for (String effect : effects) {
            event.getToolTip().add(Component.translatable(effect).withStyle(ChatFormatting.BLUE));
        }
    }

    private static void addSurvivalItemLore(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String lore = null;

        if (stack.is(ModItems.OGREBLOOD_TOTEM.get())) lore = "An Ogre Mage's crude answer to the Totem of Undying.";
        else if (stack.is(ModItems.BOG_EYE_CHARM.get())) lore = "A glass eye that wriggles in your pocket whenever there's unopened loot nearby. It's nosy, and it's usually right.";
        else if (stack.is(ModItems.WARCALLER_BELL.get())) lore = "Grunt Captains ring it to announce the camp has chosen lunch. You.";
        else if (stack.is(ModItems.RATTLEBONE_RING.get())) lore = "Ogre Mages bind finger bones into rings that celebrate every death.";
        else if (stack.is(ModItems.OGRE_TOOTH_NECKLACE.get())) lore = "Grunts wear their trophies where every rival can count them.";
        else if (stack.is(ModItems.BRUTE_HEART.get())) lore = "It still beats with the stubborn fury of its former owner.";
        else if (stack.is(ModItems.BOG_IRON_BAND.get())) lore = "Crude bog iron, hammered into a ward against swamp venom.";
        else if (stack.is(ModItems.BOGFUME_CHARM.get())) lore = "An Ogre Mage's bottled marsh spite seeps from every crack.";
        else if (stack.is(ModItems.BUCKLER.get())) lore = "Ogre Archers prize a shield small enough to never spoil their aim.";
        else if (stack.is(ModItems.PADDED_EARMUFFS.get())) lore = "Stuffed thick enough to make even an ogre's roar sound far away.";
        else if (stack.is(ModItems.HUNTERS_SPEAR.get())) lore = "The point is rude. The barbs are ruder.";
        else if (stack.is(ModItems.SKINNING_KNIFE.get())) lore = "Leaves its target bleeding and sluggish. Great for hunting, terrible manners.";
        else if (stack.is(ModItems.BOAR_CALLING_HORN.get())) lore = "Toot it and your boar comes running. Mostly for the acorns.";
        else if (stack.is(ModItems.ACORN.get())) lore = "A tiny feast, if you are a boar—or very, very hungry.";
        else if (stack.is(ModItems.TOAD_EYE.get())) lore = "Don't look at it. It blinks when you do.";
        else if (stack.is(ModItems.BOG_TOAD_SKIN.get())) lore = "Still damp. Somehow, always still damp.";
        else if (stack.is(ModItems.BOGHIDE.get())) lore = "Tough swamp-hide with a stubborn memory of its shape.";
        else if (stack.is(ModItems.BOG_TOAD_VENOM.get())) lore = "The stopper looks secure. It probably is.";
        else if (stack.is(ModItems.BOGFANG.get())) lore = "A toad fang as long as a dagger. Its owner stopped smiling about it.";
        else if (stack.is(ModItems.BLACKHIDE_LEATHER.get())) lore = "Thick, smoky leather that smells faintly of wet boar.";
        else if (stack.is(ModItems.BLACKHIDE_HELMET.get())) lore = "Made for charging headfirst into problems. The tusks are encouragement.";
        else if (stack.is(ModItems.BLACKHIDE_CHESTPLATE.get())) lore = "Thick enough to make a mudsnout proud—and twice as stubborn.";
        else if (stack.is(ModItems.BLACKHIDE_LEGGINGS.get())) lore = "Built for short tempers and very long charges.";
        else if (stack.is(ModItems.BLACKHIDE_BOOTS.get())) lore = "They always seem to be looking for a running start.";
        else if (stack.is(ModItems.MUDSNOUT_BONE.get())) lore = "A tremendous bone from an animal with tremendous opinions.";
        else if (stack.is(ModItems.MUDSNOUT_TUSK.get())) lore = "Polished smooth by roots, rocks, and poor decisions.";
        else if (stack.is(ModItems.RAW_TOAD_LEG.get())) lore = "Fresh from the bog, and best kept far from an open wound.";
        else if (stack.is(ModItems.COOKED_TOAD_LEG.get())) lore = "Cooking fixes the bog. Mostly.";
        else if (stack.is(ModItems.TOAD_STEW.get())) lore = "A bog-toad broth thick enough to put iron in an ogre's belly.";
        else if (stack.is(ModItems.RAW_MUDSNOUT_HAUNCH.get())) lore = "A heavy cut of wild boar, bone and all.";
        else if (stack.is(ModItems.ROASTED_MUDSNOUT_HAUNCH.get())) lore = "Meat on the inside, a lifetime of chewing on the bone left over.";
        else if (stack.is(ModItems.BONE_BROTH.get())) lore = "Boiled bones, healed bones. The circle of life, with salt.";
        else if (stack.is(ModBlocks.HAUNCH_PLATTER_ITEM.get())) lore = "A whole roast with an apple on the side. Whoever carves it takes the best bit.";
        else if (stack.is(ModItems.HAUNCH_PLATTER_SERVING.get())) lore = "Gone before you finish saying \"just a taste\".";
        else if (stack.is(ModItems.IRONBELLY_STEW.get())) lore = "Sits in your stomach like a boulder. Ogres call it a light snack.";
        else if (stack.is(ModItems.OGRE_HOOKBLADE.get())) lore = "A merchant's answer to anyone foolish enough to flee with his gold.";
        else if (stack.is(ModItems.KINGS_CLEAVER.get())) lore = "An old Ogre King taught the Maw to get hungry. It never learned to stop.";
        else if (stack.is(ModItems.OGRE_KINGS_CLUB.get())) lore = "The King's verdict, delivered with both hands.";
        else if (stack.is(ModItems.OGRE_KINGS_CROWN.get())) lore = "Among ogres, a crown is not inherited. It is survived.";
        else if (stack.is(ModItems.OGRE_BRUTE_BANNER.get())) lore = "Hung where the Brute does the arguing. With his fists.";
        else if (stack.is(ModItems.GRUNT_CAPTAIN_BANNER.get())) lore = "Proof that someone put a grunt in charge. Brave of them.";
        else if (stack.is(ModItems.OGRE_MAGE_BANNER.get())) lore = "Smells like a swamp in a bad mood.";
        else if (stack.is(ModItems.OGRE_KINGS_BANNER.get())) lore = "Tribute goes to the King. Helping yourself is not tribute.";
        else if (stack.is(ModItems.BRUTES_HELM.get())) lore = "Still dented from the last argument. Surprisingly comfy for something this angry.";
        else if (stack.is(ModItems.BOGSTEEL_CHESTPLATE.get())) lore = "Ogre-sized plate that politely bounces away blades meant for you.";
        else if (stack.is(ModItems.OGRE_LOINCLOTH.get())) lore = "Not much cloth, a whole lot of confidence.";
        else if (stack.is(ModItems.BRUTE_GAUNTLETS.get())) lore = "Bogsteel turns a brute's fists into something worth respecting.";
        else if (stack.is(ModItems.OGRE_STOMACH.get())) lore = "An ogre's appetite, preserved—and still impossibly impatient.";
        else if (stack.is(ModItems.BOG_IRON_SWORD.get())) lore = "An ogre would call it a toothpick. It still works.";
        else if (stack.is(ModItems.BOGFANG_BLADE.get())) lore = "A giant toad fang on a handle. Every cut leaves a little rot behind.";
        else if (stack.is(ModItems.BOG_IRON_PICKAXE.get())) lore = "Digs through stone at the pace of a swamp swallowing a boot.";
        else if (stack.is(ModItems.BOG_IRON_AXE.get())) lore = "Made for timber, though ogres rarely distinguish wood from bone.";
        else if (stack.is(ModItems.BOG_IRON_SHOVEL.get())) lore = "For mud, holes, and the occasional regret.";
        else if (stack.is(ModItems.BOG_IRON_HOE.get())) lore = "Even ogres know a fed camp begins with broken earth.";
        else if (stack.is(ModItems.BOG_IRON_HELMET.get())) lore = "Keeps the rain off your head, and the sword too.";
        else if (stack.is(ModItems.BOG_IRON_CHESTPLATE.get())) lore = "Copied from ogre armor. The ogres were flattered, then furious.";
        else if (stack.is(ModItems.BOG_IRON_LEGGINGS.get())) lore = "Made for wading through mud so you don't have to think about it.";
        else if (stack.is(ModItems.BOG_IRON_BOOTS.get())) lore = "Every step squelches. That's the iron talking.";
        else if (stack.is(ModBlocks.BOG_IRON_BLOCK_ITEM.get())) lore = "A lot of bog iron, packed tight and sulking.";
        else if (stack.is(ModBlocks.RAW_BOG_IRON_BLOCK_ITEM.get())) lore = "Dirt with ambitions.";
        else if (stack.is(ModBlocks.BOG_OAK_LOG_ITEM.get())) lore = "Soggy for centuries, stubborn forever.";

        if (lore != null) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.literal(lore)
                    .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        }
    }

    /** Places the per-piece bonus beside vanilla armor and toughness instead of in set-bonus text. */
    private static void addBlackhideArmorStat(ItemTooltipEvent event) {
        if (!isBlackhideArmor(event.getItemStack())) return;
        int insertion = event.getToolTip().size();
        for (int i = event.getToolTip().size() - 1; i >= 0; i--) {
            if (event.getToolTip().get(i).getStyle().isItalic()) {
                insertion = i > 0 && event.getToolTip().get(i - 1).getString().isEmpty() ? i - 1 : i;
                break;
            }
        }
        event.getToolTip().add(insertion,
                Component.translatable("tooltip.kingsandmonsters.blackhide_armor.deflect")
                        .withStyle(ChatFormatting.BLUE));
    }

    private static void addBogIronMaterialLore(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ModBlocks.BOG_IRON_ORE_ITEM.get())
                || stack.is(ModBlocks.DEEPSLATE_BOG_IRON_ORE_ITEM.get())) {
            addLore(event,
                    "Swamp magic has seeped into the vein, leaving its iron potent but impure.",
                    "Only the ogres seem able to draw clean Bogsteel from it.");
        } else if (stack.is(ModItems.RAW_BOG_IRON.get())) {
            addLore(event,
                    "Bog corruption clings to every muddy fragment.",
                    "The secret that cleanses it into Bogsteel remains closely guarded.");
        } else if (stack.is(ModItems.BOG_IRON_INGOT.get())) {
            addLore(event,
                    "Smelted into shape, though stagnant magic still stains the metal.",
                    "It lacks the bright purity of the Bogsteel armor worn by ogres.");
        } else if (stack.is(ModBlocks.RAW_BOG_IRON_BLOCK_ITEM.get())) {
            addLore(event,
                    "A dense cache of tainted ore with its bog magic concentrated.",
                    "No outsider has reproduced the ogres' method of cleansing it.");
        } else if (stack.is(ModBlocks.BOG_IRON_BLOCK_ITEM.get())) {
            addLore(event,
                    "Refined, yet traces of the bog's corruption remain within.",
                    "Ogre Bogsteel proves the metal can somehow be purified further.");
        }
    }

    private static void addLore(ItemTooltipEvent event, String firstLine, String secondLine) {
        event.getToolTip().add(Component.empty());
        event.getToolTip().add(Component.literal(firstLine)
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        event.getToolTip().add(Component.literal(secondLine)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private static void removeRedundantAdvancedDetails(ItemTooltipEvent event) {
        int itemTagsHeader = -1;
        for (int index = 0; index < event.getToolTip().size(); index++) {
            String text = event.getToolTip().get(index).getString().trim();
            if (text.startsWith("Item Tags")) {
                itemTagsHeader = index;
                break;
            }
        }
        if (itemTagsHeader >= 0) {
            event.getToolTip().subList(itemTagsHeader, event.getToolTip().size()).clear();
        }

        // Advanced attributes append their arithmetic to the useful final value on the same line,
        // for example "12 Attack Damage [1 + 11]". Keep the result and remove only that suffix.
        for (int index = 0; index < event.getToolTip().size(); index++) {
            Component line = event.getToolTip().get(index);
            String text = line.getString();
            String cleaned = text.replaceFirst("\\s+\\[[^\\]]+\\]$", "");
            if (!cleaned.equals(text)) {
                event.getToolTip().set(index, Component.literal(cleaned)
                        .withStyle(ChatFormatting.DARK_GREEN));
            }
        }
    }

    private static void styleOgreTooltip(ItemTooltipEvent event) {
        boolean royal = isMajorOgreRelic(event.getItemStack());
        boolean weapon = isOgreWeaponRelic(event.getItemStack());
        for (int i = 0; i < event.getToolTip().size(); i++) {
            Component line = event.getToolTip().get(i);
            event.getToolTip().set(i, line.copy().withStyle(style -> style.withFont(OGRE_FONT)));
        }
        if (!event.getToolTip().isEmpty()) {
            Component name = event.getToolTip().get(0);
            event.getToolTip().set(0, Component.literal("\u25C6 ")
                    .withStyle(royal ? ChatFormatting.DARK_PURPLE
                            : ChatFormatting.DARK_GREEN)
                    .append(name.copy().withStyle(style -> style
                            .withFont(OGRE_FONT)
                            .withColor(royal ? 0xFFD45A : weapon ? 0xB7C0BA : 0xC6A85B))));
        }
    }

    private static void styleMerchantTooltip(ItemTooltipEvent event) {
        for (int i = 0; i < event.getToolTip().size(); i++) {
            Component line = event.getToolTip().get(i);
            event.getToolTip().set(i, line.copy().withStyle(style -> style.withFont(OGRE_FONT)));
        }
        if (!event.getToolTip().isEmpty()) {
            Component name = event.getToolTip().get(0);
            event.getToolTip().set(0, Component.literal("\u2726 ")
                    .withStyle(style -> style.withFont(OGRE_FONT).withColor(0xB87516))
                    .append(name.copy().withStyle(style -> style
                            .withFont(OGRE_FONT)
                            .withColor(0xFFD45A))));
        }
    }

    private static void styleFoodTooltip(ItemTooltipEvent event) {
        int gold = ChatFormatting.GOLD.getColor();
        int blue = ChatFormatting.BLUE.getColor();
        int gray = ChatFormatting.GRAY.getColor();
        int darkGreen = ChatFormatting.DARK_GREEN.getColor();
        for (int i = 0; i < event.getToolTip().size(); i++) {
            Component line = event.getToolTip().get(i);
            var current = line.getStyle().getColor();
            Integer color = current == null ? null : current.getValue();
            event.getToolTip().set(i, line.copy().withStyle(style -> {
                style = style.withFont(OGRE_FONT);
                if (color != null && (color == gold || color == blue)) {
                    style = style.withColor(FOOD_ABILITY_COLOR);
                } else if (color != null && (color == gray || color == darkGreen)) {
                    style = style.withColor(FOOD_DETAIL_COLOR);
                }
                return style;
            }));
        }
        if (!event.getToolTip().isEmpty()) {
            Component name = event.getToolTip().get(0);
            event.getToolTip().set(0, Component.literal("◆ ")
                    .withStyle(style -> style.withFont(OGRE_FONT).withColor(FOOD_GLYPH_COLOR))
                    .append(name.copy().withStyle(style -> style
                            .withFont(OGRE_FONT)
                            .withColor(FOOD_NAME_COLOR))));
        }
    }

    private static void styleBogToadTooltip(ItemTooltipEvent event) {
        int gold = ChatFormatting.GOLD.getColor();
        int green = ChatFormatting.GREEN.getColor();
        int blue = ChatFormatting.BLUE.getColor();
        int gray = ChatFormatting.GRAY.getColor();
        for (int i = 0; i < event.getToolTip().size(); i++) {
            Component line = event.getToolTip().get(i);
            var current = line.getStyle().getColor();
            Integer color = current == null ? null : current.getValue();
            event.getToolTip().set(i, line.copy().withStyle(style -> {
                style = style.withFont(OGRE_FONT);
                if (color != null && (color == gold || color == green || color == blue)) {
                    style = style.withColor(BOG_TOAD_ABILITY_COLOR);
                } else if (color != null && color == gray) {
                    style = style.withColor(BOG_TOAD_DETAIL_COLOR);
                }
                return style;
            }));
        }
        if (!event.getToolTip().isEmpty()) {
            Component name = event.getToolTip().get(0);
            event.getToolTip().set(0, Component.literal("✦ ")
                    .withStyle(style -> style.withFont(OGRE_FONT).withColor(BOG_TOAD_GLYPH_COLOR))
                    .append(name.copy().withStyle(style -> style
                            .withFont(OGRE_FONT)
                            .withColor(BOG_TOAD_NAME_COLOR))));
        }
    }

    private static void styleGoblinTooltip(ItemTooltipEvent event) {
        int green = ChatFormatting.GREEN.getColor();
        int blue = ChatFormatting.BLUE.getColor();
        int gray = ChatFormatting.GRAY.getColor();
        for (int i = 0; i < event.getToolTip().size(); i++) {
            Component line = event.getToolTip().get(i);
            var current = line.getStyle().getColor();
            Integer color = current == null ? null : current.getValue();
            event.getToolTip().set(i, line.copy().withStyle(style -> {
                style = style.withFont(OGRE_FONT);
                if (color != null && (color == green || color == blue)) {
                    style = style.withColor(GOBLIN_ABILITY_COLOR);
                } else if (color != null && color == gray) {
                    style = style.withColor(GOBLIN_DETAIL_COLOR);
                }
                return style;
            }));
        }
        if (!event.getToolTip().isEmpty()) {
            Component name = event.getToolTip().get(0);
            event.getToolTip().set(0, Component.literal("◆ ")
                    .withStyle(style -> style.withFont(OGRE_FONT).withColor(GOBLIN_GLYPH_COLOR))
                    .append(name.copy().withStyle(style -> style
                            .withFont(OGRE_FONT)
                            .withColor(GOBLIN_NAME_COLOR))));
        }
    }

    /**
     * Dark, mossy, muddy presentation for the Bog Iron equipment set — reuses the exact same
     * renderer/font/spacing as {@link #styleOgreTooltip}, only the colors differ. Vanilla's
     * positive-attribute blue and this mod's flavor-lore dark green are recolored in place so
     * the swamp-forged palette reaches every line, not just the item name.
     */
    private static void styleBogIronEquipmentTooltip(ItemTooltipEvent event) {
        int vanillaStatColor = ChatFormatting.BLUE.getColor();
        // Vanilla renders both the "total" attack damage/speed line (e.g. sword/tool stats) and
        // this mod's italic flavor lore in the same DARK_GREEN — the italic flag is what tells
        // them apart, since only the flavor line is italic.
        int vanillaDarkGreen = ChatFormatting.DARK_GREEN.getColor();
        for (int i = 0; i < event.getToolTip().size(); i++) {
            Component line = event.getToolTip().get(i);
            var existingColor = line.getStyle().getColor();
            Integer color = existingColor != null ? existingColor.getValue() : null;
            boolean italic = line.getStyle().isItalic();
            event.getToolTip().set(i, line.copy().withStyle(style -> {
                style = style.withFont(OGRE_FONT);
                if (color != null && color == vanillaDarkGreen && italic) {
                    style = style.withColor(BOG_IRON_EQUIPMENT_FLAVOR_COLOR);
                } else if (color != null && (color == vanillaStatColor || color == vanillaDarkGreen)) {
                    style = style.withColor(BOG_IRON_EQUIPMENT_STAT_COLOR);
                }
                return style;
            }));
        }
        if (!event.getToolTip().isEmpty()) {
            Component name = event.getToolTip().get(0);
            event.getToolTip().set(0, Component.literal("❖ ")
                    .withStyle(style -> style.withFont(OGRE_FONT).withColor(BOG_IRON_EQUIPMENT_GLYPH_COLOR))
                    .append(name.copy().withStyle(style -> style
                            .withFont(OGRE_FONT)
                            .withColor(BOG_IRON_EQUIPMENT_NAME_COLOR))));
        }
    }

    private static void styleBlackhideTooltip(ItemTooltipEvent event) {
        int gold = ChatFormatting.GOLD.getColor();
        int blue = ChatFormatting.BLUE.getColor();
        int darkGreen = ChatFormatting.DARK_GREEN.getColor();
        int gray = ChatFormatting.GRAY.getColor();
        for (int i = 0; i < event.getToolTip().size(); i++) {
            Component line = event.getToolTip().get(i);
            var existingColor = line.getStyle().getColor();
            Integer color = existingColor == null ? null : existingColor.getValue();
            boolean italic = line.getStyle().isItalic();
            event.getToolTip().set(i, line.copy().withStyle(style -> {
                style = style.withFont(OGRE_FONT);
                if (color != null && color == darkGreen && italic) {
                    style = style.withColor(BLACKHIDE_FLAVOR_COLOR);
                } else if (color != null && color == gold) {
                    style = style.withColor(BLACKHIDE_ABILITY_COLOR);
                } else if (color != null && (color == blue || color == gray)) {
                    style = style.withColor(BLACKHIDE_DETAIL_COLOR);
                }
                return style;
            }));
        }
        if (!event.getToolTip().isEmpty()) {
            Component name = event.getToolTip().get(0);
            event.getToolTip().set(0, Component.literal("◆ ")
                    .withStyle(style -> style.withFont(OGRE_FONT).withColor(BLACKHIDE_GLYPH_COLOR))
                    .append(name.copy().withStyle(style -> style
                            .withFont(OGRE_FONT)
                            .withColor(BLACKHIDE_NAME_COLOR))));
        }
    }

    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        if (!ClientConfig.showCustomTooltips()) return;
        if (isBlackhideArmor(event.getItemStack())) {
            event.setBackgroundStart(0xF0080709);
            event.setBackgroundEnd(0xF018090C);
            event.setBorderStart(0xFF4B090E);
            event.setBorderEnd(0xFFB52A34);
            return;
        }
        if (isGoblinContent(event.getItemStack())) {
            event.setBackgroundStart(0xF012140C);
            event.setBackgroundEnd(0xF0201B10);
            event.setBorderStart(0xFF4E5A32);
            event.setBorderEnd(0xFF78613B);
            return;
        }
        if (isFood(event.getItemStack())) {
            event.setBackgroundStart(0xF01B1008);
            event.setBackgroundEnd(0xF02B1B0D);
            event.setBorderStart(0xFF8B4C22);
            event.setBorderEnd(0xFFE2B86F);
            return;
        }
        if (isMerchantWare(event.getItemStack())) {
            // Ogre greed: near-black coin leather with an aged amber-to-rich-gold rim.
            event.setBackgroundStart(0xF0181006);
            event.setBackgroundEnd(0xF02A1A08);
            event.setBorderStart(0xFF9A5A12);
            event.setBorderEnd(0xFFFFD45A);
            return;
        }
        if (isBogIronEquipment(event.getItemStack()) || isBogfangBlade(event.getItemStack())) {
            // Dark earth / bog-black background — noticeably darker and less saturated than the
            // Ogre relic tier below, so the King-gated gear reads as rugged swamp-forged
            // equipment rather than a magical trophy.
            event.setBackgroundStart(0xF0141A10);
            event.setBackgroundEnd(0xF01D170C);
            // Deep moss green fading into muddy olive/brown — no gold, unlike the relic tiers.
            event.setBorderStart(0xFF3C5227);
            event.setBorderEnd(0xFF6B5A2E);
            return;
        }
        if (isBoghideArmor(event.getItemStack())) {
            event.setBackgroundStart(0xF010160E);
            event.setBackgroundEnd(0xF01A170F);
            event.setBorderStart(0xFF39482B);
            event.setBorderEnd(0xFF718056);
            return;
        }
        if (!isOgreRelic(event.getItemStack())) {
            return;
        }
        if (isMajorOgreRelic(event.getItemStack())) {
            event.setBackgroundStart(0xF0181024);
            event.setBackgroundEnd(0xF0281638);
            event.setBorderStart(0xFF7D3FB2);
            event.setBorderEnd(0xFFFFC83D);
            return;
        }
        if (isOgreWeaponRelic(event.getItemStack())) {
            // Deep forest-black fading into bog green, edged with muted forged iron.
            event.setBackgroundStart(0xF0121915);
            event.setBackgroundEnd(0xF01A241C);
            event.setBorderStart(0xFF294B34);
            event.setBorderEnd(0xFF8E9992);
            return;
        }
        // Near-black bog iron with a subtle peat-brown gradient.
        event.setBackgroundStart(0xF01A2118);
        event.setBackgroundEnd(0xF0261B14);
        // Moss at the top fading into tarnished ogre gold.
        event.setBorderStart(0xFF557A32);
        event.setBorderEnd(0xFFC09A43);
    }

    private static boolean isMerchantWare(ItemStack stack) {
        return stack.is(ModItems.GOLDEN_OGRE_TOOTH.get())
                || stack.is(ModItems.OGREBLOOD_TOTEM.get())
                || stack.is(ModItems.OGRE_MERCHANT_BACKPACK.get())
                || stack.is(ModItems.KINGS_TRIBUTE_BUNDLE.get())
                || stack.is(ModItems.BOG_EYE_CHARM.get())
                || stack.is(ModItems.OGRE_HOOKBLADE.get())
                || stack.is(ModItems.RATTLEBONE_RING.get())
                || stack.is(ModItems.WARCALLER_BELL.get());
    }

    private static boolean isOgreRelic(ItemStack stack) {
        if (stack.is(ModItems.OGRE_TOOTH_NECKLACE.get())
                || stack.is(ModItems.BRUTE_HEART.get())
                || stack.is(ModItems.BOG_IRON_BAND.get())
                || stack.is(ModItems.BOGFUME_CHARM.get())
                || stack.is(ModItems.BUCKLER.get())
                || stack.is(ModItems.PADDED_EARMUFFS.get())
                || stack.is(ModItems.WARCALLER_BELL.get())
                || stack.is(ModItems.RATTLEBONE_RING.get())
                || stack.is(ModItems.HUNTERS_SPEAR.get())
                || stack.is(ModItems.SKINNING_KNIFE.get())
                || stack.is(ModItems.BOAR_CALLING_HORN.get())
                || stack.is(ModItems.BONE_CLUB.get())
                || stack.is(ModItems.OGRE_KINGS_CROWN.get())
                || stack.is(ModItems.KINGS_CLEAVER.get())
                || stack.is(ModItems.OGRE_KINGS_CLUB.get())
                || isKingsGear(stack)) {
            return true;
        }
        return EnchantmentHelper.getEnchantments(stack).keySet().stream()
                .map(ForgeRegistries.ENCHANTMENTS::getKey)
                .anyMatch(key -> key != null && KingsAndMonsters.MODID.equals(key.getNamespace()));
    }

    private static boolean isMajorOgreRelic(ItemStack stack) {
        return stack.is(ModItems.OGRE_TOOTH_NECKLACE.get())
                || stack.is(ModItems.BRUTE_HEART.get())
                || stack.is(ModItems.BOG_IRON_BAND.get())
                || stack.is(ModItems.BOGFUME_CHARM.get())
                || stack.is(ModItems.BUCKLER.get())
                || stack.is(ModItems.PADDED_EARMUFFS.get())
                || stack.is(ModItems.WARCALLER_BELL.get())
                || stack.is(ModItems.RATTLEBONE_RING.get())
                || stack.is(ModItems.HUNTERS_SPEAR.get())
                || stack.is(ModItems.KINGS_CLEAVER.get())
                || stack.is(ModItems.OGRE_KINGS_CROWN.get())
                || stack.is(ModItems.OGRE_KINGS_CLUB.get())
                || isKingsGear(stack);
    }

    /** Bogsteel armor, loincloth and stomach: gear only the Ogre King will part with. */
    private static boolean isKingsGear(ItemStack stack) {
        return stack.is(ModItems.BRUTES_HELM.get())
                || stack.is(ModItems.BOGSTEEL_CHESTPLATE.get())
                || stack.is(ModItems.OGRE_LOINCLOTH.get())
                || stack.is(ModItems.BRUTE_GAUNTLETS.get())
                || stack.is(ModItems.OGRE_STOMACH.get());
    }

    private static boolean isOgreWeaponRelic(ItemStack stack) {
        return stack.is(ModItems.KINGS_CLEAVER.get())
                || stack.is(ModItems.HUNTERS_SPEAR.get())
                || stack.is(ModItems.SKINNING_KNIFE.get());
    }

    private static boolean isGoblinContent(ItemStack stack) {
        return stack.is(ModItems.CANDLE_HELMET.get());
    }

    private static boolean isFood(ItemStack stack) {
        return stack.is(ModItems.RAW_TOAD_LEG.get())
                || stack.is(ModItems.COOKED_TOAD_LEG.get())
                || stack.is(ModItems.TOAD_STEW.get())
                || stack.is(ModItems.RAW_MUDSNOUT_HAUNCH.get())
                || stack.is(ModItems.ROASTED_MUDSNOUT_HAUNCH.get())
                || stack.is(ModItems.BONE_BROTH.get())
                || stack.is(ModItems.IRONBELLY_STEW.get())
                || stack.is(ModBlocks.HAUNCH_PLATTER_ITEM.get())
                || stack.is(ModItems.HAUNCH_PLATTER_SERVING.get());
    }

    private static boolean isBoghideArmor(ItemStack stack) {
        return stack.is(ModItems.BOGHIDE_HELMET.get())
                || stack.is(ModItems.BOGHIDE_CHESTPLATE.get())
                || stack.is(ModItems.BOGHIDE_LEGGINGS.get())
                || stack.is(ModItems.BOGHIDE_BOOTS.get());
    }

    private static boolean isBlackhideArmor(ItemStack stack) {
        return stack.is(ModItems.BLACKHIDE_HELMET.get())
                || stack.is(ModItems.BLACKHIDE_CHESTPLATE.get())
                || stack.is(ModItems.BLACKHIDE_LEGGINGS.get())
                || stack.is(ModItems.BLACKHIDE_BOOTS.get());
    }

    /**
     * The King-gated Bog Iron tool/armor set — deliberately its own presentation tier, distinct
     * from Ogre artifacts (e.g. the Bog Iron Band, which despite its name is an artifact/ring and
     * stays in {@link #isOgreRelic}). Matched by item identity, never by translation/registry name.
     */
    private static boolean isBogIronEquipment(ItemStack stack) {
        return stack.is(ModItems.BOG_IRON_SWORD.get())
                || stack.is(ModItems.BOG_IRON_PICKAXE.get())
                || stack.is(ModItems.BOG_IRON_AXE.get())
                || stack.is(ModItems.BOG_IRON_SHOVEL.get())
                || stack.is(ModItems.BOG_IRON_HOE.get())
                || stack.is(ModItems.BOG_IRON_HELMET.get())
                || stack.is(ModItems.BOG_IRON_CHESTPLATE.get())
                || stack.is(ModItems.BOG_IRON_LEGGINGS.get())
                || stack.is(ModItems.BOG_IRON_BOOTS.get());
    }

    /** The Bogfang Blade is dressed like the Bog Iron set: same swamp-forged palette. */
    private static boolean isBogfangBlade(ItemStack stack) {
        return stack.is(ModItems.BOGFANG_BLADE.get());
    }
}
