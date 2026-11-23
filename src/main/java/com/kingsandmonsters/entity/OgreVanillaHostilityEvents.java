package com.kingsandmonsters.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;

/** Makes the illagers, the ravagers and the wardens proactively recognize the Ogre faction. */
public final class OgreVanillaHostilityEvents {
    private static final double SEARCH_RANGE = 32.0;

    private OgreVanillaHostilityEvents() {
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)
                || mob.level().isClientSide()
                || !(mob instanceof AbstractIllager || mob instanceof Ravager || mob instanceof Warden)
                || (mob.tickCount + mob.getId()) % 20 != 0
                || mob.getTarget() != null && mob.getTarget().isAlive()) {
            return;
        }

        OgreGrunt target = mob.level().getEntitiesOfClass(OgreGrunt.class,
                        mob.getBoundingBox().inflate(SEARCH_RANGE),
                        ogre -> ogre.isAlive() && mob.canAttack(ogre))
                .stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
        if (target == null) {
            return;
        }

        if (mob instanceof Warden warden) {
            warden.increaseAngerAt(target, 80, true);
        } else if (mob.getSensing().hasLineOfSight(target)) {
            mob.setTarget(target);
        }
    }
}
