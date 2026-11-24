package com.kingsandmonsters.client;

import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.item.KingsTributeBagItem;
import com.kingsandmonsters.network.KingDialogueActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import com.kingsandmonsters.network.ModNetwork;

/** What the Ogre King says when he has sat down for a visitor, and what the visitor can do about it. */
public final class KingDialogueScreen extends CinematicDialogueScreen {

    private final int kingId;
    private final int favor;
    private final int appeasedFavor;
    private final int tradeFavor;
    private final int duelFavor;
    private final boolean appeased;
    private final boolean beaten;
    private final int duelRestSeconds;
    /** Picks which wording of his greeting the King uses for this audience. */
    private final int variantSeed = java.util.concurrent.ThreadLocalRandom.current().nextInt(1000);

    private KingDialogueScreen(int kingId, int favor, int appeasedFavor, int tradeFavor, int duelFavor,
                               boolean appeased, boolean beaten, int duelRestSeconds) {
        super(Component.translatable("screen.kingsandmonsters.king_dialogue.title"));
        this.kingId = kingId;
        this.favor = favor;
        this.appeasedFavor = appeasedFavor;
        this.tradeFavor = tradeFavor;
        this.duelFavor = duelFavor;
        this.appeased = appeased;
        this.beaten = beaten;
        this.duelRestSeconds = duelRestSeconds;
    }

    public static void open(int kingId, int favor, int appeasedFavor, int tradeFavor, int duelFavor,
                            boolean appeased, boolean beaten, int duelRestSeconds) {
        Minecraft.getInstance().setScreen(new KingDialogueScreen(kingId, favor, appeasedFavor, tradeFavor, duelFavor,
                appeased, beaten, duelRestSeconds));
    }

    @Override
    protected int giverId() {
        return kingId;
    }

    @Override
    protected Component speakerName() {
        return title;
    }

    @Override
    protected Component line() {
        // Someone who has beaten him in a duel gets his own words, whatever the tribe thinks of them.
        String stance = beaten ? "beaten" : appeased ? "appeased" : favor * 2 >= appeasedFavor ? "wary" : "hostile";
        String base = "screen.kingsandmonsters.king_dialogue.line." + stance;
        int variants = 0;
        while (net.minecraft.client.resources.language.I18n.exists(base + "." + (variants + 1))) {
            variants++;
        }
        return Component.translatable(variants == 0 ? base : base + "." + (1 + variantSeed % variants));
    }

    @Override
    protected void buildOptions() {
        Button trade = option(Component.translatable("screen.kingsandmonsters.king_dialogue.trade"),
                button -> act(KingDialogueActionPayload.TRADE, false));
        trade.active = appeased && favor >= tradeFavor;
        if (!trade.active) {
            trade.setTooltip(Tooltip.create(
                    Component.translatable("screen.kingsandmonsters.king_dialogue.trade_locked", tradeFavor)));
        }
        option(Component.translatable("screen.kingsandmonsters.king_dialogue.errands"),
                button -> act(KingDialogueActionPayload.ERRANDS, true));
        int offerings = carriedTribute();
        Button offer = option(Component.translatable("screen.kingsandmonsters.king_dialogue.offer", offerings),
                button -> act(KingDialogueActionPayload.OFFER_COINS, true));
        offer.active = offerings > 0;
        Button duel = option(Component.translatable("screen.kingsandmonsters.king_dialogue.duel"),
                button -> act(KingDialogueActionPayload.DUEL, false));
        duel.active = favor >= duelFavor && duelRestSeconds <= 0;
        duel.setTooltip(Tooltip.create(duelRestSeconds > 0
                ? Component.translatable("screen.kingsandmonsters.king_dialogue.duel_rest", (duelRestSeconds + 59) / 60)
                : Component.translatable(duel.active
                        ? "screen.kingsandmonsters.king_dialogue.duel_ready"
                        : "screen.kingsandmonsters.king_dialogue.duel_locked", duelFavor)));
        option(Component.translatable("screen.kingsandmonsters.king_dialogue.challenge"),
                button -> act(KingDialogueActionPayload.CHALLENGE, false));
        option(Component.translatable("screen.kingsandmonsters.king_dialogue.leave"), button -> onClose());
    }

    /** Gold Coins and sealed King's Tribute Bundles: what the King will take. */
    private static int carriedTribute() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }
        int coins = 0;
        for (var stack : player.getInventory().items) {
            if (stack.is(ModItems.GOLD_COIN.get())
                    || stack.is(ModItems.KINGS_TRIBUTE_BUNDLE.get()) && !KingsTributeBagItem.isOpened(stack)) {
                coins += stack.getCount();
            }
        }
        return coins;
    }

    /** followUp is true when the server will answer with another dialogue screen. */
    private void act(int action, boolean followUp) {
        ModNetwork.sendToServer(new KingDialogueActionPayload(kingId, action));
        if (followUp) {
            keepShot();
        }
        onClose();
    }

    @Override
    protected void drawTopBar(GuiGraphics graphics, int barBottom) {
        drawFavorBar(graphics, barBottom, favor, appeasedFavor, appeased);
    }
}
