package com.example.examplemod.items;

import com.example.examplemod.Game_Saver;
import com.example.examplemod.blocks.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Game_Saver.MODID);

    public static final DeferredItem<BlockItem> Game_Saver_Item = ITEMS.registerSimpleBlockItem("game_saver", ModBlocks.Game_Saver_Block);
}
