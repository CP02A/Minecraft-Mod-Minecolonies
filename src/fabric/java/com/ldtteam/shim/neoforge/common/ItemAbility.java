// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.ItemAbility), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An action/ability an item can perform. On Fabric there is no {@code ItemStack#canPerformAction} extension; use
 * {@link #canPerform(ItemStack)} or {@link ItemAbilities#canPerformAction(ItemStack, ItemAbility)}.
 */
public final class ItemAbility
{
    private static final Map<String, ItemAbility> actions = new ConcurrentHashMap<>();

    public static Codec<ItemAbility> CODEC = Codec.STRING.xmap(ItemAbility::get, ItemAbility::name);

    public static Collection<ItemAbility> getActions()
    {
        return Collections.unmodifiableCollection(actions.values());
    }

    public static ItemAbility get(final String name)
    {
        return actions.computeIfAbsent(name, ItemAbility::new);
    }

    private final String name;

    private ItemAbility(final String name)
    {
        this.name = name;
    }

    public String name()
    {
        return name;
    }

    /**
     * Replacement for NeoForge's {@code stack.canPerformAction(this)}.
     */
    public boolean canPerform(final ItemStack stack)
    {
        return ItemAbilities.canPerformAction(stack, this);
    }

    @Override
    public String toString()
    {
        return "ItemAbility[" + name + "]";
    }
}
