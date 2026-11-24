package com.kingsandmonsters;

import com.kingsandmonsters.block.OgreKingsCrownBlock;
import com.kingsandmonsters.block.BogToadEggBlock;
import com.kingsandmonsters.block.AcornBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, KingsAndMonsters.MODID);

    public static final RegistryObject<Block> BOG_IRON_ORE = BLOCKS.register("bog_iron_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .requiresCorrectToolForDrops()
                            .strength(3.0f, 3.0f)
                            .sound(SoundType.STONE), UniformInt.of(0, 2)));

    public static final RegistryObject<Block> DEEPSLATE_BOG_IRON_ORE = BLOCKS.register("deepslate_bog_iron_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DEEPSLATE)
                            .requiresCorrectToolForDrops()
                            .strength(4.5f, 3.0f)
                            .sound(SoundType.DEEPSLATE), UniformInt.of(0, 2)));

    public static final RegistryObject<Block> BOG_IRON_BLOCK = BLOCKS.register("bog_iron_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> RAW_BOG_IRON_BLOCK = BLOCKS.register("raw_bog_iron_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.STONE)));

    public static final RegistryObject<RotatedPillarBlock> BOG_OAK_LOG = BLOCKS.register("bog_oak_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                            .ignitedByLava()));

    public static final RegistryObject<BogToadEggBlock> BOG_TOAD_EGG = BLOCKS.register("bog_toad_egg",
            () -> new BogToadEggBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .instabreak()
                    .noCollission()
                    .noOcclusion()
                    .sound(SoundType.FROGSPAWN)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    public static final RegistryObject<AcornBlock> ACORN = BLOCKS.register("acorn",
            () -> new AcornBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .instabreak()
                    .noCollission()
                    .noOcclusion()
                    .sound(SoundType.WOOD)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    public static final RegistryObject<OgreKingsCrownBlock> OGRE_KINGS_CROWN_DISPLAY = BLOCKS.register(
            "ogre_kings_crown_display",
            () -> new OgreKingsCrownBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(0.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<com.kingsandmonsters.block.CandleHelmetBlock> CANDLE_HELMET_BLOCK = BLOCKS.register(
            "candle_helmet",
            () -> new com.kingsandmonsters.block.CandleHelmetBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5F)
                    .sound(SoundType.LANTERN)
                    .noOcclusion()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                    .lightLevel(state -> state.getValue(com.kingsandmonsters.block.CandleHelmetBlock.LIGHT))));

    public static final RegistryObject<com.kingsandmonsters.block.HaunchPlatterBlock> HAUNCH_PLATTER = BLOCKS.register(
            "haunch_platter",
            () -> new com.kingsandmonsters.block.HaunchPlatterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    public static final RegistryObject<BlockItem> BOG_IRON_ORE_ITEM =
            blockItem("bog_iron_ore", BOG_IRON_ORE);

    public static final RegistryObject<BlockItem> DEEPSLATE_BOG_IRON_ORE_ITEM =
            blockItem("deepslate_bog_iron_ore", DEEPSLATE_BOG_IRON_ORE);

    public static final RegistryObject<BlockItem> BOG_IRON_BLOCK_ITEM =
            blockItem("bog_iron_block", BOG_IRON_BLOCK);

    public static final RegistryObject<BlockItem> RAW_BOG_IRON_BLOCK_ITEM =
            blockItem("raw_bog_iron_block", RAW_BOG_IRON_BLOCK);

    public static final RegistryObject<BlockItem> BOG_OAK_LOG_ITEM =
            blockItem("bog_oak_log", BOG_OAK_LOG);

    public static final RegistryObject<BlockItem> BOG_TOAD_EGG_ITEM =
            blockItem("bog_toad_egg", BOG_TOAD_EGG);
    public static final RegistryObject<BlockItem> HAUNCH_PLATTER_ITEM = ModItems.ITEMS.register("haunch_platter",
            () -> new com.kingsandmonsters.item.HaunchPlatterBlockItem(HAUNCH_PLATTER.get(),
                    new net.minecraft.world.item.Item.Properties().stacksTo(1)
                            // One serving's values, for AppleSkin and other food-information mods.
                            .food(new net.minecraft.world.food.FoodProperties.Builder()
                                    .nutrition(10).saturationMod(0.9F)
                                    .effect(new net.minecraft.world.effect.MobEffectInstance(
                                            net.minecraft.world.effect.MobEffects.REGENERATION, 160), 1.0F)
                                    .effect(new net.minecraft.world.effect.MobEffectInstance(
                                            net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 900), 1.0F)
                                    .build())));


    private static RegistryObject<BlockItem> blockItem(String name, RegistryObject<? extends Block> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new net.minecraft.world.item.Item.Properties()));
    }
}
