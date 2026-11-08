// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.function.Function;

/**
 * 26.x recipe book categories are data driven ({@code Recipe#recipeBookCategory()}), so registrations through this event are ignored.
 */
public class RegisterRecipeBookCategoriesEvent extends Event implements IModBusEvent
{
    public RegisterRecipeBookCategoriesEvent() {}

    public <T extends Recipe<?>> void registerRecipeCategoryFinder(final RecipeType<T> type, final Function<RecipeHolder<T>, ?> lookup)
    {
        // no-op on 26.x
    }
}
