// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;

import java.util.ArrayList;

/**
 * Overlay customization events. Only {@link DebugText} is supported: it is posted by a 26.x debug screen entry
 * ({@code minecolonies:shim_debug_text}, enable it in the F3 debug options) and its lines are displayed by that entry.
 */
public abstract class CustomizeGuiOverlayEvent extends Event
{
    protected CustomizeGuiOverlayEvent() {}

    public static class DebugText extends CustomizeGuiOverlayEvent
    {
        private final ArrayList<String> left;
        private final ArrayList<String> right;

        public DebugText(final ArrayList<String> left, final ArrayList<String> right)
        {
            this.left = left;
            this.right = right;
        }

        public ArrayList<String> getLeft()
        {
            return left;
        }

        public ArrayList<String> getRight()
        {
            return right;
        }
    }
}
