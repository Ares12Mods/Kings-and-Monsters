package com.kingsandmonsters.item;

import net.minecraft.world.entity.EquipmentSlot;
import com.kingsandmonsters.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.kingsandmonsters.event.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Abilities shared by the Bog Iron sword and tool set. */
public final class BogIronEquipmentEvents {
    private static final int POISON_DURATION_TICKS = 4 * 20;
    private static final int POISON_COOLDOWN_TICKS = 6 * 20;
    private static final int HEAVY_SWING_COOLDOWN_TICKS = 20;
    private static final String POISON_READY_AT = "kingsandmonsters.bog_iron_poison_ready_at";
    private static final String HEAVY_SWING_READY_AT = "kingsandmonsters.bog_iron_heavy_swing_ready_at";

    private BogIronEquipmentEvents() {
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || event.getSource().getDirectEntity() != attacker
                || !attacker.getMainHandItem().is(ModItems.BOG_IRON_SWORD.get())) {
            return;
        }

        tryApplySwordPoison(attacker, event.getEntity());
    }

    /** Applies the Bog Iron Sword's combat ability for players and custom mob attack timelines. */
    public static void tryApplySwordPoison(LivingEntity attacker, LivingEntity target) {
        if (attacker.level().isClientSide
                || !attacker.getMainHandItem().is(ModItems.BOG_IRON_SWORD.get())) {
            return;
        }
        long now = attacker.level().getGameTime();
        if (attacker.getPersistentData().getLong(POISON_READY_AT) > now) {
            return;
        }

        if (target.addEffect(
                new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, 0), attacker)) {
            attacker.getPersistentData().putLong(POISON_READY_AT, now + POISON_COOLDOWN_TICKS);
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !player.getMainHandItem().is(ModItems.BOG_IRON_HOE.get())
                || event.getFace() != Direction.UP
                || !canTriggerHeavySwing(player)) {
            return;
        }

        BlockPos origin = event.getPos();
        if (tilledState(player.serverLevel().getBlockState(origin)) == null
                || !player.serverLevel().getBlockState(origin.above()).isAir()) {
            return;
        }

        BlockPos extra = origin.relative(player.getDirection().getClockWise());
        BlockState extraState = player.serverLevel().getBlockState(extra);
        BlockState tilled = tilledState(extraState);
        if (tilled == null || !player.serverLevel().getBlockState(extra.above()).isAir()) {
            extra = origin.relative(player.getDirection().getCounterClockWise());
            extraState = player.serverLevel().getBlockState(extra);
            tilled = tilledState(extraState);
        }
        if (tilled == null
                || !player.serverLevel().getBlockState(extra.above()).isAir()
                || !player.mayUseItemAt(extra, Direction.UP, player.getMainHandItem())) {
            return;
        }

        player.serverLevel().setBlock(extra, tilled, Block.UPDATE_ALL_IMMEDIATE);
        player.serverLevel().playSound(null, extra, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        ItemUtil.hurt(player.getMainHandItem(), 1, player, EquipmentSlot.MAINHAND);
        startHeavySwingCooldown(player);
    }

    private static BlockState tilledState(BlockState state) {
        // Matches the vanilla hoe, so the extra block is tilled the same way the clicked one is.
        if (state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.DIRT_PATH)) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
            return Blocks.FARMLAND.defaultBlockState();
        }
        return null;
    }

    private static boolean canTriggerHeavySwing(ServerPlayer player) {
        return player.getPersistentData().getLong(HEAVY_SWING_READY_AT)
                <= player.serverLevel().getGameTime();
    }

    private static void startHeavySwingCooldown(ServerPlayer player) {
        player.getPersistentData().putLong(HEAVY_SWING_READY_AT,
                player.serverLevel().getGameTime() + HEAVY_SWING_COOLDOWN_TICKS);
    }
}
