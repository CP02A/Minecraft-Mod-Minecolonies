package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.framework.E2EMode;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.sdl.SDLHints;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The test window doesn't steal the focus (docs/e2e-tests/framework.md, "No human input"): in client game test runs (watch mode and UI
 * tests) SDL is told not to activate the game window when it is shown, so starting a test run doesn't pull keyboard focus away from what the
 * person at the computer is doing. The game still counts as focused (Fabric and {@link WindowMixin}). Set right before {@code SDL_Init},
 * after Minecraft's own hints. Does nothing in a normal game ({@code runClient}).
 */
@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin
{
    @Inject(method = "initBackendSystem", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLInit;SDL_Init(I)Z"))
    private static void minecoloniesGametest$dontActivateWindow(final CallbackInfoReturnable<?> cir)
    {
        if (E2EMode.isClientTestRun())
        {
            SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOW_ACTIVATE_WHEN_SHOWN, "0");
            SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOW_ACTIVATE_WHEN_RAISED, "0");
        }
    }
}
