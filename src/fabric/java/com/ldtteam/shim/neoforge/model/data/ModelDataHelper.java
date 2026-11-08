package com.ldtteam.shim.neoforge.model.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Fabric replacements for NeoForge's model data extension methods (which cannot exist on vanilla classes).
 *
 * <ul>
 *     <li>A block entity that supplies model data overrides Fabric's {@code BlockEntity#getRenderData()} and returns
 *     its {@link ModelData} there (a plain {@code getModelData()} method can stay, just return it from
 *     {@code getRenderData()}).</li>
 *     <li>{@code blockEntity.getModelData()} on an arbitrary block entity becomes {@link #getModelData(BlockEntity)}.</li>
 *     <li>{@code level.getModelData(pos)} / {@code BlockState#getModelData(level, pos)} becomes
 *     {@link #getModelData(BlockGetter, BlockPos)} (safe during chunk meshing).</li>
 *     <li>{@code blockEntity.requestModelDataUpdate()} becomes {@link #requestModelDataUpdate(BlockEntity)}.</li>
 * </ul>
 */
public final class ModelDataHelper
{
    private ModelDataHelper()
    {
    }

    /**
     * @return the model data of the block entity (its Fabric render data if that is a {@link ModelData}), never null.
     */
    public static ModelData getModelData(final @Nullable BlockEntity blockEntity)
    {
        if (blockEntity == null)
        {
            return ModelData.EMPTY;
        }
        return blockEntity.getRenderData() instanceof final ModelData data ? data : ModelData.EMPTY;
    }

    /**
     * Thread-safe lookup through Fabric's {@code getBlockEntityRenderData}; use this from models and tint sources.
     */
    public static ModelData getModelData(final @Nullable BlockGetter level, final BlockPos pos)
    {
        if (level == null)
        {
            return ModelData.EMPTY;
        }
        return level.getBlockEntityRenderData(pos) instanceof final ModelData data ? data : ModelData.EMPTY;
    }

    /**
     * Requests a re-render of the block entity's block on the client so that the next chunk mesh picks up the new
     * model data. No-op on the server or without a level.
     */
    public static void requestModelDataUpdate(final BlockEntity blockEntity)
    {
        final Level level = blockEntity.getLevel();
        if (level != null && level.isClientSide())
        {
            final ModelDataManager manager = ModelDataManager.forLevel(level);
            if (manager != null)
            {
                manager.requestRefresh(blockEntity.getBlockPos());
            }
            final BlockState state = blockEntity.getBlockState();
            level.sendBlockUpdated(blockEntity.getBlockPos(), state, state, Block.UPDATE_IMMEDIATE);
        }
    }
}
