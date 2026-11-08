package com.ldtteam.shim.neoforge.network.registration;

import com.ldtteam.shim.fabric.network.ShimNetworking;
import com.ldtteam.shim.neoforge.network.handling.IPayloadHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Registers play payloads (NeoForge shim) with Fabric's PayloadTypeRegistry and Server/ClientPlayNetworking.
 * Versioning / optional flags are accepted for API compatibility; Fabric negotiates channels itself.
 */
public class PayloadRegistrar
{
    private final String        version;
    private final boolean       optional;
    private final HandlerThread thread;

    public PayloadRegistrar(final String version)
    {
        this(version, false, HandlerThread.MAIN);
    }

    private PayloadRegistrar(final String version, final boolean optional, final HandlerThread thread)
    {
        this.version = version;
        this.optional = optional;
        this.thread = thread;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(
        final CustomPacketPayload.Type<T> type,
        final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
        final IPayloadHandler<T> handler)
    {
        ShimNetworking.registerClientbound(type, codec, handler);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(
        final CustomPacketPayload.Type<T> type,
        final StreamCodec<? super RegistryFriendlyByteBuf, T> codec)
    {
        ShimNetworking.registerClientbound(type, codec, null);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(
        final CustomPacketPayload.Type<T> type,
        final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
        final IPayloadHandler<T> handler)
    {
        ShimNetworking.registerServerbound(type, codec, handler);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(
        final CustomPacketPayload.Type<T> type,
        final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
        final IPayloadHandler<T> handler)
    {
        ShimNetworking.registerClientbound(type, codec, handler);
        ShimNetworking.registerServerbound(type, codec, handler);
        return this;
    }

    public PayloadRegistrar versioned(final String version)
    {
        return new PayloadRegistrar(version, optional, thread);
    }

    public PayloadRegistrar optional()
    {
        return new PayloadRegistrar(version, true, thread);
    }

    public PayloadRegistrar executesOn(final HandlerThread thread)
    {
        return new PayloadRegistrar(version, optional, thread);
    }

    public String getVersion()
    {
        return version;
    }
}
