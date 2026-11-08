package com.minecolonies.api.blocks;

import com.minecolonies.api.blocks.interfaces.IBlockMinecolonies;
import net.minecraft.core.Registry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public abstract class AbstractBlockMinecoloniesHorizontal<B extends AbstractBlockMinecoloniesHorizontal<B>> extends HorizontalDirectionalBlock implements IBlockMinecolonies<B>
{
    public AbstractBlockMinecoloniesHorizontal(final Properties properties)
    {
        super(properties);
    }

    @Override
    public void registerBlockItem(final Registry<Item> registry, final Item.Properties properties)
    {
        Registry.register(registry, getRegistryName(), new BlockItem(this, properties.setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, getRegistryName())).useBlockDescriptionPrefix()));
    }

    @Override
    public B registerBlock(final Registry<Block> registry)
    {
        BlockIdHelper.register(registry, getRegistryName(), this);
        return (B) this;
    }
}
