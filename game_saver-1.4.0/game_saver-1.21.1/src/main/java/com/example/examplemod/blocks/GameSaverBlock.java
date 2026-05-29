package com.example.examplemod.blocks;

import com.example.examplemod.world_changes.LevelRewindData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GameSaverBlock extends Block {

    public static final BooleanProperty LIGHT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = makeShape();

    public GameSaverBlock(Properties prop) {
        super(prop);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIGHT, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private static VoxelShape makeShape(){
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.453125, 0, 0.390625, 0.671875, 0.0125, 0.609375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.50625, 0.009375, 0.44375, 0.61875, 0.50625, 0.55625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.33749999999999997, 0.609375, 0.46875, 0.796875, 0.621875, 0.53125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.46875, 0.0125, 0.40625, 0.65625, 0.025, 0.59375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.484375, 0.025, 0.421875, 0.640625, 0.0375, 0.578125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.534375, 0.609375, 0.27499999999999997, 0.596875, 0.621875, 0.73125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.46875, 0.503125, 0.40625, 0.65625, 0.690625, 0.59375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.325, 0.465625, 0.271875, 0.33125, 0.809375, 0.734375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.325, 0.465625, 0.734375, 0.8, 0.809375, 0.740625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.796875, 0.465625, 0.271875, 0.803125, 0.809375, 0.734375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.325, 0.465625, 0.265625, 0.8, 0.809375, 0.271875), BooleanOp.OR);

        return shape;
    }


    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit){
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel){
            LevelRewindData data = LevelRewindData.get(serverLevel);

            boolean isLIGHT = state.getValue(LIGHT);



            if (!isLIGHT){

                // Code searches for the old lamp and turns it off
                BlockPos oldPos = data.getActiveLampPos();

                if (oldPos != null && !oldPos.equals(pos)) {

                    BlockState oldState = serverLevel.getBlockState(oldPos);

                    if (oldState.is(this) && oldState.hasProperty(LIGHT) && oldState.getValue(LIGHT)) {
                        serverLevel.setBlock(oldPos, oldState.setValue(LIGHT, false), 3);
                    }
                }

                level.setBlock(pos, state.setValue(LIGHT, true), 3);

                data.setActiveLampPos(pos);

                data.clearLog();

                data.saveAllPlayers(serverLevel, player.blockPosition());

                level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 3.0f, 1.0f);

                level.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("Checkpoint has been created")
                        .withStyle(ChatFormatting.GREEN), false);

            }
            else{

                level.setBlock(pos, state.setValue(LIGHT, false), 3);

                data.clearAllData();

                level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 3.0F, 0.7F);

                level.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("Checkpoint has been deleted")
                        .withStyle(ChatFormatting.GRAY), false);

            }

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT);
    }
}
