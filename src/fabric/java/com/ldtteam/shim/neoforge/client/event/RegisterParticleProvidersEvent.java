// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

/**
 * Mod bus event to register particle providers; forwarded to Fabric's {@link ParticleProviderRegistry}.
 */
public class RegisterParticleProvidersEvent extends Event implements IModBusEvent
{
    /**
     * Creates a particle provider from the particle type's sprite set (vanilla's private ParticleResources.SpriteParticleRegistration).
     */
    @FunctionalInterface
    public interface SpriteParticleRegistration<T extends ParticleOptions>
    {
        ParticleProvider<T> create(SpriteSet spriteSet);
    }

    public RegisterParticleProvidersEvent() {}

    /**
     * Registers a provider for a particle type without textures (or with manually handled textures).
     */
    public <T extends ParticleOptions> void registerSpecial(final ParticleType<T> type, final ParticleProvider<T> provider)
    {
        ParticleProviderRegistry.getInstance().register(type, provider);
    }

    /**
     * Registers a provider for a particle type whose textures are defined in its particle json.
     */
    public <T extends ParticleOptions> void registerSpriteSet(final ParticleType<T> type, final SpriteParticleRegistration<T> registration)
    {
        ParticleProviderRegistry.getInstance().register(type, registration::create);
    }
}
