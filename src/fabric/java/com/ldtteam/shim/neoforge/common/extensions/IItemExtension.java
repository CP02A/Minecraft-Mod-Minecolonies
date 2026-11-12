// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.extensions.IItemExtension), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common.extensions;

import com.ldtteam.shim.neoforge.common.ItemAbility;
import net.minecraft.network.chat.Component;
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

    /**
     * The text shown above the hotbar when the item is selected (selected item name overlay). Called by
     * {@code com.ldtteam.shim.mixin.client.HudMixin}; the overlay also shows again when this text changes, as on NeoForge.
     *
     * @param displayName the vanilla text (hover name with rarity color, italic if renamed).
     */
    default Component getHighlightTip(final ItemStack stack, final Component displayName)
    {
        return displayName;
    }
}
