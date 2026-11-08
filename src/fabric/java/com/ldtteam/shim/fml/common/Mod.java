package com.ldtteam.shim.fml.common;

import com.ldtteam.shim.api.distmarker.Dist;

import java.lang.annotation.*;

/**
 * Marks a mod main class (NeoForge shim). On Fabric the classes are constructed explicitly by the entrypoint.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod
{
    String value();

    Dist[] dist() default {Dist.CLIENT, Dist.DEDICATED_SERVER};
}
