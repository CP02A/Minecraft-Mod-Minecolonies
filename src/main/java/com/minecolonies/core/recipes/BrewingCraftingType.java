package com.minecolonies.core.recipes;

import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.compatibility.ICompatibilityManager;
import com.minecolonies.api.crafting.GenericRecipe;
import com.minecolonies.api.crafting.IGenericRecipe;
import com.minecolonies.api.crafting.ModCraftingTypes;
import com.minecolonies.api.crafting.registry.CraftingType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A crafting type for brewing recipes
 */
public class BrewingCraftingType extends CraftingType
{
    public BrewingCraftingType()
    {
        super(ModCraftingTypes.BREWING_ID);
    }

    @Override
    @NotNull
    public List<IGenericRecipe> findRecipes(@NotNull RecipeManager recipeManager, @Nullable Level world)
    {
        final List<IGenericRecipe> recipes = new ArrayList<>();
        final ICompatibilityManager compatibilityManager = MinecoloniesAPIProxy.getInstance().getColonyManager().getCompatibilityManager();

        // 26.x: brewing is data driven (BrewingRecipe) instead of the old PotionBrewing registry.
        final Collection<RecipeHolder<BrewingRecipe>> brewingRecipes = recipeManager.getAllOfType(RecipeType.BREWING);

        final List<ItemStack> containers = compatibilityManager.getListOfAllItems().stream()
                .filter(stack -> brewingRecipes.stream().anyMatch(r -> r.value().getInput().test(stack)))
                .toList();
        final List<ItemStack> ingredients = compatibilityManager.getListOfAllItems().stream()
                .filter(stack -> brewingRecipes.stream().anyMatch(r -> r.value().getReagent().test(stack)))
                .toList();

        for (final ItemStack container : containers)
        {
            for (final ItemStack ingredient : ingredients)
            {
                final BrewingInput input = new BrewingInput(container, ingredient);
                final ItemStack output = brewingRecipes.stream()
                        .filter(r -> r.value().matches(input))
                        .findFirst()
                        .map(r -> r.value().assemble(input))
                        .orElse(ItemStack.EMPTY);
                if (!output.isEmpty())
                {
                    recipes.add(GenericRecipe.builder()
                            .withOutput(output.copyWithCount(3))
                            .withInputs(List.of(List.of(ingredient), List.of(container.copyWithCount(3))))
                            .withIntermediate(Blocks.BREWING_STAND)
                            .build());
                }
            }
        }

        return recipes;
    }
}
