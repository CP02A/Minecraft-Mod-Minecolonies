/*
 * Derived from NeoForge 26.3 (net.neoforged.neoforge.registries.DeferredItem),
 * Copyright (c) Forge Development LLC and contributors, SPDX-License-Identifier: LGPL-2.1-only.
 */
package com.ldtteam.shim.neoforge.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * Deferred holder of an item.
 */
public class DeferredItem<T extends Item> extends DeferredHolder<Item, T> implements ItemLike
{
    public ItemStack toStack()
    {
        return toStack(1);
    }

    public ItemStack toStack(final int count)
    {
        final ItemStack stack = asItem().getDefaultInstance();
        if (stack.isEmpty())
        {
            throw new IllegalStateException("Obtained empty item stack; incorrect getDefaultInstance() call?");
        }
        stack.setCount(count);
        return stack;
    }

    public static <T extends Item> DeferredItem<T> createItem(final Identifier key)
    {
        return createItem(ResourceKey.create(Registries.ITEM, key));
    }

    public static <T extends Item> DeferredItem<T> createItem(final ResourceKey<Item> key)
    {
        return new DeferredItem<>(key);
    }

    protected DeferredItem(final ResourceKey<Item> key)
    {
        super(key);
    }

    @Override
    public Item asItem()
    {
        return get();
    }
}
