package com.ldtteam.shim.fabric.config;

import com.ldtteam.shim.fml.config.ConfigTracker;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client half of server config syncing: accept synced server configs from remote servers, restore defaults on disconnect.
 */
public final class ShimConfigsClient
{
    private ShimConfigsClient()
    {
    }

    /**
     * Call from the client entrypoint (after {@link ShimConfigs#init()}).
     */
    public static void init()
    {
        ClientPlayNetworking.registerGlobalReceiver(ConfigFilePayload.TYPE, (payload, context) -> {
            // On the integrated server the client shares the server-loaded config objects.
            if (context.client().hasSingleplayerServer())
            {
                return;
            }
            ConfigTracker.INSTANCE.acceptSyncedConfig(payload.fileName(), payload.contents());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
            if (!client.hasSingleplayerServer())
            {
                client.execute(ConfigTracker.INSTANCE::loadDefaultServerConfigs);
            }
        });
    }
}
