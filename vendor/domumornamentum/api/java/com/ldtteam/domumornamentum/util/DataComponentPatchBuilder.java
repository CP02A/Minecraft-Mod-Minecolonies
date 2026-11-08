package com.ldtteam.domumornamentum.util;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Extended builder with update support.
 *
 * <p>Fabric: vanilla's {@link DataComponentPatch.Builder} cannot be subclassed (private constructor, NeoForge widened
 * it), so this wraps a vanilla builder and tracks the values set through it.
 */
public class DataComponentPatchBuilder
{
    private final DataComponentPatch.Builder delegate = DataComponentPatch.builder();
    private final Map<DataComponentType<?>, Object> values = new IdentityHashMap<>();

    public DataComponentPatchBuilder()
    {
    }

    public <T> T getOrDefault(final Supplier<DataComponentType<T>> type, final T defaultValue)
    {
        return getOrDefault(type.get(), defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(final DataComponentType<T> type, final T defaultValue)
    {
        final Object value = values.get(type);
        return value == null ? defaultValue : (T) value;
    }

    public <T> DataComponentPatchBuilder update(final Supplier<DataComponentType<T>> type, final T defaultValue, final UnaryOperator<T> updater)
    {
        return update(type.get(), defaultValue, updater);
    }

    public <T> DataComponentPatchBuilder update(final DataComponentType<T> type, final T defaultValue, final UnaryOperator<T> updater)
    {
        set(type, updater.apply(getOrDefault(type, defaultValue)));
        return this;
    }

    public <T> DataComponentPatchBuilder set(final Supplier<DataComponentType<T>> type, final T value)
    {
        return set(type.get(), value);
    }

    public <T> DataComponentPatchBuilder set(final DataComponentType<T> type, final T value)
    {
        values.put(type, value);
        delegate.set(type, value);
        return this;
    }

    public <T> DataComponentPatchBuilder remove(final DataComponentType<T> type)
    {
        values.remove(type);
        delegate.remove(type);
        return this;
    }

    public DataComponentPatch build()
    {
        return delegate.build();
    }
}
