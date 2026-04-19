package com.example.examplemod.world_rollback;

import com.example.examplemod.world_changes.LevelRewindData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

//This class is designed to gradually respawn and restore mobs around the player after the player dies
@Mod.EventBusSubscriber(modid = "game_saver")
public class PlayerTickHandler {

    private static int checkTimer = 0;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event){
        if(event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer){

            // Checks every 20 ticks
            if (serverPlayer.tickCount % 20 == 0){
                ServerLevel level = serverPlayer.serverLevel();
                LevelRewindData data = LevelRewindData.get(level);

                if (data.getGlobalSaverPos() != null) {
                    // Respawns mobs within a 128-block radius
                    data.processPendingRestores(level, serverPlayer.blockPosition(), 128.0);
                }
            }
        }
    }
}
