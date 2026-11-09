package com.ldtteam.shim.fml.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.ldtteam.shim.fml.ModContainer;
import com.ldtteam.shim.fml.event.config.ModConfigEvent;
import com.ldtteam.shim.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Tracks and loads mod configs (NeoForge/FML shim, behaviour modelled after FML 4 ConfigTracker).
 * <ul>
 * <li>COMMON / CLIENT / STARTUP: {@code config/<file>}</li>
 * <li>SERVER: {@code <world>/serverconfig/<file>} while a server runs (defaults copied from {@code defaultconfigs/}),
 * in-memory defaults otherwise, synced copies on remote clients.</li>
 * </ul>
 */
public final class ConfigTracker
{
    private static final Logger LOGGER = LoggerFactory.getLogger("ShimConfigTracker");

    public static final ConfigTracker INSTANCE = new ConfigTracker();

    private final Map<String, ModConfig>                fileMap    = new ConcurrentHashMap<>();
    private final EnumMap<ModConfig.Type, Set<ModConfig>> configSets = new EnumMap<>(ModConfig.Type.class);
    private final Map<String, List<ModConfig>>          configsByMod = new ConcurrentHashMap<>();

    /**
     * Called (on the config watcher thread) after a file based SERVER config was reloaded; used to resync clients.
     */
    @Nullable
    private volatile Consumer<ModConfig> serverConfigReloadListener;

    private ConfigTracker()
    {
        for (final ModConfig.Type type : ModConfig.Type.values())
        {
            configSets.put(type, Collections.synchronizedSet(new LinkedHashSet<>()));
        }
    }

    public ModConfig registerConfig(final ModConfig.Type type, final IConfigSpec spec, final ModContainer container)
    {
        return registerConfig(type, spec, container, String.format(Locale.ROOT, "%s-%s.toml", container.getModId(), type.extension()));
    }

    public ModConfig registerConfig(final ModConfig.Type type, final IConfigSpec spec, final ModContainer container, final String fileName)
    {
        final ModConfig modConfig = new ModConfig(type, spec, container, fileName);
        if (fileMap.putIfAbsent(fileName, modConfig) != null)
        {
            throw new IllegalStateException("Config file " + fileName + " is already registered");
        }
        spec.validateSpec(modConfig);
        configSets.get(type).add(modConfig);
        configsByMod.computeIfAbsent(container.getModId(), k -> Collections.synchronizedList(new ArrayList<>())).add(modConfig);
        LOGGER.debug("Registered config {} of type {} for {}", fileName, type, container.getModId());

        if (type == ModConfig.Type.STARTUP)
        {
            openConfig(modConfig, FMLPaths.CONFIGDIR.get(), null);
        }
        return modConfig;
    }

    public Map<String, ModConfig> fileMap()
    {
        return Collections.unmodifiableMap(fileMap);
    }

    public Map<ModConfig.Type, Set<ModConfig>> configSets()
    {
        return Collections.unmodifiableMap(configSets);
    }

    public List<ModConfig> configsForMod(final String modId)
    {
        return List.copyOf(configsByMod.getOrDefault(modId, List.of()));
    }

    public void setServerConfigReloadListener(@Nullable final Consumer<ModConfig> listener)
    {
        this.serverConfigReloadListener = listener;
    }

    /**
     * Tells the server config listener (client resync) that a SERVER config was changed and saved from code.
     */
    void notifyServerConfigChanged(final ModConfig config)
    {
        final Consumer<ModConfig> listener = serverConfigReloadListener;
        if (listener != null && config.getType() == ModConfig.Type.SERVER)
        {
            listener.accept(config);
        }
    }

    /**
     * Load all configs of the type from the directory (creating/correcting files).
     */
    public void loadConfigs(final ModConfig.Type type, final Path configBasePath)
    {
        loadConfigs(type, configBasePath, null);
    }

    /**
     * Load all configs of the type from the directory, copying missing files from the defaults directory if present.
     */
    public void loadConfigs(final ModConfig.Type type, final Path configBasePath, @Nullable final Path configOverrideBasePath)
    {
        LOGGER.debug("Loading configs type {}", type);
        for (final ModConfig config : List.copyOf(configSets.get(type)))
        {
            openConfig(config, configBasePath, configOverrideBasePath);
        }
    }

    public void unloadConfigs(final ModConfig.Type type)
    {
        LOGGER.debug("Unloading configs type {}", type);
        for (final ModConfig config : List.copyOf(configSets.get(type)))
        {
            closeConfig(config);
        }
    }

    /**
     * Load in-memory default values for all SERVER configs (so they can be read while no server runs).
     */
    public void loadDefaultServerConfigs()
    {
        for (final ModConfig config : List.copyOf(configSets.get(ModConfig.Type.SERVER)))
        {
            if (config.getSpec().isEmpty())
            {
                continue;
            }
            config.lock.lock();
            try
            {
                final CommentedConfig commented = CommentedConfig.inMemoryConcurrent();
                config.getSpec().correct(commented);
                setLoadedConfig(config, null, () -> commented, ModConfigEvent.Loading::new);
            }
            finally
            {
                config.lock.unlock();
            }
        }
    }

    /**
     * Accept a server config file synced from a remote server.
     */
    public void acceptSyncedConfig(final String fileName, final byte[] bytes)
    {
        final ModConfig config = fileMap.get(fileName);
        if (config == null || config.getType() != ModConfig.Type.SERVER)
        {
            LOGGER.warn("Received synced config {} which is not a registered server config", fileName);
            return;
        }
        config.lock.lock();
        try
        {
            final CommentedConfig parsed = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(bytes));
            final CommentedConfig commented = CommentedConfig.inMemoryConcurrent();
            commented.putAll(parsed);
            commented.putAllComments(parsed);
            if (!config.getSpec().isCorrect(commented))
            {
                config.getSpec().correct(commented);
            }
            setLoadedConfig(config, null, () -> commented, config.loadedConfig == null ? ModConfigEvent.Loading::new : ModConfigEvent.Reloading::new);
        }
        catch (final ParsingException e)
        {
            LOGGER.error("Failed to parse synced config {}", fileName, e);
        }
        finally
        {
            config.lock.unlock();
        }
    }

    private void openConfig(final ModConfig config, final Path configBasePath, @Nullable final Path configOverrideBasePath)
    {
        if (config.getSpec().isEmpty())
        {
            return;
        }
        LOGGER.trace("Loading config file type {} at {} for {}", config.getType(), config.getFileName(), config.getModId());
        final Path configPath = configBasePath.resolve(config.getFileName());
        config.lock.lock();
        try
        {
            if (Files.notExists(configPath) && configOverrideBasePath != null)
            {
                final Path override = configOverrideBasePath.resolve(config.getFileName());
                if (Files.exists(override))
                {
                    Files.createDirectories(configPath.getParent());
                    Files.copy(override, configPath);
                    LOGGER.info("Copied default config {} from {}", config.getFileName(), override);
                }
            }
            final CommentedConfig commented = readConfig(config, configPath);
            setLoadedConfig(config, configPath, () -> commented, ModConfigEvent.Loading::new);
        }
        catch (final IOException e)
        {
            throw new RuntimeException("Failed to load config " + configPath, e);
        }
        finally
        {
            config.lock.unlock();
        }

        FileWatcher.defaultInstance().setWatch(configPath, () -> reloadFromDisk(config, configPath));
    }

    private static CommentedConfig readConfig(final ModConfig config, final Path configPath) throws IOException
    {
        Files.createDirectories(configPath.getParent());
        final CommentedConfig commented = CommentedConfig.inMemoryConcurrent();
        try
        {
            TomlFormat.instance().createParser().parse(configPath, commented, ParsingMode.REPLACE, FileNotFoundAction.CREATE_EMPTY);
        }
        catch (final ParsingException e)
        {
            final Path backup = configPath.resolveSibling(configPath.getFileName() + ".bak");
            LOGGER.warn("Failed to parse config file {}, backing it up to {} and recreating it with default values", configPath, backup, e);
            Files.copy(configPath, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            commented.clear();
        }
        if (!config.getSpec().isCorrect(commented))
        {
            LOGGER.info("Config file {} is not correct, correcting it", configPath);
            config.getSpec().correct(commented);
            writeConfig(configPath, commented);
        }
        return commented;
    }

    private void reloadFromDisk(final ModConfig config, final Path configPath)
    {
        config.lock.lock();
        try
        {
            final ModConfig.LoadedConfig current = config.loadedConfig;
            if (current == null || !configPath.equals(current.path()))
            {
                return;
            }
            final CommentedConfig commented = readConfig(config, configPath);
            if (sameValues(commented, current.config()))
            {
                return;
            }
            LOGGER.info("Config file {} changed, reloading", configPath);
            setLoadedConfig(config, configPath, () -> commented, ModConfigEvent.Reloading::new);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to reload config {}", configPath, e);
            return;
        }
        finally
        {
            config.lock.unlock();
        }
        final Consumer<ModConfig> listener = serverConfigReloadListener;
        if (listener != null && config.getType() == ModConfig.Type.SERVER)
        {
            listener.accept(config);
        }
    }

    private static boolean sameValues(final UnmodifiableConfig a, final UnmodifiableConfig b)
    {
        final Map<String, Object> ma = a.valueMap();
        final Map<String, Object> mb = b.valueMap();
        if (!ma.keySet().equals(mb.keySet()))
        {
            return false;
        }
        for (final Map.Entry<String, Object> entry : ma.entrySet())
        {
            final Object va = entry.getValue();
            final Object vb = mb.get(entry.getKey());
            if (va instanceof UnmodifiableConfig ca && vb instanceof UnmodifiableConfig cb)
            {
                if (!sameValues(ca, cb))
                {
                    return false;
                }
            }
            else if (va instanceof Number na && vb instanceof Number nb)
            {
                if (Double.compare(na.doubleValue(), nb.doubleValue()) != 0)
                {
                    return false;
                }
            }
            else if (!Objects.equals(va, vb))
            {
                return false;
            }
        }
        return true;
    }

    private interface ConfigSupplier
    {
        CommentedConfig get();
    }

    private static void setLoadedConfig(final ModConfig config, @Nullable final Path path, final ConfigSupplier supplier,
        final java.util.function.Function<ModConfig, ? extends ModConfigEvent> event)
    {
        final ModConfig.LoadedConfig loaded = new ModConfig.LoadedConfig(supplier.get(), path, config);
        config.loadedConfig = loaded;
        config.getSpec().acceptConfig(loaded);
        config.postEvent(event);
    }

    private void closeConfig(final ModConfig config)
    {
        final ModConfig.LoadedConfig loaded = config.loadedConfig;
        if (loaded == null)
        {
            return;
        }
        if (loaded.path() != null)
        {
            FileWatcher.defaultInstance().removeWatch(loaded.path());
        }
        config.lock.lock();
        try
        {
            config.postEvent(ModConfigEvent.Unloading::new);
            config.loadedConfig = null;
            config.getSpec().acceptConfig(null);
        }
        finally
        {
            config.lock.unlock();
        }
    }

    /**
     * Read the raw bytes of a loaded file based config (for syncing), or null.
     */
    @Nullable
    public static byte[] readLoadedBytes(final ModConfig config)
    {
        final ModConfig.LoadedConfig loaded = config.loadedConfig;
        if (loaded == null)
        {
            return null;
        }
        return TomlFormat.instance().createWriter().writeToString(loaded.config()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    static void writeConfig(final Path path, final UnmodifiableConfig config)
    {
        try
        {
            Files.createDirectories(path.getParent());
            TomlFormat.instance().createWriter().write(config, path, WritingMode.REPLACE_ATOMIC);
        }
        catch (final Exception atomicFailure)
        {
            // Windows refuses the atomic rename while another handle (indexer, antivirus, the config watcher) has the file open.
            try
            {
                TomlFormat.instance().createWriter().write(config, path, WritingMode.REPLACE);
            }
            catch (final Exception e)
            {
                e.addSuppressed(atomicFailure);
                LOGGER.error("Failed to write config {}", path, e);
            }
        }
    }
}
