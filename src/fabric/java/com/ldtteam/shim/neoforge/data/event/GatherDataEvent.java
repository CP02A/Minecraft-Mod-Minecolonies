package com.ldtteam.shim.neoforge.data.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.ModContainer;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

/**
 * Data generation event (NeoForge shim). Data generation is not supported in the Fabric port (generated output is
 * shipped as resources), so this event is never fired; it only exists so listeners compile.
 */
public abstract class GatherDataEvent extends Event implements IModBusEvent
{
    private final ModContainer modContainer;

    protected GatherDataEvent(final ModContainer modContainer)
    {
        this.modContainer = modContainer;
    }

    public ModContainer getModContainer()
    {
        return modContainer;
    }

    public DataGenerator getGenerator()
    {
        throw new UnsupportedOperationException("Data generation is not supported on Fabric");
    }

    public PackOutput getPackOutput()
    {
        throw new UnsupportedOperationException("Data generation is not supported on Fabric");
    }

    public CompletableFuture<HolderLookup.Provider> getLookupProvider()
    {
        throw new UnsupportedOperationException("Data generation is not supported on Fabric");
    }

    public <T extends DataProvider> T addProvider(final T provider)
    {
        return provider;
    }

    public boolean includeClient()
    {
        return false;
    }

    public boolean includeServer()
    {
        return false;
    }

    public boolean includeDev()
    {
        return false;
    }

    public boolean includeReports()
    {
        return false;
    }

    /**
     * Server data generation (26.x NeoForge split).
     */
    public static class Server extends GatherDataEvent
    {
        public Server(final ModContainer modContainer)
        {
            super(modContainer);
        }
    }

    /**
     * Client data generation (26.x NeoForge split).
     */
    public static class Client extends GatherDataEvent
    {
        public Client(final ModContainer modContainer)
        {
            super(modContainer);
        }
    }
}
