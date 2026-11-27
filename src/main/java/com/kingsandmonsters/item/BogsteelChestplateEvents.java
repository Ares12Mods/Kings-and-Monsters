package com.kingsandmonsters.item;

import com.kingsandmonsters.Config;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.entity.OgreBrute;
import com.kingsandmonsters.entity.OgreGruntCaptain;
import com.kingsandmonsters.entity.OgreGuard;
import com.kingsandmonsters.entity.OgreLord;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.kingsandmonsters.event.LivingIncomingDamageEvent;

/**
 * The Bogsteel Chestplate's Glancing Blow: now and then a physical attack skids off the plate and does
 * nothing. A short cooldown follows, so it cannot stack up into a streak of dodged hits.
 */
public final class BogsteelChestplateEvents {
    private static final String READY_AT_TAG = "KingsAndMonstersGlancingBlowReadyAt";

    private BogsteelChestplateEvents() {}

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity wearer = event.getEntity();
        boolean plated = wearer.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BOGSTEEL_CHESTPLATE.get());
        // The ogres that go to war in armor have it as a trait of their own, with no plate to wear.
        boolean armoredOgre = wearer instanceof OgreBrute || wearer instanceof OgreLord
                || wearer instanceof OgreGruntCaptain || wearer instanceof OgreGuard;
        if (!(wearer.level() instanceof ServerLevel level) || event.getAmount() <= 0.0F || !plated && !armoredOgre) {
            return;
        }
        // Only what a plate could turn: blows and arrows, not fire, magic, falls, drowning or the void,
        // all of which ignore armor.
        if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || event.getSource().getEntity() == null && event.getSource().getDirectEntity() == null) {
            return;
        }
        double chance = plated ? Config.GLANCING_BLOW_CHANCE.get() : Config.OGRE_GLANCING_BLOW_CHANCE.get();
        if (onCooldown(wearer) || wearer.getRandom().nextFloat() >= chance) {
            return;
        }

        event.setCanceled(true);
        startCooldown(wearer);
        double x = wearer.getX();
        double y = wearer.getY() + wearer.getBbHeight() * 0.65;
        double z = wearer.getZ();
        // The sparks fly off the plate on the side the blow came from, in a tight burst that fades fast.
        var attacker = event.getSource().getEntity() != null
                ? event.getSource().getEntity() : event.getSource().getDirectEntity();
        if (attacker != null) {
            double dx = attacker.getX() - x;
            double dz = attacker.getZ() - z;
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 1.0E-3) {
                double out = wearer.getBbWidth() * 0.5;
                x += dx / length * out;
                z += dz / length * out;
            }
        }
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 30, 0.12, 0.12, 0.12, 0.45);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 16, 0.1, 0.1, 0.1, 0.35);
        level.playSound(null, x, y, z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.25F);
        level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.45F, 1.8F);
        // The plate still takes the knock: a point of wear, as for any other hit.
        if (plated) {
            ItemUtil.hurt(wearer.getItemBySlot(EquipmentSlot.CHEST), 1, wearer, EquipmentSlot.CHEST);
        }
    }

    /** Players use the vanilla item cooldown, so the plate shows it in the inventory; others a simple timer. */
    private static boolean onCooldown(LivingEntity wearer) {
        if (wearer instanceof Player player) {
            return player.getCooldowns().isOnCooldown(ModItems.BOGSTEEL_CHESTPLATE.get());
        }
        return wearer.level().getGameTime() < wearer.getPersistentData().getLong(READY_AT_TAG);
    }

    private static void startCooldown(LivingEntity wearer) {
        int ticks = Config.GLANCING_BLOW_COOLDOWN_SECONDS.get() * 20;
        if (wearer instanceof Player player) {
            player.getCooldowns().addCooldown(ModItems.BOGSTEEL_CHESTPLATE.get(), ticks);
        } else {
            wearer.getPersistentData().putLong(READY_AT_TAG, wearer.level().getGameTime() + ticks);
        }
    }
}
