package com.minecolonies.api.blocks;

import com.minecolonies.fabric.mixin.api.BlockBehaviourFieldsAccessor;
import com.minecolonies.fabric.mixin.api.BlockPropertiesAccessor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

/**
 * FABRIC-PORT: 26.x blocks need their id at construction time (BlockBehaviour.Properties#setId). MineColonies blocks only know their registry
 * name once constructed, so the id (and the id-derived loot table and description id) is assigned here right before registration.
 */
public final class BlockIdHelper
{
    private BlockIdHelper()
    {
    }

    /**
     * Assign the id to the block and register it.
     *
     * @param registry the block registry.
     * @param id       the registry name.
     * @param block    the block.
     * @param <B>      the block type.
     * @return the block.
     */
    public static <B extends Block> B register(final Registry<Block> registry, final Identifier id, final B block)
    {
        assignId(block, id);
        Registry.register(registry, id, block);
        return block;
    }

    /**
     * Assign the id to the block (sets the properties id and recomputes the id-derived fields).
     *
     * @param block the block.
     * @param id    the registry name.
     */
    public static void assignId(final Block block, final Identifier id)
    {
        final ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        final BlockPropertiesAccessor properties = (BlockPropertiesAccessor) block.properties();
        if (key.equals(properties.minecolonies$getId()))
        {
            return;
        }
        block.properties().setId(key);
        ((BlockBehaviourFieldsAccessor) block).minecolonies$setDrops(properties.minecolonies$effectiveDrops());
        ((BlockBehaviourFieldsAccessor) block).minecolonies$setDescriptionId(properties.minecolonies$effectiveDescriptionId());
    }
}
