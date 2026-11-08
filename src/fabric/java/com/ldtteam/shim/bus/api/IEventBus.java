package com.ldtteam.shim.bus.api;

import java.util.function.Consumer;

/**
 * NeoForge-style event bus (shim). See {@link com.ldtteam.shim.bus.EventBus} for the implementation.
 */
public interface IEventBus
{
    void register(Object target);

    <T extends Event> void addListener(Consumer<T> consumer);

    <T extends Event> void addListener(Class<T> eventType, Consumer<T> consumer);

    <T extends Event> void addListener(EventPriority priority, Consumer<T> consumer);

    <T extends Event> void addListener(EventPriority priority, Class<T> eventType, Consumer<T> consumer);

    <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Consumer<T> consumer);

    <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> eventType, Consumer<T> consumer);

    <T extends Event> void addListener(boolean receiveCanceled, Consumer<T> consumer);

    <T extends Event> void addListener(boolean receiveCanceled, Class<T> eventType, Consumer<T> consumer);

    void unregister(Object object);

    <T extends Event> T post(T event);

    <T extends Event> T post(EventPriority phase, T event);

    void start();
}
