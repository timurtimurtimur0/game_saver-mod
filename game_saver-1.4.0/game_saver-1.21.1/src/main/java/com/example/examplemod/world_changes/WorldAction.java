package com.example.examplemod.world_changes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;

public class WorldAction {
    public final BlockPos pos;
    public final BlockState previosState;
    public final Actiontype type;

    public enum Actiontype{
        BREAK, PLACE, EXPLOSION
    }

    public WorldAction(BlockPos pos, BlockState previosState, Actiontype type){
        this.pos = pos;
        this.previosState = previosState;
        this.type = type;
    }

    // Saving actions in NBT
    public CompoundTag save(HolderLookup.Provider registries){
        CompoundTag tag = new CompoundTag();

        tag.putInt("PosX", this.pos.getX());
        tag.putInt("PosY", this.pos.getY());
        tag.putInt("PosZ", this.pos.getZ());


        BlockState.CODEC.encodeStart(NbtOps.INSTANCE, this.previosState)
                .result()
                .ifPresent(blockStateTag -> tag.put("State", blockStateTag));

        tag.putString("Type", this.type.name());


        return tag;
    }

    // Reading an action from NBT
    public static WorldAction load(CompoundTag tag, HolderLookup.Provider registries){
        BlockPos pos = new BlockPos(tag.getInt("PosX"), tag.getInt("PosY"), tag.getInt("PosZ"));
        // Reading BlockState
        BlockState state = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("State"));
        Actiontype type = Actiontype.valueOf(tag.getString("Type"));
        return new WorldAction(pos, state, type);
    }


}
