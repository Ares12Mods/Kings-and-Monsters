package com.kingsandmonsters.client;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import com.kingsandmonsters.entity.OgreBrute;
import com.kingsandmonsters.entity.OgreGruntCaptain;
import com.kingsandmonsters.entity.OgreMage;
import com.kingsandmonsters.network.QuestActionPayload;
import com.kingsandmonsters.network.QuestDialoguePayload;
import com.kingsandmonsters.tribute.OgreQuest;
import com.kingsandmonsters.tribute.QuestManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/** An errand conversation with the King or a captain. */
public final class QuestDialogueScreen extends CinematicDialogueScreen {
    private QuestDialoguePayload data;
    private int lastAction = -1;
    private String reply;
    /** The quest the player is looking at while three are on offer; -1 while they are still choosing. */
    private int picked = -1;
    /** Chooses which wording of a greeting or reply is used, so the giver does not say the same thing every time. */
    private int variantSeed = ThreadLocalRandom.current().nextInt(1000);

    private QuestDialogueScreen(QuestDialoguePayload data) {
        super(Component.translatable("screen.kingsandmonsters.king_dialogue.title"));
        this.data = data;
    }

    public static void open(QuestDialoguePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof QuestDialogueScreen current && current.data.giverId() == payload.giverId()) {
            current.refresh(payload);
        } else {
            minecraft.setScreen(new QuestDialogueScreen(payload));
        }
    }

    private void refresh(QuestDialoguePayload payload) {
        // The giver answers what was just pressed, instead of repeating what he says to someone mid-errand.
        if (lastAction == QuestManager.ACCEPT && payload.state() == QuestManager.ACTIVE) {
            reply = "accept";
        } else if (lastAction == QuestManager.TURN_IN && payload.state() == QuestManager.COOLDOWN) {
            reply = "done";
        } else {
            reply = null;
        }
        lastAction = -1;
        picked = -1;
        variantSeed = ThreadLocalRandom.current().nextInt(1000);
        this.data = payload;
        restartTyping();
        rebuildWidgets();
    }

    @Override
    public void removed() {
        PacketDistributor.sendToServer(new QuestActionPayload(data.giverId(), QuestManager.END_TALK, -1));
        super.removed();
    }

    @Override
    protected void drawTopBar(GuiGraphics graphics, int barBottom) {
        drawFavorBar(graphics, barBottom, data.favor(), data.appeasedFavor(), data.appeased());
    }

    @Override
    protected int giverId() {
        return data.giverId();
    }

    @Override
    protected Component speakerName() {
        Entity giver = Minecraft.getInstance().level == null ? null
                : Minecraft.getInstance().level.getEntity(data.giverId());
        return data.king() || giver == null ? title : giver.getDisplayName();
    }

    @Override
    protected void buildOptions() {
        switch (data.state()) {
            case QuestManager.OFFER -> {
                if (picked < 0) {
                    for (int ordinal : data.choices()) {
                        OgreQuest choice = OgreQuest.byOrdinal(ordinal);
                        if (choice != null) {
                            Component name = Component.translatable("quest.kingsandmonsters." + choice.id() + ".name");
                            Button button = choice.isBigQuest()
                                    ? sideOption(name, pressed -> choose(ordinal))
                                    : option(name, pressed -> choose(ordinal));
                            button.setTooltip(Tooltip.create(Component.empty()
                                    .append(Component.translatable("quest.kingsandmonsters." + choice.id() + ".goal"))
                                    .append(": " + choice.target()).append("\n")
                                    .append(rewardLine(choice))));
                        }
                    }
                } else {
                    option(Component.translatable("screen.kingsandmonsters.quest.accept"), button -> {
                        lastAction = QuestManager.ACCEPT;
                        PacketDistributor.sendToServer(new QuestActionPayload(
                                data.giverId(), QuestManager.ACCEPT, picked));
                    });
                    option(Component.translatable("screen.kingsandmonsters.quest.back"), button -> choose(-1));
                }
                leave("decline");
            }
            case QuestManager.ACTIVE -> {
                action("abandon", QuestManager.ABANDON);
                leave("leave");
            }
            case QuestManager.READY -> {
                action(handsOverItems() ? "turn_in" : "report", QuestManager.TURN_IN);
                leave("leave");
            }
            default -> leave("leave");
        }
    }

    /** The favor a quest pays plus the items that come with it, so the player knows what they are picking. */
    private static Component rewardLine(OgreQuest quest) {
        Component line = Component.translatable("screen.kingsandmonsters.quest.reward", quest.reward());
        StringBuilder items = new StringBuilder();
        for (ItemStack bonus : quest.bonuses()) {
            items.append(items.length() == 0 ? "" : ", ")
                    .append(bonus.getCount()).append(" ").append(bonus.getHoverName().getString());
        }
        return items.length() == 0 ? line : Component.empty().append(line).append(" + " + items);
    }

    @Override
    protected Component sideTitle() {
        return data.state() == QuestManager.OFFER && picked < 0
                ? Component.translatable("screen.kingsandmonsters.quest.bold") : null;
    }

    private void action(String key, int action) {
        option(Component.translatable("screen.kingsandmonsters.quest." + key), button -> {
            lastAction = action;
            PacketDistributor.sendToServer(new QuestActionPayload(data.giverId(), action, -1));
        });
    }

    /** Looks at one of the offered quests, or (-1) goes back to the choice of three. */
    private void choose(int ordinal) {
        picked = ordinal;
        variantSeed = ThreadLocalRandom.current().nextInt(1000);
        restartTyping();
        rebuildWidgets();
    }

    private void leave(String key) {
        option(Component.translatable("screen.kingsandmonsters.quest." + key), button -> onClose());
    }

    /** Errands about items are handed over; hunting and riding errands are only reported. */
    private boolean handsOverItems() {
        OgreQuest quest = OgreQuest.byOrdinal(data.quest());
        return quest == null || quest.kind() == OgreQuest.Kind.DELIVER;
    }

    /** Each kind of captain has his own way of talking; anyone without one uses the shared lines. */
    private String voice() {
        Entity giver = Minecraft.getInstance().level == null ? null
                : Minecraft.getInstance().level.getEntity(data.giverId());
        if (giver instanceof OgreBrute) {
            return "brute";
        }
        if (giver instanceof OgreMage) {
            return "mage";
        }
        if (giver instanceof OgreGruntCaptain) {
            return "grunt";
        }
        return data.king() ? "king" : "captain";
    }

    /** A quest-specific line in the speaker's voice, or the shared line when he has none. */
    private Component questLine(OgreQuest quest, String kind) {
        String shared = "dialogue.kingsandmonsters.quest." + quest.id() + "." + kind;
        String voiced = shared + "." + voice();
        return Component.translatable(I18n.exists(voiced) ? voiced : shared);
    }

    /**
     * A general line (intro, active, ready, ...) in the speaker's voice, or the King's or captains'
     * shared voice. Each has several numbered wordings; one is picked per conversation.
     */
    private Component replyLine(String kind) {
        String base = "dialogue.kingsandmonsters.quest." + kind + ".";
        for (String voiced : new String[] {base + voice(), base + (data.king() ? "king" : "captain")}) {
            int variants = 0;
            while (I18n.exists(voiced + "." + (variants + 1))) {
                variants++;
            }
            if (variants > 0) {
                return Component.translatable(voiced + "." + (1 + Math.floorMod(variantSeed, variants)));
            }
        }
        return Component.translatable(base + voice());
    }

    @Override
    protected Component line() {
        OgreQuest quest = OgreQuest.byOrdinal(data.quest());
        if ("accept".equals(reply) && quest != null) {
            return questLine(quest, "accept");
        }
        if ("done".equals(reply)) {
            return replyLine("done");
        }
        return switch (data.state()) {
            case QuestManager.OFFER -> OgreQuest.byOrdinal(picked) == null
                    ? replyLine("intro") : questLine(OgreQuest.byOrdinal(picked), "offer");
            case QuestManager.ACTIVE -> replyLine("active");
            case QuestManager.READY -> replyLine(handsOverItems() ? "ready" : "ready_done");
            case QuestManager.BUSY -> replyLine("busy");
            default -> replyLine("cooldown");
        };
    }

    @Override
    protected Component detailLine() {
        boolean offer = data.state() == QuestManager.OFFER;
        OgreQuest quest = OgreQuest.byOrdinal(offer ? picked : data.quest());
        if (quest == null || data.state() == QuestManager.BUSY || data.state() == QuestManager.COOLDOWN) {
            return null;
        }
        Component reward = offer ? rewardLine(quest)
                : Component.translatable("screen.kingsandmonsters.quest.reward", quest.reward());
        if (offer) {
            return reward;
        }
        return Component.translatable("quest.kingsandmonsters." + quest.id() + ".goal")
                .append(": " + data.progress() + " / " + quest.target() + "     ").append(reward);
    }
}
