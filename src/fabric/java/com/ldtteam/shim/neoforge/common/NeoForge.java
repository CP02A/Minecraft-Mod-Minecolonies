package com.ldtteam.shim.neoforge.common;

import com.ldtteam.shim.bus.EventBus;
import com.ldtteam.shim.bus.api.IEventBus;

/**
 * Holder of the global game event bus (NeoForge shim).
 */
public final class NeoForge
{
    public static final IEventBus EVENT_BUS = new EventBus("game");

    private NeoForge()
    {
    }
}
