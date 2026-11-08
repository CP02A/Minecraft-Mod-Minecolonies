// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.ClientTickEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;

/**
 * Fired on the game bus once per client tick, before ({@link Pre}) and after ({@link Post}) the tick. Fired from Fabric's ClientTickEvents.
 */
public abstract class ClientTickEvent extends Event
{
    protected ClientTickEvent() {}

    public static class Pre extends ClientTickEvent
    {
        public Pre() {}
    }

    public static class Post extends ClientTickEvent
    {
        public Post() {}
    }
}
