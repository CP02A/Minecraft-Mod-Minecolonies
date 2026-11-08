package com.ldtteam.shim.neoforge.registries;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Fired on the mod buses to create custom registries (NeoForge shim). Fired by the Fabric entrypoint before RegisterEvent.
 */
public class NewRegistryEvent extends Event implements IModBusEvent
{
    public NewRegistryEvent()
    {
    }

    public <T> Registry<T> create(final RegistryBuilder<T> builder)
    {
        return builder.create();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void register(final Registry<T> registry)
    {
        if (BuiltInRegistries.REGISTRY.containsKey(registry.key().identifier()))
        {
            return;
        }
        FabricRegistryBuilder.from((WritableRegistry) registry).buildAndRegister();
    }
}
