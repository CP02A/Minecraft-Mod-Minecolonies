// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.extensions.IItemExtension), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common.extensions;

import com.ldtteam.shim.neoforge.common.ItemAbility;
import net.minecraft.world.item.ItemStack;

/**
 * Subset of NeoForge's item extension interface. Items implementing this interface are consulted by
 * {@link com.ldtteam.shim.neoforge.common.ItemAbilities#canPerformAction(ItemStack, ItemAbility)}.
 */
public interface IItemExtension
{
    /**
     * Queries if an item can perform the given action. Returning true grants the ability; false falls back to the default tag based checks.
     */
    default boolean canPerformAction(final ItemStack stack, final ItemAbility itemAbility)
    {
        return false;
    }
}
