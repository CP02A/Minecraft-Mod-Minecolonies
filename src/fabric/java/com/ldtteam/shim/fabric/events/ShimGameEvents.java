package com.ldtteam.shim.fabric.events;

import com.ldtteam.shim.fabric.ShimModBuses;
import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.event.BlockEntityTypeAddBlocksEvent;
import com.ldtteam.shim.neoforge.event.LootTableLoadEvent;
import com.ldtteam.shim.neoforge.event.OnDatapackSyncEvent;
import com.ldtteam.shim.neoforge.event.RegisterCommandsEvent;
import com.ldtteam.shim.neoforge.event.TagsUpdatedEvent;
import com.ldtteam.shim.neoforge.event.entity.EntityAttributeCreationEvent;
import com.ldtteam.shim.neoforge.event.entity.EntityJoinLevelEvent;
import com.ldtteam.shim.neoforge.event.entity.EntityLeaveLevelEvent;
import com.ldtteam.shim.neoforge.event.entity.player.AttackEntityEvent;
import com.ldtteam.shim.neoforge.event.entity.player.PlayerEvent;
import com.ldtteam.shim.neoforge.event.entity.player.PlayerInteractEvent;
import com.ldtteam.shim.neoforge.event.level.BlockEvent;
import com.ldtteam.shim.neoforge.event.level.ChunkEvent;
import com.ldtteam.shim.neoforge.event.level.LevelEvent;
import com.ldtteam.shim.neoforge.event.server.ServerAboutToStartEvent;
import com.ldtteam.shim.neoforge.event.server.ServerStartedEvent;
import com.ldtteam.shim.neoforge.event.server.ServerStartingEvent;
import com.ldtteam.shim.neoforge.event.server.ServerStoppedEvent;
import com.ldtteam.shim.neoforge.event.server.ServerStoppingEvent;
import com.ldtteam.shim.neoforge.event.tick.LevelTickEvent;
import com.ldtteam.shim.neoforge.event.tick.ServerTickEvent;
import com.ldtteam.shim.neoforge.server.ServerLifecycleHooks;
import com.ldtteam.shim.neoforge.server.command.EnumArgument;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Bridges Fabric API callbacks to the NeoForge-style game events on {@link NeoForge#EVENT_BUS}.
 * Events Fabric API has no callback for are fired by the mixins in {@code com.ldtteam.shim.mixin}.
 */
public final class ShimGameEvents
{
    private static final BooleanSupplier ALWAYS = () -> true;

    /**
     * Set by the client init: whether the client is connected to its integrated server.
     */
    static volatile BooleanSupplier integratedServerConnection = () -> false;

    private static boolean initialized;

    private ShimGameEvents()
    {
    }

    /**
     * Post EntityAttributeCreationEvent on the mod buses and register the results with Fabric.
     */
    public static void postEntityAttributeCreation()
    {
        final Map<EntityType<? extends LivingEntity>, AttributeSupplier> map = new LinkedHashMap<>();
        ShimModBuses.post(new EntityAttributeCreationEvent(map));
        map.forEach(FabricDefaultAttributeRegistry::register);
    }

    /**
     * Post BlockEntityTypeAddBlocksEvent on the mod buses; modifications are applied immediately by the event.
     */
    public static void postBlockEntityTypeAddBlocks()
    {
        ShimModBuses.post(new BlockEntityTypeAddBlocksEvent());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized void init()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;

        ArgumentTypeRegistry.registerArgumentType(Identifier.fromNamespaceAndPath("minecolonies", "shim_enum"), (Class) EnumArgument.class, (EnumArgument.Info) new EnumArgument.Info());

        // Server lifecycle
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ServerLifecycleHooks.setCurrentServer(server);
            NeoForge.EVENT_BUS.post(new ServerAboutToStartEvent(server));
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            NeoForge.EVENT_BUS.post(new ServerStartingEvent(server));
            NeoForge.EVENT_BUS.post(new ServerStartedEvent(server));
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> NeoForge.EVENT_BUS.post(new ServerStoppingEvent(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            NeoForge.EVENT_BUS.post(new ServerStoppedEvent(server));
            ServerLifecycleHooks.setCurrentServer(null);
        });
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) ->
            NeoForge.EVENT_BUS.post(new OnDatapackSyncEvent(player.level().getServer().getPlayerList(), player)));

        // Ticks
        ServerTickEvents.START_SERVER_TICK.register(server -> NeoForge.EVENT_BUS.post(new ServerTickEvent.Pre(ALWAYS, server)));
        ServerTickEvents.END_SERVER_TICK.register(server -> NeoForge.EVENT_BUS.post(new ServerTickEvent.Post(ALWAYS, server)));
        ServerTickEvents.START_LEVEL_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Pre(ALWAYS, level)));
        ServerTickEvents.END_LEVEL_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Post(ALWAYS, level)));

        // Levels, chunks, entities
        ServerLevelEvents.LOAD.register((server, level) -> NeoForge.EVENT_BUS.post(new LevelEvent.Load(level)));
        ServerLevelEvents.UNLOAD.register((server, level) -> NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level)));
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> NeoForge.EVENT_BUS.post(new ChunkEvent.Load(chunk, generated)));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> NeoForge.EVENT_BUS.post(new ChunkEvent.Unload(chunk)));
        ServerEntityEvents.ALLOW_LOAD.register((entity, level, spawnReason, loadedFromDisk) ->
            !NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(entity, level, loadedFromDisk)).isCanceled());
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> NeoForge.EVENT_BUS.post(new EntityLeaveLevelEvent(entity, level)));

        // Players
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(handler.player)));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player, origin.dimension(), destination.dimension())));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(newPlayer, oldPlayer, !alive)));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(newPlayer, alive)));

        // Interaction (Fabric fires these on both logical sides, like NeoForge)
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (player.isSpectator())
            {
                return InteractionResult.PASS;
            }
            final PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player, hand, hitResult.getBlockPos(), hitResult);
            NeoForge.EVENT_BUS.post(event);
            // FABRIC-PORT TODO: partial denial via setUseBlock/setUseItem is not supported (only full cancellation).
            if (!event.isCanceled())
            {
                return InteractionResult.PASS;
            }
            final InteractionResult result = cancelResult(event.getCancellationResult());
            // NeoForge still sends the use packet to the server when the client cancels; Fabric only sends it for a result that consumes the action.
            // Mods (e.g. MineColonies hut placement) cancel client-side and let the server react, so keep the packet flowing without a client swing.
            if (level.isClientSide() && !result.consumesAction())
            {
                return InteractionResult.SUCCESS_SERVER;
            }
            return result;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (player.isSpectator())
            {
                return InteractionResult.PASS;
            }
            final PlayerInteractEvent.RightClickItem event = new PlayerInteractEvent.RightClickItem(player, hand);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled() ? cancelResult(event.getCancellationResult()) : InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (player.isSpectator())
            {
                return InteractionResult.PASS;
            }
            if (hitResult != null)
            {
                final Vec3 local = hitResult.getLocation().subtract(entity.position());
                final PlayerInteractEvent.EntityInteractSpecific event = new PlayerInteractEvent.EntityInteractSpecific(player, hand, entity, local);
                NeoForge.EVENT_BUS.post(event);
                return event.isCanceled() ? cancelResult(event.getCancellationResult()) : InteractionResult.PASS;
            }
            final PlayerInteractEvent.EntityInteract event = new PlayerInteractEvent.EntityInteract(player, hand, entity);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled() ? cancelResult(event.getCancellationResult()) : InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            final PlayerInteractEvent.LeftClickBlock event =
              new PlayerInteractEvent.LeftClickBlock(player, pos, direction, PlayerInteractEvent.LeftClickBlock.Action.START);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
            NeoForge.EVENT_BUS.post(new AttackEntityEvent(player, entity)).isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
            !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled());

        // Data
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            NeoForge.EVENT_BUS.post(new RegisterCommandsEvent(dispatcher, selection, buildContext)));
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) ->
            NeoForge.EVENT_BUS.post(new TagsUpdatedEvent(registries, client, client && integratedServerConnection.getAsBoolean())));
        LootTableEvents.REPLACE.register((key, original, source, registries) -> {
            final LootTableLoadEvent event = new LootTableLoadEvent(registries, key.identifier(), original);
            if (NeoForge.EVENT_BUS.post(event).isCanceled())
            {
                return LootTable.EMPTY;
            }
            return event.getTable() == original ? null : event.getTable();
        });
    }

    /**
     * NeoForge stops vanilla handling when an interaction event is canceled; on Fabric a PASS result would continue,
     * so a canceled event with PASS maps to FAIL.
     */
    private static InteractionResult cancelResult(final InteractionResult result)
    {
        return result == InteractionResult.PASS ? InteractionResult.FAIL : result;
    }
}
