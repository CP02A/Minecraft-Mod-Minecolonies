// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.extensions.IHolderExtension), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common.extensions;

import net.minecraft.core.Holder;

/**
 * On Fabric holders have no delegates; {@link #getDelegate(Holder)} is the identity (usable as {@code IHolderExtension::getDelegate}).
 */
public interface IHolderExtension<T>
{
    static <T> Holder<T> getDelegate(final Holder<T> holder)
    {
        return holder;
    }
}
