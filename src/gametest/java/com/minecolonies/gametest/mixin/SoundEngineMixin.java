package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.framework.E2EMode;
import net.minecraft.client.sounds.SoundEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No sound in client game test runs (watch mode and UI tests, docs/e2e-tests/framework.md, "No human input"): the sound engine never opens
 * the audio device, from the title screen to the end of the run, so a test run makes no noise on the computer it runs on. The engine stays
 * "not loaded", exactly as on a computer without an audio device (a case vanilla supports): every {@code play} is a no-op and nothing is
 * decoded or streamed. Fabric only mutes the music category. Does nothing in a normal game ({@code runClient}).
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin
{
    @Unique
    private static final Logger minecoloniesGametest$LOGGER = LoggerFactory.getLogger("MineColonies E2E");

    @Inject(method = "loadLibrary", at = @At("HEAD"), cancellable = true)
    private void minecoloniesGametest$noAudioDevice(final CallbackInfo ci)
    {
        if (E2EMode.isClientTestRun())
        {
            minecoloniesGametest$LOGGER.info("Client game test run: sound engine disabled (no audio device opened).");
            ci.cancel();
        }
    }
}
