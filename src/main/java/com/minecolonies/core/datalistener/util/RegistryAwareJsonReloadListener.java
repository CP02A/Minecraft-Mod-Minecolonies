package com.minecolonies.core.datalistener.util;

import com.google.gson.JsonElement;
import com.ldtteam.shim.neoforge.common.conditions.ICondition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;

/**
 * FABRIC-PORT: replacement for NeoForge's ContextAwareReloadListener features used by our json data listeners.
 * Vanilla 26.x {@link SimpleJsonResourceReloadListener} is codec based; we keep reading raw {@link JsonElement}s from the given directory
 * and get the registry lookup and condition context injected when the listener is added (see FMLEventHandler#onAddReloadListenerEvent).
 */
public abstract class RegistryAwareJsonReloadListener extends SimpleJsonResourceReloadListener<JsonElement>
{
    /**
     * The registry lookup of the current reload.
     */
    private HolderLookup.Provider registryLookup = RegistryAccess.EMPTY;

    /**
     * The condition context of the current reload (tag conditions see the tags being loaded).
     */
    private ICondition.IContext conditionContext = ICondition.IContext.EMPTY;

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
     * Inject the condition context for this reload.
     *
     * @param conditionContext the context.
     */
    public void injectConditionContext(final ICondition.IContext conditionContext)
    {
        this.conditionContext = conditionContext;
    }

    /**
     * Returns the condition context held by this listener, or {@link ICondition.IContext#EMPTY} if it is unavailable.
     *
     * @return the context.
     */
    protected final ICondition.IContext getContext()
    {
        return conditionContext;
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
