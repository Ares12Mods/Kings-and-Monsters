package com.kingsandmonsters.entity;

import net.minecraft.util.Mth;

/**
 * One rulebook for how hard the ogres hit.
 *
 * <p>Every ogre has a tier, the raw damage (before armor) of its ordinary blow, and every attack
 * is one of four kinds that scale that: a light jab, a standard swing, a heavy slam or a crushing
 * one. Each step up the ladder hits clearly harder than the one below, and a heavy blow from a
 * lower tier never outdamages a standard one from the tier above. The ogres' damage attribute can
 * still be changed in the config: the values here are turned into multipliers on the default
 * attribute, so a changed attribute scales everything together.</p>
 *
 * <p>Area attacks share one rule too: the inner third of an area takes the full hit and the damage
 * fades toward the rim, to {@link #ARC_EDGE_SHARE} for swings and sweeps and
 * {@link #BLAST_EDGE_SHARE} for slams and shockwaves. Ripples (the traveling ground wave) keep
 * their own rules.</p>
 */
final class OgreDamage {
    /** An ogre's tier: its ordinary blow's raw damage and the damage attribute it ships with. */
    enum Tier {
        GRUNT(12.0F, 10.5F),
        // The ranged ogres. Their ordinary blow is the shot, throw or bolt; their melee is a stopgap.
        ARCHER(11.0F, 5.0F),
        GUARD(10.0F, 7.0F),
        MAGE(12.0F, 6.0F),
        // The Mudsnout Boar: a wild plains beast, hit for hit with a Grunt's smack, and a hard charge.
        BOAR(9.0F, 9.0F),
        CAPTAIN(16.0F, 10.5F),
        // The Trapper hits a little under the Brute: quick, mean swings rather than slams.
        TRAPPER(17.0F, 8.0F),
        BRUTE(19.0F, 11.0F),
        KING(24.0F, 18.0F);

        private final float standardDamage;
        private final float defaultAttribute;

        Tier(float standardDamage, float defaultAttribute) {
            this.standardDamage = standardDamage;
            this.defaultAttribute = defaultAttribute;
        }
    }

    /** How hard a single attack hits compared with its tier's ordinary blow. */
    enum Blow {
        // The weakest blow: a ranged ogre's stopgap melee, or one tick of a damage-over-time zone.
        MINOR(0.5F),
        LIGHT(0.75F),
        STANDARD(1.0F),
        HEAVY(1.3F),
        CRUSHING(1.8F);

        private final float factor;

        Blow(float factor) {
            this.factor = factor;
        }
    }

    /** Share of the damage left at the rim of a swing, a sweep or a lane. */
    static final float ARC_EDGE_SHARE = 0.45F;
    /** Share of the damage left at the rim of a slam or a shockwave. */
    static final float BLAST_EDGE_SHARE = 0.3F;
    // The inner part of an area attack that still takes the full hit.
    private static final double FULL_DAMAGE_FRACTION = 0.35;

    private OgreDamage() {
    }

    /** Raw damage, before armor, of this kind of blow from this tier. */
    static float raw(Tier tier, Blow blow) {
        return tier.standardDamage * blow.factor;
    }

    /** The same blow as a multiplier on the ogre's default damage attribute. */
    static float multiplier(Tier tier, Blow blow) {
        return raw(tier, blow) / tier.defaultAttribute;
    }

    /** How much of an area attack's damage reaches something this far from its centre. */
    static float falloff(double distance, double radius, float edgeShare) {
        if (radius <= 0.0) {
            return 1.0F;
        }
        double t = Mth.clamp((distance / radius - FULL_DAMAGE_FRACTION) / (1.0 - FULL_DAMAGE_FRACTION), 0.0, 1.0);
        return (float) Mth.lerp(t, 1.0, edgeShare);
    }
}
