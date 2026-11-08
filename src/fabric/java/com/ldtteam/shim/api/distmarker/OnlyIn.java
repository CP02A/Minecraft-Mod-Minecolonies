package com.ldtteam.shim.api.distmarker;

import java.lang.annotation.*;

/**
 * Marker only (NeoForge shim). On Fabric nothing is stripped; the annotation is purely informational.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
public @interface OnlyIn
{
    Dist value();

    Class<?> _interface() default Object.class;
}
