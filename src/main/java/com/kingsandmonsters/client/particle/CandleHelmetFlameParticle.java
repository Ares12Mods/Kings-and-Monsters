package com.kingsandmonsters.client.particle;

import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.client.CandleHelmetFlameParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Vanilla small-flame artwork as ONE persistent particle per wearer. It never ages out while the
 * helmet is worn, and it is re-seated on the wick every rendered frame with the interpolated head
 * rotation, so it neither flickers out between emissions nor trails behind a turning head.
 */
public final class CandleHelmetFlameParticle extends FlameParticle {
    private static final int KEEP_ALIVE_LIFETIME = 40;

    private final int wearerId;

    private CandleHelmetFlameParticle(ClientLevel level, double x, double y, double z,
                                      int wearerId, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.wearerId = wearerId;
        this.lifetime = KEEP_ALIVE_LIFETIME;
        pickSprite(sprites);
        scale(0.5F);
    }

    private Player wearer() {
        Entity entity = level.getEntity(wearerId);
        if (entity instanceof Player player
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CANDLE_HELMET.get())) {
            return player;
        }
        return null;
    }

    @Override
    public void tick() {
        // Never let the flame age out or shrink while it is being worn.
        this.age = 0;
        super.tick();
        if (removed) return;
        Player player = wearer();
        if (player == null) {
            remove();
            return;
        }
        seat(player, 1.0F);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Player player = wearer();
        if (player != null) {
            seat(player, partialTicks);
        }
        super.render(buffer, camera, partialTicks);
    }

    /** Pins both the current and previous position so the renderer's own interpolation cannot offset it. */
    private void seat(Player player, float partialTick) {
        Vec3 wick = CandleHelmetFlameParticles.wickPosition(player, partialTick);
        this.x = this.xo = wick.x;
        this.y = this.yo = wick.y;
        this.z = this.zo = wick.z;
    }

    /** Vanilla shrinks flames over their life; this one stays full size with a tiny flicker instead. */
    @Override
    public float getQuadSize(float scaleFactor) {
        float flicker = 1.0F + 0.06F * (float) Math.sin((level.getGameTime() + scaleFactor) * 0.9F);
        return this.quadSize * flicker;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new CandleHelmetFlameParticle(level, x, y, z, (int) xd, sprites);
        }
    }
}
