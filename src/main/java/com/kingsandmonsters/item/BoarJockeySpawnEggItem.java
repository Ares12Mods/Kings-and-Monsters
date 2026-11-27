package com.kingsandmonsters.item;

import com.kingsandmonsters.ModEntities;
import com.kingsandmonsters.entity.MudsnoutBoar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;

/** Spawns a Mudsnout Boar with an Ogre Archer riding it, for testing the jockeys. */
public class BoarJockeySpawnEggItem extends ForgeSpawnEggItem {
    public BoarJockeySpawnEggItem(int backgroundColor, int highlightColor, Properties properties) {
        super(ModEntities.MUDSNOUT_BOAR, backgroundColor, highlightColor, properties);
    }

    @Override
    @SuppressWarnings("deprecation") // Required 1.21.1 OverrideOnly spawn hook; no replacement exists in this target.
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        MudsnoutBoar boar = ModEntities.MUDSNOUT_BOAR.get().create(server);
        if (boar == null) {
            return InteractionResult.FAIL;
        }
        boar.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, server.random.nextFloat() * 360.0F, 0.0F);
        boar.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null, null);
        boar.addJockey(server, server.getCurrentDifficultyAt(pos));
        server.addFreshEntityWithPassengers(boar);
        ItemUtil.consume(context.getItemInHand(), 1, context.getPlayer());
        return InteractionResult.CONSUME;
    }
}
