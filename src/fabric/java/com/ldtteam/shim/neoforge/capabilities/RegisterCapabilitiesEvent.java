// Adapted from NeoForge 21.1 (net.neoforged.neoforge.capabilities), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.capabilities;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fabric.capabilities.FabricTransferBridge;
import com.ldtteam.shim.fml.event.IModBusEvent;
import com.ldtteam.shim.mixin.items.BlockEntityTypeAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Fired on the mod buses (once) to register capability providers.
 * Item handler block entity providers are additionally exposed to other Fabric mods via {@code ItemStorage.SIDED}.
 */
public class RegisterCapabilitiesEvent extends Event implements IModBusEvent
{
    @ApiStatus.Internal
    public RegisterCapabilitiesEvent() {}

    // BLOCKS

    public <T, C extends @Nullable Object> void registerBlock(final BlockCapability<T, C> capability, final IBlockCapabilityProvider<T, C> provider, final Block... blocks)
    {
        Objects.requireNonNull(provider);
        if (blocks.length == 0)
        {
            throw new IllegalArgumentException("Must register at least one block");
        }
        for (final Block block : blocks)
        {
            capability.addProvider(Objects.requireNonNull(block), provider);
        }
        if (capability == (Object) Capabilities.ItemHandler.BLOCK)
        {
            FabricTransferBridge.exposeBlocks(blocks);
        }
    }

    @SuppressWarnings("unchecked")
    public <T, C extends @Nullable Object, BE extends BlockEntity> void registerBlockEntity(
      final BlockCapability<T, C> capability,
      final BlockEntityType<BE> blockEntityType,
      final ICapabilityProvider<? super BE, C, T> provider)
    {
        Objects.requireNonNull(provider);
        final IBlockCapabilityProvider<T, C> adaptedProvider = (level, pos, state, blockEntity, context) -> {
            if (blockEntity == null || blockEntity.getType() != blockEntityType)
            {
                return null;
            }
            return provider.getCapability((BE) blockEntity, context);
        };

        for (final Block block : ((BlockEntityTypeAccessor) blockEntityType).shim$getValidBlocks())
        {
            capability.addProvider(Objects.requireNonNull(block), adaptedProvider);
        }
        if (capability == (Object) Capabilities.ItemHandler.BLOCK)
        {
            FabricTransferBridge.exposeBlockEntity(blockEntityType);
        }
    }

    public boolean isBlockRegistered(final BlockCapability<?, ?> capability, final Block block)
    {
        return capability.hasProvider(Objects.requireNonNull(block));
    }

    // ENTITIES

    @SuppressWarnings("unchecked")
    public <T, C extends @Nullable Object, E extends Entity> void registerEntity(
      final EntityCapability<T, C> capability,
      final EntityType<E> entityType,
      final ICapabilityProvider<? super E, C, T> provider)
    {
        Objects.requireNonNull(provider);
        capability.addProvider(entityType, (ICapabilityProvider<Entity, C, T>) provider);
    }

    public boolean isEntityRegistered(final EntityCapability<?, ?> capability, final EntityType<?> entityType)
    {
        return capability.hasProvider(Objects.requireNonNull(entityType));
    }

    // ITEMS

    public <T, C extends @Nullable Object> void registerItem(final ItemCapability<T, C> capability, final ICapabilityProvider<ItemStack, C, T> provider, final ItemLike... items)
    {
        Objects.requireNonNull(provider);
        if (items.length == 0)
        {
            throw new IllegalArgumentException("Must register at least one item");
        }
        for (final ItemLike itemLike : items)
        {
            final Item item = Objects.requireNonNull(itemLike.asItem());
            capability.addProvider(item, provider);
        }
    }

    public boolean isItemRegistered(final ItemCapability<?, ?> capability, final Item item)
    {
        return capability.hasProvider(Objects.requireNonNull(item));
    }
}
