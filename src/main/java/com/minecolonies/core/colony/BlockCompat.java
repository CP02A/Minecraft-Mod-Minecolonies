package com.minecolonies.core.colony;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Replacements for block state helpers removed in 26.x vanilla.
 */
public final class BlockCompat
{
    private BlockCompat()
    {
    }

    /**
     * Replacement for the removed {@code BlockStateBase#blocksMotion()} (same logic as 1.21.1).
     */
    public static boolean blocksMotion(final BlockState state)
    {
        return !state.is(Blocks.COBWEB) && !state.is(Blocks.BAMBOO_SAPLING) && state.isSolid();
    }
}
