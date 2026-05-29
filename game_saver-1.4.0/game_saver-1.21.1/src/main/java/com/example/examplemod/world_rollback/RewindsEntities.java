package com.example.examplemod.world_rollback;

import com.example.examplemod.Game_Saver;
import com.example.examplemod.world_changes.LevelRewindData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = Game_Saver.MODID)
public class RewindsEntities {

    //For mobs
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityDeath(LivingDeathEvent event) {
        Level level = event.getEntity().level();

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            LevelRewindData data = LevelRewindData.get(serverLevel);

            if (data.getGlobalSaverPos() != null) {
                if (!(event.getEntity() instanceof Player)) {
                    data.logEntityDeath(event.getEntity());
                }
            }
        }
    }

    //For other entities
    @SubscribeEvent
    public static void OtherEntityDeath(EntityLeaveLevelEvent event){
        Level level = event.getEntity().level();
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel){
            Entity entity = event.getEntity();

            if (!(entity instanceof ItemEntity)&&!(entity instanceof AbstractArrow)
                    && !(entity instanceof PrimedTnt) &&!(entity instanceof ExperienceOrb)
                    && !(entity instanceof LivingEntity)) {

                if ((entity.getRemovalReason() == Entity.RemovalReason.KILLED) || (entity.getRemovalReason() == Entity.RemovalReason.DISCARDED)) {
                    LevelRewindData data = LevelRewindData.get(serverLevel);

                    // If a checkpoint is currently active on the server
                    if (data.getGlobalSaverPos() != null) {

                        data.logEntityDeath(entity);
                    }
                }
            }
        }
    }
}
