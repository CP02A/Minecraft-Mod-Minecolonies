package com.ldtteam.shim.bus;

import com.ldtteam.shim.bus.api.*;
import net.jodah.typetools.TypeResolver;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Re-implementation of the NeoForge event bus semantics: priorities, receiveCanceled, dispatch to listeners of the
 * event class and all its super types, SubscribeEvent scanning of classes (static methods) and instances (instance methods).
 */
public class EventBus implements IEventBus
{
    private static final Logger     LOGGER = LoggerFactory.getLogger("ShimEventBus");
    private static final AtomicLong ORDER  = new AtomicLong();

    private record Listener(EventPriority priority, boolean receiveCanceled, Class<?> eventType, Consumer<Event> invoker, Object owner, long order)
    {
    }

    private final String                     name;
    private final List<Listener>             listeners = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, Listener[]>  cache     = new ConcurrentHashMap<>();

    public EventBus(final String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return "EventBus[" + name + "]";
    }

    @Override
    public void register(final Object target)
    {
        final boolean isClass = target instanceof Class<?>;
        final Class<?> clazz = isClass ? (Class<?>) target : target.getClass();
        final Set<String> seen = new HashSet<>();
        boolean found = false;
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass())
        {
            for (final Method method : c.getDeclaredMethods())
            {
                final SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
                if (annotation == null)
                {
                    continue;
                }
                final boolean isStatic = Modifier.isStatic(method.getModifiers());
                if (isClass != isStatic)
                {
                    continue;
                }
                if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0]))
                {
                    throw new IllegalArgumentException("Method " + method + " has @SubscribeEvent annotation but does not take exactly one Event parameter");
                }
                if (!seen.add(method.getName() + method.getParameterTypes()[0].getName()))
                {
                    continue; // overridden in subclass
                }
                found = true;
                addMethodListener(target, isStatic ? null : target, method, annotation);
            }
            if (isClass)
            {
                break;
            }
        }
        if (!found)
        {
            LOGGER.debug("{} has no @SubscribeEvent methods, registered to {}", clazz.getName(), name);
        }
    }

    /**
     * Register a single listener method (used for EventBusSubscriber routing).
     */
    public void registerMethod(final Object owner, final Object instance, final Method method, final SubscribeEvent annotation)
    {
        addMethodListener(owner, instance, method, annotation);
    }

    private void addMethodListener(final Object owner, final Object instance, final Method method, final SubscribeEvent annotation)
    {
        final MethodHandle handle;
        try
        {
            method.setAccessible(true);
            final MethodHandle raw = MethodHandles.lookup().unreflect(method);
            handle = instance == null ? raw : raw.bindTo(instance);
        }
        catch (final IllegalAccessException e)
        {
            throw new IllegalStateException("Cannot access event listener " + method, e);
        }
        final Consumer<Event> invoker = event -> {
            try
            {
                handle.invoke(event);
            }
            catch (final RuntimeException | Error e)
            {
                throw e;
            }
            catch (final Throwable t)
            {
                throw new RuntimeException("Exception in event listener " + method, t);
            }
        };
        addInternal(new Listener(annotation.priority(), annotation.receiveCanceled(), method.getParameterTypes()[0], invoker, owner, ORDER.incrementAndGet()));
    }

    /**
     * Resolve the event type of a lambda / method reference listener with TypeTools.
     *
     * @return the type, or null if it could not be resolved.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private <T extends Event> Class<T> resolve(final Consumer<T> consumer)
    {
        try
        {
            final Class<?> type = TypeResolver.resolveRawArgument(Consumer.class, consumer.getClass());
            if (type != null && type != TypeResolver.Unknown.class && Event.class.isAssignableFrom(type))
            {
                return (Class<T>) type;
            }
        }
        catch (final Throwable t)
        {
            LOGGER.debug("TypeTools failed to resolve listener type of {}", consumer, t);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void addInternal(final EventPriority priority, final boolean receiveCanceled, @Nullable final Class<T> type, final Consumer<T> consumer)
    {
        if (type == null)
        {
            LOGGER.warn("Could not resolve the event type of listener {}; falling back to type probing. Prefer addListener(Class, Consumer).", consumer);
            addInternal(new Listener(priority, receiveCanceled, Event.class, new ProbingInvoker((Consumer<Event>) consumer), consumer, ORDER.incrementAndGet()));
            return;
        }
        addInternal(new Listener(priority, receiveCanceled, type, (Consumer<Event>) consumer, consumer, ORDER.incrementAndGet()));
    }

    /**
     * Fallback for listeners whose event type cannot be resolved: the lambda's bridge method casts its argument to the
     * real event type before running the body, so a ClassCastException thrown directly by the bridge means "not my event".
     * Rejected event classes are remembered so the cost is paid once per event class.
     */
    private static final class ProbingInvoker implements Consumer<Event>
    {
        private final Consumer<Event> consumer;
        private final Set<Class<?>>   rejected = ConcurrentHashMap.newKeySet();
        private final Set<Class<?>>   accepted = ConcurrentHashMap.newKeySet();

        private ProbingInvoker(final Consumer<Event> consumer)
        {
            this.consumer = consumer;
        }

        @Override
        public void accept(final Event event)
        {
            final Class<?> eventClass = event.getClass();
            if (rejected.contains(eventClass))
            {
                return;
            }
            if (accepted.contains(eventClass))
            {
                consumer.accept(event);
                return;
            }
            try
            {
                consumer.accept(event);
                accepted.add(eventClass);
            }
            catch (final ClassCastException e)
            {
                final StackTraceElement[] trace = e.getStackTrace();
                // hidden lambda frames are omitted, so a bridge cast failure shows this class as the top frame
                if (trace.length > 0 && trace[0].getClassName().equals(ProbingInvoker.class.getName()) || trace.length > 0 && trace[0].getClassName().contains("$$Lambda"))
                {
                    rejected.add(eventClass);
                    return;
                }
                throw e;
            }
        }
    }

    private void addInternal(final Listener listener)
    {
        listeners.add(listener);
        cache.clear();
    }

    @Override
    public <T extends Event> void addListener(final Consumer<T> consumer)
    {
        addInternal(EventPriority.NORMAL, false, resolve(consumer), consumer);
    }

    @Override
    public <T extends Event> void addListener(final Class<T> eventType, final Consumer<T> consumer)
    {
        addInternal(EventPriority.NORMAL, false, eventType, consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final Consumer<T> consumer)
    {
        addInternal(priority, false, resolve(consumer), consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final Class<T> eventType, final Consumer<T> consumer)
    {
        addInternal(priority, false, eventType, consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCanceled, final Consumer<T> consumer)
    {
        addInternal(priority, receiveCanceled, resolve(consumer), consumer);
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCanceled, final Class<T> eventType, final Consumer<T> consumer)
    {
        addInternal(priority, receiveCanceled, eventType, consumer);
    }

    @Override
    public <T extends Event> void addListener(final boolean receiveCanceled, final Consumer<T> consumer)
    {
        addInternal(EventPriority.NORMAL, receiveCanceled, resolve(consumer), consumer);
    }

    @Override
    public <T extends Event> void addListener(final boolean receiveCanceled, final Class<T> eventType, final Consumer<T> consumer)
    {
        addInternal(EventPriority.NORMAL, receiveCanceled, eventType, consumer);
    }

    @Override
    public void unregister(final Object object)
    {
        if (listeners.removeIf(l -> l.owner() == object))
        {
            cache.clear();
        }
    }

    /**
     * @return true if any listener could receive events of the given class.
     */
    public boolean hasListeners(final Class<? extends Event> eventClass)
    {
        return listenersFor(eventClass).length > 0;
    }

    private Listener[] listenersFor(final Class<?> eventClass)
    {
        return cache.computeIfAbsent(eventClass, c -> listeners.stream()
            .filter(l -> l.eventType().isAssignableFrom(c))
            .sorted(Comparator.comparing(Listener::priority).thenComparingLong(Listener::order))
            .toArray(Listener[]::new));
    }

    @Override
    public <T extends Event> T post(final T event)
    {
        for (final Listener listener : listenersFor(event.getClass()))
        {
            invoke(listener, event);
        }
        return event;
    }

    @Override
    public <T extends Event> T post(final EventPriority phase, final T event)
    {
        for (final Listener listener : listenersFor(event.getClass()))
        {
            if (listener.priority() == phase)
            {
                invoke(listener, event);
            }
        }
        return event;
    }

    private static void invoke(final Listener listener, final Event event)
    {
        if (!listener.receiveCanceled() && event instanceof ICancellableEvent cancellable && cancellable.isCanceled())
        {
            return;
        }
        listener.invoker().accept(event);
    }

    @Override
    public void start()
    {
    }
}
