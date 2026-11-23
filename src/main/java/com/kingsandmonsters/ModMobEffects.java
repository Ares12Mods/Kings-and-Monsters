package com.kingsandmonsters;

import com.kingsandmonsters.effect.BogfumeRageEffect;
import com.kingsandmonsters.effect.CrippledEffect;
import com.kingsandmonsters.effect.DazedEffect;
import com.kingsandmonsters.effect.HamstrungEffect;
import com.kingsandmonsters.effect.BleedingEffect;
import com.kingsandmonsters.effect.PoisonResistanceEffect;
import com.kingsandmonsters.effect.WarcalledEffect;
import com.kingsandmonsters.effect.OgresBaneEffect;
import com.kingsandmonsters.effect.BogstrideEffect;
import com.kingsandmonsters.effect.ChargeEffect;
import com.kingsandmonsters.effect.BogrotEffect;
import com.kingsandmonsters.effect.UnyieldingEffect;
import com.kingsandmonsters.effect.BogSightEffect;
import com.kingsandmonsters.effect.AmphibiousnessEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, KingsAndMonsters.MODID);

    public static final Holder<MobEffect> BOGFUME_RAGE = MOB_EFFECTS.register(
            "bogfume_rage",
            BogfumeRageEffect::new);

    public static final Holder<MobEffect> DAZED = MOB_EFFECTS.register("dazed", DazedEffect::new);
    public static final Holder<MobEffect> CRIPPLED = MOB_EFFECTS.register("crippled", CrippledEffect::new);
    public static final Holder<MobEffect> HAMSTRUNG = MOB_EFFECTS.register("hamstrung", HamstrungEffect::new);
    public static final Holder<MobEffect> LESSER_BLEEDING = MOB_EFFECTS.register("lesser_bleeding",
            () -> new BleedingEffect(BleedingEffect.Severity.LESSER));
    public static final Holder<MobEffect> BLEEDING = MOB_EFFECTS.register("bleeding",
            () -> new BleedingEffect(BleedingEffect.Severity.NORMAL));
    public static final Holder<MobEffect> GREATER_BLEEDING = MOB_EFFECTS.register("greater_bleeding",
            () -> new BleedingEffect(BleedingEffect.Severity.GREATER));
    public static final Holder<MobEffect> POISON_RESISTANCE = MOB_EFFECTS.register(
            "poison_resistance", PoisonResistanceEffect::new);
    public static final Holder<MobEffect> WARCALLED = MOB_EFFECTS.register("warcalled", WarcalledEffect::new);
    public static final Holder<MobEffect> WARBAND = MOB_EFFECTS.register("warband",
            com.kingsandmonsters.effect.WarbandEffect::new);
    public static final Holder<MobEffect> OGRES_BANE = MOB_EFFECTS.register("ogres_bane", OgresBaneEffect::new);
    public static final Holder<MobEffect> BOGSTRIDE = MOB_EFFECTS.register("bogstride", BogstrideEffect::new);
    public static final Holder<MobEffect> CHARGE = MOB_EFFECTS.register("charge", ChargeEffect::new);
    public static final Holder<MobEffect> BOGROT = MOB_EFFECTS.register("bogrot", BogrotEffect::new);
    public static final Holder<MobEffect> UNYIELDING = MOB_EFFECTS.register("unyielding", UnyieldingEffect::new);
    public static final Holder<MobEffect> BOG_SIGHT = MOB_EFFECTS.register("bog_sight", BogSightEffect::new);
    public static final Holder<MobEffect> AMPHIBIOUSNESS = MOB_EFFECTS.register(
            "amphibiousness", AmphibiousnessEffect::new);
}
