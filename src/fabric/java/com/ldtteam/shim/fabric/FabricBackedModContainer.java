package com.ldtteam.shim.fabric;

import com.ldtteam.shim.bus.api.IEventBus;
import com.ldtteam.shim.fml.ModContainer;

/**
 * Shim ModContainer for a plain Fabric mod (no mod bus).
 */
public final class FabricBackedModContainer extends ModContainer
{
    public FabricBackedModContainer(final FabricModInfo info)
    {
        super(info);
    }

    @Override
    public IEventBus getEventBus()
    {
        return null;
    }
}
