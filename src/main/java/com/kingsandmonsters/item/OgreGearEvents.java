package com.kingsandmonsters.item;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.enchantment.ModEnchantmentEffects;
import com.kingsandmonsters.enchantment.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import com.kingsandmonsters.event.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The gifts of the gear the Ogre King sells: the helm and loincloth bonuses, the Jaw's bite and the Timber axe. */
public final class OgreGearEvents {
    private static final int LOINCLOTH_REFRESH_TICKS = 20;
    private static final int LOINCLOTH_EFFECT_TICKS = 60;
    private static final int MAX_TIMBER_LOGS = 128;
    private static final String TIMBER_ACTIVE = "KingsAndMonstersTimberActive";

    private OgreGearEvents() {}

    /** The helm adds health on top of what its armor material gives. */
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() == net.minecraft.world.entity.EquipmentSlot.HEAD
                && event.getItemStack().is(ModItems.BRUTES_HELM.get())) {
            event.addModifier(Attributes.MAX_HEALTH, ItemAttributes.modifier(
                    "brutes_helm_health", 4.0, AttributeModifier.Operation.ADDITION));
        }
    }

    /** Maintains the passive gifts of the Ogre King's wearable relics. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % LOINCLOTH_REFRESH_TICKS != 0) return;

        if (player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.OGRE_LOINCLOTH.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                    LOINCLOTH_EFFECT_TICKS, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    LOINCLOTH_EFFECT_TICKS, 0, true, false, true));
        }

        tickStomachHealing(player);
    }

    /**
     * Adds one saturation-powered pulse every two seconds. Vanilla supplies four pulses in the same
     * period, making saturated natural healing 25% faster while charging normal exhaustion per health.
     */
    private static void tickStomachHealing(Player player) {
        if (player.tickCount % 40 != 0 || !wearsStomach(player) || !player.isHurt()
                || !player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) return;
        var food = player.getFoodData();
        if (food.getFoodLevel() < 20 || food.getSaturationLevel() <= 0.0F) return;
        float healing = Math.min(food.getSaturationLevel(), 6.0F) / 6.0F;
        player.heal(healing);
        player.causeFoodExhaustion(healing * 6.0F);
    }

    /** The Ogre Stomach lets its wearer begin eating even when vanilla would reject a full player. */
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.getItem().isEdible() || !wearsStomach(player) || player.canEat(false)) {
            return;
        }
        player.startUsingItem(event.getHand());
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    /** Added to by modpacks for foods whose Absorption effect is applied outside their food data. */
    private static final TagKey<Item> STOMACH_ABSORPTION_FOODS = TagKey.create(Registries.ITEM,
            new ResourceLocation(KingsAndMonsters.MODID, "stomach_absorption_foods"));

    /**
     * Worn, the Ogre Stomach makes food instant unless it grants Absorption. Absorption foods retain
     * their normal eating time, which preserves the intended commitment behind golden apples and
     * compatible modded foods without imposing an additional cooldown.
     */
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || !event.getItem().getItem().isEdible()) {
            return;
        }
        if (!wearsStomach(player)) {
            return;
        }
        if (grantsAbsorption(event.getItem())) {
            return;
        }
        event.setDuration(1);
    }

    /** Detects component-based Absorption as well as foods tagged for custom on-consume effects. */
    private static boolean grantsAbsorption(ItemStack stack) {
        FoodProperties food = stack.getFoodProperties(null);
        return food != null && food.getEffects().stream()
                .anyMatch(effect -> effect.getFirst().getEffect() == MobEffects.ABSORPTION)
                || stack.is(STOMACH_ABSORPTION_FOODS);
    }

    public static boolean wearsStomach(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findFirstCurio(ModItems.OGRE_STOMACH.get()).isPresent())
                .orElse(false);
    }

    /** The Timber enchantment: chopping a log with it fells every log connected to it. Sneak to chop just one. */
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || event.isCanceled()
                || player.isShiftKeyDown()
                || player.getPersistentData().getBoolean(TIMBER_ACTIVE)
                || !event.getState().is(BlockTags.LOGS)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ItemStack tool = player.getMainHandItem();
        if (ModEnchantmentEffects.level(level, tool, ModEnchantments.TIMBER) <= 0) {
            return;
        }

        player.getPersistentData().putBoolean(TIMBER_ACTIVE, true);
        try {
            for (BlockPos log : connectedLogs(level, event.getPos())) {
                if (player.getMainHandItem().isEmpty()) {
                    break;
                }
                player.gameMode.destroyBlock(log);
            }
        } finally {
            player.getPersistentData().remove(TIMBER_ACTIVE);
        }
    }

    /** Every log touching the one chopped, diagonals included, so branching trees come down whole. */
    private static List<BlockPos> connectedLogs(ServerLevel level, BlockPos origin) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && found.size() < MAX_TIMBER_LOGS) {
            BlockPos at = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = at.offset(dx, dy, dz);
                        if (seen.add(next) && level.getBlockState(next).is(BlockTags.LOGS)) {
                            found.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return found;
    }
}
