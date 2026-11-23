package com.kingsandmonsters.world;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The hut and trap grid is pure arithmetic, so its guarantees can be checked over a huge corpus. */
class TrapperSitesInvariantTest {
    @Test
    void everyTrapSharesItsHutsCellAndSitsThreeToSixChunksAway() {
        Random corpus = new Random(0x54524150504552L);
        for (int sample = 0; sample < 200_000; sample++) {
            long seed = corpus.nextLong();
            int chunkX = corpus.nextInt(-20_000, 20_001);
            int chunkZ = corpus.nextInt(-20_000, 20_001);
            ChunkPos hut = TrapperSites.hutChunk(seed, chunkX, chunkZ);
            ChunkPos trap = TrapperSites.trapChunk(seed, chunkX, chunkZ);

            assertEquals(Math.floorDiv(hut.x, TrapperSites.GRID_SPACING), Math.floorDiv(trap.x, TrapperSites.GRID_SPACING));
            assertEquals(Math.floorDiv(hut.z, TrapperSites.GRID_SPACING), Math.floorDiv(trap.z, TrapperSites.GRID_SPACING));
            // Looking the trap up from its own chunk must find the same hut and the same trap again.
            assertEquals(hut, TrapperSites.hutChunk(seed, trap.x, trap.z));
            assertEquals(trap, TrapperSites.trapChunk(seed, trap.x, trap.z));

            assertNotEquals(hut, trap);
            double blocks = Math.hypot(hut.x - trap.x, hut.z - trap.z) * 16.0;
            assertTrue(blocks >= 48.0 && blocks <= 96.0, "pair distance " + blocks);
        }
    }
}
