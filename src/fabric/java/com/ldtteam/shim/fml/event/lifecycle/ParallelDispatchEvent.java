package com.ldtteam.shim.fml.event.lifecycle;

import com.ldtteam.shim.fml.ModContainer;
import com.ldtteam.shim.fabric.ShimModBuses;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Lifecycle event with enqueueWork support (NeoForge shim). On Fabric dispatch is serial on the main thread,
 * so enqueued work is executed after all listeners of the event ran (see ShimModBuses#runDeferredWork).
 */
public abstract class ParallelDispatchEvent extends ModLifecycleEvent
{
    protected ParallelDispatchEvent(final ModContainer container)
    {
        super(container);
    }

    public CompletableFuture<Void> enqueueWork(final Runnable work)
    {
        return ShimModBuses.enqueueWork(() -> {
            work.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> enqueueWork(final Supplier<T> work)
    {
        return ShimModBuses.enqueueWork(work);
    }
}
