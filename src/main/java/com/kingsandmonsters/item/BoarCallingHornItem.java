package com.kingsandmonsters.item;

import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.entity.MudsnoutBoar;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public final class BoarCallingHornItem extends Item {
    private static final int COOLDOWN_TICKS = 20 * 20;

    public BoarCallingHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        int hornVariant = level.getRandom().nextBoolean() ? 0 : 1;
        level.playSound(null, player.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(hornVariant).value(),
                SoundSource.PLAYERS, 1.0F, 0.82F);

        ServerPlayer serverPlayer = (ServerPlayer) player;
        MudsnoutBoar boar = findOwnedBoar(serverPlayer);
        if (boar == null) {
            BlockPos destination = findDestination(serverPlayer.serverLevel(), player.blockPosition());
            if (!MudsnoutRecallTracker.queueRememberedRecall(serverPlayer, destination)) {
                player.displayClientMessage(Component.translatable("message.kingsandmonsters.boar_horn.none"), true);
                return InteractionResultHolder.fail(stack);
            }
            consumeUse(player, hand, stack);
            return InteractionResultHolder.success(stack);
        }
        if (boar.isVehicle()) {
            player.displayClientMessage(Component.translatable("message.kingsandmonsters.boar_horn.ridden"), true);
            return InteractionResultHolder.fail(stack);
        }

        BlockPos destination = findDestination(serverPlayer.serverLevel(), player.blockPosition());
        boar.stopRiding();
        boar.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
        boar.getNavigation().stop();
        boar.setTarget(null);
        consumeUse(player, hand, stack);
        player.displayClientMessage(Component.translatable("message.kingsandmonsters.boar_horn.recalled"), true);
        return InteractionResultHolder.success(stack);
    }

    private void consumeUse(Player player, InteractionHand hand, ItemStack stack) {
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 40;
    }

    private static MudsnoutBoar findOwnedBoar(ServerPlayer player) {
        MudsnoutBoar nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity entity : player.serverLevel().getAllEntities()) {
            if (!(entity instanceof MudsnoutBoar boar) || !boar.isAlive()
                    || !boar.isTamed() || !boar.isOwner(player)) {
                continue;
            }
            double distance = boar.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearest = boar;
                nearestDistance = distance;
            }
        }
        return nearest != null ? nearest : MudsnoutRecallTracker.loadRemembered(player);
    }

    private static BlockPos findDestination(ServerLevel level, BlockPos origin) {
        int[][] offsets = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}};
        for (int[] offset : offsets) {
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    origin.offset(offset[0], 0, offset[1]));
            if (level.getBlockState(surface).isAir() && level.getBlockState(surface.above()).isAir()) {
                return surface;
            }
        }
        return origin.above();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.MUDSNOUT_TUSK.get()) || super.isValidRepairItem(stack, repairCandidate);
    }

}
