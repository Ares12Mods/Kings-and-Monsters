package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.item.BruteGauntletsItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class BruteGauntletsArmorRenderer extends GeoArmorRenderer<BruteGauntletsItem> {
    public BruteGauntletsArmorRenderer() {
        super(new BruteGauntletsModel());
    }
}
