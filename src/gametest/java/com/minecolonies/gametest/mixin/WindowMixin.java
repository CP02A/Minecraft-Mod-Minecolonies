package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.framework.E2EMode;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.sdl.SDL_Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No human interference in a test window (docs/e2e-tests/framework.md, "No human input"), window events. In client game test runs (watch
 * mode and UI tests):
 * <ul>
 * <li>Focus changes are ignored: clicking another window doesn't make the game "inactive", so it never opens the pause menu or pauses the
 * integrated server ({@link MinecraftMixin} also turns off the pause on focus loss itself).</li>
 * <li>Minimizing (and restoring) is ignored: the game keeps rendering and ticking at full speed while the window is minimized.</li>
 * <li>A changed display mode (monitor resolution) and entering or leaving fullscreen through the window system are ignored: the first would
 * set the framebuffer to the real window size (screenshots must keep the fixed test size), the second would change and save the
 * fullscreen option.</li>
 * <li>Closing the window (close button, Alt+F4) is allowed on purpose: it is the way to stop a run. It is logged and the game exits with
 * {@link E2EMode#WINDOW_CLOSED_EXIT_CODE} ({@link MainMixin}), so Gradle reports the aborted run as failed rather than passed.</li>
 * </ul>
 * Fabric's client game test framework already ignores focus, minimize and window size changes (it keeps the framebuffer at a fixed size);
 * this mixin repeats the focus and minimize part so the guarantee doesn't depend on Fabric, and UI-08 checks both. Window size changes are
 * left to Fabric alone: it records the real size to draw the fixed-size picture into the window. Does nothing in a normal game
 * ({@code runClient}).
 */
@Mixin(Window.class)
public abstract class WindowMixin
{
    @Unique
    private static final Logger minecoloniesGametest$LOGGER = LoggerFactory.getLogger("MineColonies E2E");

    /**
     * {@code SDL_EVENT_DISPLAY_CURRENT_MODE_CHANGED}.
     */
    @Unique
    private static final int minecoloniesGametest$DISPLAY_MODE_CHANGED = 0x156;

    /**
     * {@code SDL_EVENT_WINDOW_ENTER_FULLSCREEN}.
     */
    @Unique
    private static final int minecoloniesGametest$ENTER_FULLSCREEN = 0x217;

    /**
     * {@code SDL_EVENT_WINDOW_LEAVE_FULLSCREEN}.
     */
    @Unique
    private static final int minecoloniesGametest$LEAVE_FULLSCREEN = 0x218;

    @Inject(method = {"onFocus", "onIconified"}, at = @At("HEAD"), cancellable = true, require = 2)
    private void minecoloniesGametest$ignoreFocusAndMinimize(final boolean value, final CallbackInfo ci)
    {
        if (E2EMode.isClientTestRun())
        {
            ci.cancel();
        }
    }

    @Inject(method = "handleEvent", at = @At("HEAD"), cancellable = true)
    private void minecoloniesGametest$ignoreDisplayAndFullscreenEvents(final SDL_Event event, final CallbackInfo ci)
    {
        if (E2EMode.isClientTestRun())
        {
            final int type = event.type();
            if (type == minecoloniesGametest$DISPLAY_MODE_CHANGED || type == minecoloniesGametest$ENTER_FULLSCREEN
                  || type == minecoloniesGametest$LEAVE_FULLSCREEN)
            {
                ci.cancel();
            }
        }
    }

    /**
     * The window never confines the cursor (SDL mouse grab, used by exclusive fullscreen): together with {@link InputConstantsMixin} the
     * test window can't hold on to the mouse in any mode.
     */
    @Inject(method = "updateWindowMouseGrab", at = @At("HEAD"), cancellable = true)
    private void minecoloniesGametest$neverConfineCursor(final CallbackInfo ci)
    {
        if (E2EMode.isClientTestRun())
        {
            ci.cancel();
        }
    }

    @Inject(method = "requestClose", at = @At("HEAD"))
    private void minecoloniesGametest$logClose(final CallbackInfo ci)
    {
        if (E2EMode.isClientTestRun() && !E2EMode.wasWindowClosed())
        {
            E2EMode.markWindowClosed();
            minecoloniesGametest$LOGGER.error("The test window was closed: the client game test run is aborted without a test result (exit code {}).",
              E2EMode.WINDOW_CLOSED_EXIT_CODE);
        }
    }
}
