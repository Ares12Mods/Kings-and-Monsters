package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.item.OgreHelmetItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class OgreHelmetRenderer extends GeoArmorRenderer<OgreHelmetItem> {
    public OgreHelmetRenderer() {
        super(new OgreHelmetModel());
    }
}
