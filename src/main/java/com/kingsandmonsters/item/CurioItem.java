package com.kingsandmonsters.item;

import com.kingsandmonsters.ClientConfig;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class CurioItem extends Item implements ICurioItem {
    private final Multimap<Holder<Attribute>, AttributeModifier> attributeModifiers;
    private final List<Component> tooltipLines;

    public CurioItem(Properties properties, Multimap<Holder<Attribute>, AttributeModifier> attributeModifiers, List<Component> tooltipLines) {
        super(properties);
        this.attributeModifiers = attributeModifiers;
        this.tooltipLines = tooltipLines;
    }

    public CurioItem(Properties properties, Multimap<Holder<Attribute>, AttributeModifier> attributeModifiers) {
        this(properties, attributeModifiers, List.of());
    }

    public CurioItem(Properties properties) {
        this(properties, ImmutableMultimap.of(), List.of());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (!ClientConfig.showCustomTooltips()) return;
        tooltipComponents.addAll(tooltipLines);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        return attributeModifiers;
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, TooltipContext context, ItemStack stack) {
        if (!ClientConfig.showCustomTooltips()) return tooltips;
        // Our item tooltip already presents the slot in the themed header.
        return List.of();
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, TooltipContext context, ItemStack stack) {
        if (!ClientConfig.showCustomTooltips()) return tooltips;
        // Attribute bonuses are included in our structured "When worn" section.
        return List.of();
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return CuriosApi.getCuriosInventory(slotContext.entity())
                .map(handler -> handler.findCurios(equipped -> equipped.is(stack.getItem())).stream()
                        .noneMatch(result -> !isSameSlot(slotContext, result.slotContext())))
                .orElse(true);
    }

    private static boolean isSameSlot(SlotContext left, SlotContext right) {
        return left.identifier().equals(right.identifier())
                && left.index() == right.index()
                && left.cosmetic() == right.cosmetic();
    }
}
