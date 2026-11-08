// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.RecipesUpdatedEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes;

/**
 * Fired on the game bus when the client received recipes. On 26.x the client no longer has a full RecipeManager: only recipes whose
 * serializers were registered with Fabric's {@code RecipeSynchronization} are available, through {@link #getRecipes()}.
 * Fired from Fabric's ClientRecipeSynchronizedEvent.
 */
public class RecipesUpdatedEvent extends Event
{
    private final SynchronizedRecipes recipes;

    public RecipesUpdatedEvent(final SynchronizedRecipes recipes)
    {
        this.recipes = recipes;
    }

    public SynchronizedRecipes getRecipes()
    {
        return recipes;
    }
}
