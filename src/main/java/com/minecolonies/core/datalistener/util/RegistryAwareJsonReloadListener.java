package com.minecolonies.core.datalistener.util;

import com.google.gson.JsonElement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;

/**
 * FABRIC-PORT: replacement for NeoForge's ContextAwareReloadListener features used by our json data listeners.
 * Vanilla 26.x {@link SimpleJsonResourceReloadListener} is codec based; we keep reading raw {@link JsonElement}s from the given directory
 * and get the registry lookup injected when the listener is added (see FMLEventHandler#onAddReloadListenerEvent).
 */
public abstract class RegistryAwareJsonReloadListener extends SimpleJsonResourceReloadListener<JsonElement>
{
    /**
     * The registry lookup of the current reload.
     */
    private HolderLookup.Provider registryLookup = RegistryAccess.EMPTY;

    /**
     * Create a new listener reading all json files in the given data directory.
     *
     * @param directory the directory.
     */
    protected RegistryAwareJsonReloadListener(final String directory)
    {
        super(ExtraCodecs.JSON, FileToIdConverter.json(directory));
    }

    /**
     * Inject the registry lookup for this reload.
     *
     * @param registryLookup the lookup.
     */
    public void injectRegistryLookup(final HolderLookup.Provider registryLookup)
    {
        this.registryLookup = registryLookup;
    }

    /**
     * Returns the registry lookup held by this listener, or {@link RegistryAccess#EMPTY} if it is unavailable.
     *
     * @return the lookup.
     */
    protected final HolderLookup.Provider getRegistryLookup()
    {
        return registryLookup;
    }
}
