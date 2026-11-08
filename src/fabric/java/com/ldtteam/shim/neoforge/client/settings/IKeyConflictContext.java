// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.settings.IKeyConflictContext), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.settings;

/**
 * Key conflict context. Vanilla/Fabric key mappings have no conflict contexts; these are informational only (see {@link #isActive()}).
 */
public interface IKeyConflictContext
{
    boolean isActive();

    boolean conflicts(IKeyConflictContext other);
}
