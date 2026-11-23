package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.item.BoghideArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class BoghideArmorRenderer extends GeoArmorRenderer<BoghideArmorItem> {
    public BoghideArmorRenderer() {
        super(new BoghideArmorModel());
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
