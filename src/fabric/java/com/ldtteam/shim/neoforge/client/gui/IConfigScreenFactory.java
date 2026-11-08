// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.gui.IConfigScreenFactory), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.gui;

import com.ldtteam.shim.fml.IExtensionPoint;
import com.ldtteam.shim.fml.ModContainer;
import net.minecraft.client.gui.screens.Screen;

/**
 * Extension point creating a mod's configuration screen.
 */
@FunctionalInterface
public interface IConfigScreenFactory extends IExtensionPoint
{
    Screen createScreen(ModContainer container, Screen modListScreen);
}
