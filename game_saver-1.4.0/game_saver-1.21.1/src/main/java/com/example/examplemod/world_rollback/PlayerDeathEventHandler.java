package com.example.examplemod.world_rollback;

import com.example.examplemod.Game_Saver;
import com.example.examplemod.status.RollbackController;
import com.example.examplemod.world_changes.LevelRewindData;
import com.example.examplemod.world_changes.WorldAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.*;

@EventBusSubscriber(modid = Game_Saver.MODID)
public class PlayerDeathEventHandler {

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerDeath(LivingDeathEvent event) {
        // It checks that it is indeed the player who is dying, and the logic is executed on the server
        if (event.getEntity() instanceof ServerPlayer diedplayer) {
            ServerLevel overworld = diedplayer.getServer().getLevel(Level.OVERWORLD);
            if (overworld == null) return;

            LevelRewindData data = LevelRewindData.get(overworld);
            BlockPos respawnPos = data.getGlobalSaverPos();

            if (respawnPos != null) {
                // Reverses death
                event.setCanceled(true);

                overworld.setDayTime(data.getSavedTime());

                HolderLookup.Provider registry = overworld.getServer().registryAccess();

                List<ServerPlayer> allPlayers = diedplayer.getServer().getPlayerList().getPlayers();

                // Rollback of all players
                for (ServerPlayer player : allPlayers) {
                    CompoundTag savedData = data.getPlayerData(player.getUUID());

                    // Removes burning and potion effects
                    player.removeAllEffects();
                    player.clearFire();

                    //Reduces fall damage
                    player.resetFallDistance();

                    if (savedData != null) {
                        // Restores health
                        if (savedData.getFloat("Health") <= 0.0f || !savedData.contains("Health")) {
                            player.setHealth(player.getMaxHealth());
                        } else {
                            player.setHealth(savedData.getFloat("Health"));
                        }

                        // Restores inventory
                        player.getInventory().clearContent();
                        player.getInventory().load(savedData.getList("Inventory", Tag.TAG_COMPOUND));

                        // This is important for the server
                        player.containerMenu.broadcastChanges();
                        player.inventoryMenu.slotsChanged(player.getInventory());

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


                    if (player.level().dimension() != Level.OVERWORLD && overworld != null) {
                        // If a players dies in the Nether or End, they are teleported to the Overworld
                        player.teleportTo(overworld, respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());

                        //player.setHealth(savedData.getFloat("Health"));
                    } else {
                        // If a players dies in the Overworld, they are just teleported
                        player.teleportTo(respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5);
                    }

                    int CountChunks = 100 + (data.SetChunks() / 4);

                    // The screen becomes darkened
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, CountChunks, 100, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CountChunks, 5, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, CountChunks, 255, false, false));
                }

                RollbackController.Controller(10, () -> {
                    // Restores blocks
                    List<WorldAction> log = data.getLog();
                    for (int i = log.size() - 1; i >= 0; i--) {
                        WorldAction action = log.get(i);
                        overworld.setBlock(action.pos, action.previosState, 3);
                    }
                    data.clearLog();
                    data.InstantRollback(overworld); // Restores mobs and loads chunks


                });
            }
        }
    }
}
