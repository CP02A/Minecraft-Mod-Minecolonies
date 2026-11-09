package com.ldtteam.shim.fabric.capabilities;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import com.ldtteam.shim.neoforge.capabilities.Capabilities;
import com.ldtteam.shim.neoforge.items.IItemHandler;
import com.ldtteam.shim.neoforge.items.wrapper.InvWrapper;
import com.ldtteam.shim.neoforge.items.wrapper.SidedInvWrapper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Bridges the shim's {@link Capabilities.ItemHandler#BLOCK} capability with Fabric's {@link ItemStorage#SIDED}:
 * <ul>
 *     <li>block entities/blocks registered for the item handler capability are exposed to other Fabric mods as {@code Storage<ItemVariant>};</li>
 *     <li>vanilla containers and any Fabric {@code Storage<ItemVariant>} are visible as {@link IItemHandler}.</li>
 * </ul>
 */
public final class FabricTransferBridge
{
    /**
     * Recursion guard: prevents ping-pong between our SIDED export and our SIDED fallback.
     */
    private static final ThreadLocal<Boolean> IN_BRIDGE = ThreadLocal.withInitial(() -> false);

    private static final Set<BlockEntityType<?>> EXPOSED_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Block> EXPOSED_BLOCKS = Collections.newSetFromMap(new IdentityHashMap<>());

    private FabricTransferBridge() {}

    @Nullable
    public static Storage<ItemVariant> toStorage(@Nullable final IItemHandler handler)
    {
        if (handler == null)
        {
            return null;
        }
        if (handler instanceof StorageItemHandler wrapped)
        {
            return wrapped.getStorage();
        }
        return new ItemHandlerStorage(handler);
    }

    @Nullable
    public static IItemHandler toItemHandler(@Nullable final Storage<ItemVariant> storage)
    {
        if (storage == null)
        {
            return null;
        }
        if (storage instanceof ItemHandlerStorage wrapped)
        {
            return wrapped.getHandler();
        }
        return new StorageItemHandler(storage);
    }

    @Nullable
    private static Storage<ItemVariant> exportFromCapability(final Level level, final BlockPos pos, final BlockState state, @Nullable final BlockEntity be, @Nullable final Direction dir)
    {
        if (IN_BRIDGE.get())
        {
            return null;
        }
        IN_BRIDGE.set(true);
        try
        {
            return toStorage(Capabilities.ItemHandler.BLOCK.getCapability(level, pos, state, be, dir));
        }
        finally
        {
            IN_BRIDGE.set(false);
        }
    }

    /**
     * Called by RegisterCapabilitiesEvent for item handler block entity registrations.
     */
    public static synchronized void exposeBlockEntity(final BlockEntityType<?> type)
    {
        if (EXPOSED_TYPES.add(type))
        {
            // Register per block: block entity types can share blocks (e.g. the generic colony building type and the warehouse type both
            // list the warehouse hut), and Fabric warns about a second provider for the same block. The provider is generic anyway
            // (it resolves the capability for the actual block entity), so one registration per block is enough.
            final List<Block> blocks = new ArrayList<>();
            for (final Block block : BuiltInRegistries.BLOCK)
            {
                if (type.isValid(block.defaultBlockState()))
                {
                    blocks.add(block);
                }
            }
            exposeBlocks(blocks.toArray(new Block[0]));
        }
    }

    /**
     * Called by RegisterCapabilitiesEvent for item handler block registrations.
     */
    public static synchronized void exposeBlocks(final Block... blocks)
    {
        for (final Block block : blocks)
        {
            if (EXPOSED_BLOCKS.add(block) && ItemStorage.SIDED.getProvider(block) == null)
            {
                ItemStorage.SIDED.registerForBlocks(FabricTransferBridge::exportFromCapability, block);
            }
        }
    }

    /**
     * Vanilla / Fabric fallback for {@link Capabilities.ItemHandler#BLOCK}.
     */
    @Nullable
    static IItemHandler blockFallback(final Level level, final BlockPos pos, final BlockState state, @Nullable final BlockEntity be, @Nullable final Direction side)
    {
        if (IN_BRIDGE.get())
        {
            return null;
        }
        final Block block = state.getBlock();

        // A specific Fabric provider (another mod's block, or Fabric's composter wrapper) wins over generic container wrapping.
        if (ItemStorage.SIDED.getProvider(block) == null)
        {
            if (be instanceof Container container)
            {
                if (block instanceof ChestBlock chestBlock)
                {
                    final Container chest = ChestBlock.getContainer(chestBlock, state, level, pos, true);
                    if (chest != null)
                    {
                        return new InvWrapper(chest);
                    }
                }
                if (container instanceof WorldlyContainer worldly && side != null)
                {
                    return new SidedInvWrapper(worldly, side);
                }
                return new InvWrapper(container);
            }
            if (block instanceof WorldlyContainerHolder holder)
            {
                final WorldlyContainer container = holder.getContainer(state, level, pos);
                if (container != null)
                {
                    return side == null ? new InvWrapper(container) : new SidedInvWrapper(container, side);
                }
            }
        }

        IN_BRIDGE.set(true);
        try
        {
            return toItemHandler(ItemStorage.SIDED.find(level, pos, state, be, side));
        }
        finally
        {
            IN_BRIDGE.set(false);
        }
    }
}
