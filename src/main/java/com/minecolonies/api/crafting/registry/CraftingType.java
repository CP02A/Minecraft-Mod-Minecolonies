package com.minecolonies.api.crafting.registry;

import com.minecolonies.api.crafting.IGenericRecipe;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Class to represent the different types of crafting supported by MineColonies
 */
public abstract class CraftingType
{
    private Identifier registryName;

    protected CraftingType(@NotNull final Identifier id)
    {
        this.registryName = id;
    }

    /**
     * Find all teachable recipes supported by this particular crafting type
     * @param recipeManager the vanilla recipe manager
     * @param world the world (if available)
     * @return the list of teachable recipes
     */
    @NotNull
    public abstract List<IGenericRecipe> findRecipes(@NotNull final RecipeManager recipeManager,
                                                     @Nullable final Level world);

    /**
     * The recipe types {@link #findRecipes} reads from the recipe manager.
     * FABRIC-PORT: 26.x clients don't get all recipes. A dedicated server sends the recipes of these types that the Fabric recipe sync
     * doesn't cover to clients with MineColonies (CrafterRecipeSync), so JEI's crafter categories can show them. Empty: nothing is sent.
     *
     * @return the recipe types.
     */
    @NotNull
    public Set<RecipeType<?>> getRecipeTypes()
    {
        return Set.of();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof CraftingType)
        {
            return Objects.equals(registryName, ((CraftingType) obj).registryName);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return registryName.hashCode();
    }
}
