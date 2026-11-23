package com.kingsandmonsters.light;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-dimension record of every temporary {@code minecraft:light} block placed by the Candle Helmet
 * (block position -> light level it was placed with) that has not yet been confirmed removed.
 * It is the safety net for crashes/unclean shutdowns: stale records are verified (still our light
 * block with the recorded level) and removed when their chunk next loads.
 */
public final class CandleLightSavedData extends SavedData {
    private static final String NAME = "kingsandmonsters_candle_lights";
    private static final SavedData.Factory<CandleLightSavedData> FACTORY =
            new SavedData.Factory<>(CandleLightSavedData::new, CandleLightSavedData::load, null);

    private final Long2IntOpenHashMap lights = new Long2IntOpenHashMap();

    public static CandleLightSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static CandleLightSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CandleLightSavedData data = new CandleLightSavedData();
        long[] positions = tag.getLongArray("positions");
        int[] levels = tag.getIntArray("levels");
        for (int i = 0; i < positions.length && i < levels.length; i++) {
            data.lights.put(positions[i], levels[i]);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        long[] positions = new long[lights.size()];
        int[] levels = new int[lights.size()];
        int i = 0;
        for (Long2IntMap.Entry entry : lights.long2IntEntrySet()) {
            positions[i] = entry.getLongKey();
            levels[i] = entry.getIntValue();
            i++;
        }
        tag.putLongArray("positions", positions);
        tag.putIntArray("levels", levels);
        return tag;
    }

    public void record(BlockPos pos, int level) {
        long key = pos.asLong();
        if (!lights.containsKey(key) || lights.get(key) != level) {
            lights.put(key, level);
            setDirty();
        }
    }

    public void forget(BlockPos pos) {
        long key = pos.asLong();
        if (lights.containsKey(key)) {
            lights.remove(key);
            setDirty();
        }
    }

    public boolean isEmpty() {
        return lights.isEmpty();
    }

    /** Snapshot copy, safe to iterate while records are modified. */
    public Long2IntOpenHashMap snapshot() {
        return new Long2IntOpenHashMap(lights);
    }
}
