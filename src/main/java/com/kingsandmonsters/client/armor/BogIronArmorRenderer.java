package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.item.BogIronArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class BogIronArmorRenderer extends GeoArmorRenderer<BogIronArmorItem> {
    public BogIronArmorRenderer() {
        super(new BogIronArmorModel());
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        super.applyBoneVisibilityBySlot(currentSlot);

        if (currentSlot == EquipmentSlot.LEGS) {
            setBoneVisible(this.body, true);
        }
    }
}
