// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import com.ldtteam.shim.neoforge.client.IItemDecorator;
import net.fabricmc.fabric.api.client.rendering.v1.ExtractItemDecorationsCallback;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

/**
 * Mod bus event to register item decorators; forwarded to Fabric's {@link ExtractItemDecorationsCallback}.
 */
public class RegisterItemDecorationsEvent extends Event implements IModBusEvent
{
    public RegisterItemDecorationsEvent() {}

    public void register(final ItemLike itemLike, final IItemDecorator decorator)
    {
        final Item item = itemLike.asItem();
        ExtractItemDecorationsCallback.EVENT.register((graphics, font, stack, x, y) -> {
            if (stack.is(item))
            {
                decorator.render(graphics, font, stack, x, y);
            }
        });
    }
}
