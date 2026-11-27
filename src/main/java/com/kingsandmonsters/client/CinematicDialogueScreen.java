package com.kingsandmonsters.client;

import com.kingsandmonsters.tribute.TributeSavedData;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.kingsandmonsters.event.ClientTickEvent;
import net.minecraftforge.client.event.RenderHandEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A conversation shown as a cutscene: the camera holds on the speaker's face between thick
 * letterbox bars, the words run as a centred subtitle, and the choices appear once they have been
 * said. Moving from one such screen to the next keeps the same shot.
 */
public abstract class CinematicDialogueScreen extends Screen {
    protected static final int GOLD = 0xFFE6C24A;
    private static final int EASE_TICKS = 12;
    private static final float CHARS_PER_TICK = 1.6F;
    private static final float BIG_SUBTITLE_SCALE = 1.5F;
    private static final float CAMERA_EYE_HEIGHT = 1.62F;
    /** Where on the speaker the camera aims, as a share of his eye height; lower for a seated pose. */
    private static final float FACE_HEIGHT = 0.85F;
    private static final int BUTTON_HEIGHT = 20;
    private static final int HANDOVER_TICKS = 10;

    /** The camera shot shared by consecutive dialogue screens with the same speaker. */
    private static final class Session {
        int giverId;
        int ticks;
        Entity camera;
        Entity savedCamera;
        CameraType savedCameraType;
        boolean savedHideGui;
        Vec3 startPos;
        float startYaw;
        float startPitch;
        Vec3 goalPos;
        float goalYaw;
        float goalPitch;
        boolean ending;
        int endDelay;
    }

    private static Session session;

    private final List<Button> options = new ArrayList<>();
    private final List<Button> sideOptions = new ArrayList<>();
    private int sideColumnX;
    private int sideColumnWidth;
    private int sideColumnTop;
    private int typedFrom;
    /** Set by a button that expects another dialogue screen straight away, so the shot is kept. */
    private boolean expectingFollowUp;

    protected CinematicDialogueScreen(Component title) {
        super(title);
    }

    // --- what a dialogue supplies -------------------------------------------------------------

    protected abstract int giverId();

    protected abstract Component speakerName();

    protected abstract Component line();

    /** Creates the choices with {@link #option}; they are laid out and shown once the line is said. */
    protected abstract void buildOptions();

    /** A smaller line under the subtitle, such as an errand's progress and reward. */
    protected Component detailLine() {
        return null;
    }

    protected void drawTopBar(GuiGraphics graphics, int barBottom) {
    }

    private static final int FAVOR_BAR_WIDTH = 200;
    private static final int FAVOR_BAR_HEIGHT = 5;
    private static final int FAVOR_BAR_DIM_GOLD = 0xFFA8842A;

    /** The player's favor with the tribe, with the line it has to be won past marked on it. */
    protected final void drawFavorBar(GuiGraphics graphics, int barBottom, int favor, int appeasedFavor,
                                      boolean appeased) {
        int max = TributeSavedData.MAX_FAVOR;
        int left = (width - FAVOR_BAR_WIDTH) / 2;
        int y = barBottom / 2 + 3;
        graphics.drawString(font, Component.translatable("screen.kingsandmonsters.king_dialogue.favor"),
                left, y - 11, 0xFFBBBBBB, true);
        String amount = favor + " / " + max;
        graphics.drawString(font, amount, left + FAVOR_BAR_WIDTH - font.width(amount), y - 11, 0xFFBBBBBB, true);

        graphics.fill(left - 1, y - 1, left + FAVOR_BAR_WIDTH + 1, y + FAVOR_BAR_HEIGHT + 1, 0xFF000000);
        graphics.fill(left, y, left + FAVOR_BAR_WIDTH, y + FAVOR_BAR_HEIGHT, 0xFF2A2A2A);
        int filled = FAVOR_BAR_WIDTH * Math.min(favor, max) / max;
        graphics.fill(left, y, left + filled, y + FAVOR_BAR_HEIGHT, appeased ? GOLD : FAVOR_BAR_DIM_GOLD);

        int marker = left + FAVOR_BAR_WIDTH * appeasedFavor / max;
        graphics.fill(marker, y - 2, marker + 1, y + FAVOR_BAR_HEIGHT + 2, 0xFFFFFFFF);
    }

    // --- shared behaviour ---------------------------------------------------------------------

    protected final Button option(Component label, Button.OnPress press) {
        Button button = Button.builder(label, press).bounds(0, 0, 100, BUTTON_HEIGHT).build();
        options.add(button);
        return button;
    }

    /** A choice shown in a second column to the left of the main one, under {@link #sideTitle()}. */
    protected final Button sideOption(Component label, Button.OnPress press) {
        Button button = Button.builder(label, press).bounds(0, 0, 100, BUTTON_HEIGHT).build();
        sideOptions.add(button);
        return button;
    }

    /** Heading over the second column of choices, or null for none. */
    protected Component sideTitle() {
        return null;
    }

    /** Called by a choice that will be answered by another dialogue screen, so the camera stays put. */
    protected final void keepShot() {
        expectingFollowUp = true;
    }

    protected final void restartTyping() {
        typedFrom = session == null ? 0 : session.ticks;
    }

    @Override
    protected final void init() {
        claimShot();
        options.clear();
        sideOptions.clear();
        buildOptions();
        layoutOptions();
    }

    /** The choices stand in a column at the right of the picture, clear of the speaker's face. */
    private void layoutOptions() {
        int count = options.size();
        if (count == 0) {
            return;
        }
        int step = BUTTON_HEIGHT + 3;
        int buttonWidth = Math.min(150, width * 28 / 100);
        int bandTop = topBarHeight();
        int bandHeight = height - bottomBarHeight() - bandTop;
        int top = bandTop + Math.max(4, (bandHeight - count * step) / 2);
        for (int i = 0; i < count; i++) {
            Button button = options.get(i);
            button.setX(width - buttonWidth - 12);
            button.setY(top + i * step);
            button.setWidth(buttonWidth);
            button.visible = false;
            addRenderableWidget(button);
        }
        // The second column sits beside the first, with a heading line above it.
        sideColumnWidth = Math.min(150, width * 28 / 100);
        sideColumnX = width - buttonWidth - 12 - sideColumnWidth - 8;
        sideColumnTop = top + (sideTitle() == null ? 0 : 12);
        for (int i = 0; i < sideOptions.size(); i++) {
            Button button = sideOptions.get(i);
            button.setX(sideColumnX);
            button.setY(sideColumnTop + i * step);
            button.setWidth(sideColumnWidth);
            button.visible = false;
            addRenderableWidget(button);
        }
    }

    private float subtitleScale() {
        return height >= 280 ? BIG_SUBTITLE_SCALE : 1.0F;
    }

    private int subtitleWrap() {
        return (int) (Math.min(width * 0.7F, 520.0F) / subtitleScale());
    }

    /** Thin, so most of the screen stays picture; tall enough for the favor bar on the King's menu. */
    protected final int topBarHeight() {
        return Math.max(30, Math.round(height * 0.12F));
    }

    /** Sized to the words it has to hold, within limits. */
    protected final int bottomBarHeight() {
        int rows = font.split(line(), subtitleWrap()).size();
        int content = 10 + 14 + Math.round(rows * (font.lineHeight + 2) * subtitleScale()) + 18 + 8;
        return Mth.clamp(content, Math.round(height * 0.18F), Math.round(height * 0.32F));
    }

    private int ticks() {
        return session == null ? 0 : session.ticks;
    }

    private boolean lineFinished() {
        return (ticks() - typedFrom) * CHARS_PER_TICK >= line().getString().length();
    }

    @Override
    public void tick() {
        super.tick();
        if (session != null) {
            session.ticks++;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Entity giver = giver();
        if (minecraft.player == null || giver == null || !giver.isAlive()
                || minecraft.player.hurtTime > 0 || giver.distanceToSqr(minecraft.player) > 14.0 * 14.0) {
            onClose();
            return;
        }
        moveCamera(giver);
    }

    private Entity giver() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? null : minecraft.level.getEntity(giverId());
    }

    @Override
    public void removed() {
        if (session != null) {
            session.ending = true;
            session.endDelay = expectingFollowUp ? HANDOVER_TICKS : 0;
        }
        super.removed();
    }

    // --- camera -------------------------------------------------------------------------------

    private void claimShot() {
        Minecraft minecraft = Minecraft.getInstance();
        Entity giver = giver();
        if (giver == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (session != null && session.giverId == giverId() && session.camera != null) {
            session.ending = false;
            return;
        }
        endShot();

        Session shot = new Session();
        shot.giverId = giverId();
        // An entity that is never added to the world, with the player's eye height so the camera
        // does not slide while it switches over.
        shot.camera = new Villager(EntityType.VILLAGER, minecraft.level);
        shot.savedCamera = minecraft.getCameraEntity();
        shot.savedCameraType = minecraft.options.getCameraType();
        shot.savedHideGui = minecraft.options.hideGui;
        shot.startPos = minecraft.player.getEyePosition();
        shot.startYaw = minecraft.player.getYRot();
        shot.startPitch = minecraft.player.getXRot();
        session = shot;

        setCameraPose(shot.startPos, shot.startYaw, shot.startPitch);
        if (shot.camera instanceof LivingEntity living) {
            living.yHeadRotO = shot.startYaw;
        }
        shot.camera.setOldPosAndRot();
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        minecraft.options.hideGui = true;
        minecraft.setCameraEntity(shot.camera);
    }

    private void moveCamera(Entity giver) {
        if (session == null || session.camera == null) {
            return;
        }
        if (session.goalPos == null) {
            computeShot(giver);
        }
        float t = Mth.clamp(session.ticks / (float) EASE_TICKS, 0.0F, 1.0F);
        t = t * t * (3.0F - 2.0F * t);
        setCameraPose(session.startPos.lerp(session.goalPos, t),
                Mth.rotLerp(t, session.startYaw, session.goalYaw),
                Mth.lerp(t, session.startPitch, session.goalPitch));
    }

    /**
     * The shot is worked out once, from where the speaker stands and where the player is. The
     * speaker's animated head is never followed, because every sway of it would shake the picture.
     */
    private void computeShot(Entity giver) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 base = giver.position();
        Vec3 toPlayer = minecraft.player.position().subtract(base);
        Vec3 forward = new Vec3(toPlayer.x, 0.0, toPlayer.z);
        forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);

        Vec3 face = base.add(0.0, giver.getEyeHeight() * FACE_HEIGHT, 0.0);
        double distance = Math.max(3.0, giver.getBbWidth() * 2.0);
        Vec3 wanted = face.add(forward.scale(distance)).add(side.scale(distance * 0.2)).add(0.0, 0.05, 0.0);
        // Walls and furniture between the face and the camera pull it in closer.
        BlockHitResult hit = minecraft.level.clip(new ClipContext(face, wanted, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, giver));
        if (hit.getType() == HitResult.Type.BLOCK) {
            wanted = hit.getLocation().subtract(wanted.subtract(face).normalize().scale(0.3));
        }
        session.goalPos = wanted;

        double dx = face.x - session.goalPos.x;
        double dy = face.y - session.goalPos.y;
        double dz = face.z - session.goalPos.z;
        session.goalYaw = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F);
        session.goalPitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
    }

    private static void setCameraPose(Vec3 eye, float yaw, float pitch) {
        Entity camera = session.camera;
        camera.setOldPosAndRot();
        camera.setPos(eye.x, eye.y - CAMERA_EYE_HEIGHT, eye.z);
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        // A living entity is viewed along its head rotation, not its body rotation.
        if (camera instanceof LivingEntity living) {
            living.yHeadRotO = living.yHeadRot;
            living.setYHeadRot(yaw);
            living.yBodyRotO = living.yBodyRot;
            living.yBodyRot = yaw;
        }
    }

    private static void endShot() {
        if (session == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (session.camera != null) {
            minecraft.setCameraEntity(session.savedCamera != null ? session.savedCamera : minecraft.player);
            minecraft.options.setCameraType(session.savedCameraType);
            minecraft.options.hideGui = session.savedHideGui;
        }
        session = null;
    }

    /** Hands the view back once no dialogue screen has taken the shot over. */
    public static void onClientTick(ClientTickEvent.Post event) {
        if (session == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean stranded = !(minecraft.screen instanceof CinematicDialogueScreen) && !session.ending;
        if (stranded || session.ending && session.endDelay-- <= 0) {
            endShot();
        }
    }

    /** The held item would be drawn over the close-up. */
    public static void onRenderHand(RenderHandEvent event) {
        if (session != null) {
            event.setCanceled(true);
        }
    }

    // --- drawing ------------------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // The scene is the background; no blur or dimming.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float ease = Mth.clamp((ticks() + partialTick) / EASE_TICKS, 0.0F, 1.0F);
        int bottomBar = Math.round(bottomBarHeight() * ease);
        int topBar = Math.round(topBarHeight() * ease);
        graphics.fill(0, 0, width, topBar, 0xFF000000);
        graphics.fill(0, height - bottomBar, width, height, 0xFF000000);

        if (ease < 1.0F) {
            return;
        }
        drawTopBar(graphics, topBar);
        int y = height - bottomBar + 8;
        graphics.drawCenteredString(font, speakerName(), width / 2, y, 0xFFB89A3A);
        y += 14;
        y = drawSubtitle(graphics, y, partialTick);

        boolean finished = lineFinished();
        Component detail = detailLine();
        if (detail != null && finished) {
            graphics.drawCenteredString(font, detail, width / 2, y + 4, 0xFFBBBBBB);
        }
        for (Button button : options) {
            button.visible = finished;
        }
        for (Button button : sideOptions) {
            button.visible = finished;
        }
        Component sideTitle = sideTitle();
        if (sideTitle != null && finished && !sideOptions.isEmpty()) {
            graphics.drawCenteredString(font, sideTitle, sideColumnX + sideColumnWidth / 2, sideColumnTop - 12, GOLD);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int drawSubtitle(GuiGraphics graphics, int y, float partialTick) {
        String full = line().getString();
        float typed = (ticks() - typedFrom + partialTick) * CHARS_PER_TICK;
        String shown = full.substring(0, Mth.clamp((int) typed, 0, full.length()));
        // Wrap on the full line so words do not jump between rows as they are typed.
        float scale = subtitleScale();
        List<FormattedCharSequence> rows = font.split(Component.literal(full), subtitleWrap());
        int remaining = shown.length();
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        float lineY = y / scale;
        for (FormattedCharSequence row : rows) {
            StringBuilder text = new StringBuilder();
            row.accept((index, style, codePoint) -> {
                text.appendCodePoint(codePoint);
                return true;
            });
            int take = Math.min(remaining, text.length());
            remaining -= take;
            String visible = text.substring(0, take);
            // Placed by the whole row's width so the words do not shift sideways as they are typed.
            int rowLeft = (int) (width / 2 / scale) - font.width(row) / 2;
            graphics.drawString(font, visible, rowLeft, (int) lineY, GOLD, true);
            lineY += font.lineHeight + 2;
        }
        graphics.pose().popPose();
        return (int) (lineY * scale);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        // A click while the words are still running shows them all.
        if (button == 0 && !lineFinished()) {
            typedFrom = ticks() - 1000;
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
