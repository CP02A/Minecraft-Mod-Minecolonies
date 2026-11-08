package com.ldtteam.blockui.fabric;

import com.ldtteam.blockui.mod.ClientLifecycleSubscriber;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entrypoint for BlockUI registrations that NeoForge did through mod-bus events which have direct Fabric
 * API equivalents (texture atlases, picture-in-picture renderers).
 */
public class BlockUIFabricClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        ClientLifecycleSubscriber.registerFabric();
    }
}
