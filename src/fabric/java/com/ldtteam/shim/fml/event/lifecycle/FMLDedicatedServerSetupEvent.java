package com.ldtteam.shim.fml.event.lifecycle;

import com.ldtteam.shim.fml.ModContainer;

/**
 * Lifecycle event (NeoForge shim), fired by the Fabric entrypoints.
 */
public class FMLDedicatedServerSetupEvent extends ParallelDispatchEvent
{
    public FMLDedicatedServerSetupEvent(final ModContainer container)
    {
        super(container);
    }
}
