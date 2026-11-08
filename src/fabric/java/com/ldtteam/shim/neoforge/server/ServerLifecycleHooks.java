package com.ldtteam.shim.neoforge.server;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * Access to the currently running server (NeoForge shim). Set from Fabric's SERVER_STARTING and cleared on SERVER_STOPPED
 * by {@code com.ldtteam.shim.fabric.events.ShimGameEvents}.
 */
public final class ServerLifecycleHooks
{
    private static volatile @Nullable MinecraftServer currentServer;

    private ServerLifecycleHooks()
    {
    }

    /**
     * @return the current server, or null when no server is running.
     */
    public static MinecraftServer getCurrentServer()
    {
        return currentServer;
    }

    public static void setCurrentServer(final @Nullable MinecraftServer server)
    {
        currentServer = server;
    }
}
