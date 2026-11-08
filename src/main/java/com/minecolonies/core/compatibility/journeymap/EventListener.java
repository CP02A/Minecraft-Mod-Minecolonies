package com.minecolonies.core.compatibility.journeymap;

import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.eventbus.events.colony.ColonyViewUpdatedModEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static com.minecolonies.api.util.constant.Constants.MOD_ID;

public class EventListener
{
    @NotNull
    private final Journeymap jmap;
    private boolean viewsUpdated;

    public EventListener(@NotNull final Journeymap jmap)
    {
        this.jmap = jmap;

        // Fabric port: NeoForge bus events replaced by the equivalent Fabric API client events.
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> onPlayerLogout());
        ClientChunkEvents.CHUNK_LOAD.register(this::onChunkLoaded);
        ClientTickEvents.START_CLIENT_TICK.register(client -> onClientTick());
        IMinecoloniesAPI.getInstance().getEventBus().subscribe(ColonyViewUpdatedModEvent.class, this::onColonyViewUpdated);
    }

    public void onPlayerLogout()
    {
        ColonyDeathpoints.clear();
        this.jmap.getApi().removeAll(MOD_ID);
    }

    public void onChunkLoaded(@NotNull final ClientLevel level, @NotNull final LevelChunk chunk)
    {
        final ResourceKey<Level> dimension = level.dimension();

        ColonyBorderMapping.updateChunk(this.jmap, dimension, chunk);
        ColonyDeathpoints.updateChunk(this.jmap, dimension, chunk);
    }

    public void onColonyViewUpdated(@NotNull final ColonyViewUpdatedModEvent event)
    {
        final IColonyView colony = event.getColony();
        final Set<BlockPos> graves = colony.getGraveManager().getGraves().keySet();

        viewsUpdated = true;
        ColonyDeathpoints.updateGraves(this.jmap, colony, graves);
    }

    public void onClientTick()
    {
        final Level world = Minecraft.getInstance().level;
        if (world != null)
        {
            if (viewsUpdated)
            {
                viewsUpdated = false;
                ColonyBorderMapping.queueChunks(this.jmap, world.dimension());
            }

            ColonyBorderMapping.updatePending(this.jmap, world.dimension());
        }
    }
}
