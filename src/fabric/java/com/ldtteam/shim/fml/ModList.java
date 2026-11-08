package com.ldtteam.shim.fml;

import com.ldtteam.shim.fabric.FabricBackedModContainer;
import com.ldtteam.shim.fabric.FabricModInfo;
import com.ldtteam.shim.fabric.ShimModBuses;
import com.ldtteam.shim.neoforgespi.language.IModFileInfo;
import com.ldtteam.shim.neoforgespi.language.IModInfo;
import net.fabricmc.loader.api.FabricLoader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * List of loaded mods (NeoForge shim) backed by Fabric Loader. Includes provided mod ids.
 */
public final class ModList
{
    private static final ModList INSTANCE = new ModList();

    private final    Map<String, ModContainer> containers = new ConcurrentHashMap<>();
    private volatile List<IModInfo>            mods;

    private ModList()
    {
    }

    public static ModList get()
    {
        return INSTANCE;
    }

    public boolean isLoaded(final String modId)
    {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public Optional<? extends ModContainer> getModContainerById(final String modId)
    {
        final ModContainer shim = ShimModBuses.getContainer(modId);
        if (shim != null)
        {
            return Optional.of(shim);
        }
        return FabricLoader.getInstance().getModContainer(modId)
            .map(c -> containers.computeIfAbsent(modId, id -> new FabricBackedModContainer(new FabricModInfo(id, c))));
    }

    public IModFileInfo getModFileById(final String modId)
    {
        return FabricLoader.getInstance().getModContainer(modId).map(c -> (IModFileInfo) new FabricModInfo(modId, c)).orElse(null);
    }

    public List<IModInfo> getMods()
    {
        if (mods == null)
        {
            final List<IModInfo> list = new ArrayList<>();
            final Set<String> ids = new HashSet<>();
            for (final net.fabricmc.loader.api.ModContainer c : FabricLoader.getInstance().getAllMods())
            {
                if (ids.add(c.getMetadata().getId()))
                {
                    list.add(new FabricModInfo(c.getMetadata().getId(), c));
                }
                for (final String provided : c.getMetadata().getProvides())
                {
                    if (ids.add(provided))
                    {
                        list.add(new FabricModInfo(provided, c));
                    }
                }
            }
            mods = Collections.unmodifiableList(list);
        }
        return mods;
    }

    public int size()
    {
        return getMods().size();
    }

    public void forEachModContainer(final BiConsumer<String, ModContainer> consumer)
    {
        for (final IModInfo info : getMods())
        {
            getModContainerById(info.getModId()).ifPresent(c -> consumer.accept(info.getModId(), c));
        }
    }
}
