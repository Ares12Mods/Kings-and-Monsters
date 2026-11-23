package com.kingsandmonsters.item;

import com.kingsandmonsters.KingsAndMonsters;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/**
 * The Blackhide set bonus, Charge. With all four pieces on, sprinting builds momentum: the longer
 * the run, the faster the wearer goes. At full momentum the Charge effect is granted, and the next
 * thing the wearer runs into, or hits, is thrown a long way off. Hitting it with a weapon throws it
 * further and hits harder. Either impact spends the Charge, and the run has to be built up again.
 */
public final class BlackhideSet {
    /** Ticks of unbroken sprinting it takes to reach full momentum. */
    public static final int BUILD_TICKS = 3 * 20;
    /** Momentum lost per tick while not sprinting, so a stumble does not throw away a whole run. */
    public static final int DECAY_PER_TICK = 3;
    /** Extra speed at full momentum. The Charge effect itself adds none, so this is the whole bonus. */
    public static final double MAX_MOMENTUM_SPEED = 0.25;
    /** The Charge outlasts a broken sprint by this long, so a swing right after a stop still counts. */
    public static final int CHARGE_GRACE_TICKS = 10;
    /** How long a Charge can be held before the wearer is winded. */
    public static final int CHARGE_MAX_TICKS = 12 * 20;
    /** How long a winded wearer cannot build momentum. */
    public static final int WINDED_TICKS = 3 * 20;

    /** Each worn piece turns away this share of arrows: 20% with all four. */
    public static final float MISSILE_DEFLECT_PER_PIECE = 0.05F;

    /** Rams that a Charge can make: the first is charged through, the second ends it and winds the wearer. */
    public static final int RAMS_PER_CHARGE = 2;
    public static final float RAM_DAMAGE = 4.0F;
    public static final double RAM_KNOCKBACK = 1.7;
    public static final double RAM_LIFT = 0.55;
    public static final float WEAPON_DAMAGE_MULTIPLIER = 1.25F;
    public static final double WEAPON_KNOCKBACK = 2.6;
    public static final double WEAPON_LIFT = 0.7;

    private static final TagKey<Item> BLACKHIDE_ARMOR = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "blackhide_armor"));

    private BlackhideSet() {
    }

    public static int piecesWorn(LivingEntity entity) {
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (entity.getItemBySlot(slot).is(BLACKHIDE_ARMOR)) pieces++;
        }
        return pieces;
    }

    /** Only arrows can be turned aside: thrown tridents and spears are heavy enough to pierce the hide. */
    public static boolean isDeflectable(net.minecraft.world.entity.Entity missile) {
        return missile instanceof net.minecraft.world.entity.projectile.AbstractArrow
                && !(missile instanceof net.minecraft.world.entity.projectile.ThrownTrident)
                && !(missile instanceof com.kingsandmonsters.entity.OgreSpear);
    }

    public static boolean hasFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(BLACKHIDE_ARMOR)
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(BLACKHIDE_ARMOR)
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(BLACKHIDE_ARMOR)
                && entity.getItemBySlot(EquipmentSlot.FEET).is(BLACKHIDE_ARMOR);
    }
}
