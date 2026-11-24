package com.kingsandmonsters.menu;

import net.minecraft.network.FriendlyByteBuf;
import com.kingsandmonsters.ModMenus;
import com.kingsandmonsters.entity.MudsnoutBoar;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The horse screen with the armor slot taken out: a boar only wears a saddle. */
public class MudsnoutBoarMenu extends AbstractContainerMenu {
    private final Container saddleSlot;
    private final MudsnoutBoar boar;

    public MudsnoutBoarMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, findBoar(inventory, buffer.readVarInt()));
    }

    public MudsnoutBoarMenu(int containerId, Inventory inventory, MudsnoutBoar boar) {
        super(ModMenus.MUDSNOUT_BOAR.get(), containerId);
        this.boar = boar;
        this.saddleSlot = boar == null ? new SimpleContainer(1) : boar.getSaddleSlot();
        saddleSlot.startOpen(inventory.player);

        addSlot(new Slot(saddleSlot, 0, 8, 18) {
            @Override
            public void set(ItemStack stack) {
                boolean equipping = !hasItem() && stack.is(Items.SADDLE);
                boolean removing = hasItem() && stack.isEmpty();
                super.set(stack);
                if ((equipping || removing) && boar != null) {
                    boar.playSaddleEquipSound();
                }
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.SADDLE) && !hasItem();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    private static MudsnoutBoar findBoar(Inventory inventory, int entityId) {
        return inventory.player.level().getEntity(entityId) instanceof MudsnoutBoar boar ? boar : null;
    }

    public MudsnoutBoar getBoar() {
        return boar;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();

        if (index == 0) {
            if (!moveItemStackTo(original, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!(original.is(Items.SADDLE) && moveItemStackTo(original, 0, 1, false))) {
            // Everything else has nowhere to go but the inventory and hotbar.
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saddleSlot.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return boar == null
                || boar.isAlive() && !boar.isRemoved() && boar.distanceToSqr(player) < 64.0 || boar.hasPassenger(player);
    }
}
