package com.kingsandmonsters.block;

import com.kingsandmonsters.ModBlocks;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.block.entity.CandleHelmetBlockEntity;
import com.kingsandmonsters.item.CandleHelmetItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Candle Helmet set down on the ground. It remembers the exact stack (enchantments, damage, lit
 * state). Flint and steel lights the wick; an empty hand puts it out. A lit helmet picked up again keeps
 * its lit state but only works (light, flame) while worn.
 */
public final class CandleHelmetBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 0 = unlit; otherwise the light level the lit helmet gives (11 plus Brightwick). */
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    /** The armor geometry sits where a head would; this lowers its base (23px, minus 1.5px inflate, plus 2px measured from a screenshot) onto the block floor. */
    public static final double DROP_TO_FLOOR = -(23.0 - 1.5 + 2.0) / 16.0;
    /** Keeps the leaned model inside its block instead of letting the front rim overhang. */
    public static final double BACKWARD_OFFSET = 7.0 / 16.0;
    /** Tips the placed helmet farther onto its front rim; the worn armor renderer is unaffected. */
    public static final float FORWARD_TILT_DEGREES = 15.0F;
    /** Where the flame starts on the model: just above the candle top (37px), in model space. */
    private static final float WICK_MODEL_HEIGHT = 37.5F / 16.0F;
    private static final VoxelShape SHAPE = Block.box(2.5, 0.0, 2.5, 13.5, 13.5, 13.5);

    /**
     * Where the placed model's wick is in the world, found by running the point through the same
     * shift and forward tilt the renderer applies, so the flame follows every facing.
     */
    public static Vec3 wickPosition(BlockPos pos, Direction facing) {
        Matrix4f transform = new Matrix4f().translate(0.0F, (float) DROP_TO_FLOOR, 0.0F);
        float back = (float) BACKWARD_OFFSET;
        float tilt = (float) Math.toRadians(FORWARD_TILT_DEGREES);
        switch (facing) {
            case NORTH -> transform.translate(0.0F, 0.0F, back).rotateX(-tilt);
            case SOUTH -> transform.translate(0.0F, 0.0F, -back).rotateX(tilt);
            case EAST -> transform.translate(-back, 0.0F, 0.0F).rotateZ(-tilt);
            case WEST -> transform.translate(back, 0.0F, 0.0F).rotateZ(tilt);
            default -> { }
        }
        transform.translate(0.5F, 0.0F, 0.5F);
        Vector3f wick = transform.transformPosition(new Vector3f(0.0F, WICK_MODEL_HEIGHT, 0.0F));
        return new Vec3(pos.getX() + wick.x, pos.getY() + wick.y, pos.getZ() + wick.z);
    }

    public CandleHelmetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIGHT, 0));
    }

    public static boolean isLit(BlockState state) {
        return state.getValue(LIGHT) > 0;
    }

    /** Called by {@link CandleHelmetItem#useOn}: sets the helmet down on the clicked face. */
    public static InteractionResult tryPlace(UseOnContext useContext) {
        BlockPlaceContext context = new BlockPlaceContext(useContext);
        if (!context.canPlace()) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = ModBlocks.CANDLE_HELMET_BLOCK.get().getStateForPlacement(context);
        if (state == null || !state.canSurvive(level, pos)) return InteractionResult.PASS;

        ItemStack held = context.getItemInHand();
        ItemStack stored = held.copyWithCount(1);
        if (CandleHelmetItem.isLit(stored)) {
            state = state.setValue(LIGHT, CandleHelmetItem.lightLevel(stored, level.registryAccess()));
        }
        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof CandleHelmetBlockEntity helmet) {
            helmet.setStack(stored);
        }
        Player player = context.getPlayer();
        level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
        if (player == null || !player.getAbilities().instabuild) {
            held.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult igniteWithFlint(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand) {
        if (!stack.is(Items.FLINT_AND_STEEL) || isLit(state)) {
            return null;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CandleHelmetBlockEntity helmet) {
            ItemStack stored = helmet.getStack();
            CandleHelmetItem.setLit(stored, true);
            helmet.setStack(stored);
            level.setBlock(pos, state.setValue(LIGHT, CandleHelmetItem.lightLevel(stored, level.registryAccess())),
                    Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                    level.getRandom().nextFloat() * 0.4F + 0.8F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            com.kingsandmonsters.item.ItemUtil.hurt(stack, 1, player, com.kingsandmonsters.item.ItemUtil.slotForHand(hand));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        InteractionResult ignited = igniteWithFlint(player.getItemInHand(hand), state, level, pos, player, hand);
        if (ignited != null) return ignited;
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            return wear(state, level, pos, player);
        }
        if (!isLit(state)) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CandleHelmetBlockEntity helmet) {
            ItemStack stored = helmet.getStack();
            CandleHelmetItem.setLit(stored, false);
            helmet.setStack(stored);
            level.setBlock(pos, state.setValue(LIGHT, 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        if (level.isClientSide) {
            Vec3 wick = wickPosition(pos, state.getValue(FACING));
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.SMOKE,
                        wick.x,
                        wick.y,
                        wick.z,
                        0.0, 0.03, 0.0);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Sneak + empty-hand right-click puts the placed helmet straight onto your head (lit state kept). */
    private InteractionResult wear(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CandleHelmetBlockEntity helmet) {
            ItemStack worn = player.getItemBySlot(EquipmentSlot.HEAD);
            level.removeBlock(pos, false);
            player.setItemSlot(EquipmentSlot.HEAD, helmet.getStack());
            if (!worn.isEmpty() && !player.getInventory().add(worn)) {
                player.drop(worn, false);
            }
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.EQUIP, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!isLit(state)) return;
        Vec3 wick = wickPosition(pos, state.getValue(FACING));
        double x = wick.x;
        double y = wick.y;
        double z = wick.z;
        if (random.nextFloat() < 0.5F) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0, 0.0, 0.0);
        }
        if (random.nextFloat() < 0.15F) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.05, z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack stack = entity instanceof CandleHelmetBlockEntity helmet
                ? helmet.getStack()
                : new ItemStack(ModItems.CANDLE_HELMET.get());
        return List.of(stack);
    }

    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof CandleHelmetBlockEntity helmet
                ? helmet.getStack()
                : new ItemStack(ModItems.CANDLE_HELMET.get());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos supportPos = pos.below();
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CandleHelmetBlockEntity(pos, state);
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIGHT);
    }
}
