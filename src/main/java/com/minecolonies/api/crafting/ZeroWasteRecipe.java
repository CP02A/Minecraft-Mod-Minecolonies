package com.minecolonies.api.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * A shapeless recipe that leaves no remainders.
 * <p>
 * FABRIC-PORT: 26.x recipe serializers are plain records ({@link #SERIALIZER}); the datagen builder was removed together with the data generators.
 */
public class ZeroWasteRecipe extends ShapelessRecipe
{
    public static final MapCodec<ZeroWasteRecipe> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
          Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
          CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
          ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.resultTemplate),
          Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(o -> o.inputs)
        )
        .apply(i, ZeroWasteRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ZeroWasteRecipe> STREAM_CODEC = StreamCodec.composite(
      Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
      CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
      ItemStackTemplate.STREAM_CODEC, o -> o.resultTemplate,
      Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), o -> o.inputs,
      ZeroWasteRecipe::new
    );

    public static final RecipeSerializer<ZeroWasteRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private final ItemStackTemplate resultTemplate;
    private final List<Ingredient>  inputs;

    public ZeroWasteRecipe(final Recipe.CommonInfo commonInfo, final CraftingRecipe.CraftingBookInfo bookInfo, final ItemStackTemplate result, final List<Ingredient> inputs)
    {
        super(commonInfo, bookInfo, result, inputs);
        this.resultTemplate = result;
        this.inputs = inputs;
    }

    public ZeroWasteRecipe(@NotNull final ItemStack output, @NotNull final List<Ingredient> inputs)
    {
        this(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""), ItemStackTemplate.fromNonEmptyStack(output), inputs);
    }

    /**
     * @return the recipe ingredients.
     */
    public List<Ingredient> getInputs()
    {
        return inputs;
    }

    /**
     * @return a copy of the result.
     */
    public ItemStack getResult()
    {
        return resultTemplate.create();
    }

    @NotNull
    @Override
    public NonNullList<ItemStack> getRemainingItems(@NotNull final CraftingInput input)
    {
        final NonNullList<ItemStack> remainingItems = super.getRemainingItems(input);
        Collections.fill(remainingItems, ItemStack.EMPTY);
        return remainingItems;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @NotNull
    @Override
    public RecipeSerializer<ShapelessRecipe> getSerializer()
    {
        // ShapelessRecipe fixes the serializer type parameter, the dispatch codec only uses it to look up the registry id.
        return (RecipeSerializer) SERIALIZER;
    }
}
