// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mod bus event to register client resource reload listeners; forwarded to Fabric's {@link ResourceLoader} for client resources.
 */
public class RegisterClientReloadListenersEvent extends Event implements IModBusEvent
{
    private static final AtomicInteger COUNTER = new AtomicInteger();

    public RegisterClientReloadListenersEvent() {}

    public void registerReloadListener(final PreparableReloadListener reloadListener)
    {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(generateId(reloadListener), reloadListener);
    }

    static Identifier generateId(final Object listener)
    {
        final String name = listener.getClass().getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        return Identifier.fromNamespaceAndPath("minecolonies", "shim/" + name + "_" + COUNTER.incrementAndGet());
    }
}
