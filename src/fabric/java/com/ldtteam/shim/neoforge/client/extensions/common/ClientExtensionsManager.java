// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.extensions.common.ClientExtensionsManager), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.extensions.common;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientExtensionsManager
{
    static final Map<Item, IClientItemExtensions> ITEM_EXTENSIONS = new ConcurrentHashMap<>();
    static final Map<Block, IClientBlockExtensions> BLOCK_EXTENSIONS = new ConcurrentHashMap<>();

    private ClientExtensionsManager() {}
}
