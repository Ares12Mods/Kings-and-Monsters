package com.kingsandmonsters.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/** Fired at the end of every living entity tick. */
public abstract class EntityTickEvent extends Event {
    private final LivingEntity entity;

    protected EntityTickEvent(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public static class Post extends EntityTickEvent {
        public Post(LivingEntity entity) {
            super(entity);
        }
    }
}
