package com.kingsandmonsters;

import com.kingsandmonsters.loot.CachedFortMapFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModLootFunctions {
    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, KingsAndMonsters.MODID);

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<CachedFortMapFunction>>
            CACHED_FORT_MAP = LOOT_FUNCTIONS.register("cached_fort_map",
            () -> new LootItemFunctionType<>(CachedFortMapFunction.CODEC));

    private ModLootFunctions() {
    }
}
