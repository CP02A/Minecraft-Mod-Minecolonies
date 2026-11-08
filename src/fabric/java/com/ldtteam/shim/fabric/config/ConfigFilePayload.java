package com.ldtteam.shim.fabric.config;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server config file contents synced to a remote client (like NeoForge ConfigFilePayload).
 */
public record ConfigFilePayload(String fileName, byte[] contents) implements CustomPacketPayload
{
    public static final Type<ConfigFilePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("minecolonies", "shim_config_file"));

    public static final StreamCodec<FriendlyByteBuf, ConfigFilePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ConfigFilePayload::fileName,
        ByteBufCodecs.BYTE_ARRAY, ConfigFilePayload::contents,
        ConfigFilePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
