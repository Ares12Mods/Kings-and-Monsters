package com.kingsandmonsters;

import com.kingsandmonsters.loot.AddTableModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.ForgeRegistries;
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

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    KingsAndMonsters.MODID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>>
            ADD_TABLE = LOOT_MODIFIERS.register("add_table", AddTableModifier.CODEC);

    private ModLootFunctions() {
    }
}
