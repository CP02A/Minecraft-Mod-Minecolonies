package com.minecolonies.core.util;

import com.google.common.collect.ImmutableList;
import com.minecolonies.api.compatibility.IFurnaceRecipes;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.crafting.RecipeStorage;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FurnaceRecipes implements IFurnaceRecipes
{
    /**
     * Furnace recipes.
     */
    private Map<ItemStorage, RecipeStorage> recipes = new HashMap<>();
    private Map<ItemStorage, RecipeStorage> reverseRecipes = new HashMap<>();

    /**
     * Load all the recipes in the recipe storage.
     *
     * @param recipeManager  The recipe manager to parse.
     */
    public void loadRecipes(final RecipeManager recipeManager, final Level level)
    {
        loadRecipes(recipeManager.getAllOfType(RecipeType.SMELTING));
    }

    /**
     * Load all the given smelting recipes in the recipe storage.
     * FABRIC-PORT: the client has no RecipeManager in 26.x, it uses Fabric's synchronized recipes instead.
     *
     * @param smeltingRecipes the smelting recipes.
     */
    public void loadRecipes(final Collection<RecipeHolder<SmeltingRecipe>> smeltingRecipes)
    {
        recipes.clear();
        reverseRecipes.clear();
        smeltingRecipes.forEach(holder -> {
            final SmeltingRecipe recipe = holder.value();
            final Ingredient ingredient = recipe.input();
            {
                for(final ItemStack smeltable: ingredient.items().map(ItemStack::new).toList())
                {
                    if (!smeltable.isEmpty())
                    {
                        final RecipeStorage storage = RecipeStorage.builder()
                                .withInputs(ImmutableList.of(new ItemStorage(smeltable)))
                                .withPrimaryOutput(recipe.assemble(new SingleRecipeInput(smeltable)))
                                .withGridSize(1)
                                .withIntermediate(Blocks.FURNACE)
                                .withRecipeId(holder.id().identifier())
                                .build();

                        recipes.put(storage.getCleanedInput().get(0), storage);

                        final ItemStack output = recipe.assemble(new SingleRecipeInput(smeltable)).copy();
                        output.setCount(1);
                        reverseRecipes.put(new ItemStorage(output), storage);
                    }
                }
            }
        });
    }

    @Override
    public ItemStack getSmeltingResult(final ItemStack itemStack)
    {
        final RecipeStorage storage = recipes.getOrDefault(new ItemStorage(itemStack), null);
        if (storage != null)
        {
            return storage.getPrimaryOutput();
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public RecipeStorage getFirstSmeltingRecipeByResult(final ItemStorage storage)
    {
        return reverseRecipes.get(storage);
    }
}
