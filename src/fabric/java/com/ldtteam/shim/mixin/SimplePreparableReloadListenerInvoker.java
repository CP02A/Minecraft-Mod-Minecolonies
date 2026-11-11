package com.ldtteam.shim.mixin;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Lets {@link com.ldtteam.shim.fabric.events.DeferredApplyReloadListener} run the two halves of a simple reload listener separately.
 */
@Mixin(SimplePreparableReloadListener.class)
public interface SimplePreparableReloadListenerInvoker
{
    @Invoker("prepare")
    Object minecolonies$prepare(ResourceManager manager, ProfilerFiller profiler);

    @Invoker("apply")
    void minecolonies$apply(Object preparations, ResourceManager manager, ProfilerFiller profiler);
}
