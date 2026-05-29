package com.example.examplemod.status;

import com.example.examplemod.Config;
import com.example.examplemod.Game_Saver;
import com.example.examplemod.world_changes.LevelRewindData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Game_Saver.MODID)
public class StatusCommand {

    @SubscribeEvent
    public static void CommandRegister(RegisterCommandsEvent event){
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Registers the command
        dispatcher.register(Commands.literal("gamesaver").then(Commands.literal("status")
                .executes(context -> showStatus(context.getSource()))));

    }
    private static int showStatus(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        LevelRewindData data = LevelRewindData.get(level);

        int CurrentSize = data.getLog().size();
        int MaxSize = Config.MAX_LOG_SIZE.get();
        int totalDeadMobs = data.getDeadMobsCount();
        int toDeleteSize = data.getSpawnedAfterDeathCount();
        int chunkSize = data.getChunkSize();

        source.sendSuccess(() -> Component.literal("Blocks changed = " + CurrentSize + ";"), false);
        source.sendSuccess(() -> Component.literal("Max Block Log Size = " + MaxSize + ";"), false);
        source.sendSuccess(() -> Component.literal("Entities waiting for a respawn = " + totalDeadMobs + ";"), false);
        source.sendSuccess(() -> Component.literal("Entities waiting for a discard = " + toDeleteSize + ";"), false);
        source.sendSuccess(() -> Component.literal("Chunks in log = " + chunkSize + ";"), false);
        return 1;
    }
}
