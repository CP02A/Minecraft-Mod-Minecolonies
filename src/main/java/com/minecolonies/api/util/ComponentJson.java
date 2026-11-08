package com.minecolonies.api.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * FABRIC-PORT: replacement for the 1.21.1 {@code Component.Serializer#toJson/fromJson} helpers, removed in 26.x.
 */
public final class ComponentJson
{
    private ComponentJson()
    {
    }

    /**
     * Serialize a component to a json string.
     *
     * @param component the component.
     * @param provider  the registry provider.
     * @return the json string.
     */
    public static String toJson(final Component component, final HolderLookup.Provider provider)
    {
        final JsonElement json = ComponentSerialization.CODEC.encodeStart(provider.createSerializationContext(JsonOps.INSTANCE), component).getOrThrow();
        return json.toString();
    }

    /**
     * Deserialize a component from a json string.
     *
     * @param json     the json string.
     * @param provider the registry provider.
     * @return the component, or null if it could not be parsed.
     */
    @Nullable
    public static MutableComponent fromJson(final String json, final HolderLookup.Provider provider)
    {
        if (json == null || json.isEmpty())
        {
            return null;
        }
        try
        {
            return ComponentSerialization.CODEC.parse(provider.createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json))
                     .result()
                     .map(Component::copy)
                     .orElse(null);
        }
        catch (final Exception e)
        {
            return null;
        }
    }
}
