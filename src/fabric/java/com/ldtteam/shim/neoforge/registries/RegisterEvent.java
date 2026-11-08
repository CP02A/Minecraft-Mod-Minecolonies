/*
 * Derived from NeoForge 21.1 (net.neoforged.neoforge.registries.RegisterEvent),
 * Copyright (c) Forge Development LLC and contributors, SPDX-License-Identifier: LGPL-2.1-only.
 */
package com.ldtteam.shim.neoforge.registries;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fired for each registry when it is ready to have modded objects registered (NeoForge shim; fired by the Fabric entrypoint).
 */
public class RegisterEvent extends Event implements IModBusEvent
{
    private final ResourceKey<? extends Registry<?>> registryKey;
    private final Registry<?>                        registry;

    public RegisterEvent(final ResourceKey<? extends Registry<?>> registryKey, final Registry<?> registry)
    {
        this.registryKey = registryKey;
        this.registry = registry;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void register(final ResourceKey<? extends Registry<T>> registryKey, final Identifier name, final Supplier<T> valueSupplier)
    {
        if (this.registryKey.equals(registryKey))
        {
            final T value = RegistrationContext.withKey(ResourceKey.create((ResourceKey) registryKey, name), valueSupplier);
            Registry.register((Registry) this.registry, name, value);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void register(final ResourceKey<? extends Registry<T>> registryKey, final Consumer<RegisterHelper<T>> consumer)
    {
        if (this.registryKey.equals(registryKey))
        {
            consumer.accept((name, value) -> Registry.register((Registry) this.registry, name, value));
        }
    }

    public ResourceKey<? extends Registry<?>> getRegistryKey()
    {
        return this.registryKey;
    }

    public Registry<?> getRegistry()
    {
        return this.registry;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> Registry<T> getRegistry(final ResourceKey<? extends Registry<T>> key)
    {
        return key.equals(this.registryKey) ? (Registry<T>) this.registry : null;
    }

    @FunctionalInterface
    public interface RegisterHelper<T>
    {
        default void register(final ResourceKey<T> key, final T value)
        {
            register(key.identifier(), value);
        }

        void register(Identifier name, T value);
    }
}
