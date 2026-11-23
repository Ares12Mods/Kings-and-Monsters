package com.kingsandmonsters.entity.client;

import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Eases an ogre's upper body out of an attack that was thrown over the walk. While the attack
 * plays, its rendered pose is remembered; once it lets go, that pose fades out into whatever the
 * walk has the bones doing, so the arms don't snap when the clip ends.
 */
final class AttackOverlayBlender {
    private static final int VALUES_PER_BONE = 6;
    private static final int MAX_TRACKED = 32;

    private final String[] bones;
    private final float fadeOutTicks;
    private final Map<Long, float[]> poses = new HashMap<>();
    private final Map<Long, Fade> fades = new HashMap<>();

    AttackOverlayBlender(float fadeOutTicks, String... bones) {
        this.fadeOutTicks = fadeOutTicks;
        this.bones = bones;
    }

    /** Run before the model's own head tracking, which adds its offset on top of the pose. */
    void update(GeoModel<?> model, long instanceId, float now, boolean overlayVisible, boolean fullBodyOverride) {
        if (fullBodyOverride) {
            // Death, a leap or a scripted scene replaces the whole pose; nothing to blend.
            poses.remove(instanceId);
            fades.remove(instanceId);
            return;
        }

        if (overlayVisible) {
            fades.remove(instanceId);
            if (poses.size() > MAX_TRACKED) {
                poses.clear();
            }
            capture(model, poses.computeIfAbsent(instanceId, id -> new float[bones.length * VALUES_PER_BONE]));
            return;
        }

        float[] finished = poses.remove(instanceId);
        if (finished != null) {
            if (fades.size() > MAX_TRACKED) {
                fades.clear();
            }
            fades.put(instanceId, new Fade(finished, now));
        }
        Fade fade = fades.get(instanceId);
        if (fade == null) {
            return;
        }

        float progress = (now - fade.startTime) / fadeOutTicks;
        if (progress < 0.0F || progress >= 1.0F) {
            fades.remove(instanceId);
            return;
        }
        // Weight of the attack's last pose, easing from 1 down to 0.
        float eased = progress * progress * (3.0F - 2.0F * progress);
        blendToward(model, fade.pose, 1.0F - eased);
    }

    private void capture(GeoModel<?> model, float[] pose) {
        for (int i = 0; i < bones.length; i++) {
            GeoBone bone = model.getBone(bones[i]).orElse(null);
            if (bone == null) {
                continue;
            }
            int at = i * VALUES_PER_BONE;
            pose[at] = bone.getRotX();
            pose[at + 1] = bone.getRotY();
            pose[at + 2] = bone.getRotZ();
            pose[at + 3] = bone.getPosX();
            pose[at + 4] = bone.getPosY();
            pose[at + 5] = bone.getPosZ();
        }
    }

    private void blendToward(GeoModel<?> model, float[] pose, float weight) {
        for (int i = 0; i < bones.length; i++) {
            GeoBone bone = model.getBone(bones[i]).orElse(null);
            if (bone == null) {
                continue;
            }
            int at = i * VALUES_PER_BONE;
            bone.setRotX(Mth.lerp(weight, bone.getRotX(), pose[at]));
            bone.setRotY(Mth.lerp(weight, bone.getRotY(), pose[at + 1]));
            bone.setRotZ(Mth.lerp(weight, bone.getRotZ(), pose[at + 2]));
            bone.setPosX(Mth.lerp(weight, bone.getPosX(), pose[at + 3]));
            bone.setPosY(Mth.lerp(weight, bone.getPosY(), pose[at + 4]));
            bone.setPosZ(Mth.lerp(weight, bone.getPosZ(), pose[at + 5]));
        }
    }

    private record Fade(float[] pose, float startTime) {
    }
}
