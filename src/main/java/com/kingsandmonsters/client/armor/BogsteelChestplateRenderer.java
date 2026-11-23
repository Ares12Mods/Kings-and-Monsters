package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.item.BogsteelChestplateItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class BogsteelChestplateRenderer extends GeoArmorRenderer<BogsteelChestplateItem> {
    public BogsteelChestplateRenderer() {
        super(new BogsteelChestplateModel());
    }
}
