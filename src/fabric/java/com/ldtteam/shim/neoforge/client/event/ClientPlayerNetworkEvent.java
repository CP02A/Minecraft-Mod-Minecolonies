// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import org.jetbrains.annotations.Nullable;

/**
 * Client player connection events, fired on the game bus from Fabric's ClientPlayConnectionEvents.
 */
public abstract class ClientPlayerNetworkEvent extends Event
{
    @Nullable
    private final MultiPlayerGameMode multiPlayerGameMode;
    @Nullable
    private final LocalPlayer player;
    @Nullable
    private final Connection connection;

    protected ClientPlayerNetworkEvent(@Nullable final MultiPlayerGameMode multiPlayerGameMode, @Nullable final LocalPlayer player, @Nullable final Connection connection)
    {
        this.multiPlayerGameMode = multiPlayerGameMode;
        this.player = player;
        this.connection = connection;
    }

    @Nullable
    public MultiPlayerGameMode getMultiPlayerGameMode()
    {
        return multiPlayerGameMode;
    }

    @Nullable
    public LocalPlayer getPlayer()
    {
        return player;
    }

    @Nullable
    public Connection getConnection()
    {
        return connection;
    }

    public static class LoggingIn extends ClientPlayerNetworkEvent
    {
        public LoggingIn(final MultiPlayerGameMode controller, final LocalPlayer player, final Connection networkManager)
        {
            super(controller, player, networkManager);
        }

        @Override
        public MultiPlayerGameMode getMultiPlayerGameMode()
        {
            return super.getMultiPlayerGameMode();
        }

        @Override
        public LocalPlayer getPlayer()
        {
            return super.getPlayer();
        }

        @Override
        public Connection getConnection()
        {
            return super.getConnection();
        }
    }

    public static class LoggingOut extends ClientPlayerNetworkEvent
    {
        public LoggingOut(@Nullable final MultiPlayerGameMode controller, @Nullable final LocalPlayer player, @Nullable final Connection networkManager)
        {
            super(controller, player, networkManager);
        }
    }

    public static class Clone extends ClientPlayerNetworkEvent
    {
        private final LocalPlayer oldPlayer;

        public Clone(final MultiPlayerGameMode pc, final LocalPlayer oldPlayer, final LocalPlayer newPlayer, final Connection networkManager)
        {
            super(pc, newPlayer, networkManager);
            this.oldPlayer = oldPlayer;
        }

        public LocalPlayer getOldPlayer()
        {
            return oldPlayer;
        }

        public LocalPlayer getNewPlayer()
        {
            return super.getPlayer();
        }
    }
}
