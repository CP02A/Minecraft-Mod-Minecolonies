package com.ldtteam.shim.mixin.client;

import com.ldtteam.shim.neoforge.client.event.sound.PlaySoundEvent;
import com.ldtteam.shim.neoforge.common.NeoForge;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires {@link PlaySoundEvent} before a sound is played; the sound can be cancelled (set to null) or replaced.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin
{
    @Unique
    private boolean shim$replaying = false;

    @Shadow
    public abstract SoundEngine.PlayResult play(SoundInstance instance);

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void shim$onPlay(final SoundInstance instance, final CallbackInfoReturnable<SoundEngine.PlayResult> cir)
    {
        if (shim$replaying || instance == null)
        {
            return;
        }
        final PlaySoundEvent event = new PlaySoundEvent((SoundEngine) (Object) this, instance);
        NeoForge.EVENT_BUS.post(event);
        final SoundInstance result = event.getSound();
        if (result == null)
        {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
        else if (result != instance)
        {
            shim$replaying = true;
            try
            {
                cir.setReturnValue(play(result));
            }
            finally
            {
                shim$replaying = false;
            }
        }
    }
}
