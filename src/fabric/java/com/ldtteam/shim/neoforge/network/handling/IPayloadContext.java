package com.ldtteam.shim.neoforge.network.handling;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Context of a received payload (NeoForge shim). Handlers are invoked on the main thread of the receiving side
 * (Fabric API default), matching NeoForge's default handler thread.
 */
public interface IPayloadContext
{
    /**
     * @return the player: the sender on the server, the local player on the client.
     */
    Player player();

    /**
     * @return the direction the payload travelled in.
     */
    PayloadFlow flow();

    /**
     * Send a payload back to the other side.
     */
    void reply(CustomPacketPayload payload);

    /**
     * Disconnect the connection with the given reason.
     */
    void disconnect(Component reason);

    /**
     * Run the task on the main thread of the receiving side (immediately if already on it).
     */
    CompletableFuture<Void> enqueueWork(Runnable task);

    <T> CompletableFuture<T> enqueueWork(Supplier<T> task);
}
