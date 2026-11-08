package com.ldtteam.shim.bus.api;

/**
 * Events implementing this can be canceled (NeoForge shim).
 */
public interface ICancellableEvent
{
    default void setCanceled(final boolean canceled)
    {
        ((Event) this).isCanceled = canceled;
    }

    default boolean isCanceled()
    {
        return ((Event) this).isCanceled;
    }
}
