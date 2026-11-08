package com.ldtteam.shim.fabric.events;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Registry context of the most recent server data reload (set right before AddReloadListenerEvent is posted).
 * Replacement for NeoForge's ContextAwareReloadListener#getRegistryLookup for listeners added via AddReloadListenerEvent.
 */
public final class ShimReloadContext
{
    private static volatile @Nullable RegistryAccess        registryAccess;
    private static volatile @Nullable HolderLookup.Provider lookup;

    private ShimReloadContext()
    {
    }

    public static void set(final @Nullable RegistryAccess access, final @Nullable HolderLookup.Provider registryLookup)
    {
        registryAccess = access;
        lookup = registryLookup;
    }

    /**
     * @return the registry lookup (with updated tags) of the current/last server data reload.
     */
    public static @Nullable HolderLookup.Provider getRegistryLookup()
    {
        return lookup;
    }

    public static @Nullable RegistryAccess getRegistryAccess()
    {
        return registryAccess;
    }
}
