// Adapted from NeoForge 21.1 (net.neoforged.neoforge.capabilities), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.capabilities;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public abstract class BaseCapability<T, C extends @Nullable Object>
{
    private final Identifier name;
    private final Class<T> typeClass;
    private final Class<C> contextClass;

    protected BaseCapability(final Identifier name, final Class<T> typeClass, final Class<C> contextClass)
    {
        this.name = name;
        this.typeClass = typeClass;
        this.contextClass = contextClass;
    }

    public final Identifier name()
    {
        return name;
    }

    public final Class<T> typeClass()
    {
        return typeClass;
    }

    public final Class<C> contextClass()
    {
        return contextClass;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "[" + name + "]";
    }
}
