package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.BogToad;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class BogToadRenderer extends GeoEntityRenderer<BogToad> {
    public BogToadRenderer(EntityRendererProvider.Context context) {
        super(context, new BogToadModel());
        this.shadowRadius = 0.55F;
    }
}
