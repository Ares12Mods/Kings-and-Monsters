package com.kingsandmonsters.item;

import com.kingsandmonsters.ClientConfig;
import com.kingsandmonsters.block.CandleHelmetBlock;
import com.kingsandmonsters.client.armor.CandleHelmetRenderer;
import com.kingsandmonsters.enchantment.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * Mining/cave-combat helmet. Light is produced by {@link com.kingsandmonsters.light.CandleHelmetLight};
 * the visible flame is emitted client-side above the stationary modelled wick. Environmental
 * lighting and the visual flame particle are independent features.
 */
public class CandleHelmetItem extends ArmorItem implements GeoItem {
    /** Light level with no Brightwick. */
    public static final int BASE_LIGHT_LEVEL = 11;
    public static final int MAX_BRIGHTWICK_LEVEL = 3;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final String LIT_TAG = "Lit";

    /** The wick is lit only if flint and steel was used on a placed helmet; it stays lit in the inventory. */
    public static boolean isLit(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.getUnsafe().getBoolean(LIT_TAG);
    }

    public static void setLit(ItemStack stack, boolean lit) {
        if (lit) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(LIT_TAG, true));
        } else if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(LIT_TAG));
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        }
    }

    /** Right-clicking a block places the helmet down (like a carved pumpkin); right-clicking air still wears it. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return CandleHelmetBlock.tryPlace(context);
    }

    public CandleHelmetItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties);
    }

    /** 11 / 12 / 13 / 14 for Brightwick none / I / II / III. */
    public static int lightLevel(ItemStack stack, HolderLookup.Provider registries) {
        int brightwick = registries.lookup(Registries.ENCHANTMENT)
                .flatMap(lookup -> lookup.get(ModEnchantments.BRIGHTWICK))
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel((Holder<Enchantment>) holder, stack))
                .orElse(0);
        return BASE_LIGHT_LEVEL + Math.max(0, Math.min(MAX_BRIGHTWICK_LEVEL, brightwick));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!ClientConfig.showCustomTooltips()) return;
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.flavor.candle_helmet")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(isLit(stack)
                        ? "tooltip.kingsandmonsters.candle_helmet.lit"
                        : "tooltip.kingsandmonsters.candle_helmet.unlit")
                .withStyle(isLit(stack) ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.candle_helmet.light")
                .withStyle(ChatFormatting.BLUE));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private CandleHelmetRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T livingEntity,
                    ItemStack itemStack,
                    @Nullable EquipmentSlot equipmentSlot,
                    @Nullable HumanoidModel<T> original
            ) {
                if (renderer == null) {
                    renderer = new CandleHelmetRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The crossed planes in the supplied model are the wick, so they remain stationary.
        // A full-bright vanilla flame particle supplies the actual animated flame.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
