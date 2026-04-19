package com.example.examplemod.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GameSaverEntity extends BlockEntity {

    public GameSaverEntity(BlockPos pos, BlockState state){
        super(ModEntity.Game_Saver_Entity.get(), pos, state);
    }
}
