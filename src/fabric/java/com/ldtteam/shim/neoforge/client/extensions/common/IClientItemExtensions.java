// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.extensions.common.IClientItemExtensions), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.extensions.common;

import net.minecraft.client.gui.Font;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only item extensions. Registered through {@link RegisterClientExtensionsEvent}; on Fabric only the hooks that the shim
 * consults are functional (fonts are only used by code calling {@link #getFont}).
 */
public interface IClientItemExtensions
{
    IClientItemExtensions DEFAULT = new IClientItemExtensions() {};

    static IClientItemExtensions of(final ItemStack stack)
    {
        return of(stack.getItem());
    }

    static IClientItemExtensions of(final Item item)
    {
        return ClientExtensionsManager.ITEM_EXTENSIONS.getOrDefault(item, DEFAULT);
    }

    /**
     * Returns the font used to render data related to this item. Return null to use the default font renderer.
     */
    @Nullable
    default Font getFont(final ItemStack stack, final FontContext context)
    {
        return null;
    }

    default boolean shouldBobAsEntity(final ItemStack stack)
    {
        return true;
    }

    default boolean shouldSpreadAsEntity(final ItemStack stack)
    {
        return true;
    }

    enum FontContext
    {
        ITEM_COUNT,
        TOOLTIP,
        SELECTED_ITEM_NAME
    }
}
