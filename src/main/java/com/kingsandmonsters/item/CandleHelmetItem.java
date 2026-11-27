package com.kingsandmonsters.item;

import net.minecraft.world.level.Level;
import com.kingsandmonsters.ClientConfig;
import com.kingsandmonsters.block.CandleHelmetBlock;
import com.kingsandmonsters.client.armor.CandleHelmetRenderer;
import com.kingsandmonsters.enchantment.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
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
        return stack.hasTag() && stack.getTag().getBoolean(LIT_TAG);
    }

    public static void setLit(ItemStack stack, boolean lit) {
        if (lit) {
            stack.getOrCreateTag().putBoolean(LIT_TAG, true);
        } else if (stack.hasTag()) {
            stack.getTag().remove(LIT_TAG);
            if (stack.getTag().isEmpty()) stack.setTag(null);
        }
    }

    /** Right-clicking a block places the helmet down (like a carved pumpkin); right-clicking air still wears it. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return CandleHelmetBlock.tryPlace(context);
    }

    public CandleHelmetItem(ArmorMaterial material, Properties properties) {
        super(material, Type.HELMET, properties);
    }

    /** 11 / 12 / 13 / 14 for Brightwick none / I / II / III. */
    public static int lightLevel(ItemStack stack, HolderLookup.Provider registries) {
        int brightwick = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.BRIGHTWICK.get(), stack);
        return BASE_LIGHT_LEVEL + Math.max(0, Math.min(MAX_BRIGHTWICK_LEVEL, brightwick));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
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
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CandleHelmetRenderer renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (renderer == null) {
                    renderer = new CandleHelmetRenderer();
                }
                renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
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
