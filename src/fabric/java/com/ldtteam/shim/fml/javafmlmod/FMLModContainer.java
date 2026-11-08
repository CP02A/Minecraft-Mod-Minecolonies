package com.ldtteam.shim.fml.javafmlmod;

import com.ldtteam.shim.bus.EventBus;
import com.ldtteam.shim.bus.api.IEventBus;
import com.ldtteam.shim.fml.ModContainer;
import com.ldtteam.shim.neoforgespi.language.IModInfo;

/**
 * Container of a shimmed (formerly NeoForge) mod, owning its own mod event bus.
 */
public class FMLModContainer extends ModContainer
{
    private final IEventBus eventBus;

    public FMLModContainer(final IModInfo info)
    {
        super(info);
        this.eventBus = new EventBus("mod:" + info.getModId());
    }

    @Override
    public IEventBus getEventBus()
    {
        return eventBus;
    }
}
