package com.kingsandmonsters.client.armor;

import com.kingsandmonsters.item.OgreKingsCrownItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class OgreKingsCrownRenderer extends GeoArmorRenderer<OgreKingsCrownItem> {
    public OgreKingsCrownRenderer() {
        super(new OgreKingsCrownModel());
    }
}
