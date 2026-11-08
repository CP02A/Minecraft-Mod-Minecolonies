package com.ldtteam.shim.neoforge.registries;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

/**
 * Builder of custom registries (NeoForge shim) backed by FabricRegistryBuilder. create() builds and registers the
 * registry into the root registry immediately (Fabric keeps the root registry unfrozen during mod init).
 */
public class RegistryBuilder<T>
{
    private final ResourceKey<? extends Registry<T>> registryKey;
    @Nullable
    private Identifier defaultKey;
    private boolean    sync = false;

    public RegistryBuilder(final ResourceKey<? extends Registry<T>> registryKey)
    {
        this.registryKey = registryKey;
    }

    public RegistryBuilder<T> defaultKey(final Identifier key)
    {
        this.defaultKey = key;
        return this;
    }

    public RegistryBuilder<T> defaultKey(final ResourceKey<T> key)
    {
        this.defaultKey = key.identifier();
        return this;
    }

    public RegistryBuilder<T> sync(final boolean sync)
    {
        this.sync = sync;
        return this;
    }

    public RegistryBuilder<T> maxId(final int maxId)
    {
        return this;
    }

    public RegistryBuilder<T> disableRegistrationCheck()
    {
        return this;
    }

    @SuppressWarnings("unchecked")
    public Registry<T> create()
    {
        final ResourceKey<Registry<T>> key = (ResourceKey<Registry<T>>) registryKey;
        final FabricRegistryBuilder<T, ?> builder = defaultKey != null ? FabricRegistryBuilder.createDefaulted(key, defaultKey) : FabricRegistryBuilder.create(key);
        if (sync)
        {
            builder.attribute(RegistryAttribute.SYNCED);
        }
        return builder.buildAndRegister();
    }
}
