package com.ldtteam.shim.fml.event.lifecycle;

import com.ldtteam.shim.fml.ModContainer;

/**
 * Lifecycle event (NeoForge shim), fired by the Fabric entrypoints.
 */
public class FMLCommonSetupEvent extends ParallelDispatchEvent
{
    public FMLCommonSetupEvent(final ModContainer container)
    {
        super(container);
    }
}
