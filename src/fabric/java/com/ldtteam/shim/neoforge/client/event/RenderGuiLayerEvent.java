// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.RenderGuiLayerEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.bus.api.ICancellableEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Fired on the game bus around each vanilla HUD layer (wrapping Fabric's vanilla HUD elements). Layer names are the ids in
 * {@link com.ldtteam.shim.neoforge.client.gui.VanillaGuiLayers}.
 */
public abstract class RenderGuiLayerEvent extends Event
{
    private final GuiGraphicsExtractor guiGraphics;
    private final DeltaTracker partialTick;
    private final Identifier name;

    protected RenderGuiLayerEvent(final GuiGraphicsExtractor guiGraphics, final DeltaTracker partialTick, final Identifier name)
    {
        this.guiGraphics = guiGraphics;
        this.partialTick = partialTick;
        this.name = name;
    }

    public GuiGraphicsExtractor getGuiGraphics()
    {
        return guiGraphics;
    }

    public DeltaTracker getPartialTick()
    {
        return partialTick;
    }

    public Identifier getName()
    {
        return name;
    }

    /**
     * Fired before the layer is rendered. Cancel to skip rendering of the layer.
     */
    public static class Pre extends RenderGuiLayerEvent implements ICancellableEvent
    {
        public Pre(final GuiGraphicsExtractor guiGraphics, final DeltaTracker partialTick, final Identifier name)
        {
            super(guiGraphics, partialTick, name);
        }
    }

    public static class Post extends RenderGuiLayerEvent
    {
        public Post(final GuiGraphicsExtractor guiGraphics, final DeltaTracker partialTick, final Identifier name)
        {
            super(guiGraphics, partialTick, name);
        }
    }
}
