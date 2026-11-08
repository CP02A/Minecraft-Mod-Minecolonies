package com.ldtteam.shim.fml.event.lifecycle;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.ModContainer;
import com.ldtteam.shim.fml.event.IModBusEvent;

/**
 * Base of lifecycle events (NeoForge shim).
 */
public abstract class ModLifecycleEvent extends Event implements IModBusEvent
{
    private final ModContainer container;

    protected ModLifecycleEvent(final ModContainer container)
    {
        this.container = container;
    }

    public ModContainer getContainer()
    {
        return container;
    }

    public String description()
    {
        final String cn = getClass().getName();
        return cn.substring(cn.lastIndexOf('.') + 1);
    }
}
