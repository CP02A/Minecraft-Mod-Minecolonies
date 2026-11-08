package com.ldtteam.shim.neoforge.network.handling;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload handler (NeoForge shim).
 */
@FunctionalInterface
public interface IPayloadHandler<T extends CustomPacketPayload>
{
    void handle(T payload, IPayloadContext context);
}
