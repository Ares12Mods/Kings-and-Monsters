package com.kingsandmonsters.item;

import com.kingsandmonsters.ClientConfig;

import com.kingsandmonsters.ModBlocks;
import com.kingsandmonsters.client.armor.OgreKingsCrownRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class OgreKingsCrownItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OgreKingsCrownItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        BlockPlaceContext placementContext = new BlockPlaceContext(context);
        BlockPos pos = placementContext.getClickedPos();
        BlockState replacedState = level.getBlockState(pos);
        BlockPos supportPos = pos.below();
        if (!replacedState.canBeReplaced(placementContext)
                || !level.getBlockState(supportPos).isFaceSturdy(level, supportPos, Direction.UP)) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        if (player != null && !player.mayUseItemAt(pos, Direction.UP, context.getItemInHand())) {
            return InteractionResult.FAIL;
        }

        BlockState crownState = ModBlocks.OGRE_KINGS_CROWN_DISPLAY.get().defaultBlockState()
                .setValue(com.kingsandmonsters.block.OgreKingsCrownBlock.FACING,
                        context.getHorizontalDirection().getOpposite());
        if (!crownState.canSurvive(level, pos)
                || !level.setBlock(pos, crownState, Block.UPDATE_ALL_IMMEDIATE)) {
            return InteractionResult.FAIL;
        }

        var soundType = crownState.getSoundType(level, pos, player);
        level.playSound(player, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
        if (player == null || !player.hasInfiniteMaterials()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!ClientConfig.showCustomTooltips()) return;
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Ogre Sovereignty").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("When worn:").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Lesser ogres remain neutral until attacked")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Captains, Mages, and Brutes may challenge your claim")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("Ogre Kings always remain hostile")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private OgreKingsCrownRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T livingEntity,
                    ItemStack itemStack,
                    @Nullable EquipmentSlot equipmentSlot,
                    @Nullable HumanoidModel<T> original
            ) {
                if (renderer == null) {
                    renderer = new OgreKingsCrownRenderer();
                }
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
