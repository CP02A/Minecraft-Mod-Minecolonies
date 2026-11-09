package com.minecolonies.core.datalistener;

import com.google.gson.*;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import com.minecolonies.core.datalistener.util.RegistryAwareJsonReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Loads and listens to get custom nbt matching rules.
 */
public class ItemNbtListener extends RegistryAwareJsonReloadListener
{
    /**
     * Gson instance
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * Create a new listener.
     */
    public ItemNbtListener()
    {
        super("compatibility");
    }

    @Override
    protected void apply(final Map<Identifier, JsonElement> jsonElementMap, final @NotNull ResourceManager resourceManager, final @NotNull ProfilerFiller profiler)
    {
        ItemStackUtils.CHECKED_NBT_KEYS.clear();
        for (final Map.Entry<Identifier, JsonElement> entry : jsonElementMap.entrySet())
        {
            tryParse(this.getRegistryLookup(), entry);
        }
    }

    /**
     * Tries to parse the entry
     *
     * @param entry
     */
    private void tryParse(@NotNull final HolderLookup.Provider provider, final Map.Entry<Identifier, JsonElement> entry)
    {
        for (final JsonElement element : entry.getValue().getAsJsonArray())
        {
            try
            {
                final JsonObject jsonObj = element.getAsJsonObject();
                final Identifier itemLoc = Identifier.parse(jsonObj.get("item").getAsString());
                if (jsonObj.has("checkednbtkeys"))
                {
                    final HashSet<DataComponentType<?>> set = new HashSet<>();
                    final JsonArray jsonArray = jsonObj.getAsJsonArray("checkednbtkeys");
                    for (final JsonElement subElement : jsonArray)
                    {
                        final DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(subElement.getAsString()));
                        if (type == null)
                        {
                            // Unknown/removed component id (e.g. from an older data pack): a null key would break syncing to clients.
                            Log.getLogger().warn("Unknown data component {} in item nbt matching for {}", subElement.getAsString(), itemLoc);
                            continue;
                        }
                        set.add(type);
                    }

                    ItemStackUtils.CHECKED_NBT_KEYS.put(BuiltInRegistries.ITEM.getValue(itemLoc), set);
                }
                else
                {
                    ItemStackUtils.CHECKED_NBT_KEYS.put(BuiltInRegistries.ITEM.getValue(itemLoc), new HashSet<>());
                }
            }
            catch (Exception e)
            {
                Log.getLogger().warn("Could not nbt comparator for:" + entry.getKey(), e);
            }
        }
        Log.getLogger().warn("Read " + ItemStackUtils.CHECKED_NBT_KEYS.size() + " items with their nbt keys for compatibility.");
    }
}
