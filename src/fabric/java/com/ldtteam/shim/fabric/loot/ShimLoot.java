package com.ldtteam.shim.fabric.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ldtteam.shim.neoforge.common.conditions.ConditionalOps;
import com.ldtteam.shim.neoforge.common.conditions.ICondition;
import com.ldtteam.shim.neoforge.common.loot.IGlobalLootModifier;
import com.ldtteam.shim.neoforge.common.loot.LootTableIdCondition;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.*;

/**
 * NeoForge global loot modifier compatibility on Fabric: loads {@code data/<ns>/loot_modifiers/*.json} listed in the layered
 * {@code data/minecolonies_shim/loot_modifiers/global_loot_modifiers.json} files (NeoForge format, incl. {@code neoforge:conditions}),
 * and applies them on Fabric's {@link LootTableEvents#MODIFY_DROPS}. Registers the {@code minecolonies_shim:loot_table_id} loot condition.
 * <p>
 * The index and the condition use the shim namespace, not {@code neoforge}: other mods ship NeoForge shims too, and a second
 * {@code neoforge:loot_table_id} registration crashes, while a shared index would make each shim load the other's modifiers.
 */
public final class ShimLoot
{
    private static final Logger            LOGGER       = LoggerFactory.getLogger("ShimLoot");
    private static final String            FOLDER       = "loot_modifiers";
    private static final Identifier        GLOBAL_INDEX = Identifier.fromNamespaceAndPath("minecolonies_shim", FOLDER + "/global_loot_modifiers.json");
    private static final ThreadLocal<Identifier> QUERIED_TABLE = new ThreadLocal<>();

    private static volatile List<IGlobalLootModifier> modifiers = List.of();
    private static boolean initialized;

    private ShimLoot()
    {
    }

    /**
     * Call from the main entrypoint while registries are unfrozen.
     */
    public static synchronized void init()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;

        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, Identifier.fromNamespaceAndPath("minecolonies_shim", "loot_table_id"), LootTableIdCondition.CODEC);

        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Identifier.fromNamespaceAndPath("minecolonies", "shim_loot_modifiers"), new Listener());

        LootTableEvents.MODIFY_DROPS.register(ShimLoot::modifyDrops);
    }

    /**
     * @return the id of the loot table whose drops are currently being modified, or {@link LootTableIdCondition#UNKNOWN_LOOT_TABLE}.
     */
    public static Identifier getQueriedLootTableId()
    {
        final Identifier id = QUERIED_TABLE.get();
        return id == null ? LootTableIdCondition.UNKNOWN_LOOT_TABLE : id;
    }

    public static List<IGlobalLootModifier> getAllLootMods()
    {
        return modifiers;
    }

    private static void modifyDrops(final Holder<LootTable> holder, final LootContext context, final List<ItemStack> drops)
    {
        final List<IGlobalLootModifier> mods = modifiers;
        if (mods.isEmpty())
        {
            return;
        }
        final Identifier tableId = holder == null ? LootTableIdCondition.UNKNOWN_LOOT_TABLE
            : holder.unwrapKey().map(k -> k.identifier()).orElse(LootTableIdCondition.UNKNOWN_LOOT_TABLE);
        final Identifier previous = QUERIED_TABLE.get();
        QUERIED_TABLE.set(tableId);
        try
        {
            ObjectArrayList<ItemStack> loot = new ObjectArrayList<>(drops);
            for (final IGlobalLootModifier mod : mods)
            {
                loot = mod.apply(loot, context);
            }
            drops.clear();
            drops.addAll(loot);
        }
        finally
        {
            if (previous == null)
            {
                QUERIED_TABLE.remove();
            }
            else
            {
                QUERIED_TABLE.set(previous);
            }
        }
    }

    /**
     * NeoForge 1.21.1 loot JSON uses the {@code condition} key for loot condition types; 26.x uses {@code type}.
     */
    private static void upgradeLootConditionKeys(final JsonElement element)
    {
        if (element instanceof JsonArray array)
        {
            array.forEach(ShimLoot::upgradeLootConditionKeys);
        }
        else if (element instanceof JsonObject object)
        {
            if (object.has("condition") && object.get("condition").isJsonPrimitive() && !object.has("type"))
            {
                object.add("type", object.remove("condition"));
            }
            object.entrySet().forEach(e -> upgradeLootConditionKeys(e.getValue()));
        }
    }

    private static final class Listener extends SimpleReloadListener<List<IGlobalLootModifier>>
    {
        @Override
        protected List<IGlobalLootModifier> prepare(final SharedState state)
        {
            final ResourceManager manager = state.resourceManager();
            final List<Identifier> finalLocations = new ArrayList<>();
            for (final Resource resource : manager.getResourceStack(GLOBAL_INDEX))
            {
                try (Reader reader = resource.openAsReader())
                {
                    final JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (GsonHelper.getAsBoolean(json, "replace", false))
                    {
                        finalLocations.clear();
                    }
                    final JsonArray entries = GsonHelper.getAsJsonArray(json, "entries");
                    for (int i = 0; i < entries.size(); i++)
                    {
                        final Identifier loc = Identifier.parse(GsonHelper.convertToString(entries.get(i), "entries[" + i + "]"));
                        finalLocations.remove(loc);
                        finalLocations.add(loc);
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.error("Couldn't read global loot modifier list {} in data pack {}", GLOBAL_INDEX, resource.sourcePackId(), e);
                }
            }

            if (finalLocations.isEmpty())
            {
                return List.of();
            }

            final HolderLookup.Provider lookup = state.get(ResourceLoader.REGISTRY_LOOKUP_KEY);
            final DynamicOps<JsonElement> conditionOps = new ConditionalOps<>(JsonOps.INSTANCE, ICondition.IContext.TAGS_INVALID);
            final DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, lookup);
            final FileToIdConverter converter = FileToIdConverter.json(FOLDER);
            final List<IGlobalLootModifier> result = new ArrayList<>();
            for (final Identifier location : finalLocations)
            {
                final Optional<Resource> resource = manager.getResource(converter.idToFile(location));
                if (resource.isEmpty())
                {
                    LOGGER.warn("Global loot modifier {} listed but not found", location);
                    continue;
                }
                try (Reader reader = resource.get().openAsReader())
                {
                    final JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    final JsonElement neoConditions = json.remove(ConditionalOps.DEFAULT_CONDITIONS_KEY);
                    if (neoConditions != null)
                    {
                        final List<ICondition> conditions = ICondition.LIST_CODEC.parse(conditionOps, neoConditions)
                            .getOrThrow(error -> new IllegalArgumentException("Invalid " + ConditionalOps.DEFAULT_CONDITIONS_KEY + ": " + error));
                        if (!conditions.stream().allMatch(c -> c.test(ICondition.IContext.TAGS_INVALID)))
                        {
                            LOGGER.debug("Skipping global loot modifier {}: conditions not met", location);
                            continue;
                        }
                    }
                    upgradeLootConditionKeys(json.get("conditions"));
                    IGlobalLootModifier.DIRECT_CODEC.parse(ops, json)
                        .resultOrPartial(error -> LOGGER.warn("Could not decode GlobalLootModifier with json id {} - error: {}", location, error))
                        .ifPresent(result::add);
                }
                catch (final Exception e)
                {
                    LOGGER.error("Couldn't read global loot modifier {}", location, e);
                }
            }
            return List.copyOf(result);
        }

        @Override
        protected void apply(final List<IGlobalLootModifier> prepared, final SharedState state)
        {
            modifiers = prepared;
            LOGGER.debug("Loaded {} global loot modifiers", prepared.size());
        }
    }
}
