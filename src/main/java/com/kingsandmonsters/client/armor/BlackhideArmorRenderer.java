package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.item.BlackhideArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class BlackhideArmorRenderer extends GeoArmorRenderer<BlackhideArmorItem> {
    public BlackhideArmorRenderer() {
        super(new BlackhideArmorModel());
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        super.applyBoneVisibilityBySlot(currentSlot);

        // The leggings model carries a waist cube in its body bone.
        if (currentSlot == EquipmentSlot.LEGS) {
            setBoneVisible(this.body, true);
        }
    }
}
