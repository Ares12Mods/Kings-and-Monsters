package com.kingsandmonsters.api.patrol;

/**
 * Represents the strength tier of a tribute camp patrol.
 *
 * <p>Tier is determined by the camp's current anger level.
 * Other mods can read this from {@link
 * com.kingsandmonsters.api.event.PatrolSpawnEvent} to adjust their
 * own behaviour (e.g. a difficulty mod scaling enemy stats).</p>
 *
 * <ul>
 *   <li>{@link #SMALL}  — low anger</li>
 *   <li>{@link #MEDIUM} — moderate anger</li>
 *   <li>{@link #LARGE}  — high anger</li>
 * </ul>
 */
public enum PatrolTier {

    /** Small patrol: a Grunt Captain leads 2 grunts and an archer. Low anger. */
    SMALL,

    /** Medium patrol: a Mage leads 2 grunts and an archer. Moderate anger. */
    MEDIUM,

    /** Large patrol: a Brute leads a mage, 2 grunts, and an archer. High anger. */
    LARGE;

    /**
     * Returns the tier that corresponds to the given anger level.
     *
     * <ul>
     *   <li>1–9   → {@link #SMALL}  (Low)</li>
     *   <li>10–19 → {@link #MEDIUM} (Moderate)</li>
     *   <li>20+   → {@link #LARGE}  (High)</li>
     * </ul>
     *
     * @param angerLevel current anger value (0 or higher)
     * @return the matching tier
     */
    public static PatrolTier fromAnger(int angerLevel) {
        int anger = Math.max(0, angerLevel);
        if (anger <= 9) return SMALL;
        if (anger <= 19) return MEDIUM;
        return LARGE;
    }

    /**
     * Compatibility overload. The named tiers use fixed anger values, so the
     * maximum does not move the 10- and 20-point boundaries.
     */
    public static PatrolTier fromAnger(int angerLevel, int maxAngerLevel) {
        return fromAnger(angerLevel);
    }
}
