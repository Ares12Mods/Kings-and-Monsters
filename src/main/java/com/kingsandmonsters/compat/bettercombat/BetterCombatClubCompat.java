package com.kingsandmonsters.compat.bettercombat;

import net.bettercombat.forge.network.NetworkHandler;
import net.bettercombat.forge.network.PacketWrapper;
import net.bettercombat.logic.AnimatedHand;
import net.bettercombat.network.Packets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/** Plays Better Combat's one-handed slam swing for the Ogre King's Club. */
public final class BetterCombatClubCompat {
    private BetterCombatClubCompat() {
    }

    public static void playSlam(ServerPlayer player) {
        Packets.AttackAnimation animation = new Packets.AttackAnimation(
                player.getId(),
                AnimatedHand.MAIN_HAND,
                "bettercombat:one_handed_slam",
                1.0F,
                0.5F);
        NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new PacketWrapper(true, Packets.AttackAnimation.ID, animation.write()));
    }
}
