package com.minecolonies.core.event;

import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.datalistener.*;
import com.minecolonies.core.entity.pathfinding.Pathfinding;
import com.minecolonies.core.util.BackUpHelper;
import net.minecraft.server.level.ServerPlayer;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.neoforge.client.event.ClientTickEvent;
import com.ldtteam.shim.neoforge.event.AddReloadListenerEvent;
import com.ldtteam.shim.neoforge.event.entity.player.PlayerEvent;
import com.ldtteam.shim.neoforge.event.server.ServerAboutToStartEvent;
import com.ldtteam.shim.neoforge.event.server.ServerStartedEvent;
import com.ldtteam.shim.neoforge.event.server.ServerStoppingEvent;
import com.ldtteam.shim.neoforge.event.tick.LevelTickEvent;
import com.ldtteam.shim.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event handler used to catch various forge events.
 */
public class FMLEventHandler
{
    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Pre event)
    {
        IColonyManager.getInstance().onServerTick(event);
        DataPackSyncEventHandler.ServerEvents.load(event.getServer());
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Pre event)
    {
        IColonyManager.getInstance().onClientTick(event);
    }

    @SubscribeEvent
    public static void onPlayerLogin(@NotNull final PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer)
        {
            // This automatically reloads the owner of the colony if failed.
            IColonyManager.getInstance().getIColonyByOwner(event.getEntity().level(), event.getEntity());
            //ColonyManager.syncAllColoniesAchievements();
        }
    }

    @SubscribeEvent
    public static void onAddReloadListenerEvent(@NotNull final AddReloadListenerEvent event)
    {
        event.addListener(new CrafterRecipeListener());
        event.addListener(new ResearchListener());
        event.addListener(new CustomVisitorListener());
        event.addListener(new CitizenNameListener());
        event.addListener(new QuestJsonListener());
        event.addListener(new ItemNbtListener());
        event.addListener(StudyItemListener.INSTANCE);
        event.addListener(new DiseasesListener());
        event.addListener(new RecruitmentItemsListener());
    }

    @SubscribeEvent
    public static void onServerStarted(@NotNull final ServerStartedEvent event)
    {
        BackUpHelper.loadMissingColonies();
    }

    @SubscribeEvent
    public static void onWorldTick(final LevelTickEvent.Pre event)
    {
        IColonyManager.getInstance().onWorldTick(event);
    }

    @SubscribeEvent
    public static void onServerAboutToStart(@NotNull final ServerAboutToStartEvent event)
    {
        IColonyManager.getInstance().getRecipeManager().reset();
    }

    @SubscribeEvent
    public static void onServerStopped(@NotNull final ServerStoppingEvent event)
    {
        Pathfinding.shutdown();
        DataPackSyncEventHandler.ServerEvents.reset();
    }
}
