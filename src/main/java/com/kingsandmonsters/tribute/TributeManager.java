package com.kingsandmonsters.tribute;

import com.kingsandmonsters.Config;
import com.kingsandmonsters.api.KingsAndMonstersAPI;
import com.kingsandmonsters.api.event.ChiefDeathEvent;
import com.kingsandmonsters.api.event.PatrolSpawnEvent;
import com.kingsandmonsters.api.event.TributeChestLootedEvent;
import com.kingsandmonsters.api.patrol.PatrolTier;
import com.kingsandmonsters.api.tribute.ITributeData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public final class TributeManager {
    @Nullable
    private static MinecraftServer server;

    private static final int CHEST_LOOT_FAVOR_LOSS = 5;

    private TributeManager() {}

    public static void attach(MinecraftServer activeServer) {
        server = activeServer;
        KingsAndMonstersAPI.setTributeDataProvider((dimension, campOrigin) -> {
            ServerLevel level = activeServer.getLevel(dimension);
            return level == null ? null : getData(level).get(campOrigin);
        });
    }

    public static void detach() {
        server = null;
        PROVOKED_UNTIL.clear();
        TOLL_UNTIL.clear();
        KingsAndMonstersAPI.setTributeDataProvider(null);
    }

    public static ITributeData getOrCreate(ServerLevel level, BlockPos campOrigin) {
        return getData(level).getOrCreate(campOrigin);
    }

    public static Optional<ITributeData> get(Level level, BlockPos campOrigin) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }

        return Optional.ofNullable(getData(serverLevel).get(campOrigin));
    }

    public static boolean recordTributeChestLooted(Player player, BlockPos chestPos, BlockPos campOrigin) {
        return recordTributeChestLooted(player, chestPos, campOrigin, Config.TRIBUTE_DEFAULT_ANGER_DELTA.get());
    }

    public static boolean recordTributeChestLooted(Player player, BlockPos chestPos, BlockPos campOrigin, int angerDelta) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        TributeChestLootedEvent event = new TributeChestLootedEvent(player, chestPos, level.dimension(), angerDelta);
        NeoForge.EVENT_BUS.post(event);

        if (event.isCanceled()) {
            return false;
        }

        getOrCreate(level, campOrigin).incrementAnger(event.getAngerDelta());
        if (player instanceof ServerPlayer serverPlayer) {
            addFavor(level, serverPlayer, -CHEST_LOOT_FAVOR_LOSS);
            // Theft is the one thing the tribe never forgives on the spot: no peace, no paid passage.
            provoke(level, serverPlayer);
            com.kingsandmonsters.advancement.RoyalAdvancementEvents.awardPath(serverPlayer, "tribute/sticky_fingers");
        }
        return true;
    }

    public static void markChiefDead(LivingEntity chief, BlockPos campOrigin) {
        if (!(chief.level() instanceof ServerLevel level)) {
            return;
        }

        getOrCreate(level, campOrigin).setChiefAlive(false);
        NeoForge.EVENT_BUS.post(new ChiefDeathEvent(chief, campOrigin, level.dimension()));
    }

    public static Optional<PatrolTier> preparePatrol(ServerLevel level, BlockPos campOrigin) {
        ITributeData tributeData = getOrCreate(level, campOrigin);
        PatrolSpawnEvent event = new PatrolSpawnEvent(campOrigin, level.dimension(), tributeData.getCurrentPatrolTier());
        NeoForge.EVENT_BUS.post(event);

        if (event.isCanceled()) {
            return Optional.empty();
        }

        return Optional.of(event.getTier());
    }

    public static List<TributeSavedData.CampPatrolState> getPatrolStates(ServerLevel level) {
        return getData(level).getPatrolStates();
    }

    public static void registerOutpost(
            ServerLevel level,
            BlockPos campOrigin,
            OutpostCaptainType captainType,
            BoundingBox structureBounds) {
        getData(level).registerOutpost(campOrigin, captainType, structureBounds);
    }

    public static void registerEncounter(
            ServerLevel level,
            BlockPos campOrigin,
            OutpostProfile profile,
            OutpostCaptainType captainType,
            BoundingBox structureBounds,
            int groundY,
            @org.jetbrains.annotations.Nullable PitZone pit) {
        getData(level).registerEncounter(campOrigin, profile, captainType, structureBounds, groundY, pit);
    }

    public static void registerFort(ServerLevel level, BlockPos fortOrigin, BoundingBox bounds) {
        getData(level).registerFort(fortOrigin, bounds);
    }

    public static void setOutpostFortMapTarget(ServerLevel level, BlockPos campOrigin, BlockPos fortTarget) {
        getData(level).setOutpostFortMapTarget(campOrigin, fortTarget);
    }

    public static List<TributeSavedData.DiscoverySite> getDiscoverySites(ServerLevel level) {
        return getData(level).getDiscoverySites();
    }

    public static boolean markStructureDiscovered(ServerLevel level, java.util.UUID playerId, BlockPos origin) {
        return getData(level).markDiscovered(playerId, origin);
    }

    public static List<TributeSavedData.OutpostPopulationState> getOutpostPopulationStates(ServerLevel level) {
        return getData(level).getOutpostPopulationStates();
    }

    public static List<TributeSavedData.JockeyPopulationState> getJockeyPopulationStates(ServerLevel level) {
        return getData(level).getJockeyPopulationStates();
    }

    public static void updateJockeyPopulation(ServerLevel level, BlockPos origin,
                                               boolean initialized, long nextSpawnTime) {
        getData(level).updateJockeyPopulation(origin, initialized, nextSpawnTime);
    }

    public static void markPopulationInitialized(ServerLevel level, BlockPos campOrigin, long nextSpawnTime) {
        getData(level).markPopulationInitialized(campOrigin, nextSpawnTime);
    }

    public static void setNextResidentSpawnGameTime(ServerLevel level, BlockPos campOrigin, long gameTime) {
        getData(level).setNextResidentSpawnGameTime(campOrigin, gameTime);
    }

    public static void markOutpostCaptainKilled(ServerLevel level, BlockPos campOrigin, long respawnGameTime) {
        getData(level).markOutpostCaptainKilled(campOrigin, respawnGameTime);
    }

    public static void markOutpostCaptainAlive(ServerLevel level, BlockPos campOrigin) {
        getData(level).markOutpostCaptainAlive(campOrigin);
    }

    public static int getFactionAngerLevel(ServerLevel level) {
        return getData(level).getFactionAngerLevel();
    }

    public static void decayFactionAnger(ServerLevel level, int amount) {
        List<TributeSavedData.CampPatrolState> camps = getPatrolStates(level);
        if (camps.isEmpty() || amount <= 0) {
            return;
        }

        getData(level).incrementFactionAnger(-amount, camps.getFirst().campOrigin());
    }

    private static final long PROVOCATION_TICKS = 20L * 60 * 5;
    private static final int PROVOCATION_FAVOR_LOSS = 2;
    // Players who recently struck the tribe, and the game time their peace returns. Not saved: a
    // restart forgives, and the favor they lost stays lost.
    private static final java.util.Map<UUID, Long> PROVOKED_UNTIL = new java.util.HashMap<>();

    /** While provoked, neither the coin pass nor an appeased tribe protects this player. */
    public static boolean isProvoked(ServerLevel level, UUID playerId) {
        Long until = PROVOKED_UNTIL.get(playerId);
        if (until == null) {
            return false;
        }
        if (level.getGameTime() >= until) {
            PROVOKED_UNTIL.remove(playerId);
            return false;
        }
        return true;
    }

    /** Called when a player strikes a tribe ogre: the peace is off for a while, and it costs favor. */
    public static void provoke(ServerLevel level, ServerPlayer player) {
        boolean alreadyProvoked = isProvoked(level, player.getUUID());
        PROVOKED_UNTIL.put(player.getUUID(), level.getGameTime() + PROVOCATION_TICKS);
        TOLL_UNTIL.remove(player.getUUID());
        if (!alreadyProvoked) {
            addFavorQuietly(level, player, -PROVOCATION_FAVOR_LOSS);
        }
    }

    private static final long TOLL_PASS_TICKS = 20L * 60 * 10;
    // Players who paid a coin to an ogre, and the game time their safe passage runs out.
    private static final java.util.Map<UUID, Long> TOLL_UNTIL = new java.util.HashMap<>();

    public static boolean hasTollPass(ServerLevel level, UUID playerId) {
        Long until = TOLL_UNTIL.get(playerId);
        return until != null && level.getGameTime() < until;
    }

    public static void grantTollPass(ServerLevel level, UUID playerId) {
        TOLL_UNTIL.put(playerId, level.getGameTime() + TOLL_PASS_TICKS);
    }

    /** Tells a player their paid passage has run out; called on the same slow beat as the HUD sync. */
    public static void checkTollExpiry(ServerLevel level, ServerPlayer player) {
        Long until = TOLL_UNTIL.get(player.getUUID());
        if (until != null && level.getGameTime() >= until) {
            TOLL_UNTIL.remove(player.getUUID());
            player.displayClientMessage(
                    Component.literal("Your tribute has run out. The tribe will no longer stand aside.")
                            .withStyle(ChatFormatting.RED), true);
        }
    }

    public static int getFavor(ServerLevel level, UUID playerId) {
        return playerData(level).getFavor(playerId);
    }

    public static boolean isAppeased(ServerLevel level, UUID playerId) {
        return playerData(level).isAppeased(playerId);
    }

    /** Moves this player's standing with the tribe and tells them, on the action bar, how it landed. */
    public static void addFavor(ServerLevel level, ServerPlayer player, int delta) {
        changeFavor(level, player, delta, true);
    }

    /**
     * Favor lost to something the player is deliberately doing (striking or killing the tribe): no
     * action-bar line for every blow, only the message when it tips them over the appeased line.
     */
    public static void addFavorQuietly(ServerLevel level, ServerPlayer player, int delta) {
        changeFavor(level, player, delta, false);
    }

    private static void changeFavor(ServerLevel level, ServerPlayer player, int delta, boolean announceChange) {
        if (delta == 0) {
            return;
        }
        TributeSavedData data = playerData(level);
        UUID playerId = player.getUUID();
        boolean crossed = data.addFavor(playerId, delta);
        if (crossed && data.isAppeased(playerId)) {
            com.kingsandmonsters.advancement.RoyalAdvancementEvents.awardPath(player, "tribute/friend_of_the_tribe");
        }
        if (crossed) {
            player.displayClientMessage(data.isAppeased(playerId)
                    ? Component.literal("The Ogre Tribe is appeased.").withStyle(ChatFormatting.GOLD)
                    : Component.literal("The Ogre Tribe's patience has run out.").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!announceChange) {
            return;
        }
        int goal = data.isAppeased(playerId) ? TributeSavedData.MAX_FAVOR : Config.TRIBUTE_APPEASED_FAVOR.get();
        player.displayClientMessage(Component.literal("Ogre Favor " + (delta > 0 ? "+" : "") + delta
                + "  (" + data.getFavor(playerId) + " / " + goal + ")")
                .withStyle(delta > 0 ? ChatFormatting.GOLD : ChatFormatting.RED), true);
    }

    /** Favor a single Gold Coin is worth. */
    public static final int COIN_FAVOR = 5;

    /** Favor a sealed King's Tribute Bundle is worth to the King: six coins' worth. */
    public static final int BUNDLE_FAVOR = 30;

    /** Everything the King takes as tribute: every coin and every sealed bundle the player carries. */
    public static int offerTribute(ServerPlayer player) {
        int favor = 0;
        boolean offered = false;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            int worth;
            if (stack.is(com.kingsandmonsters.ModItems.GOLD_COIN.get())) {
                worth = COIN_FAVOR;
            } else if (stack.is(com.kingsandmonsters.ModItems.KINGS_TRIBUTE_BUNDLE.get())
                    && !com.kingsandmonsters.item.KingsTributeBagItem.isOpened(stack)) {
                worth = BUNDLE_FAVOR;
            } else {
                continue;
            }
            favor += worth * stack.getCount();
            offered = true;
            if (!player.getAbilities().instabuild) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        if (offered) {
            com.kingsandmonsters.advancement.RoyalAdvancementEvents.awardPath(player, "tribute/first_tribute");
            addFavor(player.serverLevel(), player, favor);
        }
        return favor;
    }

    /** Turns coins into favor: the stack held out, or everything the player is carrying. */
    public static int offerCoins(ServerPlayer player, @Nullable ItemStack held) {
        int count = 0;
        if (held != null) {
            count = held.getCount();
            if (!player.getAbilities().instabuild) {
                held.shrink(count);
            }
        } else {
            var inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.is(com.kingsandmonsters.ModItems.GOLD_COIN.get())) {
                    count += stack.getCount();
                    if (!player.getAbilities().instabuild) {
                        inventory.setItem(slot, ItemStack.EMPTY);
                    }
                }
            }
        }
        if (count > 0) {
            com.kingsandmonsters.advancement.RoyalAdvancementEvents.awardPath(player, "tribute/first_tribute");
            addFavor(player.serverLevel(), player, count * COIN_FAVOR);
        }
        return count;
    }

    public static void raiseFactionAnger(ServerLevel level, int amount) {
        List<TributeSavedData.CampPatrolState> camps = getPatrolStates(level);
        if (camps.isEmpty() || amount <= 0) {
            return;
        }
        getData(level).incrementFactionAnger(amount, camps.getFirst().campOrigin());
    }

    public static void setNextPatrolGameTime(ServerLevel level, BlockPos campOrigin, long gameTime) {
        getData(level).setNextPatrolGameTime(campOrigin, gameTime);
    }

    /** Arms (or refreshes) a player's single pending retaliation, eligible starting at gameTime. */
    public static void armPatrolRetaliation(ServerLevel level, UUID playerId, BlockPos campOrigin, long eligibleGameTime) {
        getData(level).armPatrolRetaliation(playerId, campOrigin, eligibleGameTime);
    }

    public static List<TributeSavedData.DuePatrolRetaliation> peekDuePatrolRetaliations(ServerLevel level, long gameTime) {
        return getData(level).peekDuePatrolRetaliations(gameTime);
    }

    public static boolean wasHuntedSince(ServerLevel level, UUID playerId, BlockPos pos, double radius, long since) {
        return getData(level).wasHuntedSince(playerId, pos, radius, since);
    }

    public static void recordPatrol(ServerLevel level, UUID playerId, BlockPos pos, long keepTicks) {
        getData(level).recordPatrol(playerId, pos, level.getGameTime(), keepTicks);
    }

    public static void clearPatrolRetaliation(ServerLevel level, UUID playerId) {
        getData(level).clearPatrolRetaliation(playerId);
    }

    public static Optional<BlockPos> findNearestCamp(ServerLevel level, BlockPos pos, int maxRadius) {
        return getData(level).findNearestCamp(pos, maxRadius);
    }

    public static Optional<BlockPos> findNearestFort(ServerLevel level, BlockPos pos) {
        return getData(level).findNearestFort(pos);
    }

    /** Every Fort that has already, successfully generated. */
    public static List<BlockPos> getAllConfirmedForts(ServerLevel level) {
        return getData(level).getAllConfirmedForts();
    }

    public static Optional<BlockPos> findOutpostFortMapTarget(ServerLevel level, BlockPos pos) {
        return getData(level).findOutpostFortMapTarget(pos);
    }

    public static boolean canSendPatrols(ServerLevel level, BlockPos campOrigin) {
        return getData(level).canSendPatrols(campOrigin);
    }

    public static boolean isAbandonedCamp(ServerLevel level, BlockPos campOrigin) {
        return getData(level).isAbandonedCamp(campOrigin);
    }

    public static Optional<BlockPos> findOutpostCampOrigin(ServerLevel level, BlockPos pos) {
        return getData(level).findOutpostCampOrigin(pos);
    }

    public static void setOutpostMapChestPosIfAbsent(ServerLevel level, BlockPos campOrigin, BlockPos chestPos) {
        getData(level).setOutpostMapChestPosIfAbsent(campOrigin, chestPos);
    }

    public static Optional<BlockPos> getOutpostMapChestPos(ServerLevel level, BlockPos campOrigin) {
        return getData(level).getOutpostMapChestPos(campOrigin);
    }

    public static Optional<BlockPos> getOutpostFortMapTarget(ServerLevel level, BlockPos campOrigin) {
        return getData(level).getOutpostFortMapTarget(campOrigin);
    }

    public static boolean isMapRewardFulfilled(ServerLevel level, BlockPos campOrigin) {
        return getData(level).isMapRewardFulfilled(campOrigin);
    }

    public static boolean markMapRewardFulfilled(ServerLevel level, BlockPos campOrigin) {
        return getData(level).markMapRewardFulfilled(campOrigin);
    }

    public static boolean isMapRewardFulfilledNear(ServerLevel level, BlockPos pos) {
        return getData(level).isMapRewardFulfilledNear(pos);
    }

    public static boolean markMapRewardFulfilledNear(ServerLevel level, BlockPos pos) {
        return getData(level).markMapRewardFulfilledNear(pos);
    }

    public static Optional<ServerLevel> getLevel(net.minecraft.resources.ResourceKey<Level> dimension) {
        return server == null ? Optional.empty() : Optional.ofNullable(server.getLevel(dimension));
    }

    /**
     * Where each player's own standing is kept: favor, their quest and quest lockouts. The camp data is
     * saved per dimension, but a player's standing has to follow them, or a quest taken from the King
     * would lose its kills in the Nether and the End.
     */
    static TributeSavedData playerData(ServerLevel level) {
        return getData(level.getServer().overworld());
    }

    static TributeSavedData getData(ServerLevel level) {
        SavedData.Factory<TributeSavedData> factory = new SavedData.Factory<>(
                () -> new TributeSavedData(level.dimension()),
                (tag, registries) -> TributeSavedData.load(level.dimension(), tag, registries)
        );
        return level.getDataStorage().computeIfAbsent(factory, TributeSavedData.FILE_ID);
    }
}
