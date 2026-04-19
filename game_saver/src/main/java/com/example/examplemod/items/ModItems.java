package com.example.examplemod.items;

import com.example.examplemod.Game_Saver;
import com.example.examplemod.blocks.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Game_Saver.MODID);

    public static final RegistryObject<Item> Game_Saver_Item = ITEMS.register("game_saver", () -> new BlockItem(ModBlocks.Game_Saver_Block.get(), new Item.Properties()));
}
