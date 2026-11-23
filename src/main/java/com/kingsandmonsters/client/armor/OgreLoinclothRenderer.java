package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.item.OgreLoinclothItem;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class OgreLoinclothRenderer extends GeoArmorRenderer<OgreLoinclothItem> {
    public OgreLoinclothRenderer() {
        super(new OgreLoinclothModel());
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        if (currentSlot == EquipmentSlot.LEGS) {
            // The loincloth is a rigid waist garment. GeckoLib normally hides armorBody for the
            // leggings slot, so explicitly keep it visible instead of splitting it between two
            // independently rotating leg bones.
            setAllBonesVisible(false);
            setBoneVisible(this.body, true);
            return;
        }
        super.applyBoneVisibilityBySlot(currentSlot);
    }
}
