// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.ClientTooltipFlag), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;

/**
 * Tooltip flag that also carries the current modifier key state.
 */
public record ClientTooltipFlag(boolean isAdvanced, boolean isCreative, boolean hasShiftDown, boolean hasControlDown, boolean hasAltDown) implements TooltipFlag
{
    public static TooltipFlag of(final TooltipFlag other)
    {
        final Minecraft mc = Minecraft.getInstance();
        return new ClientTooltipFlag(other.isAdvanced(), other.isCreative(), mc.hasShiftDown(), mc.hasControlDown(), mc.hasAltDown());
    }
}
