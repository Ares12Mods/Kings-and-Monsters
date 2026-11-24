package com.kingsandmonsters.block;

import com.kingsandmonsters.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * An acorn hanging under oak leaves. Wild ones are ripe. A planted one starts as a bud, ripens on
 * its own, and picking a ripe one leaves the bud behind so the tree keeps producing.
 */
public final class AcornBlock extends Block implements BonemealableBlock {
    /** 0 is small, 1 is half grown, 2 is ripe. */
    public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
    private static final int RIPE = 2;
    // The model shrinks toward the stem, so the hitbox does too.
    private static final VoxelShape[] SHAPES = {
            box(7.0, 12.4, 7.0, 9.0, 16.0, 9.0),
            box(6.25, 9.7, 6.25, 9.75, 16.0, 9.75),
            box(5.5, 7.0, 5.5, 10.5, 16.0, 10.5)
    };

    public AcornBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, RIPE));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(Blocks.OAK_LEAVES);
    }

    /** A planted acorn starts small, never ripe on the spot. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(AGE, 0);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.UP && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < RIPE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < RIPE && level.getRawBrightness(pos, 0) >= 9 && random.nextInt(6) == 0) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(AGE) != RIPE) {
            return InteractionResult.PASS;
        }
        harvest(state, level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void harvest(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            // One or two a pick; the stem stays so the tree grows the next ones.
            int count = 1 + level.random.nextInt(2);
            ItemStack acorns = new ItemStack(ModItems.ACORN.get(), count);
            if (!player.addItem(acorns)) player.drop(acorns, false);
            level.setBlock(pos, state.setValue(AGE, 0), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                    SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return state.getValue(AGE) < RIPE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(RIPE, state.getValue(AGE) + 1)), Block.UPDATE_CLIENTS);
    }
}
