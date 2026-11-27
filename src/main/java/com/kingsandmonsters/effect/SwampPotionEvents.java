package com.kingsandmonsters.effect;

import net.minecraft.world.entity.LivingEntity;
import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModMobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class SwampPotionEvents {
    private static final ResourceLocation SWIM_ID = ResourceLocation.fromNamespaceAndPath(
            KingsAndMonsters.MODID, "amphibiousness_swim_speed");

    private SwampPotionEvents() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        MobEffectInstance amphibious = player.getEffect(ModMobEffects.AMPHIBIOUSNESS);
        var swim = player.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (swim != null) {
            if (amphibious != null) {
                double bonus = amphibious.getAmplifier() > 0 ? 0.70 : 0.40;
                var existing = swim.getModifier(SWIM_ID);
                if (existing == null || existing.amount() != bonus) {
                    if (existing != null) swim.removeModifier(SWIM_ID);
                    swim.addTransientModifier(new AttributeModifier(
                            SWIM_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
            } else if (swim.hasModifier(SWIM_ID)) {
                swim.removeModifier(SWIM_ID);
            }
        }

        if (!(player instanceof ServerPlayer serverPlayer) || player.tickCount % 10 != 0) return;
        MobEffectInstance sight = player.getEffect(ModMobEffects.BOG_SIGHT);
        if (sight == null) return;
        double range = sight.getAmplifier() > 0 ? 30.0 : 20.0;
        AABB area = player.getBoundingBox().inflate(range);
        for (var target : serverPlayer.serverLevel().getEntitiesOfClass(
                LivingEntity.class, area,
                entity -> entity instanceof Enemy && entity.isAlive())) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 15, 0, false, false, false), player);
        }
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.isInWater() && player.hasEffect(ModMobEffects.AMPHIBIOUSNESS)) {
            // Cancels vanilla's 5x submerged mining penalty; it does not grant Haste on land.
            event.setNewSpeed(event.getNewSpeed() * 5.0F);
        }
    }
}
