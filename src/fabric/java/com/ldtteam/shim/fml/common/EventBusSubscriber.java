package com.ldtteam.shim.fml.common;

import com.ldtteam.shim.api.distmarker.Dist;

import java.lang.annotation.*;

/**
 * Marks a class whose static SubscribeEvent methods are registered automatically (NeoForge shim).
 * On Fabric there is no annotation scanning: classes are listed explicitly in the Fabric entrypoint.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventBusSubscriber
{
    Dist[] value() default {Dist.CLIENT, Dist.DEDICATED_SERVER};

    String modid() default "";

    Bus bus() default Bus.BOTH;

    enum Bus
    {
        /**
         * Route each listener by its event type (mod bus events go to the mod bus, others to the game bus).
         */
        BOTH,
        GAME,
        MOD
    }
}
