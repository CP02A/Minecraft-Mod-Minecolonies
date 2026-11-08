/*
 * Modelled after NeoForge 21.1.80 (net.neoforged.neoforge.common.conditions.ConditionContext), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim: 26.x has no TagManager, tags are read from a registry lookup.
 */
package com.ldtteam.shim.neoforge.common.conditions;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Condition context backed by the (tag-bound) registry lookup.
 */
public class ConditionContext implements ICondition.IContext
{
    private final HolderLookup.Provider registries;

    public ConditionContext(final HolderLookup.Provider registries)
    {
        this.registries = registries;
    }

    @Override
    public <T> Map<Identifier, Collection<Holder<T>>> getAllTags(final ResourceKey<? extends Registry<T>> registry)
    {
        return registries.lookup(registry).map(lookup -> {
            final Map<Identifier, Collection<Holder<T>>> map = new HashMap<>();
            lookup.listTags().forEach((HolderSet.Named<T> named) -> map.put(named.key().location(), named.stream().toList()));
            return Collections.unmodifiableMap(map);
        }).orElse(Collections.emptyMap());
    }
}
