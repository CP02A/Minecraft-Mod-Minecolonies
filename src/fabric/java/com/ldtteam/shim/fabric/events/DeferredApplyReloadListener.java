package com.ldtteam.shim.fabric.events;

import com.ldtteam.shim.mixin.SimplePreparableReloadListenerInvoker;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.Profiler;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * Wraps a mod's server data reload listener so its {@code apply} step runs after the reload finished binding item components.
 * <p>
 * Since 26.x, item components are data driven: during a server data reload they are bound only after all reload listeners ran
 * ({@code ReloadableServerResources#updateComponentsAndStaticRegistryTags}). NeoForge-era mod listeners create {@code ItemStack}s
 * while applying their data, which throws "Components not bound yet" on the first load. The {@code prepare} step still runs during the
 * reload; the {@code apply} step is queued and executed from Fabric's {@code TAGS_LOADED} event, which fires right after the components
 * and tags are bound (on the server thread, before the world loads / before /reload completes).
 */
public final class DeferredApplyReloadListener implements PreparableReloadListener
{
    private static final Queue<Runnable> PENDING = new ConcurrentLinkedQueue<>();

    static
    {
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (!client)
            {
                runPending();
            }
        });
    }

    private final SimplePreparableReloadListener<?> delegate;

    private DeferredApplyReloadListener(final SimplePreparableReloadListener<?> delegate)
    {
        this.delegate = delegate;
    }

    /**
     * Wrap a listener if it is a simple (prepare/apply) listener; other listeners are returned unchanged.
     *
     * @param listener the mod listener.
     * @return the listener to register.
     */
    public static PreparableReloadListener wrap(final PreparableReloadListener listener)
    {
        return listener instanceof SimplePreparableReloadListener<?> simple ? new DeferredApplyReloadListener(simple) : listener;
    }

    /**
     * Drop queued applies of a reload that never completed.
     */
    public static void startReload()
    {
        PENDING.clear();
    }

    private static void runPending()
    {
        Runnable task;
        while ((task = PENDING.poll()) != null)
        {
            task.run();
        }
    }

    @Override
    public CompletableFuture<Void> reload(
        final SharedState currentReload,
        final Executor taskExecutor,
        final PreparationBarrier preparationBarrier,
        final Executor reloadExecutor)
    {
        final ResourceManager manager = currentReload.resourceManager();
        final SimplePreparableReloadListenerInvoker invoker = (SimplePreparableReloadListenerInvoker) delegate;
        return CompletableFuture.supplyAsync(() -> invoker.shim$prepare(manager, Profiler.get()), taskExecutor)
                 .thenCompose(preparationBarrier::wait)
                 .thenAcceptAsync(prepared -> PENDING.add(() -> invoker.shim$apply(prepared, manager, Profiler.get())), reloadExecutor);
    }

    @Override
    public void prepareSharedState(final SharedState currentReload)
    {
        delegate.prepareSharedState(currentReload);
    }

    @Override
    public String getName()
    {
        return delegate.getName();
    }
}
