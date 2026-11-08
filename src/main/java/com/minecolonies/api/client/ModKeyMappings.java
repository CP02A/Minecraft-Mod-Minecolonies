package com.minecolonies.api.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import com.ldtteam.shim.neoforge.client.event.RegisterKeyMappingsEvent;
import com.ldtteam.shim.neoforge.common.util.Lazy;
import org.jetbrains.annotations.NotNull;

/**
 * Key mappings
 */
public class ModKeyMappings
{
    // FABRIC-PORT: 26.x key categories are registered records (translation key derived from the id).
    private static final Lazy<KeyMapping.Category> CATEGORY =
      Lazy.of(() -> KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecolonies", "general")));

    /**
     * Toggle
     */
    public static final Lazy<KeyMapping> TOGGLE_GOGGLES = Lazy.of(() -> new KeyMapping("key.minecolonies.toggle_goggles",
            InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY.get()));

    /**
     * Register key mappings
     */
    public static void register(@NotNull final RegisterKeyMappingsEvent event)
    {
        event.register(TOGGLE_GOGGLES.get());
    }

    /**
     * Private constructor to hide the implicit one.
     */
    private ModKeyMappings()
    {
        /*
         * Intentionally left empty.
         */
    }
}
