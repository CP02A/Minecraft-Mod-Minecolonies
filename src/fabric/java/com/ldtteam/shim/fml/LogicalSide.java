package com.ldtteam.shim.fml;

/**
 * Logical side (NeoForge shim).
 */
public enum LogicalSide
{
    CLIENT,
    SERVER;

    public boolean isServer()
    {
        return !isClient();
    }

    public boolean isClient()
    {
        return this == CLIENT;
    }
}
