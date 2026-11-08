// Adapted from NeoForge 21.1 (net.neoforged.neoforge.capabilities), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.capabilities;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CapabilityRegistry<C>
{
    private final ConcurrentMap<Identifier, StoredCap<C>> caps = new ConcurrentHashMap<>();
    private final CapabilityConstructor<C> constructor;

    public CapabilityRegistry(final CapabilityConstructor<C> constructor)
    {
        this.constructor = Objects.requireNonNull(constructor);
    }

    public C create(final Identifier name, final Class<?> typeClass, final Class<?> contextClass)
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(typeClass);
        Objects.requireNonNull(contextClass);
        final StoredCap<C> ret = caps.computeIfAbsent(name, n -> new StoredCap<>(constructor.create(n, typeClass, contextClass), typeClass, contextClass));
        if (ret.typeClass != typeClass)
        {
            throw new IllegalStateException("Attempted to register capability " + name + " with existing type class " + ret.typeClass + " != " + typeClass);
        }
        else if (ret.contextClass != contextClass)
        {
            throw new IllegalStateException("Attempted to register capability " + name + " with existing context class " + ret.contextClass + " != " + contextClass);
        }
        return ret.cap;
    }

    public List<C> getAll()
    {
        return caps.values().stream().map(StoredCap::cap).toList();
    }

    @FunctionalInterface
    public interface CapabilityConstructor<C>
    {
        C create(Identifier name, Class<?> typeClass, Class<?> contextClass);
    }

    private record StoredCap<C>(C cap, Class<?> typeClass, Class<?> contextClass) {}
}
