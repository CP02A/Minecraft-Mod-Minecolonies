package com.ldtteam.shim.neoforge.common.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shim replacement for NeoForge's condition codec registry (NeoForgeRegistries.CONDITION_SERIALIZERS).
 * Built-in {@code neoforge:*} conditions are registered lazily (to avoid class-init cycles with {@link ICondition#CODEC});
 * additional ones can be added with {@link #register(Identifier, MapCodec)}.
 */
public final class ConditionCodecs
{
    private static final Map<Identifier, MapCodec<? extends ICondition>> BY_ID   = new ConcurrentHashMap<>();
    private static final Map<MapCodec<? extends ICondition>, Identifier> BY_CODEC = new ConcurrentHashMap<>();
    private static volatile boolean builtinsRegistered;

    /**
     * Codec of a condition type (like {@code Registry#byNameCodec()}).
     */
    public static final Codec<MapCodec<? extends ICondition>> CODEC_TYPE = Identifier.CODEC.flatXmap(
        id -> {
            ensureBuiltins();
            final MapCodec<? extends ICondition> codec = BY_ID.get(id);
            return codec == null ? DataResult.error(() -> "Unknown condition type: " + id) : DataResult.success(codec);
        },
        codec -> {
            ensureBuiltins();
            final Identifier id = BY_CODEC.get(codec);
            return id == null ? DataResult.error(() -> "Unregistered condition codec: " + codec) : DataResult.success(id);
        });

    private ConditionCodecs()
    {
    }

    public static synchronized void register(final Identifier id, final MapCodec<? extends ICondition> codec)
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
        synchronized (ConditionCodecs.class)
        {
            if (builtinsRegistered)
            {
                return;
            }
            builtinsRegistered = true;
            register(neo("and"), AndCondition.CODEC);
            register(neo("false"), FalseCondition.CODEC);
            register(neo("item_exists"), ItemExistsCondition.CODEC);
            register(neo("mod_loaded"), ModLoadedCondition.CODEC);
            register(neo("not"), NotCondition.CODEC);
            register(neo("or"), OrCondition.CODEC);
            register(neo("tag_empty"), TagEmptyCondition.CODEC);
            register(neo("true"), TrueCondition.CODEC);
        }
    }

    private static Identifier neo(final String path)
    {
        return Identifier.fromNamespaceAndPath("neoforge", path);
    }

    /**
     * Codec for a list of optional elements, dropping empty values when decoding (NeoForgeExtraCodecs equivalent).
     */
    public static <A> Codec<List<A>> listWithOptionalElements(final Codec<Optional<A>> elementCodec)
    {
        return elementCodec.listOf().xmap(
            list -> list.stream().filter(Optional::isPresent).map(Optional::get).toList(),
            list -> list.stream().map(Optional::of).toList());
    }
}
