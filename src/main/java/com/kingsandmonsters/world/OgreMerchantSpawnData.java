package com.kingsandmonsters.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

final class OgreMerchantSpawnData extends SavedData {
    static final String FILE_ID = "kingsandmonsters_ogre_merchant_spawns";
    private long nextAttemptGameTime;

    static OgreMerchantSpawnData load(CompoundTag tag) {
        OgreMerchantSpawnData data = new OgreMerchantSpawnData();
        data.nextAttemptGameTime = tag.getLong("NextAttemptGameTime");
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.putLong("NextAttemptGameTime", nextAttemptGameTime);
        return tag;
    }

    long nextAttemptGameTime() { return nextAttemptGameTime; }
    void schedule(long gameTime) { nextAttemptGameTime = gameTime; setDirty(); }
}
