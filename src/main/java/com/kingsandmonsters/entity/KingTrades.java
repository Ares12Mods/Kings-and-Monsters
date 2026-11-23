package com.kingsandmonsters.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;

/**
 * The Ogre King's stall, so the vanilla trading screen can be used without him being a villager.
 * The offers are built per player, so what one player has bought stays bought only for them.
 */
final class KingTrades implements Merchant {
    private final Level level;
    private final MerchantOffers offers;
    @Nullable private Player customer;

    private final IntConsumer onSold;

    KingTrades(Level level, MerchantOffers offers, IntConsumer onSold) {
        this.level = level;
        this.offers = offers;
        this.onSold = onSold;
    }

    @Override public void setTradingPlayer(@Nullable Player player) { customer = player; }

    @Nullable @Override public Player getTradingPlayer() { return customer; }

    @Override public MerchantOffers getOffers() { return offers; }

    @Override public void overrideOffers(MerchantOffers offers) { }

    @Override public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        int index = offers.indexOf(offer);
        if (index >= 0) {
            onSold.accept(index);
        }
    }

    @Override public void notifyTradeUpdated(ItemStack stack) { }

    @Override public int getVillagerXp() { return 0; }

    @Override public void overrideXp(int xp) { }

    @Override public boolean showProgressBar() { return false; }

    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.EMPTY; }

    @Override public boolean isClientSide() { return level.isClientSide; }

    @Override public boolean canRestock() { return false; }
}
