// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.InputEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.bus.api.ICancellableEvent;

/**
 * Input events (game bus).
 */
public abstract class InputEvent extends Event
{
    protected InputEvent() {}

    /**
     * Fired when the mouse wheel is scrolled in-game (no screen open), before the hotbar/spectator handling. Cancel to suppress vanilla handling.
     * Fired from {@code com.ldtteam.shim.mixin.client.MouseHandlerMixin}.
     */
    public static class MouseScrollingEvent extends InputEvent implements ICancellableEvent
    {
        private final double scrollDeltaX;
        private final double scrollDeltaY;
        private final double mouseX;
        private final double mouseY;
        private final boolean leftDown;
        private final boolean middleDown;
        private final boolean rightDown;

        public MouseScrollingEvent(final double scrollDeltaX, final double scrollDeltaY, final boolean leftDown, final boolean middleDown, final boolean rightDown, final double mouseX, final double mouseY)
        {
            this.scrollDeltaX = scrollDeltaX;
            this.scrollDeltaY = scrollDeltaY;
            this.leftDown = leftDown;
            this.middleDown = middleDown;
            this.rightDown = rightDown;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        public double getScrollDeltaX()
        {
            return scrollDeltaX;
        }

        public double getScrollDeltaY()
        {
            return scrollDeltaY;
        }

        public boolean isLeftDown()
        {
            return leftDown;
        }

        public boolean isRightDown()
        {
            return rightDown;
        }

        public boolean isMiddleDown()
        {
            return middleDown;
        }

        public double getMouseX()
        {
            return mouseX;
        }

        public double getMouseY()
        {
            return mouseY;
        }
    }
}
