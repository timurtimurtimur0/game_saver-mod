package com.example.examplemod.blockentity;

import com.example.examplemod.Game_Saver;
import com.example.examplemod.blocks.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntity {

    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Game_Saver.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GameSaverEntity>> Game_Saver_Entity = ENTITIES.register("game_saver", () -> BlockEntityType.Builder.of(GameSaverEntity::new, ModBlocks.Game_Saver_Block.get()).build(null));
}
