package com.example.examplemod.blocks;

import com.example.examplemod.Game_Saver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Game_Saver.MODID);

    public static final DeferredBlock<Block> Game_Saver_Block = BLOCKS.registerBlock("game_saver", GameSaverBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK)
            .sound(SoundType.GLASS).noOcclusion().
            lightLevel(state -> state.getValue(GameSaverBlock.LIGHT) ? 15 : 0));

}
