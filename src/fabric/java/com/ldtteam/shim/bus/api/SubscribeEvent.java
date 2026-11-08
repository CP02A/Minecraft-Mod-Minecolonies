package com.ldtteam.shim.bus.api;

import java.lang.annotation.*;

/**
 * Marks an event listener method (NeoForge shim).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent
{
    EventPriority priority() default EventPriority.NORMAL;

    boolean receiveCanceled() default false;
}
