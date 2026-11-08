// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.settings.KeyConflictContext), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.settings;

import net.minecraft.client.Minecraft;

public enum KeyConflictContext implements IKeyConflictContext
{
    UNIVERSAL
      {
          @Override
          public boolean isActive()
          {
              return true;
          }

          @Override
          public boolean conflicts(final IKeyConflictContext other)
          {
              return true;
          }
      },
    GUI
      {
          @Override
          public boolean isActive()
          {
              return Minecraft.getInstance().gui.screen() != null;
          }

          @Override
          public boolean conflicts(final IKeyConflictContext other)
          {
              return this == other;
          }
      },
    IN_GAME
      {
          @Override
          public boolean isActive()
          {
              return !GUI.isActive();
          }

          @Override
          public boolean conflicts(final IKeyConflictContext other)
          {
              return this == other;
          }
      }
}
