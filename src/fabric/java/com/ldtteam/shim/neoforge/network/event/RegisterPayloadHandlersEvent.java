package com.ldtteam.shim.neoforge.network.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import com.ldtteam.shim.neoforge.network.registration.PayloadRegistrar;

/**
 * Fired on the mod buses to register payloads (NeoForge shim; fired by the Fabric main entrypoint).
 */
public class RegisterPayloadHandlersEvent extends Event implements IModBusEvent
{
    public RegisterPayloadHandlersEvent()
    {
    }

    /**
     * @param version the default version of payloads registered with the registrar.
     */
    public PayloadRegistrar registrar(final String version)
    {
        return new PayloadRegistrar(version);
    }
}
