package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.MudsnoutBoar;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;

public final class MudsnoutBoarModel extends OgreModel<MudsnoutBoar> {
    private static final ResourceLocation SADDLED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KingsAndMonsters.MODID, "textures/entity/mudsnout_boar_saddled.png");
    // The snoutlet is a separate, smaller model with its own texture and clips.
    private static final ResourceLocation SNOUTLET_MODEL = ResourceLocation.fromNamespaceAndPath(
            KingsAndMonsters.MODID, "geo/snoutlet.geo.json");
    private static final ResourceLocation SNOUTLET_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KingsAndMonsters.MODID, "textures/entity/snoutlet.png");
    private static final ResourceLocation SNOUTLET_ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            KingsAndMonsters.MODID, "animations/snoutlet.animation.json");

    // The bones the headbutt, ram and eating clips key, eased out when the clip ends.
    private final AttackOverlayBlender overlayBlender = new AttackOverlayBlender(
            MudsnoutBoar.OVERLAY_FADE_OUT_TICKS, "head", "jaw", "body");

    public MudsnoutBoarModel() {
        // Procedural head tracking, like the ogres: the head turns to players and targets on its own.
        super("mudsnout_boar", true);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(MudsnoutBoar boar) {
        return boar.isBaby() ? SNOUTLET_MODEL : super.getModelResource(boar);
    }

    /** Once a saddle is on, the boar swaps to its saddled texture, like a horse. The model has a saddle
     *  layer over the hump; the plain texture leaves that part of the sheet empty. */
    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(MudsnoutBoar boar) {
        if (boar.isBaby()) {
            return SNOUTLET_TEXTURE;
        }
        return boar.isSaddled() || boar.hasOgreRider() ? SADDLED_TEXTURE : super.getTextureResource(boar);
    }

    @Override
    public ResourceLocation getAnimationResource(MudsnoutBoar boar) {
        return boar.isBaby() ? SNOUTLET_ANIMATIONS : super.getAnimationResource(boar);
    }

    @Override
    public void setCustomAnimations(MudsnoutBoar boar, long instanceId, AnimationState<MudsnoutBoar> animationState) {
        overlayBlender.update(this, instanceId, boar.tickCount + animationState.getPartialTick(),
                boar.isOverlayVisible(), false);
        super.setCustomAnimations(boar, instanceId, animationState);
        // The body bone's animated height is how much the boar's back rises and falls; the rider follows it.
        getBone("body").ifPresent(body -> boar.setRiderPose(body.getPosY() / 16.0F, body.getRotX(), body.getRotZ()));
    }

    /** The head is locked straight ahead through the paw-the-ground windup and the charge. */
    @Override
    protected boolean suppressProceduralHeadTracking(MudsnoutBoar boar) {
        return boar.isHeadTrackingSuppressed();
    }
}
