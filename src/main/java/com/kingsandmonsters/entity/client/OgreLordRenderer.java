package com.kingsandmonsters.entity.client;

import com.kingsandmonsters.entity.OgreLord;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OgreLordRenderer extends GeoEntityRenderer<OgreLord> {
    // Preview scale: 5 blocks tall instead of roughly 6. Restore to 1.0F to reverse.
    private static final float OGRE_KING_SCALE = 5.0F / 6.0F;

    public OgreLordRenderer(EntityRendererProvider.Context context) {
        super(context, new OgreLordModel());
        this.shadowRadius = 1.3f;
        withScale(OGRE_KING_SCALE);
        // The King's Cleaver render layer is intentionally left out for this release — he doesn't
        // carry or use it. The item itself (ModItems.KINGS_CLEAVER) still exists as fort loot.
    }

    @Override
    protected float getDeathMaxRotation(OgreLord lord) {
        // The custom death animation already performs the King's full-body fall. Prevent
        // GeoEntityRenderer from adding its standard sideways death roll on top of it.
        return lord.isDyingAnimation() ? 0.0F : super.getDeathMaxRotation(lord);
    }
}
