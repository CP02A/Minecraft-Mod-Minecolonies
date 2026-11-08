package com.ldtteam.shim.fabric.events;

import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.event.entity.EntityJoinLevelEvent;
import com.ldtteam.shim.neoforge.event.entity.EntityLeaveLevelEvent;
import com.ldtteam.shim.neoforge.event.entity.player.ItemTooltipEvent;
import com.ldtteam.shim.neoforge.event.level.ChunkEvent;
import com.ldtteam.shim.neoforge.event.level.LevelEvent;
import com.ldtteam.shim.neoforge.event.tick.LevelTickEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.function.BooleanSupplier;

/**
 * Client-side counterparts of {@link ShimGameEvents}.
 */
public final class ShimGameEventsClient
{
    private static final BooleanSupplier ALWAYS = () -> true;

    private static ClientLevel lastLevel;
    private static boolean    initialized;

    private ShimGameEventsClient()
    {
    }

    public static synchronized void init()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;

        ShimGameEvents.integratedServerConnection = () -> Minecraft.getInstance().hasSingleplayerServer();

        ClientTickEvents.START_LEVEL_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Pre(ALWAYS, level)));
        ClientTickEvents.END_LEVEL_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Post(ALWAYS, level)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Level unload when disconnecting (AFTER_CLIENT_LEVEL_CHANGE only reports non-null levels).
            if (client.level == null && lastLevel != null)
            {
                NeoForge.EVENT_BUS.post(new LevelEvent.Unload(lastLevel));
                lastLevel = null;
            }
        });
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
            if (lastLevel != null && lastLevel != level)
            {
                NeoForge.EVENT_BUS.post(new LevelEvent.Unload(lastLevel));
            }
            lastLevel = level;
            NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
        });
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> NeoForge.EVENT_BUS.post(new ChunkEvent.Load(chunk, false)));
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> NeoForge.EVENT_BUS.post(new ChunkEvent.Unload(chunk)));
        // FABRIC-PORT TODO: client-side EntityJoinLevelEvent cannot be canceled on Fabric.
        ClientEntityEvents.ENTITY_LOAD.register((Entity entity, ClientLevel level) -> NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(entity, level)));
        ClientEntityEvents.ENTITY_UNLOAD.register((Entity entity, ClientLevel level) -> NeoForge.EVENT_BUS.post(new EntityLeaveLevelEvent(entity, level)));

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) ->
            NeoForge.EVENT_BUS.post(new ItemTooltipEvent(stack, Minecraft.getInstance().player, lines, flag, context)));
    }
}
