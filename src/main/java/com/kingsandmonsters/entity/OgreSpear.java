package com.kingsandmonsters.entity;

import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.Config;
import com.kingsandmonsters.effect.CombatEffects;
import com.kingsandmonsters.enchantment.ModEnchantmentEffects;
import com.kingsandmonsters.enchantment.ModEnchantments;
import com.kingsandmonsters.item.HunterSpearEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Thrown ogre guard spear. Built on {@link AbstractArrow} (like vanilla's {@code ThrownTrident}) so
 * it gets real arrow physics: it sticks into what it hits, falls under gravity, and reads any
 * Loyalty enchantment from its pickup item to fly back to its owner. It just carries this mod's
 * spear art. Implements {@link ItemSupplier} so it renders through {@code ThrownItemRenderer} as a
 * flat 2D icon instead of vanilla's 3D trident model.
 */
public class OgreSpear extends AbstractArrow implements ItemSupplier {
    private static final EntityDataAccessor<Byte> ID_LOYALTY = SynchedEntityData.defineId(OgreSpear.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> ID_STABLE_POST_HIT_ROTATION =
            SynchedEntityData.defineId(OgreSpear.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> ID_POST_HIT_YAW =
            SynchedEntityData.defineId(OgreSpear.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ID_POST_HIT_PITCH =
            SynchedEntityData.defineId(OgreSpear.class, EntityDataSerializers.FLOAT);

    private ItemStack spearItem = new ItemStack(ModItems.HUNTERS_SPEAR.get());
    private float damage = 10.0F;
    private boolean dealtDamage;
    private int clientSideReturnTickCount;

    public OgreSpear(EntityType<? extends OgreSpear> type, Level level) {
        super(type, level);
    }

    public OgreSpear(Level level, LivingEntity owner, Vec3 position, Vec3 velocity, double damage) {
        this(level, owner, new ItemStack(ModItems.HUNTERS_SPEAR.get()), position, velocity, damage, Pickup.DISALLOWED);
    }

    public OgreSpear(Level level, LivingEntity owner, ItemStack spearStack, Vec3 position, Vec3 velocity,
                     double damage, Pickup pickup) {
        super(ModEntities.OGRE_SPEAR.get(), position.x, position.y, position.z, level);
        this.spearItem = spearStack.copy();
        this.damage = (float) damage;
        this.pickup = pickup;
        setOwner(owner);
        setDeltaMovement(velocity);
        this.entityData.set(ID_LOYALTY, getLoyaltyFromItem(spearItem));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ID_LOYALTY, (byte) 0);
        this.entityData.define(ID_STABLE_POST_HIT_ROTATION, false);
        this.entityData.define(ID_POST_HIT_YAW, 0.0F);
        this.entityData.define(ID_POST_HIT_PITCH, 0.0F);
    }

    @Override
    protected ItemStack getPickupItem() {
        return spearItem.copy();
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    public ItemStack getItem() {
        return spearItem.copy();
    }

    /** The spear is the weapon itself (no separate launcher item), same as ThrownTrident. */
    public ItemStack getWeaponItem() {
        return spearItem.copy();
    }

    public boolean hasStablePostHitRotation() {
        return this.entityData.get(ID_STABLE_POST_HIT_ROTATION)
                && this.entityData.get(ID_LOYALTY) <= 0;
    }

    public float getStablePostHitYaw() {
        return this.entityData.get(ID_POST_HIT_YAW);
    }

    public float getStablePostHitPitch() {
        return this.entityData.get(ID_POST_HIT_PITCH);
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }

        // Mirrors ThrownTrident.tick()'s return-to-owner homing, driven by whatever Loyalty
        // level (if any) is on the pickup item's enchantments.
        Entity owner = getOwner();
        int loyalty = this.entityData.get(ID_LOYALTY);
        if (loyalty > 0 && (dealtDamage || isNoPhysics()) && owner != null) {
            if (!isAcceptableReturnOwner()) {
                discard();
            } else {
                setNoPhysics(true);
                Vec3 toOwner = owner.getEyePosition().subtract(position());
                setPosRaw(getX(), getY() + toOwner.y * 0.015 * loyalty, getZ());
                if (level().isClientSide) {
                    yOld = getY();
                }

                double acceleration = 0.05 * loyalty;
                setDeltaMovement(getDeltaMovement().scale(0.95).add(toOwner.normalize().scale(acceleration)));
                if (clientSideReturnTickCount == 0) {
                    playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
                }

                clientSideReturnTickCount++;
            }
        }

        super.tick();
    }

    private boolean isAcceptableReturnOwner() {
        Entity owner = getOwner();
        return owner != null && owner.isAlive() && !(owner instanceof ServerPlayer player && player.isSpectator());
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 startVec, Vec3 endVec) {
        return dealtDamage ? null : super.findHitEntity(startVec, endVec);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target instanceof OgreGrunt && owner instanceof OgreGrunt) {
            return;
        }

        float appliedDamage = damage;
        DamageSource damageSource = damageSources().thrown(this, owner != null ? owner : this);
        if (target instanceof LivingEntity livingTarget) {
            appliedDamage += EnchantmentHelper.getDamageBonus(spearItem, livingTarget.getMobType());
        }

        dealtDamage = true;
        if (owner instanceof Player && this.entityData.get(ID_LOYALTY) <= 0) {
            this.entityData.set(ID_STABLE_POST_HIT_ROTATION, true);
            this.entityData.set(ID_POST_HIT_YAW, getYRot());
            this.entityData.set(ID_POST_HIT_PITCH, getXRot());
        }
        if (target.hurt(damageSource, appliedDamage)) {
            if (target instanceof LivingEntity livingTarget) {
                Vec3 push = getDeltaMovement().normalize();
                livingTarget.knockback(0.4, -push.x, -push.z);
                if (owner instanceof LivingEntity livingOwner) {
                    EnchantmentHelper.doPostHurtEffects(livingTarget, livingOwner);
                    EnchantmentHelper.doPostDamageEffects(livingOwner, livingTarget);
                }
                int heavyThrow = ModEnchantmentEffects.level(level(), getWeaponItem(), ModEnchantments.HEAVY_THROW);
                double bonusKnockback = ModEnchantmentEffects.heavyThrowKnockback(heavyThrow);
                if (bonusKnockback > 0.0 && owner instanceof LivingEntity livingOwner) {
                    livingTarget.knockback(bonusKnockback,
                            livingOwner.getX() - livingTarget.getX(), livingOwner.getZ() - livingTarget.getZ());
                }
                boolean innateCrippleProc = random.nextDouble()
                        < Config.HUNTERS_SPEAR_CRIPPLE_CHANCE.get();
                if (innateCrippleProc) {
                    CombatEffects.applyCrippled(livingTarget, Config.CRIPPLED_SPEAR_DURATION_TICKS.get(),
                            owner instanceof LivingEntity livingOwner ? livingOwner : livingTarget);
                }
                LivingEntity effectSource = owner instanceof LivingEntity livingOwner ? livingOwner : livingTarget;
                HunterSpearEvents.applyHunterSpearBleeding(livingTarget, effectSource, getWeaponItem());
                doPostHurtEffects(livingTarget);
            }
        }

        setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    private byte getLoyaltyFromItem(ItemStack stack) {
        return (byte) Mth.clamp(EnchantmentHelper.getLoyalty(stack), 0, 127);
    }

    @Override
    protected void tickDespawn() {
        int loyalty = this.entityData.get(ID_LOYALTY);
        if (pickup != Pickup.ALLOWED || loyalty <= 0) {
            super.tickDespawn();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Spear", spearItem.save(new CompoundTag()));
        tag.putFloat("Damage", damage);
        tag.putBoolean("DealtDamage", dealtDamage);
        if (this.entityData.get(ID_STABLE_POST_HIT_ROTATION)) {
            tag.putBoolean("StablePostHitRotation", true);
            tag.putFloat("PostHitYaw", this.entityData.get(ID_POST_HIT_YAW));
            tag.putFloat("PostHitPitch", this.entityData.get(ID_POST_HIT_PITCH));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Spear")) {
            spearItem = ItemStack.of(tag.getCompound("Spear"));
        }
        if (tag.contains("Damage")) {
            damage = tag.getFloat("Damage");
        }
        dealtDamage = tag.getBoolean("DealtDamage");
        if (tag.getBoolean("StablePostHitRotation")) {
            this.entityData.set(ID_STABLE_POST_HIT_ROTATION, true);
            this.entityData.set(ID_POST_HIT_YAW, tag.getFloat("PostHitYaw"));
            this.entityData.set(ID_POST_HIT_PITCH, tag.getFloat("PostHitPitch"));
        }
        this.entityData.set(ID_LOYALTY, getLoyaltyFromItem(spearItem));
    }
}
