// Adapted from NeoForge 26.3 (net.neoforged.neoforge.client.event.AddClientReloadListenersEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/**
 * Mod bus event to register named client resource reload listeners; forwarded to Fabric's {@link ResourceLoader} for client resources.
 */
public class AddClientReloadListenersEvent extends Event implements IModBusEvent
{
    public AddClientReloadListenersEvent() {}

    public void addListener(final Identifier key, final PreparableReloadListener listener)
    {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(key, listener);
    }

    /**
     * Declares that {@code first} must run before {@code second}.
     */
    public void addDependency(final Identifier first, final Identifier second)
    {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).addListenerOrdering(first, second);
    }
}
