// Adapted from NeoForge 21.1.80 (event/LootTableLoadEvent.java), licensed under LGPL-2.1. Modified for the MineColonies Fabric shim.
/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.ldtteam.shim.neoforge.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.bus.api.ICancellableEvent;
import com.ldtteam.shim.mixin.LootTableAccessor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fired when a loot table is loaded (from Fabric's LootTableEvents.REPLACE). Listeners may replace the table, add pools
 * via {@link #addPool(LootPool)} (vanilla 26.x has no LootTable#addPool), or cancel to make the table empty.
 */
public class LootTableLoadEvent extends Event implements ICancellableEvent
{
    private final @Nullable HolderLookup.Provider registries;
    private final Identifier                      name;
    private LootTable                             table;

    public LootTableLoadEvent(final @Nullable HolderLookup.Provider registries, final Identifier name, final LootTable table)
    {
        this.registries = registries;
        this.name = name;
        this.table = table;
    }

    public LootTableLoadEvent(final Identifier name, final LootTable table)
    {
        this(null, name, table);
    }

    public @Nullable HolderLookup.Provider getRegistries()
    {
        return registries;
    }

    public Identifier getName()
    {
        return this.name;
    }

    public ResourceKey<LootTable> getKey()
    {
        return ResourceKey.create(Registries.LOOT_TABLE, name);
    }

    public LootTable getTable()
    {
        return this.table;
    }

    public void setTable(final LootTable table)
    {
        Objects.requireNonNull(table);
        this.table = table;
    }

    /**
     * Adds a pool to the current table (replacement for NeoForge's LootTable#addPool extension).
     */
    public void addPool(final LootPool pool)
    {
        addPool(this.table, pool);
    }

    /**
     * Adds a pool to the given table in place.
     */
    public static void addPool(final LootTable table, final LootPool pool)
    {
        final LootTableAccessor accessor = (LootTableAccessor) table;
        final List<LootPool> pools = new ArrayList<>(accessor.shim$getPools());
        pools.add(pool);
        accessor.shim$setPools(List.copyOf(pools));
    }
}
