package com.kingsandmonsters.client.particle;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/** Small client-only caps for Bogrot visuals. */
public final class BogrotParticleBudget {
    private static final int MAX_FLIES = 32;
    private static final int MAX_BUBBLES = 48;
    private static final Int2IntOpenHashMap FLIES_PER_OWNER = new Int2IntOpenHashMap();
    private static int generation;
    private static int flies;
    private static int bubbles;

    private BogrotParticleBudget() {}

    public static int generation() { return generation; }
    public static int fliesFor(int ownerId) { return FLIES_PER_OWNER.get(ownerId); }
    public static boolean canAddFly(int ownerId, int desired) {
        return flies < MAX_FLIES && FLIES_PER_OWNER.get(ownerId) < desired;
    }
    public static boolean canAddBubble() { return bubbles < MAX_BUBBLES; }
    public static void addFly(int ownerId) { flies++; FLIES_PER_OWNER.addTo(ownerId, 1); }
    public static void addBubble() { bubbles++; }
    public static void removeFly(int ownerId, int particleGeneration) {
        if (particleGeneration != generation) return;
        if (flies > 0) flies--;
        int left = FLIES_PER_OWNER.addTo(ownerId, -1) - 1;
        if (left <= 0) FLIES_PER_OWNER.remove(ownerId);
    }
    public static void removeBubble(int particleGeneration) {
        if (particleGeneration == generation && bubbles > 0) bubbles--;
    }
    public static void reset() {
        generation++;
        flies = 0;
        bubbles = 0;
        FLIES_PER_OWNER.clear();
    }
}
