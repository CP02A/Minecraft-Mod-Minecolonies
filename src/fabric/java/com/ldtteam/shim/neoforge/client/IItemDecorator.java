// Adapted from NeoForge 26.3 (net.neoforged.neoforge.client.IItemDecorator), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Custom item decoration renderer, registered through {@link com.ldtteam.shim.neoforge.client.event.RegisterItemDecorationsEvent}.
 */
public interface IItemDecorator
{
    /**
     * Is called after vanilla decorations are rendered.
     *
     * @return true if the decorator changed render state (kept for API compatibility; ignored on Fabric)
     */
    boolean render(GuiGraphicsExtractor guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset);
}
