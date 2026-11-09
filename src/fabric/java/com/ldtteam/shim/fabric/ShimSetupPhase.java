package com.ldtteam.shim.fabric;

import com.ldtteam.shim.fml.event.lifecycle.FMLClientSetupEvent;
import com.ldtteam.shim.fml.event.lifecycle.FMLCommonSetupEvent;
import com.ldtteam.shim.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import com.ldtteam.shim.fml.event.lifecycle.FMLLoadCompleteEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the FML setup lifecycle (common setup, client/dedicated-server setup, load complete) once the built-in registries are frozen.
 * <p>
 * On NeoForge these events fire after registration has finished and item components are bound. On Fabric the registries are frozen
 * by {@code BuiltInRegistries.bootStrap()} only after every mod initializer ran, so firing the events from the entrypoints would let
 * setup code observe unbound items (e.g. {@code new ItemStack(item)} throws "Components not bound yet").
 */
public final class ShimSetupPhase
{
    private static final List<Runnable> CLIENT_STEPS = new ArrayList<>();

    private static boolean commonReady;
    private static boolean dedicatedServer;
    private static boolean done;

    private ShimSetupPhase()
    {
    }

    /**
     * Called at the end of the main entrypoint.
     *
     * @param isDedicatedServer whether this is the dedicated server.
     */
    public static void commonInitialized(final boolean isDedicatedServer)
    {
        commonReady = true;
        dedicatedServer = isDedicatedServer;
    }

    /**
     * Called by the client entrypoint to queue client-only steps that must run after FMLClientSetupEvent.
     *
     * @param step the step.
     */
    public static void addClientStep(final Runnable step)
    {
        CLIENT_STEPS.add(step);
    }

    /**
     * Called (via mixin) when BuiltInRegistries.bootStrap() returns.
     */
    public static void onRegistriesFrozen()
    {
        if (done || !commonReady)
        {
            return;
        }
        done = true;

        ShimModBuses.postLifecycle(FMLCommonSetupEvent::new);
        if (dedicatedServer)
        {
            ShimModBuses.postLifecycle(FMLDedicatedServerSetupEvent::new);
        }
        else
        {
            ShimModBuses.postLifecycle(FMLClientSetupEvent::new);
            CLIENT_STEPS.forEach(Runnable::run);
        }
        ShimModBuses.postLifecycle(FMLLoadCompleteEvent::new);
    }

    /**
     * Unwraps reflective invocation failures.
     *
     * @param e the exception.
     * @return a runtime exception to throw.
     */
    public static RuntimeException unwrap(final InvocationTargetException e)
    {
        return e.getCause() instanceof RuntimeException re ? re : new IllegalStateException(e.getCause());
    }
}
