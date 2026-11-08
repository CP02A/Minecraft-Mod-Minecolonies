package com.ldtteam.shim.neoforge.network;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Server-side payload sending helpers (NeoForge shim) backed by Fabric networking. Client-to-server sending lives in
 * com.ldtteam.shim.neoforge.client.network.ClientPacketDistributor.
 */
public final class PacketDistributor
{
    private PacketDistributor()
    {
    }

    /**
     * Send payloads to the server. Only valid on the physical client.
     */
    public static void sendToServer(final CustomPacketPayload payload, final CustomPacketPayload... payloads)
    {
        com.ldtteam.shim.fabric.network.ShimNetworkingClient.sendToServer(payload, payloads);
    }

    public static void sendToPlayer(final ServerPlayer player, final CustomPacketPayload payload, final CustomPacketPayload... payloads)
    {
        send(player, payload, payloads);
    }

    public static void sendToPlayersInDimension(final ServerLevel level, final CustomPacketPayload payload, final CustomPacketPayload... payloads)
    {
        sendAll(PlayerLookup.level(level), payload, payloads);
    }

    public static void sendToPlayersNear(
        final ServerLevel level,
        @Nullable final ServerPlayer excluded,
        final double x,
        final double y,
        final double z,
        final double radius,
        final CustomPacketPayload payload,
        final CustomPacketPayload... payloads)
    {
        for (final ServerPlayer player : PlayerLookup.around(level, new Vec3(x, y, z), radius))
        {
            if (player != excluded)
            {
                send(player, payload, payloads);
            }
        }
    }

    public static void sendToAllPlayers(final CustomPacketPayload payload, final CustomPacketPayload... payloads)
    {
        final MinecraftServer server = com.ldtteam.shim.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null)
        {
            sendAll(PlayerLookup.all(server), payload, payloads);
        }
    }

    public static void sendToPlayersTrackingEntity(final Entity entity, final CustomPacketPayload payload, final CustomPacketPayload... payloads)
    {
        if (entity.level() instanceof ServerLevel)
        {
            sendAll(PlayerLookup.tracking(entity), payload, payloads);
        }
    }

    public static void sendToPlayersTrackingEntityAndSelf(final Entity entity, final CustomPacketPayload payload, final CustomPacketPayload... payloads)
    {
        if (entity.level() instanceof ServerLevel)
        {
            final Set<ServerPlayer> players = new LinkedHashSet<>(PlayerLookup.tracking(entity));
            if (entity instanceof ServerPlayer self)
            {
                players.add(self);
            }
            sendAll(players, payload, payloads);
        }
    }

    public static void sendToPlayersTrackingChunk(final ServerLevel level, final ChunkPos chunkPos, final CustomPacketPayload payload, final CustomPacketPayload... payloads)
    {
        sendAll(PlayerLookup.tracking(level, chunkPos), payload, payloads);
    }

    private static void sendAll(final Collection<ServerPlayer> players, final CustomPacketPayload payload, final CustomPacketPayload... payloads)
    {
        for (final ServerPlayer player : players)
        {
            send(player, payload, payloads);
        }
    }

    private static void send(final ServerPlayer player, final CustomPacketPayload payload, final CustomPacketPayload... payloads)
    {
        if (player.connection == null)
        {
            return;
        }
        ServerPlayNetworking.send(player, payload);
        for (final CustomPacketPayload p : payloads)
        {
            ServerPlayNetworking.send(player, p);
        }
    }
}
