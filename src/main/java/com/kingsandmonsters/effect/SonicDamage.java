package com.kingsandmonsters.effect;

import com.kingsandmonsters.ModItems;
import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Shared hearing-protection check used by the Ogre King's roar.
 */
public final class SonicDamage {
    private SonicDamage() {}

    /** Shared check used by the Ogre King's roar. */
    public static boolean isWearingPaddedEarmuffs(LivingEntity target) {
        return CuriosApi.getCuriosInventory(target)
                .map(inventory -> inventory.findFirstCurio(
                        stack -> stack.is(ModItems.PADDED_EARMUFFS.get())).isPresent())
                .orElse(false);
    }
}
