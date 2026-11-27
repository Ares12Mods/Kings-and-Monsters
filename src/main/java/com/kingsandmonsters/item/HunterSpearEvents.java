package com.kingsandmonsters.item;

import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.effect.CombatEffects;
import com.kingsandmonsters.effect.BleedingEffect;
import com.kingsandmonsters.enchantment.ModEnchantmentEffects;
import com.kingsandmonsters.enchantment.ModEnchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class HunterSpearEvents {

    private HunterSpearEvents() {}

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        // A Guard holds his spear through every swing, but only the stab hits with it.
        if (attacker instanceof com.kingsandmonsters.entity.OgreGuard guard
                && event.getSource().getDirectEntity() == guard && !guard.isSpearStab()) {
            return;
        }

        ItemStack weapon;
        boolean thrownTrident = event.getSource().getDirectEntity() instanceof ThrownTrident trident;
        if (thrownTrident) weapon = ((ThrownTrident) event.getSource().getDirectEntity()).getWeaponItem();
        else if (event.getSource().getDirectEntity() == attacker) weapon = attacker.getMainHandItem();
        else return;
        if (weapon.is(ModItems.HUNTERS_SPEAR.get())) {
            applyHunterSpearBleeding(event.getEntity(), attacker, weapon);
        }
        // Heavy Throw works on any thrown trident, vanilla ones included, not only the Hunter's Spear.
        if (thrownTrident) {
            int heavyThrowLevel = ModEnchantmentEffects.level(attacker.level(), weapon, ModEnchantments.HEAVY_THROW);
            if (heavyThrowLevel > 0) {
                var away = event.getEntity().position().subtract(attacker.position()).normalize()
                        .scale(ModEnchantmentEffects.heavyThrowKnockback(heavyThrowLevel));
                event.getEntity().push(away.x, 0.1, away.z);
                event.getEntity().hurtMarked = true;
            }
        }
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ThrownTrident trident)) return;
        ItemStack weapon = trident.getWeaponItem();
        int level = ModEnchantmentEffects.level(event.getLevel(), weapon, ModEnchantments.HEAVY_THROW);
        if (level <= 0) return;
        trident.setBaseDamage(ModEnchantmentEffects.heavyThrowDamage((float) trident.getBaseDamage(), level));
        trident.setDeltaMovement(trident.getDeltaMovement().scale(
                ModEnchantmentEffects.heavyThrowSpeed(1.0F, level)));
    }

    /** A spear opens a Lesser wound on every damaging hit; Barbed I makes it Bleeding and Barbed II Greater. */
    public static void applyHunterSpearBleeding(LivingEntity target, LivingEntity source, ItemStack weapon) {
        int barbed = ModEnchantmentEffects.level(source.level(), weapon, ModEnchantments.BARBED);
        BleedingEffect.Severity severity = barbed >= 2 ? BleedingEffect.Severity.GREATER
                : barbed == 1 ? BleedingEffect.Severity.NORMAL : BleedingEffect.Severity.LESSER;
        CombatEffects.applyBleeding(target, severity, source);
    }

}
