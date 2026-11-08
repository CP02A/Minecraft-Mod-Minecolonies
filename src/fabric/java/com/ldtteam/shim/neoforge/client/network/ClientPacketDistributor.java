// Adapted from NeoForge 26.3 (net.neoforged.neoforge.client.network.ClientPacketDistributor), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.network;

import com.ldtteam.shim.fabric.network.ShimNetworkingClient;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server packet sending.
 */
public final class ClientPacketDistributor
{
    private ClientPacketDistributor() {}

    public static void sendToServer(final CustomPacketPayload payload, final CustomPacketPayload... payloads)
    {
        ShimNetworkingClient.sendToServer(payload, payloads);
    }
}
