package com.kingsandmonsters.command;

import com.kingsandmonsters.api.patrol.PatrolTier;
import com.kingsandmonsters.api.tribute.ITributeData;
import com.kingsandmonsters.tribute.PatrolManager;
import com.kingsandmonsters.tribute.TributeManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator-only commands for quickly exercising the real v1.2.1 anger and patrol systems. */
public final class TestingCommands {
    private static final int WORLD_SEARCH_RADIUS = 30_000_000;

    private TestingCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("kingsandmonsters")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("anger")
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 30))
                                        .executes(context -> setAnger(
                                                context.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "value"))))))
                .then(Commands.literal("favor")
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(context -> setFavor(
                                                context.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "value"))))))
                .then(Commands.literal("boar")
                        .executes(context -> describeBoars(context.getSource().getPlayerOrException())))
                .then(Commands.literal("patrol")
                        .then(Commands.literal("spawn")
                                .executes(context -> spawnPatrol(
                                        context.getSource().getPlayerOrException(), false)))
                        .then(Commands.literal("retaliation")
                                .executes(context -> spawnPatrol(
                                        context.getSource().getPlayerOrException(), true)))));
    }

    /** Lists the nearest boars and why each would or would not attack this player. */
    private static int describeBoars(ServerPlayer player) {
        var boars = player.serverLevel().getEntitiesOfClass(
                com.kingsandmonsters.entity.MudsnoutBoar.class, player.getBoundingBox().inflate(32.0));
        boars.sort(java.util.Comparator.comparingDouble(boar -> boar.distanceToSqr(player)));
        if (boars.isEmpty()) {
            player.sendSystemMessage(Component.literal("No Mudsnout Boars within 32 blocks."));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Nearest boars (" + boars.size() + " within 32 blocks):"));
        for (var boar : boars.subList(0, Math.min(6, boars.size()))) {
            player.sendSystemMessage(Component.literal(" - " + boar.describeAggroFor(player)));
        }
        return boars.size();
    }

    private static int setFavor(ServerPlayer player, int favor) {
        ServerLevel level = player.serverLevel();
        TributeManager.addFavor(level, player, favor - TributeManager.getFavor(level, player.getUUID()));
        player.sendSystemMessage(Component.literal("Ogre Tribe favor set to " + favor + "/100 (anger now "
                + TributeManager.getFactionAngerLevel(level) + "/30)."));
        return 1;
    }

    private static int setAnger(ServerPlayer player, int anger) {
        ServerLevel level = player.serverLevel();
        var campOrigin = nearestCamp(level, player);
        if (campOrigin.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "No registered Ogre camp exists in this dimension."));
            return 0;
        }

        TributeManager.getOrCreate(level, campOrigin.get()).setAngerLevel(anger);
        player.sendSystemMessage(Component.literal(
                "Ogre faction anger set to " + anger + "/30 (" + angerTierName(anger) + ")."));
        return 1;
    }

    private static int spawnPatrol(ServerPlayer player, boolean tributeRetaliation) {
        ServerLevel level = player.serverLevel();
        var campOrigin = nearestCamp(level, player);
        if (campOrigin.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "No registered Ogre camp exists in this dimension."));
            return 0;
        }

        ITributeData tribute = TributeManager.getOrCreate(level, campOrigin.get());
        int anger = tribute.getAngerLevel();
        if (anger <= 0) {
            player.sendSystemMessage(Component.literal(
                    "Ogre faction anger is Appeased (0/30); no patrol should spawn."));
            return 0;
        }

        var preparedTier = TributeManager.preparePatrol(level, campOrigin.get());
        if (preparedTier.isEmpty()) {
            player.sendSystemMessage(Component.literal("Patrol spawning was cancelled by an event listener."));
            return 0;
        }

        PatrolTier tier = preparedTier.get();
        int spawned = tributeRetaliation
                ? PatrolManager.spawnTestingRetaliation(level, campOrigin.get(), player, tier)
                : PatrolManager.spawnTestingPatrol(level, campOrigin.get(), player, tier);
        player.sendSystemMessage(Component.literal(
                "Spawned " + spawned + (tributeRetaliation ? " retaliation" : " patrol")
                        + " Ogre(s) at " + anger + "/30 anger ("
                        + angerTierName(anger) + ", " + tier.name().toLowerCase() + " patrol)."));
        return spawned > 0 ? 1 : 0;
    }

    private static java.util.Optional<net.minecraft.core.BlockPos> nearestCamp(
            ServerLevel level, ServerPlayer player) {
        return TributeManager.findNearestCamp(level, player.blockPosition(), WORLD_SEARCH_RADIUS);
    }

    private static String angerTierName(int anger) {
        if (anger <= 0) return "Appeased";
        return switch (PatrolTier.fromAnger(anger)) {
            case SMALL -> "Low";
            case MEDIUM -> "Moderate";
            case LARGE -> "High";
        };
    }
}
