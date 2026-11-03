package com.kingsandmonsters.item;

import com.kingsandmonsters.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/** Isolated Creative-only kill switch for rapidly cycling development encounters. */
public final class CreativeDestroyerEvents {
    private CreativeDestroyerEvents() {
    }

    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()
                || !player.isCreative()
                || !player.getMainHandItem().is(ModItems.CREATIVE_DESTROYER.get())
                || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        // kill() uses the target's own death lifecycle (including the Ogre King's
        // authored delayed death) without passing through normal damage caps.
        target.kill();
        event.setCanceled(true);
    }
}
