// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registration of fixed render buffers. 26.x renders through submit node collectors, so registered render types are only recorded.
 */
public class RegisterRenderBuffersEvent extends Event implements IModBusEvent
{
    private static final List<Object> RENDER_TYPES = Collections.synchronizedList(new ArrayList<>());

    public RegisterRenderBuffersEvent() {}

    public void registerRenderBuffer(final Object renderType)
    {
        RENDER_TYPES.add(renderType);
    }
}
