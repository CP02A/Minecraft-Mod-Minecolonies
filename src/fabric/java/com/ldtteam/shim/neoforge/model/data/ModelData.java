package com.ldtteam.shim.neoforge.model.data;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * NeoForge shim: an immutable container of {@link ModelProperty} values attached to a block entity for rendering.
 *
 * <p>On Fabric, model data travels through Fabric's block entity render data: a block entity that provides model data
 * overrides {@code BlockEntity#getRenderData()} (Fabric API, injected interface {@code RenderDataBlockEntity}) and
 * returns its {@link ModelData}. Models read it back with
 * {@link ModelDataHelper#getModelData(net.minecraft.world.level.BlockGetter, net.minecraft.core.BlockPos)}, which uses
 * the thread-safe {@code BlockGetter#getBlockEntityRenderData(BlockPos)}.
 *
 * <p>Instances are value-comparable (equals/hashCode over the property map) so they can be part of geometry cache keys.
 */
public final class ModelData
{
    public static final ModelData EMPTY = new ModelData(new Reference2ReferenceArrayMap<>());

    private final Map<ModelProperty<?>, Object> properties;
    private @Nullable Set<ModelProperty<?>> propertySetView;

    private ModelData(final Map<ModelProperty<?>, Object> properties)
    {
        this.properties = properties;
    }

    public Set<ModelProperty<?>> getProperties()
    {
        var view = propertySetView;
        if (view == null)
        {
            propertySetView = view = Collections.unmodifiableSet(properties.keySet());
        }
        return view;
    }

    public boolean has(final ModelProperty<?> property)
    {
        return properties.containsKey(property);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(final ModelProperty<T> property)
    {
        return (T) properties.get(property);
    }

    public <T> T getOrDefault(final ModelProperty<T> property, final T fallback)
    {
        final T value = get(property);
        return value == null ? fallback : value;
    }

    public boolean isEmpty()
    {
        return properties.isEmpty();
    }

    public Builder derive()
    {
        return new Builder(this);
    }

    public static Builder builder()
    {
        return new Builder(null);
    }

    public static <T> ModelData of(final ModelProperty<T> property, final T value)
    {
        Preconditions.checkState(property.test(value), "The provided value is invalid for this property.");
        final Reference2ReferenceArrayMap<ModelProperty<?>, Object> map = new Reference2ReferenceArrayMap<>(1);
        map.put(property, value);
        return new ModelData(map);
    }

    @Override
    public boolean equals(final Object o)
    {
        return this == o || (o instanceof ModelData other && properties.equals(other.properties));
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(properties);
    }

    @Override
    public String toString()
    {
        return "ModelData" + properties;
    }

    public static final class Builder
    {
        private static final int HASH_THRESHOLD = 4;
        private Map<ModelProperty<?>, Object> properties;

        private Builder(final @Nullable ModelData parent)
        {
            if (parent != null)
            {
                properties = parent.properties.size() >= HASH_THRESHOLD
                    ? new Reference2ReferenceOpenHashMap<>(parent.properties)
                    : new Reference2ReferenceArrayMap<>(parent.properties);
            }
            else
            {
                properties = new Reference2ReferenceArrayMap<>(HASH_THRESHOLD);
            }
        }

        @Contract("_, _ -> this")
        public <T> Builder with(final ModelProperty<T> property, final T value)
        {
            Preconditions.checkState(property.test(value), "The provided value is invalid for this property.");
            properties.put(property, value);
            if (properties.size() == HASH_THRESHOLD && properties instanceof Reference2ReferenceArrayMap<ModelProperty<?>, Object>)
            {
                properties = new Reference2ReferenceOpenHashMap<>(properties);
            }
            return this;
        }

        public ModelData build()
        {
            return properties.isEmpty() ? EMPTY : new ModelData(properties);
        }
    }
}
