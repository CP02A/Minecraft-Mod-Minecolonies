package com.ldtteam.shim.fml.event.lifecycle;

import com.ldtteam.shim.fml.ModContainer;

/**
 * Lifecycle event (NeoForge shim), fired by the Fabric entrypoints.
 */
public class InterModEnqueueEvent extends ParallelDispatchEvent
{
    public InterModEnqueueEvent(final ModContainer container)
    {
        super(container);
    }
}
