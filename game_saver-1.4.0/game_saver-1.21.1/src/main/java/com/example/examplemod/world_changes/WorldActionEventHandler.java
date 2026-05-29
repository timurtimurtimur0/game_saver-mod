package com.example.examplemod.world_changes;

import com.example.examplemod.Game_Saver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = Game_Saver.MODID)
public class WorldActionEventHandler {

    // Tracks block destruction by players
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension() != Level.OVERWORLD) return;

            // Let's get our storage for this measurement
            LevelRewindData data = LevelRewindData.get(serverLevel);

            if (data.getGlobalSaverPos() != null) {
                BlockState brokenState = event.getState();

                // Writes an event of type BREAK
                data.addAction(new WorldAction(event.getPos(), brokenState, WorldAction.Actiontype.BREAK), serverLevel);
            }
        }
    }

    // Monitors the destruction of blocks by explosions
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension() != Level.OVERWORLD) return;

            LevelRewindData data = LevelRewindData.get(serverLevel);

            if (data.getGlobalSaverPos() != null) {
                for (var pos : event.getAffectedBlocks()) {
                    BlockState state = serverLevel.getBlockState(pos);

                    // Ignores the air and logs an event of type EXPLOSION
                    if (!state.isAir()) {
                        data.addAction(new WorldAction(pos, state, WorldAction.Actiontype.EXPLOSION), serverLevel);
                    }
                }
            }
        }
    }

    // Tracks the installation of blocks
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension() != Level.OVERWORLD) return;

            LevelRewindData data = LevelRewindData.get(serverLevel);
            if (data.getGlobalSaverPos() != null) {

                // getBlockSnapshot() contains the block's previous state and returns it
                // if the player or any entity has placed the block
                BlockState previousState = event.getBlockSnapshot().getState();
                // Writes an event of type PLACE
                data.addAction(new WorldAction(event.getPos(), previousState, WorldAction.Actiontype.PLACE), serverLevel);

            }
        }
    }


}