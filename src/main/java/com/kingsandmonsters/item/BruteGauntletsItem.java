package com.kingsandmonsters.item;

import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/** The Bogsteel Gauntlets: a Hands-slot curio that only adds stats. It is a GeoItem so it can be drawn on the hands. */
public final class BruteGauntletsItem extends CurioItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BruteGauntletsItem(Properties properties,
                              Multimap<Holder<Attribute>, AttributeModifier> attributeModifiers,
                              List<Component> tooltipLines) {
        super(properties, attributeModifiers, tooltipLines);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
