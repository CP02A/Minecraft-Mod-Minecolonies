package com.ldtteam.shim.fml.event.lifecycle;

import com.ldtteam.shim.fml.ModContainer;

/**
 * Lifecycle event (NeoForge shim), fired by the Fabric entrypoints.
 */
public class InterModProcessEvent extends ParallelDispatchEvent
{
    public InterModProcessEvent(final ModContainer container)
    {
        super(container);
    }
}
