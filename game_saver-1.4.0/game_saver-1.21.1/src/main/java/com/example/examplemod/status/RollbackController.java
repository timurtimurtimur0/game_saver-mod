package com.example.examplemod.status;

import com.example.examplemod.Game_Saver;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.LinkedList;
import java.util.Queue;

@EventBusSubscriber(modid = Game_Saver.MODID)
public class RollbackController {

    private static final Queue<Runnable> later = new LinkedList<>();
    private static int delayTicks = 0;

    public static void Controller(int ticks, Runnable task){
        delayTicks = ticks;
        later.add(task);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (delayTicks > 0) {
            delayTicks--;
        }
        else if (!later.isEmpty()) {
            later.poll().run();

        }

    }
}
