package com.kingsandmonsters.tribute;

/**
 * A hole in the middle of a camp that its residents must stay out of (the quarry pit). Residents
 * spawn in the ring between {@code avoidRadius} and {@code keepRadius} from the centre, which is
 * the stretch inside the walls and clear of the hole, and their pathfinding treats the disc inside
 * {@code avoidRadius} as solid. The centre is in world coordinates.
 */
public record PitZone(int x, int z, int avoidRadius, int keepRadius) {

    /** True when (x, z) is inside the ring where residents may spawn. */
    public boolean isSpawnRing(int px, int pz) {
        long dx = px - x;
        long dz = pz - z;
        long distanceSqr = dx * dx + dz * dz;
        return distanceSqr >= (long) avoidRadius * avoidRadius
                && distanceSqr <= (long) keepRadius * keepRadius;
    }
}
