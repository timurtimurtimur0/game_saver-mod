package com.example.examplemod.world_rollback;

import com.example.examplemod.world_changes.LevelRewindData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="game_saver")
public class DeleteEntities {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {

        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel serverLevel) {
            Entity entity = event.getEntity();
            LevelRewindData data = LevelRewindData.get(serverLevel);

            if (data.getGlobalSaverPos() != null && !(entity instanceof Player)) {

                // Kills mobs that spawned from loaded chunks after a rollback
                if (data.shouldDelete(entity.getUUID())) {
                    event.setCanceled(true); // Bans spawning
                    return;
                }

                // Monitors new spawns
                if (!event.loadedFromDisk() && !entity.getTags().contains("rewind_revived")) {
                    if (entity instanceof LivingEntity) {
                        data.logNewSpawn(entity.getUUID());
                    }
                }
            }
        }
    }
}
