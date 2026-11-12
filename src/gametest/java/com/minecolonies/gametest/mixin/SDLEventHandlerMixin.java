package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.framework.E2EMode;
import com.mojang.blaze3d.platform.SDLEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No human interference in a test window (docs/e2e-tests/framework.md, "No human input"): in client game test runs (watch mode and UI
 * tests) real keyboard, mouse and text input from the window is dropped before Minecraft sees it, so a person touching the keyboard or mouse
 * can't move the player, click a button or type into a window. Simulated input from tests ({@code ClientGameTestContext.getInput()}) goes
 * straight to Minecraft's keyboard and mouse handlers and is not affected.
 * <p>
 * Fabric's client game test framework already drops real input the same way; this mixin keeps the guarantee in the test mod, so it holds
 * even if Fabric changes, and UI-08 checks that both are applied. Does nothing in a normal game ({@code runClient}).
 */
@Mixin(SDLEventHandler.class)
public abstract class SDLEventHandlerMixin
{
    @Inject(method = {"handleKeyEvent", "handleTextInputEvent", "handleTextEditingEvent", "handleMouseMotionEvent", "handleMouseButtonEvent",
      "handleMouseWheelEvent", "handleDropFileEvent"}, at = @At("HEAD"), cancellable = true, require = 7)
    private void minecoloniesGametest$dropHumanInput(final CallbackInfo ci)
    {
        if (E2EMode.isClientTestRun())
        {
            ci.cancel();
        }
    }
}
