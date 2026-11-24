package com.kingsandmonsters.item;

import com.kingsandmonsters.ClientConfig;

import com.kingsandmonsters.client.armor.BogIronArmorRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import software.bernie.geckolib.animatable.GeoItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class BogIronArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BogIronArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public boolean usesLeggingsModel() {
        return getType() == Type.LEGGINGS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!ClientConfig.showCustomTooltips()) return;
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.set_bonus")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.bog_iron_armor.unyielding")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.bog_iron_armor.unyielding.trigger")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.bog_iron_armor.unyielding.bonuses")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.bog_iron_armor.unyielding.bonuses_2")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.bog_iron_armor.unyielding.shockwave")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.bog_iron_armor.unyielding.cooldown")
                .withStyle(ChatFormatting.BLUE));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BogIronArmorRenderer renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (renderer == null) {
                    renderer = new BogIronArmorRenderer();
                }

                renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
