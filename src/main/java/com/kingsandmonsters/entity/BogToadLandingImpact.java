package com.kingsandmonsters.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared Bog Toad touchdown presentation used by the mob and Boghide Boots leap. */
public final class BogToadLandingImpact {
    public static final double RADIUS = 2.1;
    public static final double VERTICAL_TOLERANCE = 1.5;

    private BogToadLandingImpact() {}

    public static void spawnVisuals(ServerLevel level, Entity source, boolean shockwave) {
        double impactY = source.getY() + 0.08;
        BlockState surface = level.getBlockState(source.blockPosition().below());
        if (surface.isAir()) surface = Blocks.DIRT.defaultBlockState();

        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.DUST_PILLAR, surface);
        BlockParticleOption chips = new BlockParticleOption(ParticleTypes.BLOCK, surface);
        level.sendParticles(chips, source.getX(), impactY, source.getZ(), 22,
                0.40, 0.08, 0.40, 0.11);
        for (int i = 0; i < 26; i++) {
            double angle = Math.PI * 2.0 * i / 26.0 + source.getRandom().nextDouble() * 0.12;
            double radius = RADIUS * (0.25 + source.getRandom().nextDouble() * 0.75);
            level.sendParticles(debris,
                    source.getX() + Math.cos(angle) * radius, impactY,
                    source.getZ() + Math.sin(angle) * radius,
                    2, 0.10, 0.04, 0.10, 0.07);
        }
        if (shockwave) StaticShockwaveParticles.spawn(level, source.position(), (float) RADIUS);
        // Broadcast through the level so the leaping player also hears the landing.
        level.playSound(null, source.getX(), source.getY(), source.getZ(),
                SoundEvents.SLIME_BLOCK_FALL, source.getSoundSource(), 1.35F, 0.58F);
        level.playSound(null, source.getX(), source.getY(), source.getZ(),
                SoundEvents.FROG_STEP, source.getSoundSource(), 0.85F, 0.55F);
    }
}
