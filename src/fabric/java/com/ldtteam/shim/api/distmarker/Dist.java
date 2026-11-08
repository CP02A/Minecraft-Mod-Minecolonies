package com.ldtteam.shim.api.distmarker;

/**
 * Physical side (NeoForge shim).
 */
public enum Dist
{
    CLIENT,
    DEDICATED_SERVER;

    public boolean isDedicatedServer()
    {
        return !isClient();
    }

    public boolean isClient()
    {
        return this == CLIENT;
    }
}
