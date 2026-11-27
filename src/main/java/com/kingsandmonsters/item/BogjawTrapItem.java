package com.kingsandmonsters.item;

import com.kingsandmonsters.entity.BogjawTrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Bogjaw trap to set down or throw. It opens where it is placed and will not close on whoever set it, but it
 * snaps shut on enemies that walk onto it. Hit it with a stick to pick it up safely; hit it with anything
 * else and it goes off. Used on open air it is thrown like the Trapper throws his: it hurts whatever it lands
 * on, then snaps open on the ground.
 */
public final class BogjawTrapItem extends Item {
    public BogjawTrapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        BlockPos below = pos.below();
        // It needs solid ground to lie on, and room for its jaws.
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return InteractionResult.FAIL;
        }
        Vec3 at = Vec3.atBottomCenterOf(pos);
        BogjawTrap trap = BogjawTrap.placed(level, player, at);
        if (!level.noCollision(trap)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel) {
            level.addFreshEntity(trap);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1.0F, 0.7F);
            ItemUtil.consume(context.getItemInHand(), 1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static final double THROW_SPEED = 1.1;
    private static final int THROW_COOLDOWN_TICKS = 20;

    /** Right-clicking at no block throws it along the line of sight, with the Trapper's flight physics. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }
        if (level instanceof ServerLevel) {
            Vec3 look = player.getLookAngle();
            Vec3 at = player.getEyePosition().add(look.scale(0.6)).add(0.0, -0.25, 0.0);
            level.addFreshEntity(BogjawTrap.thrown(level, player, at, look.scale(THROW_SPEED)));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.8F, 0.9F);
            ItemUtil.consume(stack, 1, player);
        }
        player.getCooldowns().addCooldown(this, THROW_COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
