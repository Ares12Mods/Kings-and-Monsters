package com.kingsandmonsters.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.kingsandmonsters.entity.MudsnoutBoar;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import com.kingsandmonsters.event.ServerTickEvent;

import java.util.HashMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Remembers the last loaded location of a player's tame Mudsnout for long-distance horn recalls. */
public final class MudsnoutRecallTracker {
    private static final String UUID_TAG = "KingsAndMonstersMudsnoutRecallUuid";
    private static final String DIMENSION_TAG = "KingsAndMonstersMudsnoutRecallDimension";
    private static final String X_TAG = "KingsAndMonstersMudsnoutRecallX";
    private static final String Y_TAG = "KingsAndMonstersMudsnoutRecallY";
    private static final String Z_TAG = "KingsAndMonstersMudsnoutRecallZ";
    private static final int LOAD_TIMEOUT_TICKS = 200;
    private static final int TICKET_RADIUS = 2;
    private static final TicketType<UUID> RECALL_TICKET = TicketType.create(
            "kingsandmonsters_mudsnout_recall", Comparator.comparing(UUID::toString), LOAD_TIMEOUT_TICKS);
    private static final Map<UUID, PendingRecall> PENDING = new HashMap<>();

    private MudsnoutRecallTracker() {}

    public static void remember(MudsnoutBoar boar, ServerPlayer owner) {
        var data = owner.getPersistentData();
        data.putUUID(UUID_TAG, boar.getUUID());
        data.putString(DIMENSION_TAG, boar.level().dimension().location().toString());
        BlockPos pos = boar.blockPosition();
        data.putInt(X_TAG, pos.getX());
        data.putInt(Y_TAG, pos.getY());
        data.putInt(Z_TAG, pos.getZ());
    }

    public static MudsnoutBoar loadRemembered(ServerPlayer owner) {
        var data = owner.getPersistentData();
        if (!data.hasUUID(UUID_TAG)
                || !data.getString(DIMENSION_TAG).equals(owner.serverLevel().dimension().location().toString())) {
            return null;
        }

        BlockPos pos = new BlockPos(data.getInt(X_TAG), data.getInt(Y_TAG), data.getInt(Z_TAG));
        // A synchronous FULL chunk request restores persistent entities before the UUID lookup.
        owner.serverLevel().getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        Entity entity = owner.serverLevel().getEntity(data.getUUID(UUID_TAG));
        if (entity instanceof MudsnoutBoar boar && boar.isAlive() && boar.isTamed()
                && boar.isOwner(owner)) {
            return boar;
        }
        return null;
    }

    /**
     * Loads an unloaded Mudsnout over subsequent server ticks. Entity sections can arrive after the
     * synchronous chunk request, so an immediate UUID lookup is not reliable at long range.
     */
    public static boolean queueRememberedRecall(ServerPlayer owner, BlockPos destination) {
        var data = owner.getPersistentData();
        if (!data.hasUUID(UUID_TAG)
                || !data.getString(DIMENSION_TAG).equals(owner.serverLevel().dimension().location().toString())) {
            return false;
        }
        BlockPos source = new BlockPos(data.getInt(X_TAG), data.getInt(Y_TAG), data.getInt(Z_TAG));
        PendingRecall pending = new PendingRecall(owner.getUUID(), data.getUUID(UUID_TAG),
                owner.serverLevel().dimension(), source, destination.immutable(), LOAD_TIMEOUT_TICKS);
        PendingRecall previous = PENDING.put(owner.getUUID(), pending);
        if (previous != null) {
            var previousLevel = owner.server.getLevel(previous.dimension());
            if (previousLevel != null) {
                removeTicket(previousLevel, previous);
            }
        }
        ChunkPos chunk = new ChunkPos(source);
        owner.serverLevel().getChunkSource().addRegionTicket(RECALL_TICKET, chunk, TICKET_RADIUS, owner.getUUID());
        return true;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, PendingRecall>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingRecall> entry = iterator.next();
            PendingRecall pending = entry.getValue();
            ServerPlayer owner = event.getServer().getPlayerList().getPlayer(pending.ownerId());
            var level = event.getServer().getLevel(pending.dimension());
            if (owner == null || level == null) {
                if (level != null) {
                    removeTicket(level, pending);
                }
                iterator.remove();
                continue;
            }

            Entity entity = level.getEntity(pending.boarId());
            if (entity instanceof MudsnoutBoar boar && boar.isAlive() && boar.isTamed() && boar.isOwner(owner)) {
                if (boar.isVehicle()) {
                    owner.displayClientMessage(Component.translatable(
                            "message.kingsandmonsters.boar_horn.ridden"), true);
                } else {
                    boar.stopRiding();
                    boar.teleportTo(pending.destination().getX() + 0.5, pending.destination().getY(),
                            pending.destination().getZ() + 0.5);
                    boar.getNavigation().stop();
                    boar.setTarget(null);
                    remember(boar, owner);
                    owner.displayClientMessage(Component.translatable(
                            "message.kingsandmonsters.boar_horn.recalled"), true);
                }
                removeTicket(level, pending);
                iterator.remove();
            } else if (pending.ticksLeft() <= 1) {
                owner.displayClientMessage(Component.translatable(
                        "message.kingsandmonsters.boar_horn.none"), true);
                removeTicket(level, pending);
                iterator.remove();
            } else {
                entry.setValue(pending.withTicksLeft(pending.ticksLeft() - 1));
            }
        }
    }

    private static void removeTicket(ServerLevel level, PendingRecall pending) {
        level.getChunkSource().removeRegionTicket(RECALL_TICKET, new ChunkPos(pending.source()),
                TICKET_RADIUS, pending.ownerId());
    }

    private record PendingRecall(UUID ownerId, UUID boarId, ResourceKey<Level> dimension,
                                 BlockPos source, BlockPos destination, int ticksLeft) {
        private PendingRecall withTicksLeft(int value) {
            return new PendingRecall(ownerId, boarId, dimension, source, destination, value);
        }
    }

    /** Drops state tied to a world that has closed, so it cannot leak into the next one opened in this session. */
    public static void resetState() {
        PENDING.clear();
    }
}
