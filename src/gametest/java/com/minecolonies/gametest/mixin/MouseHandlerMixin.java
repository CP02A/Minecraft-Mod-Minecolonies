package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.framework.E2EMode;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No human interference in a test window (docs/e2e-tests/framework.md, "No human input"): in client game test runs (watch mode and UI
 * tests) the game's cursor position never comes from the real mouse. Minecraft reads the real cursor position whenever the GUI is resized
 * ({@code Minecraft#resizeGui}, also when a test resizes the window); in a test that would put the cursor wherever the person's mouse
 * happens to be, hovering buttons and showing tooltips in screenshots. In a test run only tests move the cursor
 * ({@code ClientGameTestContext.getInput().setCursorPos}). Fabric doesn't cover this. Does nothing in a normal game ({@code runClient}).
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin
{
    @Inject(method = "resyncMousePosition", at = @At("HEAD"), cancellable = true)
    private void minecoloniesGametest$keepTestCursorPosition(final CallbackInfo ci)
    {
        if (E2EMode.isClientTestRun())
        {
            ci.cancel();
        }
    }
}
