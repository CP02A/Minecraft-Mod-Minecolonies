// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.util.BlockSnapshot), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Snapshot of a block position (state only; block entity data is not captured in the Fabric shim).
 */
public class BlockSnapshot
{
    private final ResourceKey<Level> dim;
    private final BlockPos pos;
    private final int flags;
    private final BlockState state;
    private final WeakReference<LevelAccessor> level;

    private BlockSnapshot(final ResourceKey<Level> dim, final LevelAccessor level, final BlockPos pos, final BlockState state, final int flags)
    {
        this.dim = dim;
        this.pos = pos.immutable();
        this.state = state;
        this.flags = flags;
        this.level = new WeakReference<>(level);
    }

    public static BlockSnapshot create(final ResourceKey<Level> dim, final LevelAccessor level, final BlockPos pos, final int flag)
    {
        return new BlockSnapshot(dim, level, pos, level.getBlockState(pos), flag);
    }

    public static BlockSnapshot create(final ResourceKey<Level> dim, final LevelAccessor level, final BlockPos pos)
    {
        return create(dim, level, pos, 3);
    }

    public ResourceKey<Level> getDimension()
    {
        return dim;
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public int getFlags()
    {
        return flags;
    }

    public BlockState getState()
    {
        return state;
    }

    @Nullable
    public LevelAccessor getLevel()
    {
        return level.get();
    }

    public BlockState getCurrentState()
    {
        final LevelAccessor l = getLevel();
        return l == null ? Blocks.AIR.defaultBlockState() : l.getBlockState(pos);
    }

    public boolean restoreToLocation(final LevelAccessor level, final BlockPos pos, final int flags)
    {
        final BlockState current = level.getBlockState(pos);
        if (!level.setBlock(pos, state, flags))
        {
            return false;
        }
        if (level instanceof Level realLevel)
        {
            realLevel.sendBlockUpdated(pos, current, state, flags);
        }
        return true;
    }

    public boolean restore(final int flags)
    {
        final LevelAccessor l = getLevel();
        return l != null && restoreToLocation(l, pos, flags);
    }

    public boolean restore()
    {
        return restore(flags);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof BlockSnapshot other))
        {
            return false;
        }
        return dim.equals(other.dim) && pos.equals(other.pos) && state == other.state && flags == other.flags;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(dim, pos, state, flags);
    }

    @Override
    public String toString()
    {
        return "BlockSnapshot[World:" + dim.identifier() + ",Pos:" + pos + ",State:" + state + "]";
    }
}
