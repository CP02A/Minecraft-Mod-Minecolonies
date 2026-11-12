// Adapted from NeoForge 26.3 (net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.fabricmc.fabric.api.client.rendering.v1.AtlasRegistry;
import net.minecraft.client.resources.model.sprite.AtlasManager;

/**
 * Mod bus event to register texture atlases; forwarded to Fabric's {@link AtlasRegistry}. Metadata sections go into the
 * {@link AtlasManager.AtlasConfig} (NeoForge's {@code addAdditionalMetadata} for existing atlases is left out: no mod code uses it).
 */
public class RegisterTextureAtlasesEvent extends Event implements IModBusEvent
{
    public RegisterTextureAtlasesEvent() {}

    public void register(final AtlasManager.AtlasConfig atlasConfig)
    {
        AtlasRegistry.register(atlasConfig);
    }
}
