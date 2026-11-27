package com.kingsandmonsters.tribute;

import com.kingsandmonsters.Config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import com.kingsandmonsters.ModParticles;
import com.kingsandmonsters.advancement.RoyalAdvancementEvents;
import com.kingsandmonsters.entity.OgreGrunt;
import com.kingsandmonsters.entity.OgreLord;
import com.kingsandmonsters.network.QuestDialoguePayload;
import com.kingsandmonsters.network.QuestTrackerPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Quests for favor: the King and the captains offer one at a time, and each player keeps their own. */
public final class QuestManager {
    public static final int OFFER = 0;
    public static final int ACTIVE = 1;
    public static final int READY = 2;
    public static final int COOLDOWN = 3;
    /** The player is carrying out an errand for the other kind of giver. */
    public static final int BUSY = 4;

    public static final int ACCEPT = 0;
    public static final int TURN_IN = 1;
    public static final int ABANDON = 2;
    /** Sent when the dialogue closes, so a captain who stopped to talk can get on with his day. */
    public static final int END_TALK = 3;

    private static final long COOLDOWN_TICKS = 20L * 60 * 5;
    /** Giving up an errand costs time, so it cannot be used to shop for an easier one. */
    private static final long ABANDON_COOLDOWN_TICKS = 20L * 60 * 3;
    /** A finished quest is not offered to that player again for this long, so the whole pool gets used. */
    private static final long QUEST_LOCKOUT_TICKS = 20L * 60 * 30;
    private static final long KING_QUEST_LOCKOUT_TICKS = 20L * 60 * 60;
    private static final int XP_PER_FAVOR = 2;

    private QuestManager() {}

    /** What a giver should show this player right now. quest is -1 when there is nothing to show. */
    public record View(int state, int quest, int progress, List<Integer> choices) {
        View(int state, int quest, int progress) {
            this(state, quest, progress, List.of());
        }
    }

    /** How many errands a giver lays out to pick from. */
    public static final int CHOICES = 3;

    public static View view(ServerPlayer player, OgreGrunt giver) {
        boolean king = giver instanceof OgreLord;
        ServerLevel level = player.serverLevel();
        UUID id = player.getUUID();
        TributeSavedData.QuestRecord record = TributeManager.playerData(level).getQuest(id);
        OgreQuest active = OgreQuest.byOrdinal(record.quest());
        if (active != null) {
            if (active.isKingQuest() != king) {
                return new View(BUSY, active.ordinal(), 0);
            }
            int progress = progressOf(player, active, record);
            return new View(progress >= active.target() ? READY : ACTIVE, active.ordinal(), progress);
        }
        if (level.getGameTime() < record.cooldownUntil()) {
            return new View(COOLDOWN, -1, 0);
        }
        boolean outpostCaptain = !king && giver.isOutpostCaptain() && giver.getOutpostHome() != null;
        boolean swampOutpost = outpostCaptain && level.getBiome(giver.getOutpostHome()).is(Biomes.SWAMP);
        List<OgreQuest> pool = OgreQuest.pool(king, outpostCaptain, swampOutpost);
        // Errands this player finished recently are held back. If that leaves fewer than three, the
        // ones that come free soonest fill the gaps, so there is always a choice.
        TributeSavedData data = TributeManager.playerData(level);
        long now = level.getGameTime();
        List<OgreQuest> fresh = new ArrayList<>();
        List<OgreQuest> recent = new ArrayList<>();
        List<OgreQuest> bigQuests = new ArrayList<>();
        for (OgreQuest quest : pool) {
            if (quest.isBigQuest()) {
                bigQuests.add(quest);
            } else {
                (data.questLockedUntil(id, quest.ordinal()) > now ? recent : fresh).add(quest);
            }
        }
        recent.sort(Comparator.comparingLong(quest -> data.questLockedUntil(id, quest.ordinal())));
        // The order is shuffled by a seed that only changes when an errand is finished, so the offer is
        // the same every time this player looks (and cannot be rerolled by walking away) but is not a
        // pattern that comes round again.
        long seed = id.getMostSignificantBits() * 31 + id.getLeastSignificantBits()
                + giver.getUUID().getLeastSignificantBits() * 17 + record.completed() * 0x9E3779B97F4A7C15L;
        Collections.shuffle(fresh, new Random(seed));
        fresh.addAll(recent);
        List<Integer> choices = new ArrayList<>();
        for (int i = 0; i < Math.min(CHOICES, fresh.size()); i++) {
            choices.add(fresh.get(i).ordinal());
        }
        // One of the King's biggest errands is an optional extra beside the three, for anyone bold
        // enough. It is picked the same way: shuffled by the same kind of seed, so it is stable for
        // this player until they finish an errand, and skipping any they finished recently.
        List<OgreQuest> boldPool = new ArrayList<>();
        for (OgreQuest big : bigQuests) {
            if (data.questLockedUntil(id, big.ordinal()) <= now) {
                boldPool.add(big);
            }
        }
        if (!boldPool.isEmpty()) {
            Collections.shuffle(boldPool, new Random(seed ^ 0x5DEECE66DL));
            choices.add(boldPool.get(0).ordinal());
        }
        return new View(OFFER, choices.get(0), 0, choices);
    }

    /** Opens the errand dialogue with this giver on the player's screen. */
    public static void open(ServerPlayer player, OgreGrunt giver) {
        boolean king = giver instanceof OgreLord;
        if (!king) {
            giver.beginErrandTalk(player);
        }
        View view = view(player, giver);
        PacketDistributor.sendToPlayer(player, new QuestDialoguePayload(
                giver.getId(), king, view.state(), view.quest(), view.progress(), view.choices(),
                TributeManager.getFavor(player.serverLevel(), player.getUUID()),
                Config.TRIBUTE_APPEASED_FAVOR.get(),
                TributeManager.isAppeased(player.serverLevel(), player.getUUID())));
    }

    /** Applies a dialogue button; returns true when the screen should be refreshed. */
    public static boolean act(ServerPlayer player, OgreGrunt giver, int action, int chosen) {
        ServerLevel level = player.serverLevel();
        TributeSavedData data = TributeManager.playerData(level);
        TributeSavedData.QuestRecord record = data.getQuest(player.getUUID());
        View view = view(player, giver);
        OgreQuest quest = OgreQuest.byOrdinal(view.quest());

        if (action == ACCEPT && view.state() == OFFER && view.choices().contains(chosen)) {
            quest = OgreQuest.byOrdinal(chosen);
            data.setQuest(player.getUUID(), new TributeSavedData.QuestRecord(
                    quest.ordinal(), 0,
                    record.cooldownUntil(), record.completed()));
            return true;
        }
        if (action == ACCEPT && view.state() == OFFER) {
            return true; // the offer changed under the player; show the current one
        }
        if (action == ABANDON && (view.state() == ACTIVE || view.state() == READY)) {
            data.setQuest(player.getUUID(), new TributeSavedData.QuestRecord(
                    -1, 0, level.getGameTime() + ABANDON_COOLDOWN_TICKS, record.completed()));
            return true;
        }
        if (action == TURN_IN && view.state() == READY && quest != null) {
            if (isDelivery(quest) && !takeItems(player, quest)) {
                return true;
            }
            data.setQuest(player.getUUID(), new TributeSavedData.QuestRecord(
                    -1, 0, level.getGameTime() + COOLDOWN_TICKS, record.completed() + 1));
            data.lockQuest(player.getUUID(), quest.ordinal(),
                    level.getGameTime() + (quest.isKingQuest() ? KING_QUEST_LOCKOUT_TICKS : QUEST_LOCKOUT_TICKS));
            complete(player, quest);
            return true;
        }
        return false;
    }

    private static void complete(ServerPlayer player, OgreQuest quest) {
        player.giveExperiencePoints(quest.reward() * XP_PER_FAVOR);
        TributeManager.addFavor(player.serverLevel(), player, quest.reward());
        for (ItemStack bonus : quest.bonuses()) {
            player.sendSystemMessage(Component.translatable(
                    "message.kingsandmonsters.errand_bonus", bonus.getCount(), bonus.getHoverName())
                    .withStyle(ChatFormatting.GOLD));
            if (!player.getInventory().add(bonus)) {
                player.drop(bonus, false);
            }
        }
        RoyalAdvancementEvents.awardPath(player, "tribute/hired_hand");
        if (quest.isKingQuest()) {
            RoyalAdvancementEvents.awardPath(player, "tribute/royal_errand");
        }
    }

   /** Counts a kill toward whatever hunting errand the killer is on. */
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && !(event.getEntity() instanceof Player)) {
            onKill(player, event.getEntity());
        }
    }

    private static void onKill(ServerPlayer player, LivingEntity victim) {
        TributeSavedData data = TributeManager.playerData(player.serverLevel());
        TributeSavedData.QuestRecord record = data.getQuest(player.getUUID());
        OgreQuest quest = OgreQuest.byOrdinal(record.quest());
        if (quest == null || quest.kind() != OgreQuest.Kind.CULL || !quest.isPrey(victim)
                || record.progress() >= quest.target()) {
            return;
        }
        int progress = record.progress() + 1;
        data.setQuest(player.getUUID(), new TributeSavedData.QuestRecord(
                record.quest(), progress, record.cooldownUntil(), record.completed()));
        player.displayClientMessage(Component.translatable("quest.kingsandmonsters." + quest.id() + ".goal")
                .append(" " + progress + " / " + quest.target()).withStyle(ChatFormatting.GOLD), true);
    }

   // --- on-screen tracker ---------------------------------------------------------------------

    private static final int SYNC_INTERVAL_TICKS = 10;
    private static final Map<UUID, QuestTrackerPayload> LAST_SENT = new HashMap<>();
    private static int ticksUntilSync;

    /** The player's active errand and how far along it is, whoever gave it. */
    public static QuestTrackerPayload tracker(ServerPlayer player) {
        TributeSavedData.QuestRecord record = TributeManager.playerData(player.serverLevel()).getQuest(player.getUUID());
        OgreQuest quest = OgreQuest.byOrdinal(record.quest());
        return quest == null ? QuestTrackerPayload.NONE
                : new QuestTrackerPayload(quest.ordinal(), progressOf(player, quest, record), quest.isKingQuest());
    }

    /** Keeps each player's tracker current; item counts change without any errand event to hook. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (--ticksUntilSync > 0) {
            return;
        }
        ticksUntilSync = SYNC_INTERVAL_TICKS;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            QuestTrackerPayload payload = tracker(player);
            if (!payload.equals(LAST_SENT.get(player.getUUID()))) {
                PacketDistributor.sendToPlayer(player, payload);
                LAST_SENT.put(player.getUUID(), payload);
            }
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    public static void resetSync() {
        LAST_SENT.clear();
        ticksUntilSync = 0;
    }

    private static int progressOf(ServerPlayer player, OgreQuest quest, TributeSavedData.QuestRecord record) {
        return switch (quest.kind()) {
            case DELIVER -> Math.min(quest.target(), countItems(player, quest));
            case CULL -> Math.min(quest.target(), record.progress());
        };
    }

    private static boolean isDelivery(OgreQuest quest) {
        return quest.kind() == OgreQuest.Kind.DELIVER;
    }

    private static int countItems(ServerPlayer player, OgreQuest quest) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (quest.accepts(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean takeItems(ServerPlayer player, OgreQuest quest) {
        if (countItems(player, quest) < quest.target()) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        int left = quest.target();
        for (ItemStack stack : player.getInventory().items) {
            if (left > 0 && quest.accepts(stack)) {
                int taken = Math.min(left, stack.getCount());
                stack.shrink(taken);
                left -= taken;
            }
        }
        return true;
    }

    /** Plays once when a player first opens a conversation with the King. */
    public static void playDialogueStartParticle(OgreLord giver) {
        if (giver.level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.DIALOGUE_EXCLAMATION.get(),
                    giver.getX(), giver.getY() + giver.getBbHeight() + 1.25, giver.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

}
