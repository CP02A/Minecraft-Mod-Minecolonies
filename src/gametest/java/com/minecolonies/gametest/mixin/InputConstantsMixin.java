package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.framework.E2EMode;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No human interference in a test window (docs/e2e-tests/framework.md, "No human input"), and none the other way round: in client game
 * test runs (watch mode and UI tests) the test window never captures the mouse. Minecraft normally hides the cursor and locks it in the
 * window (SDL relative mouse mode) and moves it to the window's center whenever no screen is open; in a test run the cursor stays free and
 * where it is, so the person at the computer can keep using it. The game still thinks the mouse is grabbed and behaves as in a normal
 * game. Fabric's client game test framework already does this; this repeats it so the guarantee doesn't depend on Fabric, and UI-08 checks
 * both. Does nothing in a normal game ({@code runClient}).
 */
@Mixin(InputConstants.class)
public abstract class InputConstantsMixin
{
    @Inject(method = {"grabMouse", "releaseMouse"}, at = @At("HEAD"), cancellable = true, require = 2)
    private static void minecoloniesGametest$neverCaptureCursor(final CallbackInfo ci)
    {
        if (E2EMode.isClientTestRun())
        {
            ci.cancel();
        }
    }
}
