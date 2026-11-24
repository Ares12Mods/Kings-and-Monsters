package com.kingsandmonsters.block;

import net.minecraft.world.InteractionResult;
import com.kingsandmonsters.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import com.kingsandmonsters.block.entity.HaunchPlatterBlockEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A whole roast on a plate. Four servings come off it with a bowl, then the empty plate is cleared away. */
public final class HaunchPlatterBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int MAX_SERVINGS = 4;
    public static final IntegerProperty SERVINGS = IntegerProperty.create("servings", 0, MAX_SERVINGS);

    private static final VoxelShape FULL_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 10.0, 15.0);
    private static final VoxelShape LOW_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 6.0, 15.0);
    private static final VoxelShape EMPTY_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0);

    public HaunchPlatterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SERVINGS, MAX_SERVINGS));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HaunchPlatterBlockEntity(pos, state);
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SERVINGS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int servings = state.getValue(SERVINGS);
        return servings >= 2 ? FULL_SHAPE : servings == 1 ? LOW_SHAPE : EMPTY_SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        int servings = state.getValue(SERVINGS);
        if (servings == 0) {
            // Nothing left on it: pick the plate up as a bowl.
            level.removeBlock(pos, false);
            level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            if (!level.isClientSide) {
                ItemStack bowl = new ItemStack(Items.BOWL);
                if (!player.getInventory().add(bowl)) {
                    player.drop(bowl, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!stack.is(Items.BOWL)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.kingsandmonsters.haunch_platter.bowl"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(SERVINGS, servings - 1), Block.UPDATE_ALL);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.BLOCKS, 1.0F, 0.8F);
            ItemStack serving = new ItemStack(ModItems.HAUNCH_PLATTER_SERVING.get());
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (stack.isEmpty()) {
                player.setItemInHand(hand, serving);
            } else if (!player.getInventory().add(serving)) {
                player.drop(serving, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(SERVINGS);
    }
}
