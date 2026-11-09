package com.ldtteam.shim.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stand-in for NeoForge's level-aware {@code IBlockExtension#rotate(BlockState, LevelAccessor, BlockPos, Rotation)}: blocks that
 * need to rotate their block entity data (e.g. Domum Ornamentum dynamic timber frames) implement this, and structure placement
 * calls {@link #rotateInLevel(BlockState, LevelAccessor, BlockPos, Rotation)} instead of the plain vanilla rotate.
 */
public interface IBlockRotationExtension
{
    BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation);

    /**
     * Rotates a placed block state, giving blocks implementing this interface access to the level.
     */
    static BlockState rotateInLevel(final BlockState state, final LevelAccessor level, final BlockPos pos, final Rotation rotation)
    {
        if (state.getBlock() instanceof IBlockRotationExtension extension)
        {
            return extension.rotate(state, level, pos, rotation);
        }
        return state.rotate(rotation);
    }
}
