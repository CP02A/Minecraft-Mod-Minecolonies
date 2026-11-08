// Adapted from NeoForge 21.1 (net.neoforged.neoforge.capabilities), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.capabilities;

import com.ldtteam.shim.fabric.capabilities.ShimCapabilityHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Block capability. On Fabric there is no {@code Level#getCapability} extension method; call
 * {@link #getCapability(Level, BlockPos, BlockState, BlockEntity, Object)} or {@link #getCapability(Level, BlockPos, Object)} directly.
 */
public final class BlockCapability<T, C extends @Nullable Object> extends BaseCapability<T, C>
{
    @SuppressWarnings("unchecked")
    public static <T, C extends @Nullable Object> BlockCapability<T, C> create(final Identifier name, final Class<T> typeClass, final Class<C> contextClass)
    {
        return (BlockCapability<T, C>) registry.create(name, typeClass, contextClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> BlockCapability<T, @Nullable Void> createVoid(final Identifier name, final Class<T> typeClass)
    {
        return create(name, typeClass, (Class<Void>) (Class<?>) void.class);
    }

    public static <T> BlockCapability<T, @Nullable Direction> createSided(final Identifier name, final Class<T> typeClass)
    {
        return create(name, typeClass, Direction.class);
    }

    public static synchronized List<BlockCapability<?, ?>> getAll()
    {
        return registry.getAll();
    }

    private static final CapabilityRegistry<BlockCapability<?, ?>> registry = new CapabilityRegistry<BlockCapability<?, ?>>(BlockCapability::new);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private BlockCapability(final Identifier name, final Class<?> typeClass, final Class<?> contextClass)
    {
        super(name, (Class) typeClass, (Class) contextClass);
    }

    private final Map<Block, List<IBlockCapabilityProvider<T, C>>> providers = new IdentityHashMap<>();
    private final List<IBlockCapabilityProvider<T, C>> fallbacks = new CopyOnWriteArrayList<>();

    /**
     * Shim-internal: register a provider that is queried for every block after all specific providers.
     */
    @ApiStatus.Internal
    public void registerFallback(final IBlockCapabilityProvider<T, C> provider)
    {
        fallbacks.add(provider);
    }

    synchronized void addProvider(final Block block, final IBlockCapabilityProvider<T, C> provider)
    {
        providers.computeIfAbsent(block, b -> new ArrayList<>()).add(provider);
    }

    boolean hasProvider(final Block block)
    {
        return providers.containsKey(block);
    }

    @Nullable
    public T getCapability(final Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity blockEntity, final C context)
    {
        ShimCapabilityHooks.ensureInitialized();
        pos = pos.immutable();
        if (blockEntity == null)
        {
            if (state == null)
            {
                state = level.getBlockState(pos);
            }
            if (state.hasBlockEntity())
            {
                blockEntity = level.getBlockEntity(pos);
            }
        }
        else if (state == null)
        {
            state = blockEntity.getBlockState();
        }

        for (final IBlockCapabilityProvider<T, C> provider : providers.getOrDefault(state.getBlock(), List.of()))
        {
            final T ret = provider.getCapability(level, pos, state, blockEntity, context);
            if (ret != null)
            {
                return ret;
            }
        }
        for (final IBlockCapabilityProvider<T, C> provider : fallbacks)
        {
            final T ret = provider.getCapability(level, pos, state, blockEntity, context);
            if (ret != null)
            {
                return ret;
            }
        }
        return null;
    }

    /**
     * Replacement for NeoForge's {@code level.getCapability(cap, pos, context)}.
     */
    @Nullable
    public T getCapability(final Level level, final BlockPos pos, final C context)
    {
        return getCapability(level, pos, null, null, context);
    }
}
