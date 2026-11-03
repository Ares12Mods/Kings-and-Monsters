package com.kingsandmonsters.api.patrol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatrolTierTest {
    @Test
    void usesFixedGameplayBoundaries() {
        assertEquals(PatrolTier.SMALL, PatrolTier.fromAnger(1));
        assertEquals(PatrolTier.SMALL, PatrolTier.fromAnger(9));
        assertEquals(PatrolTier.MEDIUM, PatrolTier.fromAnger(10));
        assertEquals(PatrolTier.MEDIUM, PatrolTier.fromAnger(19));
        assertEquals(PatrolTier.LARGE, PatrolTier.fromAnger(20));
        assertEquals(PatrolTier.LARGE, PatrolTier.fromAnger(30));
    }

    @Test
    void configuredMaximumDoesNotShiftTierBoundaries() {
        assertEquals(PatrolTier.MEDIUM, PatrolTier.fromAnger(10, 30));
        assertEquals(PatrolTier.LARGE, PatrolTier.fromAnger(20, 30));
    }
}
