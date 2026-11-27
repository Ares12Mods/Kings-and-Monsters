package com.kingsandmonsters.item;

import com.kingsandmonsters.ModMobEffects;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.kingsandmonsters.Config;
import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.ModItems;
import com.kingsandmonsters.ModSoundEvents;
import com.kingsandmonsters.entity.MudsnoutBoar;
import com.kingsandmonsters.entity.OgreArcher;
import com.kingsandmonsters.entity.OgreGrunt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * The Ogre King's Crown calls an army: a warband of ogres that fights for its wearer for a while and
 * then goes. Each call rolls one of four warbands at random.
 */
public final class OgreCrownArmy {
    private static final String READY_AT_TAG = "KingsAndMonstersArmyReadyAt";

    private enum Unit { GRUNT, ARCHER, GUARD, BRUTE, MAGE, TRAPPER, CAPTAIN, BOAR_RIDER }

    private record Warband(String key, List<Unit> units) {}

    /** The rank and file, the melee vanguard, the riders and the spellguard. */
    private static final List<Warband> WARBANDS = List.of(
            new Warband("ranks", List.of(Unit.CAPTAIN, Unit.ARCHER, Unit.ARCHER, Unit.GRUNT, Unit.GRUNT, Unit.GRUNT)),
            new Warband("vanguard", List.of(Unit.CAPTAIN, Unit.BRUTE, Unit.TRAPPER, Unit.GRUNT, Unit.GRUNT)),
            new Warband("riders", List.of(Unit.GUARD, Unit.GUARD, Unit.BOAR_RIDER, Unit.BOAR_RIDER,
                    Unit.BOAR_RIDER, Unit.MAGE)),
            new Warband("spellguard", List.of(Unit.MAGE, Unit.TRAPPER, Unit.GRUNT, Unit.GRUNT, Unit.ARCHER,
                    Unit.ARCHER)));

    private OgreCrownArmy() {}

    private static final int SPAWN_SPACING_TICKS = 3;

    private record Pending(UUID player, Unit unit, double angle, int ticks, int dueTick) {}

    /** Units still to arrive, in order. Only touched on the server thread. */
    private static final ArrayList<Pending> PENDING = new ArrayList<>();

    /** Brings in whichever units of a called warband are due. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        int now = event.getServer().getTickCount();
        var due = PENDING.iterator();
        while (due.hasNext()) {
            Pending next = due.next();
            if (next.dueTick() > now) {
                continue;
            }
            due.remove();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(next.player());
            if (player != null && player.level() instanceof ServerLevel level && player.isAlive()) {
                spawn(level, player, next.unit(), next.angle(), next.ticks());
            }
        }
    }

    /** An army never hurts what it was called to protect: its summoner, players, villagers or anyone's pets. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        boolean army = attacker instanceof OgreGrunt ogre && ogre.isArmyAlly()
                || attacker instanceof MudsnoutBoar boar && boar.getArmyOwnerId() != null;
        if (army && OgreGrunt.spareByArmy(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Nor do its splash potions: a harmful effect thrown by an army Mage is stripped from those it spares. */
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        var effect = event.getEffectInstance();
        if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL
                || !OgreGrunt.spareByArmy(event.getEntity())) {
            return;
        }
        Entity source = event.getEffectSource();
        if (source instanceof Projectile projectile) {
            source = projectile.getOwner();
        }
        boolean army = source instanceof OgreGrunt ogre && ogre.isArmyAlly()
                || source instanceof MudsnoutBoar boar && boar.getArmyOwnerId() != null;
        if (army) {
            event.getEntity().removeEffect(effect.getEffect());
            var previous = event.getOldEffectInstance();
            if (previous != null) {
                event.getEntity().forceAddEffect(previous, null);
            }
        }
    }

    /** Server side: the wearer pressed the summon key. */
    public static void summon(ServerPlayer player) {
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OGRE_KINGS_CROWN.get())
                || player.isSpectator() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        // The crown's rest is kept as a timer on the player, not as a vanilla item cooldown: those grey out
        // every copy of the item, including the crown that is the creative tab's icon.
        long now = level.getGameTime();
        long readyAt = player.getPersistentData().getLong(READY_AT_TAG);
        if (now < readyAt) {
            player.displayClientMessage(Component.translatable("message.kingsandmonsters.army.resting",
                    (readyAt - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return;
        }

        Warband warband = WARBANDS.get(level.random.nextInt(WARBANDS.size()));
        int ticks = Config.CROWN_ARMY_DURATION_SECONDS.get() * 20;

        int count = warband.units().size();
        double facing = Math.toRadians(player.getYRot() + 90.0);
        // Spawning six or so large mobs in a single tick is what made the game stutter, so the warband
        // marches in one unit every few ticks instead: still over in under a second.
        int now0 = level.getServer().getTickCount();
        for (int i = 0; i < count; i++) {
            // A loose arc across the front and sides of the wearer, so they come up around them.
            double angle = facing + Math.PI * (-0.85 + 1.7 * (i + 0.5) / count);
            PENDING.add(new Pending(player.getUUID(), warband.units().get(i), angle, ticks, now0 + i * SPAWN_SPACING_TICKS));
        }

        player.getPersistentData().putLong(READY_AT_TAG, now + Config.CROWN_ARMY_COOLDOWN_SECONDS.get() * 20L);
        player.addEffect(new MobEffectInstance(
                ModMobEffects.WARBAND, ticks, 0, false, false, true));
        level.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 1.4F, 0.7F);
        level.playSound(null, player.blockPosition(), ModSoundEvents.OGRE_LORD_HUFF.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
        player.displayClientMessage(Component.translatable("message.kingsandmonsters.army." + warband.key())
                .withStyle(ChatFormatting.GOLD), true);
    }

    private static boolean spawn(ServerLevel level, ServerPlayer player, Unit unit, double angle, int ticks) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double radius = 3.5 + attempt * 0.5 + level.random.nextDouble();
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
            double y = Math.abs(top.getY() - player.getY()) <= 5.0 ? top.getY() : player.getY();
            if (place(level, player, unit, x, y, z, ticks)) {
                return true;
            }
            // A different slot on the arc next time round.
            angle += 0.35 * (attempt % 2 == 0 ? 1 : -1) * (attempt / 2 + 1);
        }
        return false;
    }

    private static boolean place(ServerLevel level, ServerPlayer player, Unit unit, double x, double y, double z, int ticks) {
        BlockPos pos = BlockPos.containing(x, y, z);
        var difficulty = level.getCurrentDifficultyAt(pos);
        float yaw = player.getYRot();
        if (unit == Unit.BOAR_RIDER) {
            MudsnoutBoar boar = ModEntities.MUDSNOUT_BOAR.get().create(level);
            if (boar == null) {
                return false;
            }
            boar.moveTo(x, y, z, yaw, 0.0F);
            if (!level.noCollision(boar)) {
                boar.discard();
                return false;
            }
            boar.finalizeSpawn(level, difficulty, MobSpawnType.MOB_SUMMONED, null);
            boar.joinArmy(player, ticks);
            level.addFreshEntity(boar);
            // The same Archer a boar jockey normally carries, put in the army with it.
            boar.addJockey(level, difficulty);
            if (boar.getFirstPassenger() instanceof OgreArcher archer) {
                archer.joinArmy(player, ticks);
            }
            puff(level, x, y, z);
            return true;
        }

        Mob mob = switch (unit) {
            case GRUNT -> ModEntities.OGRE_GRUNT.get().create(level);
            case ARCHER -> ModEntities.OGRE_ARCHER.get().create(level);
            case GUARD -> ModEntities.OGRE_GUARD.get().create(level);
            case BRUTE -> ModEntities.OGRE_BRUTE.get().create(level);
            case MAGE -> ModEntities.OGRE_MAGE.get().create(level);
            case TRAPPER -> ModEntities.OGRE_TRAPPER.get().create(level);
            default -> ModEntities.OGRE_GRUNT_CAPTAIN.get().create(level);
        };
        if (!(mob instanceof OgreGrunt ogre)) {
            return false;
        }
        ogre.moveTo(x, y, z, yaw, 0.0F);
        if (!level.noCollision(ogre)) {
            ogre.discard();
            return false;
        }
        ogre.finalizeSpawn(level, difficulty, MobSpawnType.MOB_SUMMONED, null);
        ogre.joinArmy(player, ticks);
        level.addFreshEntity(ogre);
        puff(level, x, y, z);
        return true;
    }

    private static void puff(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.POOF, x, y + 1.0, z, 14, 0.5, 0.8, 0.5, 0.04);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.2, z, 8, 0.4, 0.1, 0.4, 0.02);
    }
}
