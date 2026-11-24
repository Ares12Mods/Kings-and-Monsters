package com.kingsandmonsters.advancement;

import com.kingsandmonsters.KingsAndMonsters;
import com.kingsandmonsters.entity.MudsnoutBoar;
import com.kingsandmonsters.entity.OgreBrute;
import com.kingsandmonsters.entity.OgreGrunt;
import com.kingsandmonsters.entity.OgreGruntCaptain;
import com.kingsandmonsters.entity.OgreLord;
import com.kingsandmonsters.entity.OgreMage;
import com.kingsandmonsters.entity.OgreTrapper;
import com.kingsandmonsters.ModMobEffects;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

public final class RoyalAdvancementEvents {
    private static final String CRITERION = "defeated";

    private RoyalAdvancementEvents() {
    }

    /** Awards an advancement whose only criterion is "done", for the ones the mod triggers itself. */
    public static void awardPath(ServerPlayer player, String path) {
        awardAt(player, path, "done");
    }

    /** Breeding two Mudsnout Boars. */
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (event.getParentA() instanceof MudsnoutBoar && event.getCausedByPlayer() instanceof ServerPlayer player) {
            awardPath(player, "boars/born_in_the_mud");
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getEntity() instanceof OgreGrunt && event.getEntity().getVehicle() instanceof MudsnoutBoar) {
            awardPath(player, "boars/unhorsed");
        }

        if (event.getEntity() instanceof OgreLord) {
            award(player, "usurped");
            if (player.hasEffect(ModMobEffects.OGRES_BANE.get())) award(player, "swamps_champion");
        } else if (event.getEntity() instanceof OgreMage) {
            award(player, "mage_captain");
        } else if (event.getEntity() instanceof OgreBrute) {
            award(player, "brute_captain");
        } else if (event.getEntity() instanceof OgreTrapper) {
            award(player, "trapper");
        } else if (event.getEntity() instanceof OgreGruntCaptain) {
            award(player, "grunt_captain");
        }
    }

    public static void awardBogfangRupture(ServerPlayer player) {
        awardAt(player, "the_swamp_has_teeth", "ruptured");
    }

    public static void awardBogburstChain(ServerPlayer player) {
        awardAt(player, "chain_reaction", "spread");
    }

    private static void award(ServerPlayer player, String path) {
        awardAt(player, "royal_defenses/" + path, CRITERION);
    }

    private static void awardAt(ServerPlayer player, String path, String criterion) {
        ResourceLocation id = new ResourceLocation(
                KingsAndMonsters.MODID, path);
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
