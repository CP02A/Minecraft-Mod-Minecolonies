package com.ldtteam.shim.fml;

import com.ldtteam.shim.bus.api.IEventBus;
import com.ldtteam.shim.fml.config.ConfigTracker;
import com.ldtteam.shim.fml.config.IConfigSpec;
import com.ldtteam.shim.fml.config.ModConfig;
import com.ldtteam.shim.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A loaded mod (NeoForge shim).
 */
public abstract class ModContainer
{
    protected final IModInfo modInfo;
    private final Map<Class<?>, Supplier<?>> extensionPoints = new IdentityHashMap<>();

    protected ModContainer(final IModInfo modInfo)
    {
        this.modInfo = modInfo;
    }

    public final String getModId()
    {
        return modInfo.getModId();
    }

    public final String getNamespace()
    {
        return modInfo.getNamespace();
    }

    public IModInfo getModInfo()
    {
        return modInfo;
    }

    /**
     * @return the mod bus, or null if this container has none (plain Fabric mods).
     */
    @Nullable
    public abstract IEventBus getEventBus();

    public <T extends IExtensionPoint> void registerExtensionPoint(final Class<? extends T> point, final Supplier<T> extension)
    {
        extensionPoints.put(point, extension);
    }

    public <T extends IExtensionPoint> void registerExtensionPoint(final Class<? extends T> point, final T extension)
    {
        extensionPoints.put(point, () -> extension);
    }

    @SuppressWarnings("unchecked")
    public <T extends IExtensionPoint> Optional<T> getCustomExtension(final Class<T> point)
    {
        final Supplier<?> supplier = extensionPoints.get(point);
        return supplier == null ? Optional.empty() : Optional.ofNullable((T) supplier.get());
    }

    public void registerConfig(final ModConfig.Type type, final IConfigSpec configSpec)
    {
        ConfigTracker.INSTANCE.registerConfig(type, configSpec, this);
    }

    public void registerConfig(final ModConfig.Type type, final IConfigSpec configSpec, final String fileName)
    {
        ConfigTracker.INSTANCE.registerConfig(type, configSpec, this, fileName);
    }

    @Override
    public String toString()
    {
        return "ModContainer[" + getModId() + "]";
    }
}
