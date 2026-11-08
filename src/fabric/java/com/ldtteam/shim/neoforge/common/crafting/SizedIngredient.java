// Adapted from NeoForge 21.1/26.3 (net.neoforged.neoforge.common.crafting.SizedIngredient), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common.crafting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * An ingredient with a count.
 */
public final class SizedIngredient
{
    /**
     * Ingredient part of the flat codec. Accepts {@code "item": id}, {@code "tag": id} (1.21.1-style) or {@code "ingredient": <ingredient>}.
     */
    private static final MapCodec<Ingredient> FLAT_INGREDIENT_CODEC = Codec.mapEither(
        Item.CODEC.fieldOf("item"),
        Codec.mapEither(TagKey.codec(Registries.ITEM).fieldOf("tag"), Ingredient.CODEC.fieldOf("ingredient"))
      ).xmap(
        either -> either.map(holder -> Ingredient.of(holder.value()), inner -> inner.map(SizedIngredient::ofTag, i -> i)),
        ingredient -> ingredient.getSingleItem().<Either<Holder<Item>, Either<TagKey<Item>, Ingredient>>>map(Either::left)
                        .orElseGet(() -> Either.right(Either.right(ingredient))));

    /**
     * The "flat" codec: the count is serialized next to the ingredient fields, e.g. {@code {"item": "minecraft:apple", "count": 3}}.
     */
    public static final Codec<SizedIngredient> FLAT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        FLAT_INGREDIENT_CODEC.forGetter(SizedIngredient::ingredient),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(SizedIngredient::count)
      ).apply(instance, SizedIngredient::new));

    /**
     * The "nested" codec: {@code {"ingredient": ..., "count": 3}}.
     */
    public static final Codec<SizedIngredient> NESTED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(SizedIngredient::ingredient),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(SizedIngredient::count)
      ).apply(instance, SizedIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SizedIngredient> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC,
      SizedIngredient::ingredient,
      ByteBufCodecs.VAR_INT,
      SizedIngredient::count,
      SizedIngredient::new);

    private static Ingredient ofTag(final TagKey<Item> tag)
    {
        return Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(tag));
    }

    public static SizedIngredient of(final ItemLike item, final int count)
    {
        return new SizedIngredient(Ingredient.of(item), count);
    }

    public static SizedIngredient of(final TagKey<Item> tag, final int count)
    {
        return new SizedIngredient(ofTag(tag), count);
    }

    private final Ingredient ingredient;
    private final int count;
    @Nullable
    private ItemStack[] cachedStacks;

    public SizedIngredient(final Ingredient ingredient, final int count)
    {
        if (count <= 0)
        {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.ingredient = ingredient;
        this.count = count;
    }

    public Ingredient ingredient()
    {
        return ingredient;
    }

    public int count()
    {
        return count;
    }

    /**
     * @return the matching stacks, with the count of this sized ingredient. The returned array must not be modified.
     */
    @SuppressWarnings("deprecation")
    public ItemStack[] getItems()
    {
        if (cachedStacks == null)
        {
            cachedStacks = ingredient.items().map(holder -> new ItemStack(holder, count)).toArray(ItemStack[]::new);
        }
        return cachedStacks;
    }

    public boolean test(final ItemStack stack)
    {
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    @Override
    public boolean equals(@Nullable final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof SizedIngredient other))
        {
            return false;
        }
        return count == other.count && ingredient.equals(other.ingredient);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(ingredient, count);
    }

    @Override
    public String toString()
    {
        return count + "x " + ingredient;
    }
}
