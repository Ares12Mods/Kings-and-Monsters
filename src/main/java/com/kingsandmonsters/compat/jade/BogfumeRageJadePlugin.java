package com.kingsandmonsters.compat.jade;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.ModMobEffects;
import com.kingsandmonsters.entity.MudsnoutBoar;
import com.kingsandmonsters.entity.OgreGrunt;
import com.kingsandmonsters.entity.OgreLord;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.ITooltip;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin(KingsAndMonsters.MODID)
public class BogfumeRageJadePlugin implements IWailaPlugin {
    private static final ResourceLocation BOGFUME_RAGE =
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "bogfume_rage");
    private static final ResourceLocation BOGROT =
            ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "bogrot");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(BogfumeRageProvider.INSTANCE, OgreGrunt.class);
        registration.registerEntityComponent(BogrotProvider.INSTANCE, LivingEntity.class);
        registration.registerEntityComponent(BoarTraitsProvider.INSTANCE, MudsnoutBoar.class);
        registration.registerEntityComponent(BleedingProvider.INSTANCE, LivingEntity.class);
    }

    /**
     * Bleeding is applied without vanilla's particle flag, which is also what Jade's own effect list keys
     * on, so the worst active tier is listed here instead.
     */
    private enum BleedingProvider implements IComponentProvider<EntityAccessor> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "bleeding");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof LivingEntity entity) || entity instanceof OgreLord) {
                return;
            }
            for (var effect : java.util.List.of(ModMobEffects.GREATER_BLEEDING, ModMobEffects.BLEEDING,
                    ModMobEffects.LESSER_BLEEDING)) {
                var active = entity.getEffect(effect);
                if (active != null && active.getDuration() > 0) {
                    tooltip.add(Component.translatable("jade.kingsandmonsters.bleeding",
                            effect.value().getDisplayName(), Math.max(1, (active.getDuration() + 19) / 20))
                            .withStyle(ChatFormatting.RED));
                    return;
                }
            }
        }
    }

    private enum BogfumeRageProvider implements IComponentProvider<EntityAccessor> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return BOGFUME_RAGE;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof LivingEntity entity && entity.hasEffect(ModMobEffects.BOGFUME_RAGE)) {
                tooltip.add(Component.translatable("jade.kingsandmonsters.bogfume_rage").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    /** A tame adult boar shows its speed and health tier, so a breeder can compare animals at a glance. */
    private enum BoarTraitsProvider implements IComponentProvider<EntityAccessor> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(KingsAndMonsters.MODID, "boar_traits");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof MudsnoutBoar boar && boar.isTamed() && !boar.isBaby()) {
                tooltip.add(Component.translatable("boar.kingsandmonsters.traits",
                        Component.translatable(boar.getSpeedTierKey()),
                        Component.translatable(boar.getHealthTierKey())).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    private enum BogrotProvider implements IComponentProvider<EntityAccessor> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return BOGROT;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof LivingEntity entity && !(entity instanceof OgreLord)) {
                var bogrot = entity.getEffect(ModMobEffects.BOGROT);
                if (bogrot != null && bogrot.getDuration() > 0) {
                    tooltip.add(Component.translatable("jade.kingsandmonsters.bogrot",
                            bogrot.getAmplifier() + 1,
                            Math.max(1, (bogrot.getDuration() + 19) / 20))
                            .withStyle(ChatFormatting.DARK_GREEN));
                }
            }
        }
    }
}
