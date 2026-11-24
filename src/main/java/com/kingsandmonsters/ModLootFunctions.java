package com.kingsandmonsters;

import com.kingsandmonsters.loot.CachedFortMapFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModLootFunctions {
    public static final DeferredRegister<LootItemFunctionType> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, KingsAndMonsters.MODID);

    public static final RegistryObject<LootItemFunctionType>
            CACHED_FORT_MAP = LOOT_FUNCTIONS.register("cached_fort_map",
            () -> new LootItemFunctionType(new CachedFortMapFunction.Serializer()));

    public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    KingsAndMonsters.MODID);

    public static final RegistryObject<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>>
            ADD_TABLE = LOOT_MODIFIERS.register("add_table", com.kingsandmonsters.loot.AddTableModifier.CODEC);

    private ModLootFunctions() {
    }
}
