package com.minecolonies.fabric;

import com.ldtteam.shim.fabric.ShimModBuses;
import com.ldtteam.shim.fabric.config.ShimConfigsClient;
import com.ldtteam.shim.fabric.events.ShimGameEventsClient;
import com.ldtteam.shim.fabric.network.ShimNetworkingClient;
import com.ldtteam.shim.fml.event.lifecycle.FMLClientSetupEvent;
import com.ldtteam.shim.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.fabricmc.api.ClientModInitializer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Fabric client entrypoint: client half of the NeoForge loading sequence. Runs after {@link MineColoniesFabric#onInitialize()}.
 */
public class MineColoniesFabricClient implements ClientModInitializer
{
    /**
     * Client event wiring of the client shim (owned by the client shim area); optional.
     */
    private static final String CLIENT_HOOKS = "com.ldtteam.shim.fabric.client.ShimClientHooks";

    @Override
    public void onInitializeClient()
    {
        ShimNetworkingClient.init();
        ShimConfigsClient.init();
        ShimGameEventsClient.init();
        callHook("init");

        // Development aid: log screen changes (a headless run can then tell which screen the client is sitting on).
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment())
        {
            final String[] lastScreen = {""};
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc -> {
                final Object screen = mc.gui.screen();
                final String name = screen == null ? "none" : screen.getClass().getName() + " " + ((net.minecraft.client.gui.screens.Screen) screen).getTitle().getString();
                if (!name.equals(lastScreen[0]))
                {
                    lastScreen[0] = name;
                    org.slf4j.LoggerFactory.getLogger("minecolonies-dev").info("Screen: {}", name);
                }
            });
        }

        // FMLClientSetupEvent / FMLLoadCompleteEvent fire after the registries are frozen (see ShimSetupPhase).
        com.ldtteam.shim.fabric.ShimSetupPhase.addClientStep(() -> callHook("postClientSetup"));
    }

    private static void callHook(final String methodName)
    {
        final Method method;
        try
        {
            method = Class.forName(CLIENT_HOOKS).getMethod(methodName);
        }
        catch (final ClassNotFoundException | NoSuchMethodException e)
        {
            return;
        }
        try
        {
            method.invoke(null);
        }
        catch (final IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
        catch (final InvocationTargetException e)
        {
            if (e.getCause() instanceof RuntimeException re)
            {
                throw re;
            }
            throw new IllegalStateException(e.getCause());
        }
    }
}
