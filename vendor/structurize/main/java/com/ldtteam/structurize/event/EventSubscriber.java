package com.ldtteam.structurize.event;

import com.ldtteam.structurize.commands.EntryPoint;
import com.ldtteam.structurize.management.Manager;
import com.ldtteam.structurize.util.BlockUtils;
import com.ldtteam.structurize.util.IOPool;
import net.minecraft.server.level.ServerLevel;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.neoforge.event.RegisterCommandsEvent;
import com.ldtteam.shim.neoforge.event.server.ServerStoppingEvent;
import com.ldtteam.shim.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Class with methods for receiving various forge events
 */
public class EventSubscriber
{
    /**
     * Private constructor to hide implicit public one.
     */
    private EventSubscriber()
    {
        /*
         * Intentionally left empty
         */
    }

    /**
     * Called when world is about to load.
     *
     * @param event event
     */
    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event)
    {
        EntryPoint.register(event.getDispatcher(), event.getCommandSelection());
    }

    @SubscribeEvent
    public static void onWorldTick(final LevelTickEvent.Pre event)
    {
        BlockUtils.checkOrInit();
        if (event.getLevel() instanceof ServerLevel serverLevel)
        {
            Manager.onWorldTick(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(@NotNull final ServerStoppingEvent event)
    {
        IOPool.shutdown();
    }
}
