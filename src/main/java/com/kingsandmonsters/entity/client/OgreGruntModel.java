package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.OgreGrunt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;

public class OgreGruntModel extends OgreModel<OgreGrunt> {
    // The bones the swings thrown over the combat walk animate.
    private final AttackOverlayBlender overlayBlender = new AttackOverlayBlender(
            OgreGrunt.ATTACK_OVERLAY_FADE_OUT_TICKS,
            "body", "head", "jaw", "left_eyebrow", "right_eyebrow", "left_arm", "right_arm");

    private static final ResourceLocation BASE_MODEL = new ResourceLocation(
            KingsAndMonsters.MODID, "geo/ogre_grunt.geo.json");
    private static final ResourceLocation HAT_MODEL = new ResourceLocation(
            KingsAndMonsters.MODID, "geo/ogre_grunt_hat.geo.json");
    private static final ResourceLocation[] VARIANT_TEXTURES = {
            texture("ogre_grunt"),
            texture("ogre_grunt_hat"),
            texture("ogre_grunt_boots"),
            texture("ogre_grunt_hat_chestplate"),
            texture("ogre_grunt_full_blackhide")
    };

    public OgreGruntModel() {
        super("ogre_grunt", true);
    }

    @Override
    public void setCustomAnimations(OgreGrunt grunt, long instanceId, AnimationState<OgreGrunt> animationState) {
        overlayBlender.update(this, instanceId, grunt.tickCount + animationState.getPartialTick(),
                grunt.isAttackOverlayVisible(), false);
        super.setCustomAnimations(grunt, instanceId, animationState);
    }

    @Override
    protected float getMaximumDownwardHeadPitch() {
        return 12.0F * Mth.DEG_TO_RAD;
    }

    @Override
    public ResourceLocation getModelResource(OgreGrunt grunt) {
        return switch (grunt.getArmorVariant()) {
            case 1, 3, 4 -> HAT_MODEL;
            default -> BASE_MODEL;
        };
    }

    @Override
    public ResourceLocation getTextureResource(OgreGrunt grunt) {
        return VARIANT_TEXTURES[Mth.clamp(grunt.getArmorVariant(), 0, VARIANT_TEXTURES.length - 1)];
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(
                KingsAndMonsters.MODID, "textures/entity/" + name + ".png");
    }
}
