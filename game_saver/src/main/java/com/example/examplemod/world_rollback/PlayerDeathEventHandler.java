package com.example.examplemod.world_rollback;

import com.example.examplemod.world_changes.LevelRewindData;
import com.example.examplemod.world_changes.WorldAction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.Entity;

import java.util.*;

@Mod.EventBusSubscriber(modid="game_saver", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerDeathEventHandler {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // It checks that it is indeed the player who is dying, and the logic is executed on the server
        if (event.getEntity() instanceof ServerPlayer diedplayer && diedplayer.level() instanceof ServerLevel serverLevel) {
            LevelRewindData data = LevelRewindData.get(serverLevel);
            BlockPos respawnPos = data.getGlobalSaverPos();

            if (respawnPos != null) {
                // Reverses death
                event.setCanceled(true);

                serverLevel.setDayTime(data.getSavedTime());

                List<WorldAction> log = data.getLog();
                for (int i = log.size() - 1; i >= 0; i--) {
                    WorldAction action = log.get(i);
                    serverLevel.setBlock(action.pos, action.previosState, 3);
                }

                data.clearLog();

                data.prepareDeletions();
                List<Entity> toRemove = new ArrayList<>();

                //Removing dropped items
                for (Entity entity : serverLevel.getAllEntities()) {
                    if (entity instanceof ItemEntity) {
                        toRemove.add(entity);
                    }

                    else if(data.shouldDelete(entity.getUUID())){
                        toRemove.add(entity);
                    }
                }

                for (Entity entity : toRemove) {
                    entity.discard();
                }

                // creates a queue instead of an instant spawn
                data.prepareMobRestoration();

                // Rollback of all players
                for (ServerPlayer player : serverLevel.players()) {
                    CompoundTag savedData = data.getPlayerData(player.getUUID());

                    // Removes burning and potion effects
                    player.removeAllEffects();
                    player.clearFire();

                    //Reduces fall damage
                    player.resetFallDistance();

                    if (savedData != null) {
                        // Restores health
                        player.setHealth(savedData.getFloat("Health"));

                        // Restores inventory
                        player.getInventory().clearContent(); // Сначала очищаем текущий мусор
                        player.getInventory().load(savedData.getList("Inventory", Tag.TAG_COMPOUND));

                        //Restores changes made during the save
                        if (savedData.contains("Effects", Tag.TAG_LIST)) {
                            ListTag effectsList = savedData.getList("Effects", Tag.TAG_COMPOUND);
                            for (int i = 0; i < effectsList.size(); i++) {
                                MobEffectInstance effect = MobEffectInstance.load(effectsList.getCompound(i));
                                if (effect != null) {
                                    player.addEffect(effect);
                                }
                            }
                        }

                    } else {
                        // If the player wasn't on the server when the save was made, they are simply healed
                        player.setHealth(player.getMaxHealth());
                    }

                    // Teleports to the block
                    player.teleportTo(respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5);

                    // The screen becomes darkened
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 100, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 5, false, false));

                }


            }
        }
    }
}
