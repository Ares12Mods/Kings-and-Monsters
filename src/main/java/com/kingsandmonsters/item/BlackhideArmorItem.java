package com.kingsandmonsters.item;

import com.kingsandmonsters.ClientConfig;
import com.kingsandmonsters.client.armor.BlackhideArmorRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/** One piece of the Blackhide set. Wear all four and a long sprint builds into a Charge. */
public final class BlackhideArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BlackhideArmorItem(ArmorMaterial material, Type type, Properties properties) {
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
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.blackhide_armor.charge")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.blackhide_armor.charge.build")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.blackhide_armor.charge.ram")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.blackhide_armor.charge.weapon")
                .withStyle(ChatFormatting.BLUE));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlackhideArmorRenderer renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (renderer == null) renderer = new BlackhideArmorRenderer();
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
