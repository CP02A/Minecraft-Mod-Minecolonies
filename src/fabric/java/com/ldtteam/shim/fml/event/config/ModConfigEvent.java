package com.ldtteam.shim.fml.event.config;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.config.ModConfig;
import com.ldtteam.shim.fml.event.IModBusEvent;

/**
 * Config lifecycle events, fired on the owning mod bus (NeoForge/FML shim).
 */
public abstract class ModConfigEvent extends Event implements IModBusEvent
{
    private final ModConfig config;

    protected ModConfigEvent(final ModConfig config)
    {
        this.config = config;
    }

    public ModConfig getConfig()
    {
        return config;
    }

    /**
     * Fired when a config is loaded.
     */
    public static class Loading extends ModConfigEvent
    {
        public Loading(final ModConfig config)
        {
            super(config);
        }
    }

    /**
     * Fired when a config is reloaded (file changed or saved).
     */
    public static class Reloading extends ModConfigEvent
    {
        public Reloading(final ModConfig config)
        {
            super(config);
        }
    }

    /**
     * Fired when a config is unloaded (server configs on server stop / disconnect).
     */
    public static class Unloading extends ModConfigEvent
    {
        public Unloading(final ModConfig config)
        {
            super(config);
        }
    }
}
