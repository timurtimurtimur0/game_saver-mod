package com.example.examplemod.blocks;

import com.example.examplemod.Game_Saver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Game_Saver.MODID);

    public static final RegistryObject<Block> Game_Saver_Block = BLOCKS.register("game_saver", () -> new GameSaverBlock(BlockBehaviour.Properties.copy(Blocks.BEDROCK)
            .sound(SoundType.GLASS).noOcclusion().
            lightLevel(state -> state.getValue(GameSaverBlock.LIGHT) ? 15 : 0)));

}
