package com.kingsandmonsters.client.block;

import com.kingsandmonsters.block.entity.OgreKingsCrownBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class OgreKingsCrownBlockRenderer extends GeoBlockRenderer<OgreKingsCrownBlockEntity> {
    public OgreKingsCrownBlockRenderer() {
        super(new OgreKingsCrownBlockModel());
    }
}
