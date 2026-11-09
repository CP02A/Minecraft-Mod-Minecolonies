package com.ldtteam.shim.fabric.network;

import com.ldtteam.shim.neoforge.network.handling.IPayloadContext;
import com.ldtteam.shim.neoforge.network.handling.IPayloadHandler;
import com.ldtteam.shim.neoforge.network.handling.PayloadFlow;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fabric backend of the NeoForge payload registration shim.
 */
public final class ShimNetworking
{
    /**
     * Max payload sizes for split (large) payloads. NeoForge splits big payloads transparently; colony views and blueprints can be several MB.
     */
    private static final int MAX_CLIENTBOUND_SIZE = 64 * 1024 * 1024;
    private static final int MAX_SERVERBOUND_SIZE = 1024 * 1024;
    /**
     * Serverbound payloads that carry whole blueprints may be large; everything else is capped lower so a client cannot make the
     * server buffer and decode many megabytes per packet.
     */
    private static final int MAX_SERVERBOUND_BLUEPRINT_SIZE = 16 * 1024 * 1024;
    private static final Set<String> LARGE_SERVERBOUND_PAYLOADS = Set.of("structurize:blueprint_sync", "structurize:sync_preview_cache_to_server");

    public record ClientHandler<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler)
    {
    }

    private static final Set<CustomPacketPayload.Type<?>> CLIENTBOUND_TYPES = new HashSet<>();
    private static final Set<CustomPacketPayload.Type<?>> SERVERBOUND_TYPES = new HashSet<>();
    private static final List<ClientHandler<?>>           CLIENT_HANDLERS   = new ArrayList<>();
    @Nullable
    private static Consumer<ClientHandler<?>> clientRegistrar;

    private ShimNetworking()
    {
    }

    public static synchronized <T extends CustomPacketPayload> void registerClientbound(
        final CustomPacketPayload.Type<T> type,
        final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
        @Nullable final IPayloadHandler<T> handler)
    {
        if (CLIENTBOUND_TYPES.add(type))
        {
            PayloadTypeRegistry.clientboundPlay().registerLarge(type, codec, MAX_CLIENTBOUND_SIZE);
        }
        if (handler != null)
        {
            final ClientHandler<T> entry = new ClientHandler<>(type, handler);
            CLIENT_HANDLERS.add(entry);
            if (clientRegistrar != null)
            {
                clientRegistrar.accept(entry);
            }
        }
    }

    public static synchronized <T extends CustomPacketPayload> void registerServerbound(
        final CustomPacketPayload.Type<T> type,
        final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
        final IPayloadHandler<T> handler)
    {
        if (SERVERBOUND_TYPES.add(type))
        {
            final int maxSize = LARGE_SERVERBOUND_PAYLOADS.contains(type.id().toString()) ? MAX_SERVERBOUND_BLUEPRINT_SIZE : MAX_SERVERBOUND_SIZE;
            PayloadTypeRegistry.serverboundPlay().registerLarge(type, codec, maxSize);
        }
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.handle(payload, new ServerContext(context)));
    }

    /**
     * Called by the client side to receive all (current and future) clientbound handlers.
     */
    public static synchronized void setClientRegistrar(final Consumer<ClientHandler<?>> registrar)
    {
        clientRegistrar = registrar;
        CLIENT_HANDLERS.forEach(registrar);
    }

    private record ServerContext(ServerPlayNetworking.Context context) implements IPayloadContext
    {
        @Override
        public Player player()
        {
            return context.player();
        }

        @Override
        public PayloadFlow flow()
        {
            return PayloadFlow.SERVERBOUND;
        }

        @Override
        public void reply(final CustomPacketPayload payload)
        {
            context.responseSender().sendPacket(payload);
        }

        @Override
        public void disconnect(final Component reason)
        {
            context.player().connection.disconnect(reason);
        }

        @Override
        public CompletableFuture<Void> enqueueWork(final Runnable task)
        {
            if (context.server().isSameThread())
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
            return logFailures(context.server().submit(task));
        }

        @Override
        public <T> CompletableFuture<T> enqueueWork(final Supplier<T> task)
        {
            if (context.server().isSameThread())
            {
                return CompletableFuture.completedFuture(task.get());
            }
            return logFailures(context.server().submit(task));
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
