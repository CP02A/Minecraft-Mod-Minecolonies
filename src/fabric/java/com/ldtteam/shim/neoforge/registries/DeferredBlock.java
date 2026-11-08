/*
 * Derived from NeoForge 26.3 (net.neoforged.neoforge.registries.DeferredBlock),
 * Copyright (c) Forge Development LLC and contributors, SPDX-License-Identifier: LGPL-2.1-only.
 */
package com.ldtteam.shim.neoforge.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * Deferred holder of a block.
 */
public class DeferredBlock<T extends Block> extends DeferredHolder<Block, T> implements ItemLike
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
            throw new IllegalStateException("Block does not have a corresponding item: " + this.key());
        }
        stack.setCount(count);
        return stack;
    }

    public static <T extends Block> DeferredBlock<T> createBlock(final Identifier key)
    {
        return createBlock(ResourceKey.create(Registries.BLOCK, key));
    }

    public static <T extends Block> DeferredBlock<T> createBlock(final ResourceKey<Block> key)
    {
        return new DeferredBlock<>(key);
    }

    protected DeferredBlock(final ResourceKey<Block> key)
    {
        super(key);
    }

    @Override
    public Item asItem()
    {
        return get().asItem();
    }
}
