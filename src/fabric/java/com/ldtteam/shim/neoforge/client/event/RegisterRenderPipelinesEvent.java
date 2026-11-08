// Adapted from NeoForge 26.3 (net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mod bus event to register render pipelines for pre-compilation. On Fabric pipelines are compiled lazily on first use, so registration
 * only records them.
 */
public class RegisterRenderPipelinesEvent extends Event implements IModBusEvent
{
    private static final List<RenderPipeline> PIPELINES = Collections.synchronizedList(new ArrayList<>());

    public RegisterRenderPipelinesEvent() {}

    public void registerPipeline(final RenderPipeline pipeline)
    {
        PIPELINES.add(pipeline);
    }

    public void registerOptionalPipeline(final RenderPipeline pipeline)
    {
        PIPELINES.add(pipeline);
    }

    public static List<RenderPipeline> getRegisteredPipelines()
    {
        return List.copyOf(PIPELINES);
    }
}
