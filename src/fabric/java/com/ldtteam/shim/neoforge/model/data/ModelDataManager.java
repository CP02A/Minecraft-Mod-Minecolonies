package com.ldtteam.shim.neoforge.model.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * NeoForge shim: per-level model data lookup.
 *
 * <p>On Fabric model data is not cached by the level: it is read from the block entity's render data whenever it is
 * needed (see {@link ModelDataHelper}). This manager therefore resolves {@link #getAt(BlockPos)} live from the level's
 * block entity, which keeps the NeoForge API (used by fake levels) working with the same results.
 */
public class ModelDataManager
{
    public static final Long2ObjectFunction<ModelData> EMPTY_SNAPSHOT = key -> ModelData.EMPTY;

    private static final Map<Level, ModelDataManager> MANAGERS = new WeakHashMap<>();

    private final Level level;

    public ModelDataManager(final Level level)
    {
        this.level = level;
        synchronized (MANAGERS)
        {
            MANAGERS.put(level, this);
        }
    }

    /**
     * @return the manager constructed for the given level, if any.
     */
    public static @Nullable ModelDataManager forLevel(final Level level)
    {
        synchronized (MANAGERS)
        {
            return MANAGERS.get(level);
        }
    }

    public void requestRefresh(final BlockPos pos)
    {
        // data is always resolved live from the block entity, nothing to invalidate
    }

    public void requestRefresh(final BlockEntity blockEntity)
    {
        requestRefresh(blockEntity.getBlockPos());
    }

    public ModelData getAt(final BlockPos pos)
    {
        return ModelDataHelper.getModelData(level.getBlockEntity(pos));
    }

    public Long2ObjectMap<ModelData> getAt(final SectionPos pos)
    {
        final Long2ObjectMap<ModelData> result = new Long2ObjectOpenHashMap<>();
        final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = pos.minBlockX(); x <= pos.maxBlockX(); x++)
        {
            for (int y = pos.minBlockY(); y <= pos.maxBlockY(); y++)
            {
                for (int z = pos.minBlockZ(); z <= pos.maxBlockZ(); z++)
                {
                    mutable.set(x, y, z);
                    final ModelData data = getAt(mutable);
                    if (data != ModelData.EMPTY)
                    {
                        result.put(mutable.asLong(), data);
                    }
                }
            }
        }
        return result;
    }
}
