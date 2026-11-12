package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.framework.E2EMode;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No human interference in a test window (docs/e2e-tests/framework.md, "No human input"): in client game test runs (watch mode and UI
 * tests) the game never pauses because the window lost focus ("Pause on Lost Focus"), so clicking another window opens no pause menu and
 * doesn't stop the integrated server. {@link WindowMixin} (and Fabric) already keep the window "focused"; this makes it hold even if the
 * focus state changes some other way. Tests can still pause the game on purpose. Does nothing in a normal game ({@code runClient}).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin
{
    @Inject(method = "pauseIfInactive", at = @At("HEAD"), cancellable = true)
    private void minecoloniesGametest$neverPauseOnFocusLoss(final CallbackInfo ci)
    {
        if (E2EMode.isClientTestRun())
        {
            ci.cancel();
        }
    }
}
