/*
 * Derived from NeoForge 26.3 (net.neoforged.neoforge.registries.DeferredHolder),
 * Copyright (c) Forge Development LLC and contributors, SPDX-License-Identifier: LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim.
 */
package com.ldtteam.shim.neoforge.registries;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A lazily bound holder of a registry entry, created by {@link DeferredRegister}.
 */
public class DeferredHolder<R, T extends R> extends Holder.Reference<R> implements Supplier<T>
{
    public static <R, T extends R> DeferredHolder<R, T> create(final ResourceKey<? extends Registry<R>> registryKey, final Identifier valueName)
    {
        return create(ResourceKey.create(registryKey, valueName));
    }

    public static <R, T extends R> DeferredHolder<R, T> create(final Identifier registryName, final Identifier valueName)
    {
        return create(ResourceKey.createRegistryKey(registryName), valueName);
    }

    public static <R, T extends R> DeferredHolder<R, T> create(final ResourceKey<R> key)
    {
        return new DeferredHolder<>(key);
    }

    @Nullable
    private Holder<R> holder = null;

    @SuppressWarnings("unchecked")
    protected DeferredHolder(final ResourceKey<R> key)
    {
        super(Holder.Reference.Type.STAND_ALONE, (HolderOwner<R>) BuiltInRegistries.REGISTRY.getValue(Objects.requireNonNull(key).registry()), key, null);
        this.bind(false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T value()
    {
        bind(true);
        if (this.holder == null)
        {
            throw new NullPointerException("Trying to access unbound value: " + this.key());
        }
        return (T) this.holder.value();
    }

    @Override
    public T get()
    {
        return this.value();
    }

    public Optional<T> asOptional()
    {
        return isBound() ? Optional.of(value()) : Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public HolderLookup.RegistryLookup<R> unwrapLookup()
    {
        return (Registry<R>) BuiltInRegistries.REGISTRY.getValue(this.key().registry());
    }

    protected final void bind(final boolean throwOnMissingRegistry)
    {
        if (this.holder != null)
        {
            return;
        }
        final HolderLookup.RegistryLookup<R> registry = unwrapLookup();
        if (registry != null)
        {
            this.holder = registry.get(this.key()).orElse(null);
        }
        else if (throwOnMissingRegistry)
        {
            throw new IllegalStateException("Registry not present for " + this + ": " + this.key().registry());
        }
    }

    public Identifier getId()
    {
        return this.key().identifier();
    }

    public ResourceKey<R> getKey()
    {
        return this.key();
    }

    @Override
    public String toString()
    {
        return String.format(Locale.ENGLISH, "DeferredHolder{%s}", this.key());
    }

    @Override
    public boolean isBound()
    {
        bind(false);
        return this.holder != null && this.holder.isBound();
    }

    @Override
    public boolean areComponentsBound()
    {
        bind(false);
        return this.holder != null && this.holder.areComponentsBound();
    }

    @Override
    public DataComponentMap components()
    {
        bind(true);
        return this.holder != null ? this.holder.components() : DataComponentMap.EMPTY;
    }

    @Override
    public boolean is(final TagKey<R> tag)
    {
        bind(false);
        return this.holder != null && this.holder.is(tag);
    }

    @Override
    public boolean is(final Holder<R> holder)
    {
        bind(false);
        return this.holder != null ? this.holder.is(holder) : holder.is(this.key());
    }

    @Override
    public Stream<TagKey<R>> tags()
    {
        bind(false);
        return this.holder != null ? this.holder.tags() : Stream.empty();
    }

    @Override
    public boolean canSerializeIn(final HolderOwner<R> owner)
    {
        bind(false);
        return this.holder != null && this.holder.canSerializeIn(owner);
    }

    /**
     * @return the real registry holder if bound, else this.
     */
    public Holder<R> getDelegate()
    {
        bind(false);
        return this.holder != null ? this.holder : this;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        return obj instanceof Holder<?> h && h.kind() == Kind.REFERENCE && h.unwrapKey().map(k -> k.equals(this.key())).orElse(false);
    }

    @Override
    public int hashCode()
    {
        return this.key().hashCode();
    }
}
