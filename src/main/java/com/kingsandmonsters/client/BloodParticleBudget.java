package com.kingsandmonsters.client;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.Util;

/**
 * Client-only bookkeeping for the bleeding visuals: cheap counters instead of scanning particles.
 * Every particle remembers the {@link #generation()} it was counted in and only releases its slot if
 * that generation is still current, so a {@link #reset()} (logout / dimension change) can never be
 * followed by a double decrement.
 */
public final class BloodParticleBudget {
    public static final int MAX_DROPLETS = 24;
    public static final int MAX_STAINS = 48;
    public static final int MAX_STAINS_PER_ENTITY = 6;
    public static final int NO_OWNER = -1;

    // Particles wiped by the engine (resource reload, F3+A) never report their removal, so a count that
    // stays above zero with no activity this long can only be stale.
    private static final long STALE_COUNT_MILLIS = 15_000L;
    private static long lastActivity;

    private static int generation;
    private static int droplets;
    private static int stains;
    private static final Int2IntOpenHashMap STAINS_PER_OWNER = new Int2IntOpenHashMap();

    private BloodParticleBudget() {}

    public static int generation() {
        return generation;
    }

    /** Forget every counted particle; called on level change and on logout. */
    public static void reset() {
        generation++;
        droplets = 0;
        stains = 0;
        STAINS_PER_OWNER.clear();
    }

    public static ParticleStatus status() {
        return Minecraft.getInstance().options.particles().get();
    }

    /** Fraction of the full stain budget available under the current particle setting. */
    private static double stainScale() {
        return switch (status()) {
            case ALL -> 1.0;
            case DECREASED -> 0.5;
            case MINIMAL -> 0.25;
        };
    }

    private static void releaseStaleCounts() {
        if ((droplets > 0 || stains > 0) && Util.getMillis() - lastActivity > STALE_COUNT_MILLIS) {
            reset();
        }
    }

    public static boolean canSpawnDroplet() {
        releaseStaleCounts();
        return droplets < MAX_DROPLETS;
    }

    public static boolean canSpawnStain(int ownerId) {
        releaseStaleCounts();
        double scale = stainScale();
        if (scale <= 0.0 || stains >= (int) (MAX_STAINS * scale)) return false;
        return ownerId == NO_OWNER || STAINS_PER_OWNER.get(ownerId) < (int) (MAX_STAINS_PER_ENTITY * scale);
    }

    public static void dropletAdded() {
        lastActivity = Util.getMillis();
        droplets++;
    }

    public static void dropletRemoved(int addedGeneration) {
        lastActivity = Util.getMillis();
        if (addedGeneration == generation && droplets > 0) droplets--;
    }

    public static void stainAdded() {
        lastActivity = Util.getMillis();
        stains++;
    }

    public static void stainOwned(int ownerId) {
        if (ownerId != NO_OWNER) STAINS_PER_OWNER.addTo(ownerId, 1);
    }

    public static void stainRemoved(int addedGeneration, int ownerId) {
        if (addedGeneration != generation) return;
        lastActivity = Util.getMillis();
        if (stains > 0) stains--;
        if (ownerId != NO_OWNER && STAINS_PER_OWNER.containsKey(ownerId)) {
            int left = STAINS_PER_OWNER.addTo(ownerId, -1) - 1;
            if (left <= 0) STAINS_PER_OWNER.remove(ownerId);
        }
    }
}
