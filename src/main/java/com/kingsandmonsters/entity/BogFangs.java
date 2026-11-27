package com.kingsandmonsters.entity;

import java.util.List;
import com.kingsandmonsters.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** The Mage's fangs: vanilla fangs that bite a wider and taller patch of ground. */
public class BogFangs extends EvokerFangs {
    // Vanilla catches things within 0.2 blocks of its 0.5 wide box; this reaches about a block out.
    private static final double HIT_GROW_SIDE = 0.55;
    private static final double HIT_GROW_UP = 0.6;
    // Vanilla bites 8 ticks after its warm-up runs out.
    private static final int BITE_AFTER_WARMUP_TICKS = 8;
    private static final int POISON_DURATION_TICKS = 5 * 20;

    private int biteTick = BITE_AFTER_WARMUP_TICKS;
    private int ticksAlive;

    public BogFangs(EntityType<? extends BogFangs> type, Level level) {
        super(type, level);
    }

    public BogFangs(Level level, double x, double y, double z, float yRot, int warmupDelay, LivingEntity owner) {
        this(ModEntities.BOG_FANGS.get(), level);
        CompoundTag data = new CompoundTag();
        data.putInt("Warmup", warmupDelay);
        readAdditionalSaveData(data);
        setOwner(owner);
        setYRot(yRot * (180F / (float) Math.PI));
        setPos(x, y, z);
        this.biteTick = warmupDelay + BITE_AFTER_WARMUP_TICKS;
    }

    @Override
    public void tick() {
        ticksAlive++;
        boolean biting = !level().isClientSide && ticksAlive == biteTick;
        AABB normal = getBoundingBox();
        List<LivingEntity> poisoned = List.of();
        if (biting) {
            // The vanilla bite looks for victims in the box at this moment, so widen it just for the tick.
            setBoundingBox(normal.inflate(HIT_GROW_SIDE, 0.0, HIT_GROW_SIDE).expandTowards(0.0, HIT_GROW_UP, 0.0));
            LivingEntity owner = getOwner();
            poisoned = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.2, 0.0, 0.2),
                    target -> target.isAlive() && !target.isInvulnerable() && target != owner
                            && (owner == null || !owner.isAlliedTo(target)));
        }
        super.tick();
        if (biting) {
            LivingEntity owner = getOwner();
            for (LivingEntity target : poisoned) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, 1), owner);
            }
            setBoundingBox(normal);
        }
    }
}
