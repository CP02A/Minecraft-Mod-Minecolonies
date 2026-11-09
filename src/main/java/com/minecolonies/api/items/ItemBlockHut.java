package com.minecolonies.api.items;

import com.minecolonies.api.blocks.AbstractBlockHut;
import net.minecraft.world.item.BlockItem;

/**
 * A custom item class for hut blocks.
 */
public class ItemBlockHut extends BlockItem
{
    /**
     * This items block.
     */
    private AbstractBlockHut<?> block;

    /**
     * Creates a new ItemBlockHut representing the item form of the given {@link AbstractBlockHut}.
     * 
     * @param block   the {@link AbstractBlockHut} this item represents.
     * @param builder the item properties to use.
     */
    public ItemBlockHut(AbstractBlockHut<?> block, Properties builder)
    {
        super(block, builder);
        this.block = block;
    }

    /**
     * FABRIC-PORT: Block#appendHoverText was removed in 26.x, so the hut tooltip (level, colony) is added from the item.
     */
    @Override
    public void appendHoverText(final net.minecraft.world.item.ItemStack stack, final TooltipContext context,
        final net.minecraft.world.item.component.TooltipDisplay display, final java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip,
        final net.minecraft.world.item.TooltipFlag flags)
    {
        super.appendHoverText(stack, context, display, tooltip, flags);
        block.appendHoverText(stack, context, display, tooltip, flags);
    }
}
