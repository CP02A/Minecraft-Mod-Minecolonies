package com.minecolonies.core.colony;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Replacements for the 1.21.1 NBT helpers that were removed in 26.x vanilla.
 */
public final class NbtCompat
{
    private NbtCompat()
    {
    }

    /**
     * Replacement for the removed {@code ItemStack#saveOptional(Provider)}: empty stacks serialize to an empty compound.
     */
    public static Tag saveOptional(final ItemStack stack, final HolderLookup.Provider provider)
    {
        if (stack.isEmpty())
        {
            return new CompoundTag();
        }
        return ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
    }

    /**
     * Replacement for the removed {@code ItemStack#save(Provider)}.
     */
    public static Tag save(final ItemStack stack, final HolderLookup.Provider provider)
    {
        return saveOptional(stack, provider);
    }

    /**
     * Replacement for the removed {@code ItemStack#parseOptional(Provider, CompoundTag)}: returns {@link ItemStack#EMPTY} on empty/invalid input.
     */
    public static ItemStack parseOptional(final HolderLookup.Provider provider, final Tag tag)
    {
        if (tag == null || (tag instanceof CompoundTag compound && compound.isEmpty()))
        {
            return ItemStack.EMPTY;
        }
        return ItemStack.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
    }
}
