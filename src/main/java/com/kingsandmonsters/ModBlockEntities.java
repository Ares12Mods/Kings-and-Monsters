package com.kingsandmonsters;

import com.kingsandmonsters.block.entity.CandleHelmetBlockEntity;
import com.kingsandmonsters.block.entity.HaunchPlatterBlockEntity;
import com.kingsandmonsters.block.entity.OgreKingsCrownBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KingsAndMonsters.MODID);

    public static final RegistryObject<BlockEntityType<OgreKingsCrownBlockEntity>> OGRE_KINGS_CROWN =
            BLOCK_ENTITY_TYPES.register("ogre_kings_crown", () ->
                    BlockEntityType.Builder.of(OgreKingsCrownBlockEntity::new, ModBlocks.OGRE_KINGS_CROWN_DISPLAY.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<CandleHelmetBlockEntity>> CANDLE_HELMET =
            BLOCK_ENTITY_TYPES.register("candle_helmet", () ->
                    BlockEntityType.Builder.of(CandleHelmetBlockEntity::new,
                            ModBlocks.CANDLE_HELMET_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<HaunchPlatterBlockEntity>> HAUNCH_PLATTER =
            BLOCK_ENTITY_TYPES.register("haunch_platter", () ->
                    BlockEntityType.Builder.of(HaunchPlatterBlockEntity::new,
                            ModBlocks.HAUNCH_PLATTER.get()).build(null));

    private ModBlockEntities() {
    }
}
