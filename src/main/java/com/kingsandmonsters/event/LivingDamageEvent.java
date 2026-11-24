package com.kingsandmonsters.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/** Damage after armor: {@link Pre} can still change it, {@link Post} reports what was dealt. */
public abstract class LivingDamageEvent extends Event {
    private final LivingEntity entity;
    private final DamageSource source;
    protected float newDamage;

    protected LivingDamageEvent(LivingEntity entity, DamageSource source, float newDamage) {
        this.entity = entity;
        this.source = source;
        this.newDamage = newDamage;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getNewDamage() {
        return newDamage;
    }

    public static class Pre extends LivingDamageEvent {
        public Pre(LivingEntity entity, DamageSource source, float newDamage) {
            super(entity, source, newDamage);
        }

        public void setNewDamage(float newDamage) {
            this.newDamage = newDamage;
        }
    }

    public static class Post extends LivingDamageEvent {
        public Post(LivingEntity entity, DamageSource source, float newDamage) {
            super(entity, source, newDamage);
        }
    }
}
