// Adapted from NeoForge 21.1 (net.neoforged.neoforge.capabilities), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.capabilities;

import com.ldtteam.shim.fabric.capabilities.ShimCapabilityHooks;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entity capability. On Fabric there is no {@code Entity#getCapability} extension method; call {@link #getCapability(Entity, Object)} directly.
 */
public final class EntityCapability<T, C extends @Nullable Object> extends BaseCapability<T, C>
{
    @SuppressWarnings("unchecked")
    public static <T, C extends @Nullable Object> EntityCapability<T, C> create(final Identifier name, final Class<T> typeClass, final Class<C> contextClass)
    {
        return (EntityCapability<T, C>) registry.create(name, typeClass, contextClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> EntityCapability<T, @Nullable Void> createVoid(final Identifier name, final Class<T> typeClass)
    {
        return create(name, typeClass, (Class<Void>) (Class<?>) void.class);
    }

    public static <T> EntityCapability<T, @Nullable Direction> createSided(final Identifier name, final Class<T> typeClass)
    {
        return create(name, typeClass, Direction.class);
    }

    public static synchronized List<EntityCapability<?, ?>> getAll()
    {
        return registry.getAll();
    }

    private static final CapabilityRegistry<EntityCapability<?, ?>> registry = new CapabilityRegistry<EntityCapability<?, ?>>(EntityCapability::new);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private EntityCapability(final Identifier name, final Class<?> typeClass, final Class<?> contextClass)
    {
        super(name, (Class) typeClass, (Class) contextClass);
    }

    private final Map<EntityType<?>, List<ICapabilityProvider<Entity, C, T>>> providers = new IdentityHashMap<>();
    private final List<ICapabilityProvider<Entity, C, T>> fallbacks = new CopyOnWriteArrayList<>();

    @ApiStatus.Internal
    public void registerFallback(final ICapabilityProvider<Entity, C, T> provider)
    {
        fallbacks.add(provider);
    }

    synchronized void addProvider(final EntityType<?> type, final ICapabilityProvider<Entity, C, T> provider)
    {
        providers.computeIfAbsent(type, b -> new ArrayList<>()).add(provider);
    }

    boolean hasProvider(final EntityType<?> type)
    {
        return providers.containsKey(type);
    }

    @Nullable
    public T getCapability(final Entity entity, final C context)
    {
        ShimCapabilityHooks.ensureInitialized();
        for (final ICapabilityProvider<Entity, C, T> provider : providers.getOrDefault(entity.getType(), List.of()))
        {
            final T ret = provider.getCapability(entity, context);
            if (ret != null)
            {
                return ret;
            }
        }
        for (final ICapabilityProvider<Entity, C, T> provider : fallbacks)
        {
            final T ret = provider.getCapability(entity, context);
            if (ret != null)
            {
                return ret;
            }
        }
        return null;
    }

    /**
     * Replacement for NeoForge's {@code entity.getCapability(cap)} for void-context capabilities.
     */
    @Nullable
    public T getCapability(final Entity entity)
    {
        return getCapability(entity, null);
    }
}
