package com.minecolonies.fabric;

import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.crafting.registry.CraftingType;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FABRIC-PORT: sends the recipes MineColonies' crafters can use to MineColonies clients, for JEI's crafter recipe categories on dedicated
 * servers (JEIPlugin#onRecipesLoaded). 26.x clients only get the recipes of serializers registered with Fabric's
 * {@link RecipeSynchronization}; NeoForge 1.21.1 synced all recipes. MineColonies synchronizes the serializers it needs itself
 * ({@link #synchronizeSerializer}); recipes of other mods' serializers of the crafter recipe types ({@link CraftingType#getRecipeTypes()})
 * come through this mod's own payload instead.
 * <p>
 * Isolation: other mods' serializers are not added to the Fabric recipe sync (that would make the server send them to every client with
 * Fabric API, which might not know them), and the payload is only sent to clients that registered its channel
 * ({@link ServerPlayNetworking#canSend}), i.e. clients with MineColonies.
 */
public final class CrafterRecipeSync
{
    private static final Logger LOGGER = LoggerFactory.getLogger("MineColonies CrafterRecipeSync");

    /**
     * Recipes per payload; Fabric splits payloads above the vanilla packet size limit (registerLarge) up to {@link #MAX_PAYLOAD_SIZE}.
     */
    static final int RECIPES_PER_PAYLOAD = 256;

    /**
     * Max encoded size of one payload.
     */
    private static final int MAX_PAYLOAD_SIZE = 16 * 1024 * 1024;

    /**
     * Serializers MineColonies registered with the Fabric recipe sync: their recipes already reach every MineColonies client.
     */
    private static final Set<RecipeSerializer<?>> SYNCHRONIZED_SERIALIZERS = Collections.synchronizedSet(new HashSet<>());

    /**
     * Client side: the recipes received from the server, by id.
     */
    private static final Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> CLIENT_RECIPES = new LinkedHashMap<>();

    /**
     * Server side cache: the recipes to send, for the recipe manager they were collected from (a new one after every reload).
     */
    private static RecipeManager cachedManager;
    private static List<RecipeHolder<?>> cachedRecipes = List.of();

    private CrafterRecipeSync()
    {
    }

    /**
     * One part of the crafter recipes.
     *
     * @param first   true for the first part: the client drops the recipes it had (after a datapack reload).
     * @param recipes the recipes.
     */
    public record Payload(boolean first, List<RecipeHolder<?>> recipes) implements CustomPacketPayload
    {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("minecolonies", "crafter_recipes"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of(CrafterRecipeSync::encode, CrafterRecipeSync::decode);

        @Override
        @NotNull
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    /**
     * Registers the payload type (both sides). Called by the main entrypoint.
     */
    public static void register()
    {
        PayloadTypeRegistry.clientboundPlay().registerLarge(Payload.TYPE, Payload.CODEC, MAX_PAYLOAD_SIZE);
    }

    /**
     * Registers a serializer MineColonies needs on the client with Fabric's recipe sync, and remembers it (its recipes are not sent again).
     *
     * @param serializer the serializer.
     */
    public static void synchronizeSerializer(@NotNull final RecipeSerializer<?> serializer)
    {
        RecipeSynchronization.synchronizeRecipeSerializer(serializer);
        SYNCHRONIZED_SERIALIZERS.add(serializer);
    }

    /**
     * @param player a player.
     * @return true if the crafter recipes are sent to this player: the client has MineColonies (registered the payload channel) and doesn't
     *     play on its own integrated server (JEI uses the server's recipe manager there).
     */
    public static boolean shouldSend(@NotNull final ServerPlayer player)
    {
        if (player.connection == null || !ServerPlayNetworking.canSend(player, Payload.TYPE))
        {
            return false;
        }
        final MinecraftServer server = player.level().getServer();
        return server.isDedicatedServer() || !server.isSingleplayerOwner(player.nameAndId());
    }

    /**
     * Sends the crafter recipes the Fabric recipe sync doesn't cover to a player, in parts. Called on login and after a datapack reload,
     * before MineColonies' custom recipe packets (whose arrival makes JEI rebuild its crafter categories).
     *
     * @param player the player.
     * @return the number of payloads sent (0 if the player doesn't get them, see {@link #shouldSend}).
     */
    public static int sendTo(@NotNull final ServerPlayer player)
    {
        if (!shouldSend(player))
        {
            return 0;
        }
        final List<RecipeHolder<?>> recipes = recipesToSend(player.level().getServer());
        int sent = 0;
        int from = 0;
        do
        {
            final int to = Math.min(recipes.size(), from + RECIPES_PER_PAYLOAD);
            ServerPlayNetworking.send(player, new Payload(from == 0, List.copyOf(recipes.subList(from, to))));
            sent++;
            from = to;
        }
        while (from < recipes.size());
        return sent;
    }

    /**
     * @param server the server.
     * @return the server's recipes of the crafter recipe types whose serializer MineColonies doesn't synchronize through Fabric.
     */
    @NotNull
    public static synchronized List<RecipeHolder<?>> recipesToSend(@NotNull final MinecraftServer server)
    {
        final RecipeManager manager = server.getRecipeManager();
        if (manager != cachedManager)
        {
            final Set<RecipeType<?>> types = new HashSet<>();
            for (final CraftingType type : MinecoloniesAPIProxy.getInstance().getCraftingTypeRegistry())
            {
                types.addAll(type.getRecipeTypes());
            }
            final List<RecipeHolder<?>> recipes = new ArrayList<>();
            for (final RecipeHolder<?> holder : manager.getRecipes())
            {
                final Recipe<?> recipe = holder.value();
                if (types.contains(recipe.getType()) && !SYNCHRONIZED_SERIALIZERS.contains(recipe.getSerializer())
                      && BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()) != null)
                {
                    recipes.add(holder);
                }
            }
            cachedManager = manager;
            cachedRecipes = List.copyOf(recipes);
            LOGGER.debug("{} crafter recipes of serializers outside the Fabric recipe sync", cachedRecipes.size());
        }
        return cachedRecipes;
    }

    /**
     * Client side: stores a received part.
     *
     * @param payload the part.
     */
    public static void receive(@NotNull final Payload payload)
    {
        synchronized (CLIENT_RECIPES)
        {
            if (payload.first())
            {
                CLIENT_RECIPES.clear();
            }
            payload.recipes().forEach(holder -> CLIENT_RECIPES.put(holder.id(), holder));
        }
    }

    /**
     * Client side: forget the received recipes (on disconnect).
     */
    public static void clearClient()
    {
        synchronized (CLIENT_RECIPES)
        {
            CLIENT_RECIPES.clear();
        }
    }

    /**
     * Client side: the recipes JEI's crafter categories are built from on a dedicated server.
     *
     * @param synchronizedRecipes the recipes of the Fabric recipe sync.
     * @return those plus the received crafter recipes that are not among them (same id).
     */
    @NotNull
    public static List<RecipeHolder<?>> withReceivedRecipes(@NotNull final Collection<RecipeHolder<?>> synchronizedRecipes)
    {
        final List<RecipeHolder<?>> all = new ArrayList<>(synchronizedRecipes);
        final Set<ResourceKey<Recipe<?>>> ids = new HashSet<>();
        synchronizedRecipes.forEach(holder -> ids.add(holder.id()));
        synchronized (CLIENT_RECIPES)
        {
            for (final RecipeHolder<?> holder : CLIENT_RECIPES.values())
            {
                if (ids.add(holder.id()))
                {
                    all.add(holder);
                }
            }
        }
        return all;
    }

    /**
     * @return the number of recipes the client received (for tests).
     */
    public static int receivedCount()
    {
        synchronized (CLIENT_RECIPES)
        {
            return CLIENT_RECIPES.size();
        }
    }

    // Each recipe: id, serializer id, then the recipe encoded by its serializer with a length prefix, so that a client which can't decode
    // one (e.g. a server-only mod's serializer) skips it instead of failing the whole payload.

    @SuppressWarnings("unchecked")
    private static void encode(final RegistryFriendlyByteBuf buf, final Payload payload)
    {
        buf.writeBoolean(payload.first());
        buf.writeVarInt(payload.recipes().size());
        for (final RecipeHolder<?> holder : payload.recipes())
        {
            final RecipeSerializer<Recipe<?>> serializer = (RecipeSerializer<Recipe<?>>) holder.value().getSerializer();
            buf.writeResourceKey(holder.id());
            buf.writeIdentifier(BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer));
            final RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.buffer(), buf.registryAccess());
            try
            {
                serializer.streamCodec().encode(data, holder.value());
                buf.writeVarInt(data.readableBytes());
                buf.writeBytes(data);
            }
            finally
            {
                data.release();
            }
        }
    }

    private static Payload decode(final RegistryFriendlyByteBuf buf)
    {
        final boolean first = buf.readBoolean();
        final int count = buf.readVarInt();
        final List<RecipeHolder<?>> recipes = new ArrayList<>(Math.min(count, RECIPES_PER_PAYLOAD));
        for (int i = 0; i < count; i++)
        {
            final ResourceKey<Recipe<?>> id = buf.readResourceKey(Registries.RECIPE);
            final Identifier serializerId = buf.readIdentifier();
            final int length = buf.readVarInt();
            final RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(buf.readSlice(length), buf.registryAccess());
            final RecipeSerializer<?> serializer = BuiltInRegistries.RECIPE_SERIALIZER.getValue(serializerId);
            if (serializer == null)
            {
                LOGGER.debug("Skipping crafter recipe {}: unknown serializer {}", id.identifier(), serializerId);
                continue;
            }
            try
            {
                recipes.add(new RecipeHolder<>(id, serializer.streamCodec().decode(data)));
            }
            catch (final RuntimeException e)
            {
                LOGGER.warn("Skipping crafter recipe {} (serializer {}): {}", id.identifier(), serializerId, e.toString());
            }
        }
        return new Payload(first, recipes);
    }
}
