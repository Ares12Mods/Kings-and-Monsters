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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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

    private static BogIronProgression load(CompoundTag tag, HolderLookup.Provider registries) {
        BogIronProgression data = new BogIronProgression();
        data.unlocked = tag.getBoolean("Unlocked");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Unlocked", unlocked);
        return tag;
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof OgreLord)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BogIronProgression data = get(player.server);
        if (data.unlocked) {
            return;
        }

        data.unlocked = true;
        data.setDirty();
        for (ServerPlayer recipient : player.server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(recipient, OgreOverlayPayload.major(
                    "THE BOG YIELDS",
                    "The Ogre King has fallen. Its buried iron now answers to your kind."));
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
            PacketDistributor.sendToPlayer(player, OgreOverlayPayload.major(
                    "The bog iron rejects your claim.",
                    "Only the fall of an Ogre King can break the mire's ancient oath."));
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
        SavedData.Factory<BogIronProgression> factory = new SavedData.Factory<>(
                BogIronProgression::new,
                BogIronProgression::load);
        return overworld.getDataStorage().computeIfAbsent(factory, FILE_ID);
    }
}
