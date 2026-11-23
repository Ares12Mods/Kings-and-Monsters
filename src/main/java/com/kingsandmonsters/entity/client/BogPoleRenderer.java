package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.BogPole;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class BogPoleRenderer extends GeoEntityRenderer<BogPole> {
    public BogPoleRenderer(EntityRendererProvider.Context context) {
        super(context, new BogPoleModel());
        shadowRadius = 0.3F;
    }
}
