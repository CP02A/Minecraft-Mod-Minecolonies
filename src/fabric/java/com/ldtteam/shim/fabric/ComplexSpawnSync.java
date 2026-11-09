package com.ldtteam.shim.fabric;

import com.ldtteam.shim.neoforge.entity.IEntityWithComplexSpawn;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * Sends {@link IEntityWithComplexSpawn} extra spawn data in a separate payload after the vanilla add-entity packet.
 */
public final class ComplexSpawnSync
{
    public record Payload(int entityId, byte[] data) implements CustomPacketPayload
    {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("minecolonies", "shim_complex_spawn"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT, Payload::entityId,
          ByteBufCodecs.BYTE_ARRAY, Payload::data,
          Payload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    private static boolean initialized = false;

    /*
     * Ordering difference to NeoForge: NeoForge reads the spawn data before the entity is added to the client level; here it
     * arrives in a separate payload right after the vanilla spawn packet. readSpawnData implementations must tolerate the
     * entity existing (and possibly ticking or rendering once) with default values first. Today only NewBobberEntity uses
     * this, with an empty readSpawnData.
     */
    private ComplexSpawnSync() {}

    public static synchronized void init()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;
        PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity instanceof IEntityWithComplexSpawn complex && ServerPlayNetworking.canSend(player, Payload.TYPE))
            {
                final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
                try
                {
                    complex.writeSpawnData(buf);
                    final byte[] data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                    ServerPlayNetworking.send(player, new Payload(entity.getId(), data));
                }
                finally
                {
                    buf.release();
                }
            }
        });
    }

    /**
     * Client side handling, called from the client payload receiver.
     */
    public static void handle(final Entity entity, final byte[] data)
    {
        if (entity instanceof IEntityWithComplexSpawn complex)
        {
            final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), entity.registryAccess());
            try
            {
                complex.readSpawnData(buf);
            }
            finally
            {
                buf.release();
            }
        }
    }
}
