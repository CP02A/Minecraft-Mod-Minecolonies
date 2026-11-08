// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.extensions.common;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * Mod bus event to register client-side block/item extensions.
 */
public class RegisterClientExtensionsEvent extends Event implements IModBusEvent
{
    public RegisterClientExtensionsEvent() {}

    public void registerBlock(final IClientBlockExtensions extensions, final Block... blocks)
    {
        for (final Block block : blocks)
        {
            ClientExtensionsManager.BLOCK_EXTENSIONS.put(block, extensions);
        }
    }

    public boolean isBlockRegistered(final Block block)
    {
        return ClientExtensionsManager.BLOCK_EXTENSIONS.containsKey(block);
    }

    public void registerItem(final IClientItemExtensions extensions, final ItemLike... items)
    {
        for (final ItemLike item : items)
        {
            ClientExtensionsManager.ITEM_EXTENSIONS.put(item.asItem(), extensions);
        }
    }

    public boolean isItemRegistered(final Item item)
    {
        return ClientExtensionsManager.ITEM_EXTENSIONS.containsKey(item);
    }
}
