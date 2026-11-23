package com.kingsandmonsters.block;

import net.minecraft.core.BlockPos;
import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.entity.BogPole;
import com.kingsandmonsters.entity.BogToad;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BogToadEggBlock extends Block {
    private static final VoxelShape NORMAL_SHAPE = box(2.0, 0.0, 2.0, 14.0, 0.25, 14.0);

    public BogToadEggBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return NORMAL_SHAPE;
    }

    /** About one random tick in HATCH_CHANCE hatches the egg, so a clutch hatches over several minutes. */
    private static final int HATCH_CHANCE = 3;
    private static final int WATER_SEARCH_RADIUS = 8;
    private static final double POPULATION_RADIUS = 24.0;
    private static final int LOCAL_CAP = 6;

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(HATCH_CHANCE) != 0 || !hasWaterNearby(level, pos)) {
            return;
        }
        // A swamp only supports so many toads and poles; a crowded one keeps its eggs unhatched.
        int population = level.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(POPULATION_RADIUS),
                animal -> animal instanceof BogToad || animal instanceof BogPole).size();
        if (population >= LOCAL_CAP) {
            return;
        }
        BogPole pole = ModEntities.BOG_POLE.get().create(level);
        if (pole == null) {
            return;
        }
        level.removeBlock(pos, false);
        pole.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        pole.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);
        level.addFreshEntity(pole);
        level.playSound(null, pos, SoundEvents.FROGSPAWN_HATCH, SoundSource.BLOCKS, 0.6F, 1.0F);
    }

    /** A pole that hatches far from water would just suffocate, so eggs wait until a pool is close. */
    private static boolean hasWaterNearby(ServerLevel level, BlockPos pos) {
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-WATER_SEARCH_RADIUS, -2, -WATER_SEARCH_RADIUS),
                pos.offset(WATER_SEARCH_RADIUS, 1, WATER_SEARCH_RADIUS))) {
            if (level.getFluidState(check).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
