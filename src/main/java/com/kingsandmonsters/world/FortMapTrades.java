package com.kingsandmonsters.world;

import com.kingsandmonsters.Config;
import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

public final class FortMapTrades {
    public static final TagKey<Structure> FORT_MAP_DESTINATIONS = TagKey.create(
            net.minecraft.core.registries.Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(
                    KingsAndMonsters.MODID, "on_ogre_fort_maps"));

    private FortMapTrades() {
    }

    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.CARTOGRAPHER
                || !Config.CARTOGRAPHER_FORT_MAP_ENABLED.get()) {
            return;
        }

        int level = Config.CARTOGRAPHER_FORT_MAP_LEVEL.get();
        VillagerTrades.ItemListing trade = new VillagerTrades.TreasureMapForEmeralds(
                Config.CARTOGRAPHER_FORT_MAP_PRICE.get(),
                FORT_MAP_DESTINATIONS,
                "filled_map.kingsandmonsters.ogre_kings_fort",
                MapDecorationTypes.RED_X,
                1,
                30);
        event.getTrades().get(level).add(trade);
    }
}
