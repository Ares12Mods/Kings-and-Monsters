package com.kingsandmonsters.api.patrol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatrolTierTest {
    @Test
    void usesFixedGameplayBoundaries() {
        assertEquals(PatrolTier.SMALL, PatrolTier.fromAnger(0));
        assertEquals(PatrolTier.SMALL, PatrolTier.fromAnger(14));
        assertEquals(PatrolTier.MEDIUM, PatrolTier.fromAnger(15));
        assertEquals(PatrolTier.MEDIUM, PatrolTier.fromAnger(24));
        assertEquals(PatrolTier.LARGE, PatrolTier.fromAnger(25));
        assertEquals(PatrolTier.LARGE, PatrolTier.fromAnger(40));
    }

    @Test
    void configuredMaximumDoesNotShiftTierBoundaries() {
        assertEquals(PatrolTier.MEDIUM, PatrolTier.fromAnger(15, 30));
        assertEquals(PatrolTier.LARGE, PatrolTier.fromAnger(25, 30));
    }
}
