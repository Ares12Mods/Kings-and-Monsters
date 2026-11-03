package com.kingsandmonsters.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Shared instantaneous white ground shockwave used by Ogre impacts. */
final class StaticShockwaveParticles {
    private StaticShockwaveParticles() {}

    static void spawn(ServerLevel level, Vec3 center, float radius) {
        int points = Math.max(18, (int) (radius * 10.0F));
        double y = center.y + 0.12;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.CLOUD, x, y, z,
                    1, 0.04, 0.03, 0.04, 0.01);
        }

        level.sendParticles(ParticleTypes.POOF, center.x, y + 0.05, center.z,
                14, radius * 0.35, 0.08, radius * 0.35, 0.02);
    }
}
