package com.ldtteam.shim.fabric;

import com.ldtteam.shim.api.distmarker.Dist;
import com.ldtteam.shim.bus.EventBus;
import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.fml.ModContainer;
import com.ldtteam.shim.fml.common.EventBusSubscriber;
import com.ldtteam.shim.fml.event.IModBusEvent;
import com.ldtteam.shim.fml.javafmlmod.FMLModContainer;
import com.ldtteam.shim.fml.loading.FMLEnvironment;
import com.ldtteam.shim.neoforge.common.NeoForge;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registry of the shimmed mods (each with its own mod bus) and helpers to post mod-bus events to all of them.
 */
public final class ShimModBuses
{
    private static final Logger LOGGER = LoggerFactory.getLogger("ShimModBuses");

    /**
     * The fabric mod that physically contains all shimmed mods.
     */
    public static final String FABRIC_MOD_ID = "minecolonies";

    private static final Map<String, FMLModContainer> CONTAINERS = new LinkedHashMap<>();
    private static final List<Runnable>               DEFERRED   = new ArrayList<>();

    private ShimModBuses()
    {
    }

    /**
     * Create (or get) the container of a shimmed mod. Containers are kept in creation order, which is the order mod-bus events are posted in.
     */
    public static synchronized FMLModContainer createContainer(final String modId)
    {
        return CONTAINERS.computeIfAbsent(modId, id -> new FMLModContainer(new FabricModInfo(id,
            FabricLoader.getInstance().getModContainer(id).or(() -> FabricLoader.getInstance().getModContainer(FABRIC_MOD_ID)).orElseThrow())));
    }

    /**
     * @return the shim container for the mod id or null.
     */
    public static synchronized FMLModContainer getContainer(final String modId)
    {
        return CONTAINERS.get(modId);
    }

    public static synchronized List<FMLModContainer> containers()
    {
        return List.copyOf(CONTAINERS.values());
    }

    /**
     * Post an event to every mod bus, in mod order.
     */
    public static <T extends Event> T post(final T event)
    {
        for (final FMLModContainer container : containers())
        {
            container.getEventBus().post(event);
        }
        return event;
    }

    /**
     * Post a per-mod event (e.g. lifecycle events carrying the container) to every mod bus.
     */
    public static void postPerMod(final Function<ModContainer, ? extends Event> factory)
    {
        for (final FMLModContainer container : containers())
        {
            container.getEventBus().post(factory.apply(container));
        }
    }

    /**
     * Post a lifecycle event to all mods and then run the work enqueued with enqueueWork.
     */
    public static void postLifecycle(final Function<ModContainer, ? extends Event> factory)
    {
        postPerMod(factory);
        runDeferredWork();
    }

    public static synchronized <T> CompletableFuture<T> enqueueWork(final Supplier<T> work)
    {
        final CompletableFuture<T> future = new CompletableFuture<>();
        DEFERRED.add(() -> {
            try
            {
                future.complete(work.get());
            }
            catch (final Throwable t)
            {
                future.completeExceptionally(t);
                throw t;
            }
        });
        return future;
    }

    public static void runDeferredWork()
    {
        final List<Runnable> work;
        synchronized (ShimModBuses.class)
        {
            work = new ArrayList<>(DEFERRED);
            DEFERRED.clear();
        }
        for (final Runnable runnable : work)
        {
            runnable.run();
        }
    }

    /**
     * Register an {@code @EventBusSubscriber} class like FML does: honours the dist filter, and routes each static
     * {@code @SubscribeEvent} method to the mod bus (IModBusEvent) or the game bus, or to the declared bus.
     */
    public static void registerSubscriber(final Class<?> clazz)
    {
        final EventBusSubscriber annotation = clazz.getAnnotation(EventBusSubscriber.class);
        if (annotation == null)
        {
            throw new IllegalArgumentException(clazz + " is not annotated with @EventBusSubscriber");
        }
        if (!Arrays.asList(annotation.value()).contains(FMLEnvironment.dist))
        {
            return;
        }
        final String modId = annotation.modid().isEmpty() ? FABRIC_MOD_ID : annotation.modid();
        final FMLModContainer container = Optional.ofNullable(getContainer(modId)).orElseGet(() -> createContainer(modId));
        for (final Method method : clazz.getDeclaredMethods())
        {
            final SubscribeEvent subscribe = method.getAnnotation(SubscribeEvent.class);
            if (subscribe == null || !Modifier.isStatic(method.getModifiers()))
            {
                continue;
            }
            if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0]))
            {
                throw new IllegalArgumentException("Method " + method + " has @SubscribeEvent annotation but does not take exactly one Event parameter");
            }
            final boolean modBus = switch (annotation.bus())
            {
                case MOD -> true;
                case GAME -> false;
                case BOTH -> IModBusEvent.class.isAssignableFrom(method.getParameterTypes()[0]);
            };
            final EventBus bus = (EventBus) (modBus ? container.getEventBus() : NeoForge.EVENT_BUS);
            bus.registerMethod(clazz, null, method, subscribe);
        }
        LOGGER.debug("Registered @EventBusSubscriber {} for {}", clazz.getName(), modId);
    }

    public static boolean isClient()
    {
        return FMLEnvironment.dist == Dist.CLIENT;
    }
}
