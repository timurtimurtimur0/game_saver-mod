package com.example.examplemod;

import com.example.examplemod.blockentity.ModEntity;
import com.example.examplemod.blocks.ModBlocks;
import com.example.examplemod.items.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;


@Mod(Game_Saver.MODID)
public class Game_Saver
{

    public static final String MODID = "game_saver";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Game_Saver(IEventBus modEventBus, ModContainer modContainer)
    {

        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntity.ENTITIES.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC, "game_saver-server.toml");
        //NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(ModItems.Game_Saver_Item.get());

        }
    }



}
