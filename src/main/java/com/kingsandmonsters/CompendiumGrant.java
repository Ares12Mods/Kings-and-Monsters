package com.kingsandmonsters;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Gives each player the Compendium once, unless the config turns that off. This replaced an
 * advancement reward, which a config cannot switch off.
 */
public final class CompendiumGrant {
    private static final ResourceLocation LEGACY_ADVANCEMENT =
            new ResourceLocation(KingsAndMonsters.MODID, "grant_compendium");
    private static final ResourceLocation BOOK_TABLE = new ResourceLocation(KingsAndMonsters.MODID, "gameplay/grant_compendium");
    private static final String GIVEN_TAG = "KingsAndMonstersCompendiumGiven";

    private CompendiumGrant() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getPersistentData().getBoolean(GIVEN_TAG)) {
            return;
        }

        // Players from before this was a config already got the book from the old advancement
        // reward, which stays in the data pack only as a marker for them.
        Advancement legacy = player.server.getAdvancements().getAdvancement(LEGACY_ADVANCEMENT);
        if (legacy != null && player.getAdvancements().getOrStartProgress(legacy).isDone()) {
            player.getPersistentData().putBoolean(GIVEN_TAG, true);
            return;
        }
        if (!Config.GIVE_COMPENDIUM_ON_JOIN.get()) {
            return;
        }

        LootTable table = player.server.getLootData().getLootTable(BOOK_TABLE);
        LootParams params = new LootParams.Builder(player.serverLevel())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .create(LootContextParamSets.ADVANCEMENT_REWARD);
        for (ItemStack book : table.getRandomItems(params)) {
            if (!player.getInventory().add(book)) {
                player.drop(book, false);
            }
        }
        player.getPersistentData().putBoolean(GIVEN_TAG, true);
    }
}
