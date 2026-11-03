package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreMerchant;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OgreMerchantRenderer extends GeoEntityRenderer<OgreMerchant> {
    public OgreMerchantRenderer(EntityRendererProvider.Context context) {
        super(context, new OgreMerchantModel());
        shadowRadius = 0.72F;
    }
}
