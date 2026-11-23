package com.kingsandmonsters.tribute;

import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.entity.OgreGrunt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Who lives at a registered camp and how fast the tribe replaces them. The standard Ogre outposts
 * keep their original 3 Grunts, 2 Archers and a captain; the newer encounters each get their own
 * crew so the faction reads as more than one kind of camp.
 *
 * <p>Residents are tracked by {@link OutpostPopulationManager}, which tops the crew back up one
 * ogre at a time after a random delay inside {@code [minRespawnTicks, maxRespawnTicks]}.</p>
 */
public enum OutpostProfile {
    /** The classic outposts: a standing garrison and one captain of the camp's role. */
    OUTPOST("outpost", true, 2, 4, 5 * 24_000,
            slot(() -> ModEntities.OGRE_GRUNT.get(), 3),
            slot(() -> ModEntities.OGRE_ARCHER.get(), 2)),

    /** Tribute caravans moving goods between outposts: a spear escort and a few hands, no captain. */
    SUPPLY_CAMP("supply_camp", false, 4, 7, 0,
            slot(() -> ModEntities.OGRE_GUARD.get(), 1),
            slot(() -> ModEntities.OGRE_GRUNT.get(), 2),
            slot(() -> ModEntities.OGRE_ARCHER.get(), 1)),

    /** An outpost still being raised. No banners yet, so whichever captain arrived first runs it. */
    MOVING_CAMP("moving_camp", true, 3, 6, 7 * 24_000,
            slot(() -> ModEntities.OGRE_GRUNT.get(), 2),
            slot(() -> ModEntities.OGRE_ARCHER.get(), 1)),

    /** A camp the plague took: nothing of the tribe left, only the infected that wander it. */
    ABANDONED_CAMP("abandoned_camp", false, 6, 10, 0,
            slot(() -> ModEntities.PLAGUE_OGRE.get(), 3)),

    /** The King's Quarry: a Brute overseer, a Mage foreman, spear guards and a crew of workers. */
    QUARRY("quarry", true, 4, 8, 7 * 24_000,
            slot(() -> ModEntities.OGRE_GRUNT.get(), 4),
            slot(() -> ModEntities.OGRE_GUARD.get(), 2),
            slot(() -> ModEntities.OGRE_ARCHER.get(), 2),
            slot(() -> ModEntities.OGRE_MAGE.get(), 1)),

    /** A solitary hunter's woodland camp. */
    TRAPPER_HUT("trapper_hut", false, 8, 14, 0,
            slot(() -> ModEntities.OGRE_TRAPPER.get(), 1),
            slot(() -> ModEntities.OGRE_ARCHER.get(), 1),
            slot(() -> ModEntities.OGRE_GRUNT.get(), 1)),

    /** A satellite snare near a Trapper Hut; it has no resident roster of its own. */
    TRAPPER_TRAP("trapper_trap", false, 8, 14, 0);

    /** One kind of resident and how many of them the camp keeps. */
    public record Slot(Supplier<EntityType<? extends OgreGrunt>> type, int count) {
    }

    private final String id;
    private final boolean hasCaptain;
    private final int minRespawnTicks;
    private final int maxRespawnTicks;
    private final int captainRespawnTicks;
    private final List<Slot> slots;

    OutpostProfile(String id, boolean hasCaptain, int minRespawnMinutes, int maxRespawnMinutes,
                   int captainRespawnTicks, Slot... slots) {
        this.id = id;
        this.hasCaptain = hasCaptain;
        this.minRespawnTicks = minRespawnMinutes * 60 * 20;
        this.maxRespawnTicks = maxRespawnMinutes * 60 * 20;
        this.captainRespawnTicks = captainRespawnTicks;
        this.slots = List.of(slots);
    }

    private static Slot slot(Supplier<EntityType<? extends OgreGrunt>> type, int count) {
        return new Slot(type, count);
    }

    public String id() {
        return id;
    }

    public boolean hasCaptain() {
        return hasCaptain;
    }

    public int minRespawnTicks() {
        return minRespawnTicks;
    }

    public int maxRespawnTicks() {
        return maxRespawnTicks;
    }

    public int captainRespawnTicks() {
        return captainRespawnTicks;
    }

    public List<Slot> slots() {
        return slots;
    }

    /** Site-specific crew size; Trapper Huts deliberately use several small hunting parties. */
    public int residentTarget(Slot slot, BlockPos origin) {
        if (this != TRAPPER_HUT) {
            return slot.count();
        }
        int variant = trapperVariant(origin);
        EntityType<? extends OgreGrunt> type = slot.type().get();
        if (type == ModEntities.OGRE_TRAPPER.get()) {
            return 1;
        }
        if (type == ModEntities.OGRE_ARCHER.get()) {
            return switch (variant) {
                case 1 -> 0;
                case 2 -> 2;
                default -> 1;
            };
        }
        if (type == ModEntities.OGRE_GRUNT.get()) {
            return variant == 0 || variant == 2 ? 1 : 2;
        }
        return slot.count();
    }

    /** Mounted support varies between Trapper Huts but remains stable for a given site. */
    public int jockeyTarget(BlockPos origin) {
        int roll = trapperVariant(origin);
        return switch (this) {
            case QUARRY -> 1 + (roll & 1);
            case SUPPLY_CAMP -> roll == 0 ? 1 : 0;
            case TRAPPER_HUT -> roll < 2 ? 1 : 0;
            default -> 0;
        };
    }

    private static int trapperVariant(BlockPos origin) {
        return Math.floorMod(Long.hashCode(origin.asLong()), 4);
    }

    /** Looks up a saved id; anything unknown (older saves included) is a classic outpost. */
    public static OutpostProfile byId(String id) {
        for (OutpostProfile profile : values()) {
            if (profile.id.equals(id)) {
                return profile;
            }
        }
        return OUTPOST;
    }

    /** The structure JSON's {@code encounter} field uses the same names as the saved ids. */
    public static OutpostProfile byEncounter(String encounter) {
        return byId(encounter.toLowerCase(Locale.ROOT));
    }
}
