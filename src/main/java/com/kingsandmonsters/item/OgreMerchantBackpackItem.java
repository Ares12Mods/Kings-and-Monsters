package com.kingsandmonsters.item;

import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.CompoundTag;
import com.kingsandmonsters.ClientConfig;

import com.kingsandmonsters.menu.OgreMerchantBackpackMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

public class OgreMerchantBackpackItem extends CurioItem {
    public static final int SLOT_COUNT = 54;
    private static final String INVENTORY_TAG = "BackpackInventory";

    public OgreMerchantBackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer, stack, hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void openEquipped(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).resolve().flatMap(handler -> handler.findFirstCurio(
                stack -> stack.getItem() instanceof OgreMerchantBackpackItem))
                .ifPresent(result -> open(player, result.stack(), -1));
    }

    private static void open(ServerPlayer player, ItemStack stack, int lockedInventorySlot) {
        net.minecraftforge.network.NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new OgreMerchantBackpackMenu(
                        containerId, inventory, stack, lockedInventorySlot),
                Component.translatable("container.kingsandmonsters.ogre_merchant_backpack")),
                buffer -> buffer.writeVarInt(lockedInventorySlot));
    }

    public static ItemStackHandler loadInventory(ItemStack backpack) {
        boolean[] loading = {true};
        ItemStackHandler handler = new ItemStackHandler(SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                if (!loading[0]) {
                    saveInventory(backpack, this);
                }
            }

            @Override
            public boolean isItemValid(int slot, ItemStack candidate) {
                return !(candidate.getItem() instanceof OgreMerchantBackpackItem);
            }
        };
        CompoundTag data = backpack.getTag();
        if (data != null && data.contains(INVENTORY_TAG)) {
            ItemStackHandler stored = new ItemStackHandler();
            stored.deserializeNBT(data.getCompound(INVENTORY_TAG));
            int copiedSlots = Math.min(stored.getSlots(), SLOT_COUNT);
            for (int slot = 0; slot < copiedSlots; slot++) {
                handler.setStackInSlot(slot, stored.getStackInSlot(slot));
            }
            if (stored.getSlots() != SLOT_COUNT) {
                saveInventory(backpack, handler);
            }
        }
        loading[0] = false;
        return handler;
    }

    /**
     * Also called explicitly by the menu after every click and on close. ItemStackHandler's own
     * onContentsChanged callback is skipped when a stack is grown in place (stacking onto an existing
     * stack or shift-clicking), because setStackInSlot sees the same, already-mutated stack as unchanged.
     */
    public static void saveInventory(ItemStack backpack, ItemStackHandler handler) {
        backpack.getOrCreateTag().put(INVENTORY_TAG, handler.serializeNBT());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!ClientConfig.showCustomTooltips()) return;
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.backpack.slots", SLOT_COUNT)
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.kingsandmonsters.backpack.open")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
