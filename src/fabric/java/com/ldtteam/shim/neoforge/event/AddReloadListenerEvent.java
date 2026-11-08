// Adapted from NeoForge 21.1.80 (event/AddReloadListenerEvent.java), licensed under LGPL-2.1. Modified for the MineColonies Fabric shim.
/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.ldtteam.shim.neoforge.event;

import com.google.common.collect.ImmutableList;
import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.neoforge.common.conditions.ICondition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Fired on each server data reload (from ReloadableServerResources#listeners, see the shim mixin) and lets mods add
 * their own reload listeners for server-side resources. Fired on the NeoForge game bus.
 */
public class AddReloadListenerEvent extends Event
{
    private final List<PreparableReloadListener> listeners = new ArrayList<>();
    private final ReloadableServerResources      serverResources;
    private final RegistryAccess                 registryAccess;
    private final HolderLookup.Provider          lookup;

    public AddReloadListenerEvent(final ReloadableServerResources serverResources, final RegistryAccess registryAccess, final HolderLookup.Provider lookup)
    {
        this.serverResources = serverResources;
        this.registryAccess = registryAccess;
        this.lookup = lookup;
    }

    /**
     * @param listener the listener to add to the ResourceManager on reload
     */
    public void addListener(final PreparableReloadListener listener)
    {
        listeners.add(listener);
    }

    public List<PreparableReloadListener> getListeners()
    {
        return ImmutableList.copyOf(listeners);
    }

    public ReloadableServerResources getServerResources()
    {
        return serverResources;
    }

    /**
     * FABRIC-PORT TODO: no staged-tag condition context on Fabric; tag conditions evaluate against the empty context.
     */
    public ICondition.IContext getConditionContext()
    {
        return ICondition.IContext.EMPTY;
    }

    /**
     * The registry lookup (with the updated tags) of this reload. Also available to listeners during the reload via
     * {@code com.ldtteam.shim.fabric.events.ShimReloadContext#getRegistryLookup()} (replacement for NeoForge's ContextAwareReloadListener).
     */
    public HolderLookup.Provider getRegistryLookup()
    {
        return lookup;
    }

    public RegistryAccess getRegistryAccess()
    {
        return registryAccess;
    }
}
