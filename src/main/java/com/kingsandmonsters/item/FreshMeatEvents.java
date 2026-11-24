package com.kingsandmonsters.item;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

/** The Skinning Knife's Fresh Meat: raw meat from a kill made with it is fresh (see {@link FreshFood}). */
public final class FreshMeatEvents {
    private static final TagKey<Item> FRESH_MEATS = TagKey.create(Registries.ITEM,
            new ResourceLocation(KingsAndMonsters.MODID, "fresh_meats"));

    private FreshMeatEvents() {}

    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide
                || !player.getMainHandItem().is(ModItems.SKINNING_KNIFE.get())) {
            return;
        }
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(FRESH_MEATS)) {
                FreshFood.freshen(stack);
                drop.setItem(stack);
            }
        }
    }
}
