// Adapted from NeoForge 21.1 (net.neoforged.neoforge.capabilities), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.capabilities;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ICapabilityProvider<O, C extends @Nullable Object, T>
{
    @Nullable
    T getCapability(O object, C context);
}
