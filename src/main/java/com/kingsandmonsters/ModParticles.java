package com.kingsandmonsters;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/** Bog Toad particles whose behavior needs no serialized payload. */
public final class ModParticles {
    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, KingsAndMonsters.MODID);

    public static final RegistryObject<SimpleParticleType> BOGROT_MOTE =
            PARTICLE_TYPES.register("bogrot_mote", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BOGROT_FLY =
            PARTICLE_TYPES.register("bogrot_fly", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BOGROT_FLIES =
            PARTICLE_TYPES.register("bogrot_flies", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BOGROT_BUBBLE =
            PARTICLE_TYPES.register("bogrot_bubble", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BLOOD_DROPLET =
            PARTICLE_TYPES.register("blood_droplet", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BLOOD_STAIN =
            PARTICLE_TYPES.register("blood_stain", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> CANDLE_HELMET_FLAME =
            PARTICLE_TYPES.register("candle_helmet_flame", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> CANDLE_HELMET_SMOKE =
            PARTICLE_TYPES.register("candle_helmet_smoke", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SLEEP_Z =
            PARTICLE_TYPES.register("sleep_z", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> DIALOGUE_EXCLAMATION =
            PARTICLE_TYPES.register("dialogue_exclamation", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> DAZED =
            PARTICLE_TYPES.register("dazed", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> GRAZING_CRUMB =
            PARTICLE_TYPES.register("grazing_crumb", () -> new SimpleParticleType(false));
}
