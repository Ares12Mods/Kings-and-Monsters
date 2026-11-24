package com.kingsandmonsters;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, KingsAndMonsters.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_BRUTE_TANTRUM_WHINE =
            register("entity.ogre.brute.tantrum_whine");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_BRUTE_BELLY_SLAM_GRUNT =
            register("entity.ogre.brute.belly_slam_grunt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_BRUTE_UPSWING_WHOOSH =
            register("entity.ogre.brute.upswing_whoosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_BRUTE_HURT =
            register("entity.ogre.brute.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_BRUTE_IDLE =
            register("entity.ogre.brute.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_BRUTE_DEATH =
            register("entity.ogre.brute.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_GRUNT_HURT =
            register("entity.ogre.grunt.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_ARCHER_HURT =
            register("entity.ogre.archer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_ARCHER_IDLE =
            register("entity.ogre.archer.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_ARCHER_DEATH =
            register("entity.ogre.archer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_MAGE_HURT =
            register("entity.ogre.mage.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_MAGE_DEATH =
            register("entity.ogre.mage.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_MAGE_CAST_PREPARE =
            register("entity.ogre.mage.cast_prepare");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_MAGE_CAST_RELEASE =
            register("entity.ogre.mage.cast_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_LORD_HURT =
            register("entity.ogre_lord.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_LORD_DEATH =
            register("entity.ogre_lord.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_LORD_ROAR =
            register("entity.ogre_lord.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_LORD_OVERHEAD_GRUNT =
            register("entity.ogre_lord.overhead_grunt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_LORD_DOUBLE_ARM_UPSWING_GRUNT =
            register("entity.ogre_lord.double_arm_upswing_grunt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_LORD_CLOSE_WARNING_SNORT =
            register("entity.ogre_lord.close_warning_snort");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_LORD_BELLY_BUMP_ROAR =
            register("entity.ogre_lord.belly_bump_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_LORD_SUMMON_GRUNT =
            register("entity.ogre_lord.summon_grunt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_LORD_ARROW_STORM_GRUNT =
            register("entity.ogre_lord.arrow_storm_grunt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_LORD_HUFF =
            register("entity.ogre_lord.huff");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_TRAPPER_IDLE =
            register("entity.ogre_trapper.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_TRAPPER_HURT =
            register("entity.ogre_trapper.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_TRAPPER_DEATH =
            register("entity.ogre_trapper.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLAGUE_OGRE_IDLE =
            register("entity.plague_ogre.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLAGUE_OGRE_HURT =
            register("entity.plague_ogre.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLAGUE_OGRE_DEATH =
            register("entity.plague_ogre.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_GRUNT_IDLE =
            register("entity.ogre.grunt.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_GRUNT_DEATH =
            register("entity.ogre.grunt.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_MAGE_HUFF =
            register("entity.ogre.mage.huff");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_GRUNT_CAPTAIN_SALUTE =
            register("entity.ogre.grunt_captain.salute");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_GRUNT_CAPTAIN_IDLE =
            register("entity.ogre.grunt_captain.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_GRUNT_CAPTAIN_HURT =
            register("entity.ogre.grunt_captain.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_GRUNT_CAPTAIN_DEATH =
            register("entity.ogre.grunt_captain.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_GUARD_IDLE =
            register("entity.ogre_guard.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_GUARD_HURT =
            register("entity.ogre_guard.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_GUARD_DEATH =
            register("entity.ogre_guard.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGREBLOOD_TOTEM_ACTIVATE =
            register("item.ogreblood_totem.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_MERCHANT_IDLE =
            register("entity.ogre_merchant.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_MERCHANT_HURT =
            register("entity.ogre_merchant.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_MERCHANT_DEATH =
            register("entity.ogre_merchant.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_MERCHANT_DISAPPROVE =
            register("entity.ogre_merchant.disapprove");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_MERCHANT_TRADE =
            register("entity.ogre_merchant.trade");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_KING_INTRO = register("music.ogre_king_intro");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_KING_PHASE_ONE_ONE = register("music.ogre_king_phase_01_01");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_KING_PHASE_ONE_TWO = register("music.ogre_king_phase_01_02");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_KING_PHASE_ONE_THREE = register("music.ogre_king_phase_01_03");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_KING_TRANSITION_FILL = register("music.ogre_king_transition");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_KING_PHASE_TWO_ONE = register("music.ogre_king_phase_02_01");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_KING_PHASE_TWO_TWO = register("music.ogre_king_phase_02_02");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_KING_PHASE_TWO_THREE = register("music.ogre_king_phase_02_03");
    public static final DeferredHolder<SoundEvent, SoundEvent> OGRE_KING_END = register("music.ogre_king_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOG_TOAD_IDLE_DEEP_A =
            register("entity.bog_toad.idle_deep_a");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOG_TOAD_IDLE_DEEP_B =
            register("entity.bog_toad.idle_deep_b");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOG_TOAD_IDLE_DEEP_C =
            register("entity.bog_toad.idle_deep_c");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOG_TOAD_HURT_DEEP_A =
            register("entity.bog_toad.hurt_deep_a");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOG_TOAD_HURT_DEEP_B =
            register("entity.bog_toad.hurt_deep_b");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOG_TOAD_HURT_DEEP_C =
            register("entity.bog_toad.hurt_deep_c");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOG_TOAD_DEATH_DEEP =
            register("entity.bog_toad.death_deep");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUDSNOUT_AMBIENT =
            register("entity.mudsnout.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUDSNOUT_HURT =
            register("entity.mudsnout.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUDSNOUT_DEATH =
            register("entity.mudsnout.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUDSNOUT_BABY_AMBIENT =
            register("entity.mudsnout.baby.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUDSNOUT_BABY_HURT =
            register("entity.mudsnout.baby.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUDSNOUT_BABY_DEATH =
            register("entity.mudsnout.baby.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUDSNOUT_CHARGE_SNORT_FIRST =
            register("entity.mudsnout.charge_snort_first");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUDSNOUT_CHARGE_SNORT_SECOND =
            register("entity.mudsnout.charge_snort_second");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUDSNOUT_CHARGE_LAUNCH =
            register("entity.mudsnout.charge_launch");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, name)));
    }
}
