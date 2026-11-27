package com.kingsandmonsters.client;

import com.kingsandmonsters.item.CandleHelmetItem;
import net.minecraft.world.item.ItemStack;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps one small flame seated on each Candle Helmet wick, plus an occasional puff of candle smoke.
 * The flame is a single persistent particle per wearer (not a stream), so there are never gaps or
 * trails; it re-seats itself on the wick every rendered frame (see CandleHelmetFlameParticle).
 */
public final class CandleHelmetFlameParticles {
    private static final double FLAME_ABOVE_EYES = 0.72;
    private static final int SMOKE_INTERVAL_TICKS = 4;
    private static final float SMOKE_CHANCE = 0.3F;

    /** wearer entity id -> that wearer's live flame particle. */
    private static final Map<Integer, Particle> FLAMES = new HashMap<>();

    private CandleHelmetFlameParticles() {}

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused() || minecraft.level == null) {
            return;
        }
        FLAMES.values().removeIf(flame -> !flame.isAlive());

        for (Player player : minecraft.level.players()) {
            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!head.is(ModItems.CANDLE_HELMET.get()) || !CandleHelmetItem.isLit(head)) {
                continue;
            }
            Vec3 wick = wickPosition(player, 1.0F);

            if (!FLAMES.containsKey(player.getId())) {
                Particle flame = minecraft.particleEngine.createParticle(ModParticles.CANDLE_HELMET_FLAME.get(),
                        wick.x, wick.y, wick.z, player.getId(), 0.0, 0.0);
                if (flame != null) {
                    FLAMES.put(player.getId(), flame);
                }
            }

            // Same odds as AbstractCandleBlock.addParticlesAndSound (30%), but not every tick: a moving
            // helmet needs far fewer emissions than a stationary candle block.
            if (player.tickCount % SMOKE_INTERVAL_TICKS == 0
                    && minecraft.level.random.nextFloat() < SMOKE_CHANCE) {
                minecraft.level.addParticle(ModParticles.CANDLE_HELMET_SMOKE.get(),
                        wick.x, wick.y, wick.z, player.getId(), 0.0, 0.0);
            }
        }
    }

    /** Wick position for the given partial tick, using the same interpolated head rotation the armor renders with. */
    public static Vec3 wickPosition(Player player, float partialTick) {
        float pitch = -Mth.lerp(partialTick, player.xRotO, player.getXRot()) * ((float) Math.PI / 180.0F);
        float yaw = -Mth.rotLerp(partialTick, player.yHeadRotO, player.getYHeadRot()) * ((float) Math.PI / 180.0F);
        Vec3 fromHeadPivot = new Vec3(0.0, FLAME_ABOVE_EYES, 0.0).xRot(pitch).yRot(yaw);
        return player.getEyePosition(partialTick).add(fromHeadPivot);
    }
}
