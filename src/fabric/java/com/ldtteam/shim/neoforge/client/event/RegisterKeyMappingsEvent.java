// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * Mod bus event to register key mappings; forwarded to Fabric's {@link KeyMappingHelper}.
 */
public class RegisterKeyMappingsEvent extends Event implements IModBusEvent
{
    public RegisterKeyMappingsEvent() {}

    public void register(final KeyMapping key)
    {
        KeyMappingHelper.registerKeyMapping(key);
    }

    /**
     * Categories register themselves on creation ({@link KeyMapping.Category#register}); kept for NeoForge 26 API compatibility.
     */
    public void registerCategory(final KeyMapping.Category category)
    {
        // no-op: vanilla categories are registered when created
    }
}
