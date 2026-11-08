package com.minecolonies.apiimp.initializer;

import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.particles.SleepingParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import com.ldtteam.shim.api.distmarker.Dist;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.fml.common.EventBusSubscriber;
import com.ldtteam.shim.fml.common.Mod;
import com.ldtteam.shim.neoforge.client.event.RegisterParticleProvidersEvent;
import com.ldtteam.shim.neoforge.registries.RegisterEvent;

/**
 * Initializes the particle type.
 */
public class ModParticleTypesInitializer
{
    /**
     * Particle type
     */
    public static final SimpleParticleType SLEEPINGPARTICLE_TYPE = new SimpleParticleType(true);
    public static final Identifier  SLEEPING_TEXTURE      = new Identifier(Constants.MOD_ID, "particle/sleeping");

    /**
     * Register the particle
     */
    @EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class CommonRegistration
    {
        @SubscribeEvent
        public static void registerParticles(final RegisterEvent event)
        {
            event.register(Registries.PARTICLE_TYPE, SLEEPING_TEXTURE, () -> SLEEPINGPARTICLE_TYPE);
        }
    }

    /**
     * Register the client side factory
     */
    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientRegistration
    {
        @SubscribeEvent
        public static void registerParticleFactories(RegisterParticleProvidersEvent event)
        {
            event.registerSpriteSet(SLEEPINGPARTICLE_TYPE, SleepingParticle.Factory::new);
        }
    }
}
