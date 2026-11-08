// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.sound.PlaySoundEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event.sound;

import com.ldtteam.shim.bus.api.Event;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.jetbrains.annotations.Nullable;

/**
 * Fired on the game bus when a sound is about to be played (from {@code com.ldtteam.shim.mixin.client.SoundEngineMixin}).
 * Set the sound to null to prevent it from playing, or replace it.
 */
public class PlaySoundEvent extends Event
{
    private final SoundEngine engine;
    private final SoundInstance originalSound;
    private final String name;
    @Nullable
    private SoundInstance sound;

    public PlaySoundEvent(final SoundEngine engine, final SoundInstance sound)
    {
        this.engine = engine;
        this.originalSound = sound;
        this.name = sound.getIdentifier().getPath();
        this.sound = sound;
    }

    public SoundEngine getEngine()
    {
        return engine;
    }

    public String getName()
    {
        return name;
    }

    public SoundInstance getOriginalSound()
    {
        return originalSound;
    }

    @Nullable
    public SoundInstance getSound()
    {
        return sound;
    }

    public void setSound(@Nullable final SoundInstance newSound)
    {
        this.sound = newSound;
    }
}
