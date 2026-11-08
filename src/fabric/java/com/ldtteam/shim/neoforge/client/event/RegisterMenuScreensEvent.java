// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.RegisterMenuScreensEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Mod bus event to register menu screens; forwarded to {@link MenuScreens#register}.
 */
public class RegisterMenuScreensEvent extends Event implements IModBusEvent
{
    public RegisterMenuScreensEvent() {}

    public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(final MenuType<? extends M> menuType, final MenuScreens.ScreenConstructor<M, U> screenConstructor)
    {
        MenuScreens.register(menuType, screenConstructor);
    }
}
