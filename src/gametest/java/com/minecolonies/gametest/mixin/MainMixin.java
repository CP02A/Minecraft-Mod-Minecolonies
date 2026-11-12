package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.framework.E2EMode;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A client game test run (watch mode, UI tests) whose window a person closed exits with {@link E2EMode#WINDOW_CLOSED_EXIT_CODE} after the
 * game shut down normally (world saved, logs written). Without this the game would exit with 0 and Gradle would report the aborted run as
 * successful (docs/e2e-tests/framework.md, "No human input"). See {@link WindowMixin}. Does nothing in a normal game ({@code runClient}).
 */
@Mixin(Main.class)
public abstract class MainMixin
{
    @Inject(method = "main", at = @At("RETURN"))
    private static void minecoloniesGametest$failWhenWindowClosed(final String[] args, final CallbackInfo ci)
    {
        if (E2EMode.isClientTestRun() && E2EMode.wasWindowClosed())
        {
            System.exit(E2EMode.WINDOW_CLOSED_EXIT_CODE);
        }
    }
}
