package com.ldtteam.shim.fml.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.ldtteam.shim.fml.ModContainer;
import com.ldtteam.shim.fml.event.config.ModConfigEvent;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * A registered mod config (NeoForge/FML shim, mirrors FML 4 ModConfig).
 */
public final class ModConfig
{
    private final Type         type;
    private final IConfigSpec  spec;
    private final String       fileName;
    final         ModContainer container;
    final         Lock         lock = new ReentrantLock();

    @Nullable
    LoadedConfig loadedConfig;

    ModConfig(final Type type, final IConfigSpec spec, final ModContainer container, final String fileName)
    {
        this.type = type;
        this.spec = spec;
        this.fileName = fileName;
        this.container = container;
    }

    public Type getType()
    {
        return type;
    }

    public String getFileName()
    {
        return fileName;
    }

    public IConfigSpec getSpec()
    {
        return spec;
    }

    public String getModId()
    {
        return container.getModId();
    }

    public ModContainer getContainer()
    {
        return container;
    }

    /**
     * @return the full path of the loaded config file, or null if not loaded or loaded from memory (synced server configs).
     */
    @Nullable
    public Path getFullPath()
    {
        final LoadedConfig loaded = this.loadedConfig;
        return loaded == null ? null : loaded.path();
    }

    @Nullable
    public IConfigSpec.ILoadedConfig getLoadedConfig()
    {
        return loadedConfig;
    }

    void postEvent(final Function<ModConfig, ? extends ModConfigEvent> factory)
    {
        if (container.getEventBus() != null)
        {
            container.getEventBus().post(factory.apply(this));
        }
    }

    record LoadedConfig(CommentedConfig config, @Nullable Path path, ModConfig modConfig) implements IConfigSpec.ILoadedConfig
    {
        @Override
        public void save()
        {
            if (path != null)
            {
                ConfigTracker.writeConfig(path, config);
            }
            modConfig.lock.lock();
            try
            {
                modConfig.postEvent(ModConfigEvent.Reloading::new);
            }
            finally
            {
                modConfig.lock.unlock();
            }
        }
    }

    public enum Type
    {
        /**
         * Loaded on both sides, from the global config folder.
         */
        COMMON,
        /**
         * Physical client only, from the global config folder.
         */
        CLIENT,
        /**
         * Per world, from {@code <world>/serverconfig}; synced to clients.
         */
        SERVER,
        /**
         * Loaded immediately on registration, from the global config folder.
         */
        STARTUP;

        public String extension()
        {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
