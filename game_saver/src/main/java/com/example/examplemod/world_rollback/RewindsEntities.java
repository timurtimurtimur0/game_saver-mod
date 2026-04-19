package com.example.examplemod.world_rollback;

import com.example.examplemod.world_changes.LevelRewindData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "game_saver", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RewindsEntities {
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        Level level = event.getEntity().level();

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            LevelRewindData data = LevelRewindData.get(serverLevel);

            if (data.getGlobalSaverPos() != null) {
                if (!(event.getEntity() instanceof Player)) {
                    data.logEntityDeath(event.getEntity());
                }
            }
        }
    }
}
