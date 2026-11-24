package com.kingsandmonsters.client;

import com.kingsandmonsters.entity.MudsnoutBoar;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import com.kingsandmonsters.network.ModNetwork;
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
    public static void onClientTick(com.kingsandmonsters.event.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            jumpWasDown = false;
            return;
        }
        boolean riding = player.getControlledVehicle() instanceof MudsnoutBoar;
        boolean down = riding && minecraft.screen == null && minecraft.options.keyJump.isDown();
        if (down != jumpWasDown) {
            ModNetwork.sendToServer(new com.kingsandmonsters.network.BoarChargePayload(down));
        }
        jumpWasDown = down;
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

    /**
     * While riding: attack is the headbutt, and an empty-handed right-click does nothing at all
     * (no arm swing), so the two can't be mistaken for each other.
     */
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.getControlledVehicle() instanceof MudsnoutBoar)) {
            return;
        }
        if (event.isAttack()) {
            // The boar does the attacking: the rider neither punches nor swings their arm.
            ModNetwork.sendToServer(new BoarHeadbuttPayload());
            event.setCanceled(true);
            event.setSwingHand(false);
        } else if (event.isUseItem() && player.getItemInHand(event.getHand()).isEmpty()) {
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
            float yaw = net.minecraft.util.Mth.rotLerp(event.getPartialTick(), boar.yBodyRotO, boar.yBodyRot);
            net.minecraft.world.phys.Vec3 forward = net.minecraft.world.phys.Vec3.directionFromRotation(0.0F, yaw);
            double ahead = boar.getRiderForward();
            poseStack.translate(forward.x * ahead, boar.getRiderBob() + boar.getRiderLeanDrop(), forward.z * ahead);
        }
    }
}
