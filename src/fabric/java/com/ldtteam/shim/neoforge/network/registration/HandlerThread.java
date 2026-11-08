package com.ldtteam.shim.neoforge.network.registration;

/**
 * Thread payload handlers run on (NeoForge shim). On Fabric, play handlers always run on the main thread.
 */
public enum HandlerThread
{
    MAIN,
    NETWORK
}
