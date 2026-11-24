package com.kingsandmonsters.item;

import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.effect.CombatEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.kingsandmonsters.event.LivingDamageEvent;
import com.kingsandmonsters.event.LivingIncomingDamageEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import com.kingsandmonsters.event.PlayerTickEvent;

/** Charged Bone Club hits crush armor; the first charged falling critical each jump also Dazes. */
public final class BoneClubEvents {
    private static final String CRIT_TARGET = "KingsAndMonstersBoneClubCritTarget";
    private static final String CRIT_TICK = "KingsAndMonstersBoneClubCritTick";
    private static final String DAZE_USED = "KingsAndMonstersBoneClubDazeUsed";
    private static final String DAZE_COOLDOWN_UNTIL = "KingsAndMonstersBoneClubDazeCooldownUntil";
    private static final float FULL_CHARGE = 0.9F;
    private static final float ARMOR_PIERCE = 0.20F;
    private static final int DAZED_TICKS = 20 * 6;
    private static final int DAZE_COOLDOWN_TICKS = 20 * 10;

    private BoneClubEvents() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().onGround()) event.getEntity().getPersistentData().remove(DAZE_USED);
    }

    public static void onCriticalHit(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.isVanillaCritical()
                || player.getAttackStrengthScale(0.5F) < FULL_CHARGE
                || !player.getMainHandItem().is(ModItems.BONE_CLUB.get())
                || player.getPersistentData().getBoolean(DAZE_USED)
                || player.getPersistentData().getLong(DAZE_COOLDOWN_UNTIL) > player.level().getGameTime()) return;
        player.getPersistentData().putInt(CRIT_TARGET, event.getTarget().getId());
        player.getPersistentData().putLong(CRIT_TICK, player.level().getGameTime());
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player // a swing of the club, not an arrow or spell
                || !player.getMainHandItem().is(ModItems.BONE_CLUB.get())
                || player.getAttackStrengthScale(0.5F) < FULL_CHARGE) return;

        LivingEntity target = event.getEntity();
        float armor = (float) target.getAttributeValue(Attributes.ARMOR);
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        if (armor <= 0.0F) return;
        float raw = event.getAmount();
        float normal = CombatRules.getDamageAfterAbsorb(raw, armor, toughness);
        float pierced = CombatRules.getDamageAfterAbsorb(raw, armor * (1.0F - ARMOR_PIERCE), toughness);
        if (normal > 0.0F && pierced > normal) event.setAmount(raw * pierced / normal);
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || player.getPersistentData().getInt(CRIT_TARGET) != target.getId()
                || player.getPersistentData().getLong(CRIT_TICK) != player.level().getGameTime()) return;
        player.getPersistentData().remove(CRIT_TARGET);
        player.getPersistentData().remove(CRIT_TICK);
        player.getPersistentData().putBoolean(DAZE_USED, true);
        player.getPersistentData().putLong(DAZE_COOLDOWN_UNTIL,
                player.level().getGameTime() + DAZE_COOLDOWN_TICKS);
        CombatEffects.applyDazed(target, DAZED_TICKS, player);
    }
}
