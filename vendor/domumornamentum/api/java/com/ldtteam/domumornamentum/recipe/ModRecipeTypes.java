package com.ldtteam.domumornamentum.recipe;

import com.ldtteam.domumornamentum.recipe.architectscutter.ArchitectsCutterRecipe;
import com.ldtteam.domumornamentum.util.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;
import com.ldtteam.shim.neoforge.registries.DeferredHolder;
import com.ldtteam.shim.neoforge.registries.DeferredRegister;

public class ModRecipeTypes
{
    public static final DeferredRegister<RecipeType<?>> RECIPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Constants.MOD_ID);

    public static DeferredHolder<RecipeType<?>, RecipeType<ArchitectsCutterRecipe>> ARCHITECTS_CUTTER  = RECIPES.register("architects_cutter", () -> new RecipeType<>() { @Override public String toString() { return Constants.resLocDO("architects_cutter").toString(); } });

    private ModRecipeTypes()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ModRecipeTypes. This is a utility class");
    }
}
