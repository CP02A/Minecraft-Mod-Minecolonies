// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.settings.KeyModifier), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.settings;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * Key modifiers. Vanilla/Fabric key mappings do not support modifiers; this only offers the query helpers.
 */
public enum KeyModifier
{
    CONTROL
      {
          @Override
          public boolean matches(final InputConstants.Key key)
          {
              final int keyCode = key.getValue();
              return keyCode == InputConstants.KEY_LCONTROL || keyCode == InputConstants.KEY_RCONTROL
;
          }

          @Override
          public boolean isActive(@Nullable final IKeyConflictContext conflictContext)
          {
              return Minecraft.getInstance().hasControlDown();
          }
      },
    SHIFT
      {
          @Override
          public boolean matches(final InputConstants.Key key)
          {
              return key.getValue() == InputConstants.KEY_LSHIFT || key.getValue() == InputConstants.KEY_RSHIFT;
          }

          @Override
          public boolean isActive(@Nullable final IKeyConflictContext conflictContext)
          {
              return Minecraft.getInstance().hasShiftDown();
          }
      },
    ALT
      {
          @Override
          public boolean matches(final InputConstants.Key key)
          {
              return key.getValue() == InputConstants.KEY_LALT || key.getValue() == InputConstants.KEY_RALT;
          }

          @Override
          public boolean isActive(@Nullable final IKeyConflictContext conflictContext)
          {
              return Minecraft.getInstance().hasAltDown();
          }
      },
    NONE
      {
          @Override
          public boolean matches(final InputConstants.Key key)
          {
              return false;
          }

          @Override
          public boolean isActive(@Nullable final IKeyConflictContext conflictContext)
          {
              for (final KeyModifier modifier : MODIFIER_VALUES)
              {
                  if (modifier.isActive(conflictContext))
                  {
                      return false;
                  }
              }
              return true;
          }
      };

    public static final KeyModifier[] MODIFIER_VALUES = {SHIFT, CONTROL, ALT};

    public static KeyModifier getActiveModifier()
    {
        for (final KeyModifier keyModifier : MODIFIER_VALUES)
        {
            if (keyModifier.isActive(null))
            {
                return keyModifier;
            }
        }
        return NONE;
    }

    public static KeyModifier getKeyModifier(final InputConstants.Key key)
    {
        for (final KeyModifier keyModifier : MODIFIER_VALUES)
        {
            if (keyModifier.matches(key))
            {
                return keyModifier;
            }
        }
        return NONE;
    }

    public static boolean isKeyCodeModifier(final InputConstants.Key key)
    {
        return getKeyModifier(key) != NONE;
    }

    public static KeyModifier valueFromString(final String stringValue)
    {
        try
        {
            return valueOf(stringValue);
        }
        catch (final NullPointerException | IllegalArgumentException ignored)
        {
            return NONE;
        }
    }

    public abstract boolean matches(InputConstants.Key key);

    public abstract boolean isActive(@Nullable IKeyConflictContext conflictContext);
}
