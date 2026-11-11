package com.ldtteam.shim.neoforge.common.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shim replacement for NeoForge's global loot modifier codec registry. {@code minecolonies_shim:add_table} is built in;
 * more can be added with {@link #register(Identifier, MapCodec)}.
 */
public final class GlobalLootModifierCodecs
{
    private static final Map<Identifier, MapCodec<? extends IGlobalLootModifier>> BY_ID    = new ConcurrentHashMap<>();
    private static final Map<MapCodec<? extends IGlobalLootModifier>, Identifier> BY_CODEC = new ConcurrentHashMap<>();
    private static volatile boolean builtinsRegistered;

    public static final Codec<MapCodec<? extends IGlobalLootModifier>> CODEC_TYPE = Identifier.CODEC.flatXmap(
        id -> {
            ensureBuiltins();
            final MapCodec<? extends IGlobalLootModifier> codec = BY_ID.get(id);
            return codec == null ? DataResult.error(() -> "Unknown loot modifier type: " + id) : DataResult.success(codec);
        },
        codec -> {
            ensureBuiltins();
            final Identifier id = BY_CODEC.get(codec);
            return id == null ? DataResult.error(() -> "Unregistered loot modifier codec: " + codec) : DataResult.success(id);
        });

    private GlobalLootModifierCodecs()
    {
    }

    public static synchronized void register(final Identifier id, final MapCodec<? extends IGlobalLootModifier> codec)
    {
        BY_ID.put(id, codec);
        BY_CODEC.put(codec, id);
    }

    private static void ensureBuiltins()
    {
        if (builtinsRegistered)
        {
            return;
        }
        synchronized (GlobalLootModifierCodecs.class)
        {
            if (!builtinsRegistered)
            {
                builtinsRegistered = true;
                register(Identifier.fromNamespaceAndPath("minecolonies_shim", "add_table"), AddTableLootModifier.CODEC);
            }
        }
    }
}
