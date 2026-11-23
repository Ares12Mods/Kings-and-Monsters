package com.kingsandmonsters.client.particle;

import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.client.CandleHelmetFlameParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SmokeParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Vanilla smoke artwork. It leaves the wick attached for its first few ticks (so it visibly comes
 * off the flame even while the head turns) and is then released to drift upward on its own, like
 * smoke from a candle block, instead of being dragged along behind or vanishing early.
 */
public final class CandleHelmetSmokeParticle extends SmokeParticle {
    private static final int ATTACHED_TICKS = 4;

    private final int wearerId;

    private CandleHelmetSmokeParticle(ClientLevel level, double x, double y, double z,
                                      int wearerId, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0, 1.0F, sprites);
        this.wearerId = wearerId;
        this.lifetime = 14;
    }

    @Override
    public void tick() {
        super.tick();
        if (removed || age > ATTACHED_TICKS) return;
        Entity entity = level.getEntity(wearerId);
        if (entity instanceof Player player
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CANDLE_HELMET.get())) {
            Vec3 wick = CandleHelmetFlameParticles.wickPosition(player, 1.0F);
            setPos(wick.x, wick.y + age * 0.018, wick.z);
        } else {
            remove();
        }
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
            return new CandleHelmetSmokeParticle(level, x, y, z, (int) xd, sprites);
        }
    }
}
