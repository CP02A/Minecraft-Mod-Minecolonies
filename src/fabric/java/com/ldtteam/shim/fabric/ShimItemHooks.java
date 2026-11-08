package com.ldtteam.shim.fabric;

import com.ldtteam.shim.fabric.capabilities.ShimCapabilityHooks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Common hook-up of the items/capabilities half of the shim. Call once from the common entrypoint after registries are filled.
 */
public final class ShimItemHooks
{
    private static boolean initialized = false;

    private ShimItemHooks() {}

    public static synchronized void init()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;
        ComplexSpawnSync.init();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ShimCapabilityHooks.ensureInitialized());
    }
}
