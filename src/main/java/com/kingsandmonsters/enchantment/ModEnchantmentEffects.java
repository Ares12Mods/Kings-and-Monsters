package com.kingsandmonsters.enchantment;

import com.kingsandmonsters.Config;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Java-side mechanics for effects that cannot be expressed by vanilla enchantment components. */
public final class ModEnchantmentEffects {
    private static final TagKey<Item> TYRANT_WEAPONS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("kingsandmonsters", "enchantable/tyrant_weapons"));
    private ModEnchantmentEffects() {}

    public static int level(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return stack.getEnchantmentLevel(enchantment);
    }

    public static float heavyThrowDamage(float baseDamage, int level) {
        return baseDamage + (float) (Config.HEAVY_THROW_DAMAGE_PER_LEVEL.get() * level);
    }

    public static float heavyThrowSpeed(float baseSpeed, int level) {
        double multiplier = Math.max(0.35, 1.0 - Config.HEAVY_THROW_SPEED_PENALTY_PER_LEVEL.get() * level);
        return (float) (baseSpeed * multiplier);
    }

    public static double heavyThrowKnockback(int level) {
        return Config.HEAVY_THROW_KNOCKBACK_PER_LEVEL.get() * level;
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof Player attacker) || event.getSource().getDirectEntity() != attacker) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        LivingEntity target = event.getEntity();
        if (weapon.is(TYRANT_WEAPONS)) {
            int tyrantLevel = level(attacker.level(), weapon, ModEnchantments.TYRANT);
            if (tyrantLevel > 0) {
                double radius = Config.TYRANT_SEARCH_RADIUS.get();
                AABB area = attacker.getBoundingBox().inflate(radius);
                int hostiles = attacker.level().getEntitiesOfClass(Monster.class, area,
                        // Ogres at peace with the attacker are not a crowd to fight, so they add no damage.
                        mob -> mob.isAlive() && mob != target
                                && !(mob instanceof com.kingsandmonsters.entity.OgreGrunt ogre
                                        && !ogre.canAttack(attacker))).size();
                // Fully surrounded, each level tops out at 1.6 / 2.75 / 3.75 bonus damage. Sharpness V adds 3.0,
                // so a full crowd with Tyrant III hits a quarter harder than the best vanilla damage enchantment.
                double damagePerHostile = switch (tyrantLevel) {
                    case 1 -> 0.4;
                    case 2 -> 0.55;
                    default -> 0.625;
                };
                int hostileCap = switch (tyrantLevel) {
                    case 1 -> 4;
                    case 2 -> 5;
                    default -> 6;
                };
                double bonus = Math.min(hostiles, hostileCap) * damagePerHostile;
                event.setAmount(event.getAmount() + (float) bonus);
            }
        }
    }
}
