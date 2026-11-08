package com.ldtteam.common.config;

import com.ldtteam.shim.fml.ModContainer;
import com.ldtteam.shim.neoforge.client.gui.ConfigurationScreen;
import com.ldtteam.shim.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client bouncer class
 */
public class ClientConfigHelper
{
    static void registerClient(final ModContainer modContainer)
    {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
