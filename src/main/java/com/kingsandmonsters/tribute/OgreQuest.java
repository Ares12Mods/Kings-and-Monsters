package com.kingsandmonsters.tribute;

import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.entity.BogToad;
import com.kingsandmonsters.entity.PlagueOgre;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * The errands the tribe hands out. Captains give the small ones; the King gives bigger ones. An
 * errand is handing items over as tribute, killing a kind of creature, or riding a tamed boar.
 */
public enum OgreQuest {
    ZOMBIES("zombies", 25, 5, false, hunt(victim -> victim instanceof Zombie)),
    SPIDERS("spiders", 20, 5, false, hunt(victim -> victim instanceof Spider)),
    CREEPERS("creepers", 10, 6, false, hunt(victim -> victim instanceof Creeper)),
    ENDERMEN("endermen", 10, 6, false, hunt(victim -> victim instanceof EnderMan)),
    SKELETONS("skeletons", 20, 5, false, hunt(victim -> victim instanceof Skeleton)),
    TOAD_HUNT("toad_hunt", 10, 6, false, hunt(victim -> victim instanceof BogToad && !victim.isBaby())),
    PILLAGERS("pillagers", 15, 6, false, hunt(victim -> victim instanceof Pillager)),
    CAPTAIN_FEAST("captain_feast", 10, 5, false, items(OgreQuest::isCookedMeat)),

    VINDICATORS("vindicators", 15, 10, true, hunt(victim -> victim instanceof Vindicator)),
    WITCHES("witches", 5, 10, true, hunt(victim -> victim instanceof Witch)),
    PIGLIN_BRUTES("piglin_brutes", 10, 12, true, hunt(victim -> victim instanceof PiglinBrute)),
    RAVAGERS("ravagers", 2, 15, true, hunt(victim -> victim instanceof Ravager)),
    WITHER("wither", 1, 20, true, hunt(victim -> victim instanceof WitherBoss)),
    WARDEN("warden", 1, 25, true, hunt(victim -> victim instanceof Warden)),
    PLAGUE_PURGE("plague_purge", 15, 10, true, hunt(victim -> victim instanceof PlagueOgre)),
    KING_TOAD_HUNT("king_toad_hunt", 10, 10, true, hunt(victim -> victim instanceof BogToad && !victim.isBaby())),
    KING_FEAST("king_feast", 20, 8, true, items(OgreQuest::isCookedMeat)),
    // Proof of the kill: a Ghast only weeps its tears when it dies.
    GHAST_TEARS("ghast_tears", 2, 15, true, items(stack -> stack.is(Items.GHAST_TEAR)));

    public enum Kind { DELIVER, CULL }

    /** What counts toward an errand; at most one of the two is set. */
    private record Goal(Predicate<ItemStack> item, Predicate<LivingEntity> prey) {}

    private static Goal items(Predicate<ItemStack> item) {
        return new Goal(item, null);
    }

    private static Goal hunt(Predicate<LivingEntity> prey) {
        return new Goal(null, prey);
    }

    private final String id;
    private final int target;
    private final int reward;
    private final boolean king;
    private final Goal goal;

    OgreQuest(String id, int target, int reward, boolean king, Goal goal) {
        this.id = id;
        this.target = target;
        this.reward = reward;
        this.king = king;
        this.goal = goal;
    }

    private static boolean isCookedMeat(ItemStack stack) {
        return stack.is(ModItems.ROASTED_MUDSNOUT_HAUNCH.get()) || stack.is(Items.COOKED_BEEF)
                || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.COOKED_CHICKEN) || stack.is(Items.COOKED_RABBIT);
    }

    /**
     * Gold Coins are the everyday pay, scaled to how hard the errand is. Golden teeth are only
     * for the three boss-tier hunts, three or four of them each.
     */
    public List<ItemStack> bonuses() {
        return switch (this) {
            case ZOMBIES, SPIDERS -> coins(2);
            case SKELETONS, CAPTAIN_FEAST -> coins(3);
            case CREEPERS, PILLAGERS, TOAD_HUNT -> coins(4);
            case ENDERMEN -> coins(5);
            case KING_FEAST -> coins(4);
            case VINDICATORS -> coins(5);
            case WITCHES, PLAGUE_PURGE, KING_TOAD_HUNT -> coins(6);
            case PIGLIN_BRUTES -> coins(8);
            case RAVAGERS -> coinsAndTeeth(8, 3);
            case WITHER -> coinsAndTeeth(8, 4);
            case WARDEN -> coinsAndTeeth(9, 5);
            case GHAST_TEARS -> coinsAndTeeth(8, 3);
        };
    }

    private static List<ItemStack> coins(int count) {
        return List.of(new ItemStack(ModItems.GOLD_COIN.get(), count));
    }

    private static List<ItemStack> coinsAndTeeth(int coins, int teeth) {
        return List.of(new ItemStack(ModItems.GOLDEN_OGRE_TOOTH.get(), teeth),
                new ItemStack(ModItems.GOLD_COIN.get(), coins));
    }

    public String id() { return id; }
    public int target() { return target; }
    public int reward() { return reward; }
    public boolean isKingQuest() { return king; }

    /** The King's boss-tier errands (Ravagers, Wither, Warden), which he always keeps among his offers. */
    public boolean isBigQuest() { return king && reward >= 15; }

    public Kind kind() {
        return goal.item() != null ? Kind.DELIVER : Kind.CULL;
    }

    /** True when this stack counts toward a delivery errand. */
    public boolean accepts(ItemStack stack) {
        return goal.item() != null && goal.item().test(stack);
    }

    /** True when killing this creature counts toward a hunting errand. */
    public boolean isPrey(LivingEntity victim) {
        return goal.prey() != null && goal.prey().test(victim);
    }

    public static OgreQuest byOrdinal(int ordinal) {
        OgreQuest[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }

    public static List<OgreQuest> pool(boolean king, boolean outpostCaptain, boolean swampOutpost) {
        return Arrays.stream(values()).filter(quest -> {
            if (quest.king != king) return false;
            if (king) return true;
            if (quest == TOAD_HUNT) return outpostCaptain && swampOutpost;
            if (quest == PILLAGERS) return outpostCaptain && !swampOutpost;
            return true;
        }).toList();
    }
}
