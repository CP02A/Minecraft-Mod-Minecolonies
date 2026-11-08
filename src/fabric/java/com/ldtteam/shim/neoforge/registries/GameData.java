package com.ldtteam.shim.neoforge.registries;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Map;

/**
 * Registry helpers (NeoForge shim).
 */
public final class GameData
{
    private GameData()
    {
    }

    public static Map<Block, Item> getBlockItemMap()
    {
        return Item.BY_BLOCK;
    }
}
