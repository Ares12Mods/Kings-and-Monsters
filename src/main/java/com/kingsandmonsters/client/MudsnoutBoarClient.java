package com.kingsandmonsters.client;

import com.kingsandmonsters.network.BoarChargePayload;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.kingsandmonsters.entity.MudsnoutBoar;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.kingsandmonsters.network.BoarHeadbuttPayload;

/** Client-only pieces of riding the Mudsnout Boar, kept out of the common entity class. */
public final class MudsnoutBoarClient {
    private MudsnoutBoarClient() {}

    private static boolean jumpWasDown;

    /**
     * While riding, the jump key (space) charges the boar: the press starts the wind-up and the release
     * launches it, so how long it is held sets how far it goes. The server decides whether the boar is
     * ready; this only reports the press and the release, once each rather than every tick it is held.
     * Getting off or opening a menu while it is held counts as a release.
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            jumpWasDown = false;
            return;
        }
        boolean riding = player.getControlledVehicle() instanceof MudsnoutBoar;
        boolean down = riding && minecraft.screen == null && minecraft.options.keyJump.isDown();
        if (down != jumpWasDown) {
            PacketDistributor.sendToServer(new BoarChargePayload(down));
        }
        jumpWasDown = down;
    }

    /**
     * While riding, a click of the attack key is the boar's headbutt, whatever the rider holds. It is
     * taken before the game reads the key, so a combat mod can't turn it into a weapon swing, and a held
     * spear stays free to throw.
     */
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null
                || !(minecraft.player.getControlledVehicle() instanceof MudsnoutBoar)) {
            return;
        }
        boolean clicked = false;
        while (minecraft.options.keyAttack.consumeClick()) {
            clicked = true;
        }
        minecraft.options.keyAttack.setDown(false);
        if (clicked) {
            PacketDistributor.sendToServer(new BoarHeadbuttPayload());
        }
    }

    /** Whether the local player is holding their sprint key. */
    public static boolean isSprintHeld() {
        return Minecraft.getInstance().options.keySprint.isDown();
    }

    /** The inventory renders a second live copy of the boar; that preview must not emit world footfalls. */
    public static boolean isInventoryPreviewOpen(MudsnoutBoar boar) {
        return Minecraft.getInstance().screen instanceof MudsnoutBoarScreen screen
                && screen.getMenu().getBoar() == boar;
    }

    /** While riding, an empty-handed right-click does nothing at all, so it can't be mistaken for an attack. */
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.getControlledVehicle() instanceof MudsnoutBoar)) {
            return;
        }
        if (event.isUseItem() && player.getItemInHand(event.getHand()).isEmpty()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /**
     * A rider moves with the boar's body: the model's own animated body bone gives the bob of each
     * clip, and the rider's whole model is shifted by the same amount every frame.
     */
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof Player rider && rider.getVehicle() instanceof MudsnoutBoar boar) {
            PoseStack poseStack = event.getPoseStack();
            // The back rises and falls with each clip, and lunges forward and rears back with the
            // headbutt and the ram as the body pitches.
            float yaw = Mth.rotLerp(event.getPartialTick(), boar.yBodyRotO, boar.yBodyRot);
            Vec3 forward = Vec3.directionFromRotation(0.0F, yaw);
            double ahead = boar.getRiderForward();
            poseStack.translate(forward.x * ahead, boar.getRiderBob() + boar.getRiderLeanDrop(), forward.z * ahead);
        }
    }
}
