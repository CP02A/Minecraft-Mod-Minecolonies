package com.ldtteam.shim.fabric.network;

import com.ldtteam.shim.neoforge.network.handling.IPayloadContext;
import com.ldtteam.shim.neoforge.network.handling.PayloadFlow;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Client half of the payload shim: registers clientbound handlers with ClientPlayNetworking. Client-only class.
 */
public final class ShimNetworkingClient
{
    private ShimNetworkingClient()
    {
    }

    public static void init()
    {
        ShimNetworking.setClientRegistrar(ShimNetworkingClient::register);
    }

    public static void sendToServer(final CustomPacketPayload payload, final CustomPacketPayload... payloads)
    {
        ClientPlayNetworking.send(payload);
        for (final CustomPacketPayload p : payloads)
        {
            ClientPlayNetworking.send(p);
        }
    }

    private static <T extends CustomPacketPayload> void register(final ShimNetworking.ClientHandler<T> entry)
    {
        ClientPlayNetworking.registerGlobalReceiver(entry.type(), (payload, context) -> entry.handler().handle(payload, new ClientContext(context)));
    }

    private record ClientContext(ClientPlayNetworking.Context context) implements IPayloadContext
    {
        @Override
        public Player player()
        {
            return context.player();
        }

        @Override
        public PayloadFlow flow()
        {
            return PayloadFlow.CLIENTBOUND;
        }

        @Override
        public void reply(final CustomPacketPayload payload)
        {
            context.responseSender().sendPacket(payload);
        }

        @Override
        public void disconnect(final Component reason)
        {
            if (context.client().getConnection() != null)
            {
                context.client().getConnection().getConnection().disconnect(reason);
            }
        }

        @Override
        public CompletableFuture<Void> enqueueWork(final Runnable task)
        {
            if (context.client().isSameThread())
            {
                try
                {
                    task.run();
                }
                catch (final RuntimeException e)
                {
                    LOGGER.error("Exception while handling network message", e);
                    return CompletableFuture.failedFuture(e);
                }
                return CompletableFuture.completedFuture(null);
            }
            // FABRIC-PORT: NeoForge logs handler exceptions; a bare submit() would keep them in the returned future, unseen.
            return logFailures(context.client().submit(task));
        }

        @Override
        public <T> CompletableFuture<T> enqueueWork(final Supplier<T> task)
        {
            if (context.client().isSameThread())
            {
                return CompletableFuture.completedFuture(task.get());
            }
            return logFailures(context.client().submit(task));
        }
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("shim-network");

    private static <T> CompletableFuture<T> logFailures(final CompletableFuture<T> future)
    {
        return future.whenComplete((result, error) -> {
            if (error != null)
            {
                LOGGER.error("Exception while handling network message", error);
            }
        });
    }
}
