package com.ldtteam.shim.neoforge.model.data;

import java.util.function.Predicate;

/**
 * NeoForge shim: a typed key into {@link ModelData}. Identity-compared; create one static instance per property.
 */
public class ModelProperty<T> implements Predicate<T>
{
    private final Predicate<T> predicate;

    public ModelProperty()
    {
        this(value -> true);
    }

    public ModelProperty(final Predicate<T> predicate)
    {
        this.predicate = predicate;
    }

    @Override
    public boolean test(final T value)
    {
        return predicate.test(value);
    }
}
