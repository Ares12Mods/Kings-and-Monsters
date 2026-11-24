package com.kingsandmonsters.tribute;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModBlocks;
import com.kingsandmonsters.entity.OgreLord;
import com.kingsandmonsters.network.OgreOverlayPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import com.kingsandmonsters.network.ModNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** World-wide progression gate for the ore claimed by the Ogre King. */
public final class BogIronProgression extends SavedData {
    private static final String FILE_ID = KingsAndMonsters.MODID + "_bog_iron_progression";
    private static final long DENIAL_MESSAGE_COOLDOWN = 40L;
    private static final Map<UUID, Long> LAST_DENIAL_MESSAGE = new HashMap<>();

    private boolean unlocked;

    private BogIronProgression() {
    }

    private static BogIronProgression load(CompoundTag tag) {
        BogIronProgression data = new BogIronProgression();
        data.unlocked = tag.getBoolean("Unlocked");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Unlocked", unlocked);
        return tag;
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof OgreLord
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            unlock(player.server, "The Ogre King has fallen. Its buried iron now answers to your kind.");
        }
    }

    /** The bog iron opens to everyone, whether the King fell or yielded. Does nothing once it already has. */
    public static void unlock(MinecraftServer server, String subtitle) {
        BogIronProgression data = get(server);
        if (data.unlocked) {
            return;
        }

        data.unlocked = true;
        data.setDirty();
        for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
            ModNetwork.sendToPlayer(recipient, OgreOverlayPayload.major("THE BOG YIELDS", subtitle));
        }
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || player.isCreative()
                || !isBogIronOre(event.getState())) {
            return;
        }

        if (get(player.server).unlocked) {
            return;
        }

        event.setCanceled(true);
        long now = player.serverLevel().getGameTime();
        long last = LAST_DENIAL_MESSAGE.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now - last >= DENIAL_MESSAGE_COOLDOWN) {
            LAST_DENIAL_MESSAGE.put(player.getUUID(), now);
            ModNetwork.sendToPlayer(player, OgreOverlayPayload.major(
                    "The bog iron rejects your claim.",
                    "Only the Ogre King can break the mire's ancient oath."));
        }
    }

    private static boolean isBogIronOre(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(ModBlocks.BOG_IRON_ORE.get())
                || state.is(ModBlocks.DEEPSLATE_BOG_IRON_ORE.get());
    }

    private static BogIronProgression get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Bog Iron progression requires an overworld");
        }
        return overworld.getDataStorage().computeIfAbsent(BogIronProgression::load, BogIronProgression::new, FILE_ID);
    }

    /** Drops state tied to a world that has closed, so it cannot leak into the next one opened in this session. */
    public static void resetState() {
        LAST_DENIAL_MESSAGE.clear();
    }
}
