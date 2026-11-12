package com.minecolonies.gametest.framework;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped crafting recipe with its own serializer, like other mods' custom crafting recipe types (UI-13). The serializer is registered only
 * by the test mod and not with Fabric's recipe sync, so a dedicated server doesn't synchronize these recipes through Fabric; MineColonies'
 * crafter recipe payload ({@code CrafterRecipeSync}) has to bring them to the client. Recipe: {@code data/minecolonies-gametest/recipe/}.
 */
public class TestShapedRecipe extends ShapedRecipe
{
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath("minecolonies-gametest", "test_shaped");

    /**
     * The test recipe (a sawmill recipe: logs and sticks to planks).
     */
    public static final Identifier RECIPE_ID = Identifier.fromNamespaceAndPath("minecolonies-gametest", "crafter_sync_test");

    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate   result;

    public static final MapCodec<TestShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
      CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
      ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
      ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result)).apply(i, TestShapedRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TestShapedRecipe> STREAM_CODEC = StreamCodec.composite(
      Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
      CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
      ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
      ItemStackTemplate.STREAM_CODEC, r -> r.result,
      TestShapedRecipe::new);

    public static final RecipeSerializer<TestShapedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public TestShapedRecipe(final Recipe.CommonInfo commonInfo, final CraftingRecipe.CraftingBookInfo bookInfo, final ShapedRecipePattern pattern,
                            final ItemStackTemplate result)
    {
        super(commonInfo, bookInfo, pattern, result);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ShapedRecipe> getSerializer()
    {
        return (RecipeSerializer) SERIALIZER;
    }

    /**
     * Registers the serializer (test mod initializer).
     */
    public static void register()
    {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SERIALIZER_ID, SERIALIZER);
    }
}
