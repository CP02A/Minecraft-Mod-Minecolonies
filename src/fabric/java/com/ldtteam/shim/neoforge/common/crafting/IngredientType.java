// Adapted from NeoForge 26.3 (net.neoforged.neoforge.common.crafting.IngredientType), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common.crafting;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Type of a {@link ICustomIngredient}. On Fabric each type is backed by a Fabric {@link CustomIngredientSerializer}, which is
 * registered when the type is registered under an id (the NeoForge ingredient type registry calls {@link #register(Identifier, IngredientType)}).
 */
public final class IngredientType<T extends ICustomIngredient>
{
    private final MapCodec<T> codec;
    private final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;
    @Nullable
    private Serializer<T> serializer;

    public IngredientType(final MapCodec<T> codec, final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec)
    {
        this.codec = codec;
        this.streamCodec = streamCodec;
    }

    public IngredientType(final MapCodec<T> codec)
    {
        this(codec, ByteBufCodecs.fromCodecWithRegistries(codec.codec()));
    }

    public MapCodec<T> codec()
    {
        return codec;
    }

    public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec()
    {
        return streamCodec;
    }

    /**
     * Registers the type with Fabric's custom ingredient system under the given id. Idempotent.
     */
    public static synchronized <T extends ICustomIngredient> IngredientType<T> register(final Identifier id, final IngredientType<T> type)
    {
        if (type.serializer == null)
        {
            type.serializer = new Serializer<>(id, type);
            if (CustomIngredientSerializer.get(id) == null)
            {
                CustomIngredientSerializer.register(type.serializer);
            }
        }
        return type;
    }

    CustomIngredientSerializer<T> serializer()
    {
        return Objects.requireNonNull(serializer, "IngredientType was not registered (missing IngredientType.register(id, type))");
    }

    private record Serializer<T extends ICustomIngredient>(Identifier id, IngredientType<T> type) implements CustomIngredientSerializer<T>
    {
        @Override
        public Identifier getIdentifier()
        {
            return id;
        }

        @Override
        public MapCodec<T> getCodec()
        {
            return type.codec;
        }

        @SuppressWarnings("unchecked")
        @Override
        public StreamCodec<RegistryFriendlyByteBuf, T> getStreamCodec()
        {
            return (StreamCodec<RegistryFriendlyByteBuf, T>) type.streamCodec;
        }
    }
}
