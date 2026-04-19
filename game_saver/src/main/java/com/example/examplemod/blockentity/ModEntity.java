package com.example.examplemod.blockentity;

import com.example.examplemod.Game_Saver;
import com.example.examplemod.blocks.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntity {

    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Game_Saver.MODID);

    public static final RegistryObject<BlockEntityType<GameSaverEntity>> Game_Saver_Entity = ENTITIES.register("game_saver", () -> BlockEntityType.Builder.of(GameSaverEntity::new, ModBlocks.Game_Saver_Block.get()).build(null));
}
