package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreBrute;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OgreBruteRenderer extends GeoEntityRenderer<OgreBrute> {

    public OgreBruteRenderer(EntityRendererProvider.Context context) {
        super(context, new OgreBruteModel());
        this.shadowRadius = 0.95f;
    }
}
