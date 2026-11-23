package com.kingsandmonsters.client.block;

import com.kingsandmonsters.block.entity.HaunchPlatterBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class HaunchPlatterBlockRenderer extends GeoBlockRenderer<HaunchPlatterBlockEntity> {
    public HaunchPlatterBlockRenderer() {
        super(new HaunchPlatterBlockModel());
    }
}
