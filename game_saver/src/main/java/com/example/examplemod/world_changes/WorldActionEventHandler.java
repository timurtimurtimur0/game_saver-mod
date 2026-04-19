package com.example.examplemod.world_changes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "game_saver")
public class WorldActionEventHandler {

    // Tracks block destruction by players
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {

            // Let's get our storage for this measurement
            LevelRewindData data = LevelRewindData.get(serverLevel);

            if (data.getGlobalSaverPos() != null) {
                BlockState brokenState = event.getState();

                // Writes an event of type BREAK
                data.addAction(new WorldAction(event.getPos(), brokenState, WorldAction.Actiontype.BREAK));
            }
        }
    }

    // Monitors the destruction of blocks by explosions
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            LevelRewindData data = LevelRewindData.get(serverLevel);

            if (data.getGlobalSaverPos() != null) {
                for (var pos : event.getAffectedBlocks()) {
                    BlockState state = serverLevel.getBlockState(pos);

                    // Ignores the air and logs an event of type EXPLOSION
                    if (!state.isAir()) {
                        data.addAction(new WorldAction(pos, state, WorldAction.Actiontype.EXPLOSION));
                    }
                }
            }
        }
    }

    // Tracks the installation of blocks
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {

            LevelRewindData data = LevelRewindData.get(serverLevel);
            if (data.getGlobalSaverPos() != null) {

                // getBlockSnapshot() contains the block's previous state and returns it
                // if the player or any entity has placed the block
                BlockState previousState = event.getBlockSnapshot().getReplacedBlock();

                // Writes an event of type PLACE
                data.addAction(new WorldAction(event.getPos(), previousState, WorldAction.Actiontype.PLACE));

            }
        }
    }

}