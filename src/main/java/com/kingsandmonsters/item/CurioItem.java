package com.kingsandmonsters.item;

import org.jetbrains.annotations.Nullable;
import com.kingsandmonsters.ClientConfig;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.UUID;
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
    private final Multimap<Attribute, AttributeModifier> attributeModifiers;
    private final List<Component> tooltipLines;

    public CurioItem(Properties properties, Multimap<Attribute, AttributeModifier> attributeModifiers, List<Component> tooltipLines) {
        super(properties);
        this.attributeModifiers = attributeModifiers;
        this.tooltipLines = tooltipLines;
    }

    public CurioItem(Properties properties, Multimap<Attribute, AttributeModifier> attributeModifiers) {
        this(properties, attributeModifiers, List.of());
    }

    public CurioItem(Properties properties) {
        this(properties, ImmutableMultimap.of(), List.of());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (!ClientConfig.showCustomTooltips()) return;
        tooltipComponents.addAll(tooltipLines);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID id, ItemStack stack) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> perSlot = ImmutableMultimap.builder();
        attributeModifiers.forEach((attribute, modifier) -> perSlot.put(attribute,
                new AttributeModifier(id, modifier.getName(), modifier.getAmount(), modifier.getOperation())));
        return perSlot.build();
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        if (!ClientConfig.showCustomTooltips()) return tooltips;
        // Our item tooltip already presents the slot in the themed header.
        return List.of();
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, ItemStack stack) {
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
