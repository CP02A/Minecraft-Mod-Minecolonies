// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.function.Function;

/**
 * Mod bus event mapping {@link TooltipComponent}s to {@link ClientTooltipComponent}s; forwarded to Fabric's {@link ClientTooltipComponentCallback}.
 */
public class RegisterClientTooltipComponentFactoriesEvent extends Event implements IModBusEvent
{
    public RegisterClientTooltipComponentFactoriesEvent() {}

    public <T extends TooltipComponent> void register(final Class<T> type, final Function<? super T, ? extends ClientTooltipComponent> factory)
    {
        ClientTooltipComponentCallback.EVENT.register(component -> type.isInstance(component) ? factory.apply(type.cast(component)) : null);
    }
}
