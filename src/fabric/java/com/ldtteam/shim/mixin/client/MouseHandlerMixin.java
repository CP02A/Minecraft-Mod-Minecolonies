package com.ldtteam.shim.mixin.client;

import com.ldtteam.shim.neoforge.client.event.InputEvent;
import com.ldtteam.shim.neoforge.common.NeoForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link InputEvent.MouseScrollingEvent} for in-world scrolling (no screen open), before hotbar/spectator handling.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin
{
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract double xpos();

    @Shadow
    public abstract double ypos();

    @Shadow
    public abstract boolean isLeftPressed();

    @Shadow
    public abstract boolean isMiddlePressed();

    @Shadow
    public abstract boolean isRightPressed();

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void shim$onScroll(final long handle, final double xoffset, final double yoffset, final CallbackInfo ci)
    {
        if (handle == 0L || handle != minecraft.getWindow().handle() || minecraft.gui.overlay() != null || minecraft.gui.screen() != null || minecraft.player == null)
        {
            return;
        }
        final boolean discreteScroll = minecraft.options.discreteMouseScroll().get();
        final double sensitivity = minecraft.options.mouseWheelSensitivity().get();
        final double scaledX = (discreteScroll ? Math.signum(xoffset) : xoffset) * sensitivity;
        final double scaledY = (discreteScroll ? Math.signum(yoffset) : yoffset) * sensitivity;
        final InputEvent.MouseScrollingEvent event =
          new InputEvent.MouseScrollingEvent(scaledX, scaledY, isLeftPressed(), isMiddlePressed(), isRightPressed(), xpos(), ypos());
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled())
        {
            ci.cancel();
        }
    }
}
