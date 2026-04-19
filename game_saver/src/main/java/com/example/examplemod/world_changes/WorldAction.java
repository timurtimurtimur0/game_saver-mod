package com.example.examplemod.world_changes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
    public CompoundTag save(){
        CompoundTag tag = new CompoundTag();
        tag.put("Pos", NbtUtils.writeBlockPos(this.pos));
        tag.put("State", NbtUtils.writeBlockState(this.previosState));
        tag.putString("Type", this.type.name());
        return tag;
    }

    // Reading an action from NBT
    public static WorldAction load(CompoundTag tag){
        BlockPos pos = NbtUtils.readBlockPos(tag.getCompound("Pos"));
        // Reading BlockState
        BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("State"));
        Actiontype type = Actiontype.valueOf(tag.getString("Type"));
        return new WorldAction(pos, state, type);
    }


}
