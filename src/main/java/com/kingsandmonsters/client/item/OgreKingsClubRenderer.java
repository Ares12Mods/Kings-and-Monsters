package com.kingsandmonsters.client.item;

import com.kingsandmonsters.item.OgreKingsClubItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class OgreKingsClubRenderer extends GeoItemRenderer<OgreKingsClubItem> {
    public OgreKingsClubRenderer() {
        super(new OgreKingsClubModel());
    }
}
