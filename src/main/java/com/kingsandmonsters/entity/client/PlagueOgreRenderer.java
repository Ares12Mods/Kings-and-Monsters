package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.PlagueOgre;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PlagueOgreRenderer extends GeoEntityRenderer<PlagueOgre> {
    public PlagueOgreRenderer(EntityRendererProvider.Context context) {
        super(context, new PlagueOgreModel());
        this.shadowRadius = 0.7F;
    }
}
