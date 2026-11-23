package com.kingsandmonsters.entity;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class OgreDamageCapEvents {

    // No single ogre hit should take more than this fraction of a player's max health, whatever the
    // armor; otherwise the brute's heavier attacks can one-shot an unarmored player. Fires after
    // armor and shield reductions, so it is a final ceiling and changes nothing about armor or blocking.
    private static final float MAX_SINGLE_HIT_FRACTION = 0.7F;

    private OgreDamageCapEvents() {}

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Indirect sources (arrows, the mage's bolt) don't consistently put the ogre in the same
        // slot — check both the causing and direct entity to catch every attack type.
        if (!(event.getSource().getEntity() instanceof OgreGrunt) && !(event.getSource().getDirectEntity() instanceof OgreGrunt)) {
            return;
        }

        float cap = player.getMaxHealth() * MAX_SINGLE_HIT_FRACTION;
        if (event.getNewDamage() > cap) {
            event.setNewDamage(cap);
        }
    }
}
