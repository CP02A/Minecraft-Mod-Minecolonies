package com.ldtteam.shim.fml.event.lifecycle;

import com.ldtteam.shim.fml.ModContainer;

/**
 * Lifecycle event (NeoForge shim), fired by the Fabric entrypoints.
 */
public class FMLLoadCompleteEvent extends ParallelDispatchEvent
{
    public FMLLoadCompleteEvent(final ModContainer container)
    {
        super(container);
    }
}
