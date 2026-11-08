// Adapted from NeoForge 21.1 (net.neoforged.neoforge.capabilities), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.capabilities;

import com.ldtteam.shim.fabric.capabilities.ShimCapabilityHooks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Item capability. On Fabric there is no {@code ItemStack#getCapability} extension method; call {@link #getCapability(ItemStack, Object)} directly.
 */
public final class ItemCapability<T, C extends @Nullable Object> extends BaseCapability<T, C>
{
    @SuppressWarnings("unchecked")
    public static <T, C extends @Nullable Object> ItemCapability<T, C> create(final Identifier name, final Class<T> typeClass, final Class<C> contextClass)
    {
        return (ItemCapability<T, C>) registry.create(name, typeClass, contextClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> ItemCapability<T, @Nullable Void> createVoid(final Identifier name, final Class<T> typeClass)
    {
        return create(name, typeClass, (Class<Void>) (Class<?>) void.class);
    }

    public static synchronized List<ItemCapability<?, ?>> getAll()
    {
        return registry.getAll();
    }

    private static final CapabilityRegistry<ItemCapability<?, ?>> registry = new CapabilityRegistry<ItemCapability<?, ?>>(ItemCapability::new);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ItemCapability(final Identifier name, final Class<?> typeClass, final Class<?> contextClass)
    {
        super(name, (Class) typeClass, (Class) contextClass);
    }

    private final Map<Item, List<ICapabilityProvider<ItemStack, C, T>>> providers = new IdentityHashMap<>();
    private final List<ICapabilityProvider<ItemStack, C, T>> fallbacks = new CopyOnWriteArrayList<>();

    @ApiStatus.Internal
    public void registerFallback(final ICapabilityProvider<ItemStack, C, T> provider)
    {
        fallbacks.add(provider);
    }

    synchronized void addProvider(final Item item, final ICapabilityProvider<ItemStack, C, T> provider)
    {
        providers.computeIfAbsent(item, b -> new ArrayList<>()).add(provider);
    }

    boolean hasProvider(final Item item)
    {
        return providers.containsKey(item);
    }

    @Nullable
    public T getCapability(final ItemStack stack, final C context)
    {
        if (stack.isEmpty())
        {
            return null;
        }
        ShimCapabilityHooks.ensureInitialized();
        for (final ICapabilityProvider<ItemStack, C, T> provider : providers.getOrDefault(stack.getItem(), List.of()))
        {
            final T ret = provider.getCapability(stack, context);
            if (ret != null)
            {
                return ret;
            }
        }
        for (final ICapabilityProvider<ItemStack, C, T> provider : fallbacks)
        {
            final T ret = provider.getCapability(stack, context);
            if (ret != null)
            {
                return ret;
            }
        }
        return null;
    }

    /**
     * Replacement for NeoForge's {@code stack.getCapability(cap)} for void-context capabilities.
     */
    @Nullable
    public T getCapability(final ItemStack stack)
    {
        return getCapability(stack, null);
    }
}
