package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.Map;
import java.util.WeakHashMap;

abstract class OgreModel<T extends Entity & GeoAnimatable> extends GeoModel<T> {
    private static final float MAX_HEAD_YAW = 40.0F * Mth.DEG_TO_RAD;
    private static final float MAX_HEAD_PITCH = 22.5F * Mth.DEG_TO_RAD;
    private static final float TRACKING_RESPONSE_PER_TICK = 0.18F;
    private static final float TRACKING_FADE_OUT_TICKS = 6.0F;
    private static final float TRACKING_FADE_IN_TICKS = 6.0F;

    private final ResourceLocation modelResource;
    private final ResourceLocation textureResource;
    private final ResourceLocation animationResource;
    private final boolean proceduralHeadTracking;
    private final Map<T, TrackingPose> trackingPoses = new WeakHashMap<>();

    protected OgreModel(String name) {
        this(name, false);
    }

    protected OgreModel(String name, boolean proceduralHeadTracking) {
        this.modelResource = ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "geo/" + name + ".geo.json");
        this.textureResource = ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "textures/entity/" + name + ".png");
        this.animationResource = ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "animations/" + name + ".animation.json");
        this.proceduralHeadTracking = proceduralHeadTracking;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(T animatable) {
        return modelResource;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(T animatable) {
        return textureResource;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return animationResource;
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        if (!proceduralHeadTracking) {
            return;
        }

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) {
            return;
        }

        TrackingPose pose = trackingPoses.get(animatable);
        boolean trackingSuppressed = suppressProceduralHeadTracking(animatable);
        float renderTime = animatable.tickCount + animationState.getPartialTick();
        if (pose == null) {
            pose = new TrackingPose(renderTime, trackingSuppressed ? 0.0F : 1.0F);
            trackingPoses.put(animatable, pose);
        }

        TrackingPose resolvedPose = pose;
        getBone("head").ifPresent(head -> {
            float targetYaw = Mth.clamp(modelData.netHeadYaw() * Mth.DEG_TO_RAD, -MAX_HEAD_YAW, MAX_HEAD_YAW);
            // GeckoLib 4's GeoEntityRenderer negates the entity's xRot before handing it off as
            // EntityModelData.headPitch(), so a positive headPitch here means looking UP and a
            // negative headPitch means looking DOWN — the inverse of the raw in-game xRot sign.
            float targetPitch = Mth.clamp(modelData.headPitch() * Mth.DEG_TO_RAD,
                    -getMaximumDownwardHeadPitch(), getMaximumUpwardHeadPitch());
            float fadeElapsedTicks = Mth.clamp(renderTime - resolvedPose.lastRenderTime,
                    0.0F, Math.max(TRACKING_FADE_OUT_TICKS, TRACKING_FADE_IN_TICKS));
            float trackingElapsedTicks = Math.min(fadeElapsedTicks, 1.0F);
            float smoothing = 1.0F - (float)Math.pow(1.0F - TRACKING_RESPONSE_PER_TICK, trackingElapsedTicks);

            resolvedPose.yaw = Mth.rotLerp(smoothing, resolvedPose.yaw, targetYaw);
            resolvedPose.pitch = Mth.lerp(smoothing, resolvedPose.pitch, targetPitch);
            float fadeTicks = trackingSuppressed ? TRACKING_FADE_OUT_TICKS : TRACKING_FADE_IN_TICKS;
            float weightStep = fadeTicks <= 0.0F ? 1.0F : fadeElapsedTicks / fadeTicks;
            resolvedPose.trackingWeight = Mth.clamp(resolvedPose.trackingWeight
                    + (trackingSuppressed ? -weightStep : weightStep), 0.0F, 1.0F);
            resolvedPose.lastRenderTime = renderTime;

            float easedWeight = smoothStep(resolvedPose.trackingWeight);
            head.setRotY(head.getRotY() + resolvedPose.yaw * easedWeight);
            head.setRotX(head.getRotX() + resolvedPose.pitch * easedWeight);
            head.resetStateChanges();
        });
    }

    protected boolean suppressProceduralHeadTracking(T animatable) {
        return false;
    }

    protected float getMaximumUpwardHeadPitch() {
        return MAX_HEAD_PITCH;
    }

    protected float getMaximumDownwardHeadPitch() {
        return MAX_HEAD_PITCH;
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static final class TrackingPose {
        private float yaw;
        private float pitch;
        private float trackingWeight;
        private float lastRenderTime;

        private TrackingPose(float renderTime, float trackingWeight) {
            this.lastRenderTime = renderTime;
            this.trackingWeight = trackingWeight;
        }
    }
}
