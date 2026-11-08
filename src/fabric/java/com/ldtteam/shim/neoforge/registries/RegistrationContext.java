package com.ldtteam.shim.neoforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Fabric shim helper: remembers the key of the entry currently being constructed by a DeferredRegister supplier, so that
 * Block/Item properties without an id (26.x requires Properties#setId) get it assigned automatically (see the
 * com.ldtteam.shim.mixin.registries mixins).
 */
public final class RegistrationContext
{
    private static final ThreadLocal<ResourceKey<?>> CURRENT = new ThreadLocal<>();

    private RegistrationContext()
    {
    }

    public static <T> T withKey(final ResourceKey<?> key, final Supplier<T> supplier)
    {
        final ResourceKey<?> previous = CURRENT.get();
        CURRENT.set(key);
        try
        {
            return supplier.get();
        }
        finally
        {
            CURRENT.set(previous);
        }
    }

    /**
     * @return the key currently being registered if it belongs to the given registry, else null.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> ResourceKey<T> current(final ResourceKey<? extends Registry<T>> registry)
    {
        final ResourceKey<?> key = CURRENT.get();
        return key != null && key.isFor(registry) ? (ResourceKey<T>) key : null;
    }
}
