package com.ldtteam.shim.fabric.client.config;

import com.ldtteam.shim.fabric.ShimModBuses;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Mod Menu integration (optional: only the {@code modmenu} entrypoint in fabric.mod.json refers to this class, and only Mod Menu loads it,
 * so the game runs without Mod Menu). MineColonies' entry gets the config screens of the bundled mods ({@link ShimConfigScreens}); the
 * bundled mods' provided ids get their own screens too.
 */
public class ShimModMenuIntegration implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return ShimConfigScreens::createModListScreen;
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories()
    {
        final Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>();
        for (final Map.Entry<String, Function<net.minecraft.client.gui.screens.Screen, net.minecraft.client.gui.screens.Screen>> entry : ShimConfigScreens.factories().entrySet())
        {
            if (!entry.getKey().equals(ShimModBuses.FABRIC_MOD_ID))
            {
                factories.put(entry.getKey(), parent -> entry.getValue().apply(parent));
            }
        }
        return factories;
    }
}
