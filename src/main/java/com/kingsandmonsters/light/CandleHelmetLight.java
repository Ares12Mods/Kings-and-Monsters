package com.kingsandmonsters.light;

import com.kingsandmonsters.item.CandleHelmetItem;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Environmental light for the Candle Helmet, done with temporary {@code minecraft:light} blocks
 * (there is no dynamic-light library in this project).
 *
 * <p><b>Ownership.</b> Each wearer (player UUID) has one {@link Wearer} entry. Light positions are
 * tracked per dimension as {@link Slot}s holding the UUIDs of their owners and each one's wanted
 * level; the block shows the highest. A slot is removed only when its last owner releases it, so
 * one wearer can never remove another's light.</p>
 *
 * <p><b>Placement.</b> Only into air, in a loaded chunk, inside world bounds: the head block, the
 * block above, then up to three horizontal neighbours. Written with {@link Block#UPDATE_CLIENTS}
 * only. Removal checks the block is still our light at the level we placed; a waterlogged light
 * becomes water, not air. The light moves only when the head block or level changes, plus a
 * once-per-second integrity check, and the new light is acquired before the old one is released
 * so walking doesn't flicker.</p>
 *
 * <p><b>Cleanup.</b> Released on helmet removal (equipment event and per-tick check), death,
 * logout, dimension change or any removal from the level ({@link EntityLeaveLevelEvent}),
 * spectator mode, and server stop.</p>
 *
 * <p><b>Crash safety.</b> {@link CandleLightSavedData} records every placed light until removal is
 * confirmed. After an unclean shutdown the records survive and are verified and removed at server
 * start and on each chunk load. Chunks are never force-loaded: if a light's chunk is unloaded when
 * its owner releases it, the record stays and the block goes when the chunk loads. A crash between
 * a chunk save and the SavedData save in the same autosave can still leave one unrecorded light.</p>
 */
public final class CandleHelmetLight {
    private static final int RECHECK_INTERVAL_TICKS = 20;
    private static final int CHUNK_RETRY_LIMIT = 100;

    private static final Map<UUID, Wearer> WEARERS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<Long, Slot>> SLOTS = new HashMap<>();

    /** Chunks that contain recorded-but-not-live lights. Concurrent: read from the chunk-load event. */
    private static final Set<ChunkKey> ORPHAN_CHUNKS = ConcurrentHashMap.newKeySet();
    private static final Queue<ChunkKey> CHUNKS_TO_CLEAN = new ConcurrentLinkedQueue<>();
    private static final Deque<RetryChunk> RETRY = new ArrayDeque<>();

    private record ChunkKey(ResourceKey<Level> dimension, long chunk) {}

    private record RetryChunk(ChunkKey key, int tries) {}

    private static final class Slot {
        final Map<UUID, Integer> owners = new HashMap<>();
        int appliedLevel;

        int maxLevel() {
            int max = 0;
            for (int level : owners.values()) {
                max = Math.max(max, level);
            }
            return max;
        }
    }

    private static final class Wearer {
        final ResourceKey<Level> dimension;
        BlockPos head;
        int level;
        BlockPos lightPos;

        Wearer(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }
    }

    private CandleHelmetLight() {}

    public static void register(IEventBus bus) {
        bus.addListener(CandleHelmetLight::onServerStarting);
        bus.addListener(CandleHelmetLight::onServerStarted);
        bus.addListener(CandleHelmetLight::onServerStopping);
        bus.addListener(CandleHelmetLight::onServerTick);
        bus.addListener(CandleHelmetLight::onPlayerTick);
        bus.addListener(CandleHelmetLight::onEquipmentChange);
        bus.addListener(CandleHelmetLight::onLivingDeath);
        bus.addListener(CandleHelmetLight::onLoggedOut);
        bus.addListener(CandleHelmetLight::onChangedDimension);
        bus.addListener(CandleHelmetLight::onEntityLeave);
        bus.addListener(CandleHelmetLight::onChunkLoad);
    }

    // ------------------------------------------------------------------ events

    private static void onServerStarting(ServerStartingEvent event) {
        resetState();
    }

    private static void onServerStarted(ServerStartedEvent event) {
        // Queue every recorded position's chunk from a previous (possibly crashed) session.
        for (ServerLevel level : event.getServer().getAllLevels()) {
            CandleLightSavedData data = CandleLightSavedData.get(level);
            for (long pos : data.snapshot().keySet()) {
                ChunkKey key = chunkKey(level, pos);
                ORPHAN_CHUNKS.add(key);
                CHUNKS_TO_CLEAN.add(key);
            }
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        for (UUID uuid : WEARERS.keySet().toArray(new UUID[0])) {
            release(server, uuid);
        }
        resetState();
    }

    private static void resetState() {
        WEARERS.clear();
        SLOTS.clear();
        ORPHAN_CHUNKS.clear();
        CHUNKS_TO_CLEAN.clear();
        RETRY.clear();
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        boolean wearing = head.getItem() instanceof CandleHelmetItem && CandleHelmetItem.isLit(head);
        if (!wearing || !player.isAlive() || player.isSpectator()) {
            if (!WEARERS.isEmpty()) {
                release(player.server, player.getUUID());
            }
            return;
        }
        update(player, head);
    }

    private static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.HEAD) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTo().getItem() instanceof CandleHelmetItem)) {
            release(player.server, player.getUUID());
        }
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            release(player.server, player.getUUID());
        }
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            release(player.server, player.getUUID());
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            release(player.server, player.getUUID());
        }
    }

    private static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            release(player.server, player.getUUID());
        }
    }

    /** May be fired off the main thread: only touches concurrent collections. */
    private static void onChunkLoad(ChunkEvent.Load event) {
        if (ORPHAN_CHUNKS.isEmpty() || event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ChunkKey key = new ChunkKey(level.dimension(), event.getChunk().getPos().toLong());
        if (ORPHAN_CHUNKS.contains(key)) {
            CHUNKS_TO_CLEAN.add(key);
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (CHUNKS_TO_CLEAN.isEmpty() && RETRY.isEmpty()) return;
        MinecraftServer server = event.getServer();
        ChunkKey key;
        while ((key = CHUNKS_TO_CLEAN.poll()) != null) {
            cleanChunk(server, key, 0);
        }
        int pending = RETRY.size();
        for (int i = 0; i < pending; i++) {
            RetryChunk retry = RETRY.poll();
            if (retry != null) {
                cleanChunk(server, retry.key(), retry.tries());
            }
        }
    }

    // ------------------------------------------------------------------ per-wearer update

    private static void update(ServerPlayer player, ItemStack helmet) {
        ServerLevel level = player.serverLevel();
        UUID uuid = player.getUUID();
        int wanted = CandleHelmetItem.lightLevel(helmet, level.registryAccess());
        BlockPos headPos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());

        Wearer wearer = WEARERS.get(uuid);
        if (wearer != null && !wearer.dimension.equals(level.dimension())) {
            release(player.server, uuid);
            wearer = null;
        }
        if (wearer == null) {
            wearer = new Wearer(level.dimension());
            WEARERS.put(uuid, wearer);
            wearer.level = -1; // force acquisition below
        }

        boolean sameState = headPos.equals(wearer.head) && wanted == wearer.level;
        boolean periodic = player.tickCount % RECHECK_INTERVAL_TICKS == 0;
        if (sameState && !periodic) return;
        if (sameState && wearer.lightPos != null && wearer.lightPos.equals(headPos)
                && isOurLight(level, wearer.lightPos, slotLevel(level, wearer.lightPos))) {
            return; // ideal position, intact
        }

        wearer.head = headPos.immutable();
        wearer.level = wanted;

        BlockPos old = wearer.lightPos;
        BlockPos acquired = acquire(level, uuid, candidates(player, headPos), wanted);
        wearer.lightPos = acquired;
        if (old != null && !old.equals(acquired)) {
            removeOwner(level, old, uuid);
        }
    }

    private static BlockPos[] candidates(ServerPlayer player, BlockPos head) {
        Direction facing = player.getDirection();
        return new BlockPos[] {
                head,
                head.above(),
                head.relative(facing.getOpposite()),
                head.relative(facing.getClockWise()),
                head.relative(facing.getCounterClockWise())
        };
    }

    /** Joins or creates a slot at the first safe candidate. Returns that position or null. */
    private static BlockPos acquire(ServerLevel level, UUID uuid, BlockPos[] candidates, int wantedLevel) {
        Map<Long, Slot> slots = SLOTS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        for (BlockPos candidate : candidates) {
            if (!level.isInWorldBounds(candidate) || !level.isLoaded(candidate)) continue;
            BlockState state = level.getBlockState(candidate);
            long key = candidate.asLong();
            Slot slot = slots.get(key);

            if (slot != null && isOurLight(level, candidate, slot.appliedLevel)) {
                slot.owners.put(uuid, wantedLevel);
                applyLevel(level, candidate, slot);
                return candidate.immutable();
            }
            if (state.isAir()) {
                BlockPos pos = candidate.immutable();
                if (slot == null) {
                    slot = new Slot();
                    slots.put(key, slot);
                }
                slot.owners.put(uuid, wantedLevel);
                slot.appliedLevel = slot.maxLevel();
                level.setBlock(pos, lightState(slot.appliedLevel, false), Block.UPDATE_CLIENTS);
                CandleLightSavedData.get(level).record(pos, slot.appliedLevel);
                return pos;
            }
            // Solid block, fluid, block entity, or anything else: never touched.
        }
        return null;
    }

    private static int slotLevel(ServerLevel level, BlockPos pos) {
        Map<Long, Slot> slots = SLOTS.get(level.dimension());
        Slot slot = slots == null ? null : slots.get(pos.asLong());
        return slot == null ? -1 : slot.appliedLevel;
    }

    /** Re-applies the slot's max owner level to its (verified-ours) block when it changed. */
    private static void applyLevel(ServerLevel level, BlockPos pos, Slot slot) {
        int target = slot.maxLevel();
        if (target == slot.appliedLevel) return;
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.LIGHT)) {
            level.setBlock(pos, state.setValue(LightBlock.LEVEL, target), Block.UPDATE_CLIENTS);
            slot.appliedLevel = target;
            CandleLightSavedData.get(level).record(pos, target);
        }
    }

    // ------------------------------------------------------------------ release / removal

    private static void release(MinecraftServer server, UUID uuid) {
        Wearer wearer = WEARERS.remove(uuid);
        if (wearer == null || wearer.lightPos == null) return;
        ServerLevel level = server.getLevel(wearer.dimension);
        if (level != null) {
            removeOwner(level, wearer.lightPos, uuid);
        } else {
            Map<Long, Slot> slots = SLOTS.get(wearer.dimension);
            if (slots != null) {
                slots.remove(wearer.lightPos.asLong());
            }
        }
    }

    private static void removeOwner(ServerLevel level, BlockPos pos, UUID uuid) {
        Map<Long, Slot> slots = SLOTS.get(level.dimension());
        Slot slot = slots == null ? null : slots.get(pos.asLong());
        if (slot == null) return;
        slot.owners.remove(uuid);
        if (!slot.owners.isEmpty()) {
            applyLevel(level, pos, slot); // other wearers keep the light (possibly at a lower level)
            return;
        }
        slots.remove(pos.asLong());
        if (!level.isLoaded(pos)) {
            // Chunk is unloaded: never force-load. The SavedData record stays; the block is removed on next load.
            ChunkKey key = chunkKey(level, pos.asLong());
            ORPHAN_CHUNKS.add(key);
            return;
        }
        removeIfOurs(level, pos, slot.appliedLevel);
        CandleLightSavedData.get(level).forget(pos);
    }

    private static boolean isOurLight(ServerLevel level, BlockPos pos, int expectedLevel) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.LIGHT) && state.getValue(LightBlock.LEVEL) == expectedLevel;
    }

    /** Removes the block only if it is still a light block at the level we placed. */
    private static void removeIfOurs(ServerLevel level, BlockPos pos, int expectedLevel) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.LIGHT) || state.getValue(LightBlock.LEVEL) != expectedLevel) return;
        BlockState replacement = state.getValue(LightBlock.WATERLOGGED)
                ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                : Blocks.AIR.defaultBlockState();
        level.setBlock(pos, replacement, Block.UPDATE_CLIENTS);
    }

    private static BlockState lightState(int lightLevel, boolean waterlogged) {
        return Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, lightLevel)
                .setValue(LightBlock.WATERLOGGED, waterlogged);
    }

    // ------------------------------------------------------------------ orphan cleanup

    private static ChunkKey chunkKey(ServerLevel level, long blockPos) {
        BlockPos pos = BlockPos.of(blockPos);
        return new ChunkKey(level.dimension(), ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
    }

    private static void cleanChunk(MinecraftServer server, ChunkKey key, int tries) {
        ServerLevel level = server.getLevel(key.dimension());
        if (level == null) return;
        int chunkX = ChunkPos.getX(key.chunk());
        int chunkZ = ChunkPos.getZ(key.chunk());
        if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
            // Not loaded (yet). The chunk-load event re-queues it later; retry briefly for chunks still finishing.
            if (tries < CHUNK_RETRY_LIMIT) {
                RETRY.add(new RetryChunk(key, tries + 1));
            }
            return;
        }
        CandleLightSavedData data = CandleLightSavedData.get(level);
        Long2IntOpenHashMap records = data.snapshot();
        Map<Long, Slot> live = SLOTS.get(level.dimension());
        boolean remaining = false;
        for (Long2IntMap.Entry entry : records.long2IntEntrySet()) {
            long packed = entry.getLongKey();
            if (!chunkKey(level, packed).equals(key)) continue;
            if (live != null && live.containsKey(packed)) continue; // owned by a live wearer
            BlockPos pos = BlockPos.of(packed);
            removeIfOurs(level, pos, entry.getIntValue());
            data.forget(pos);
        }
        for (long packed : data.snapshot().keySet()) {
            if (chunkKey(level, packed).equals(key)) {
                remaining = true;
                break;
            }
        }
        if (!remaining) {
            ORPHAN_CHUNKS.remove(key);
        }
    }
}
