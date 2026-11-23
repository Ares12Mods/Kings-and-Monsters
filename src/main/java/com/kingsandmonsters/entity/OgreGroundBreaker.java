package com.kingsandmonsters.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How the ogres' heavy blows chew up the ground. Only soft terrain gives way (dirt, grass, sand,
 * gravel, snow and the like), and never anything with stored data, so their own stone and timber
 * builds are safe. A fort can take a little wear, but each patch of ground has a small budget:
 * inside a structure a blow is far less likely to break anything and the budget is tiny, and
 * everywhere else it refills every minute, so a long brawl can't turn a camp into a pit.
 */
final class OgreGroundBreaker {
    /**
     * How hard a blow is on the ground. Dirt is 0.5 hardness, grass 0.6, stone 1.5, planks and logs 2,
     * obsidian 50. Soft is the Grunts' and the Brute's; Heavy is the King's alone and takes any
     * ordinary block (stone, planks, ore), but only out in the open.
     */
    record Strength(float maxHardness, int openBudget, int structureBudget, float structureChanceScale) {
        // Inside a structure the chance drops to a fifth and only 2 blocks a minute can go.
        static final Strength SOFT = new Strength(0.6F, 8, 2, 0.2F);
        // The King still only breaks soft blocks inside a structure, but he is let a little further.
        static final Strength HEAVY = new Strength(20.0F, 20, 4, 0.35F);
    }

    private static final int BUDGET_WINDOW_TICKS = 1200;
    private static final Map<Long, long[]> BUDGETS = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, long[]> eldest) {
            return size() > 512;
        }
    };

    private OgreGroundBreaker() {
    }

    /** Tries to break the block at {@code pos}; true if it broke. */
    static boolean breakBlock(ServerLevel level, Mob ogre, BlockPos pos, float chance) {
        return breakBlock(level, ogre, pos, chance, Strength.SOFT);
    }

    static boolean breakBlock(ServerLevel level, Mob ogre, BlockPos pos, float chance, Strength strength) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || level.getBlockEntity(pos) != null) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        boolean inStructure = level.structureManager().hasAnyStructureAt(pos);
        // Inside a structure even the King only breaks soft blocks.
        float limit = inStructure ? Strength.SOFT.maxHardness() : strength.maxHardness();
        if (hardness < 0.0F || hardness > limit || !EventHooks.canEntityGrief(level, ogre)) {
            return false;
        }
        float scaled = inStructure ? chance * strength.structureChanceScale() : chance;
        int budget = inStructure ? strength.structureBudget() : strength.openBudget();
        if (ogre.getRandom().nextFloat() >= scaled || !spendBudget(level, pos, budget)) {
            return false;
        }
        return level.destroyBlock(pos, false, ogre);
    }

    /**
     * Breaks up to {@code count} blocks from the 3x3x3 around {@code center}, picked at random, the
     * way the King's slams always have. Each one goes through the same hardness, structure and
     * budget rules as any other break.
     */
    static void breakNearby(ServerLevel level, Mob ogre, Vec3 center, int count, Strength strength) {
        BlockPos centerPos = BlockPos.containing(center.x, center.y, center.z);
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(centerPos.offset(-1, -1, -1), centerPos.offset(1, 1, 1))) {
            candidates.add(pos.immutable());
        }
        int broken = 0;
        while (broken < count && !candidates.isEmpty()) {
            BlockPos pos = candidates.remove(ogre.getRandom().nextInt(candidates.size()));
            if (breakBlock(level, ogre, pos, 1.0F, strength)) {
                broken++;
            }
        }
    }

    /** A few tries at random spots on the ground around {@code center}. */
    static void breakAround(ServerLevel level, Mob ogre, Vec3 center, double radius, int attempts, float chance) {
        breakAround(level, ogre, center, radius, attempts, chance, Strength.SOFT);
    }

    static void breakAround(ServerLevel level, Mob ogre, Vec3 center, double radius, int attempts, float chance,
                            Strength strength) {
        for (int i = 0; i < attempts; i++) {
            BlockPos pos = BlockPos.containing(
                    center.x + (ogre.getRandom().nextDouble() * 2.0 - 1.0) * radius,
                    center.y - 0.5,
                    center.z + (ogre.getRandom().nextDouble() * 2.0 - 1.0) * radius);
            breakBlock(level, ogre, pos, chance, strength);
        }
    }

    private static boolean spendBudget(ServerLevel level, BlockPos pos, int limit) {
        long key = ChunkPos.asLong(pos) * 31L + level.dimension().location().hashCode();
        long now = level.getGameTime();
        synchronized (BUDGETS) {
            long[] budget = BUDGETS.computeIfAbsent(key, k -> new long[]{0L, now});
            if (now - budget[1] >= BUDGET_WINDOW_TICKS) {
                budget[0] = 0L;
                budget[1] = now;
            }
            if (budget[0] >= limit) {
                return false;
            }
            budget[0]++;
            return true;
        }
    }
}
