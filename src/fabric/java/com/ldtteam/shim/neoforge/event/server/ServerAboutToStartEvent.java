// Adapted from NeoForge 21.1.80 (event/server/ServerAboutToStartEvent.java), licensed under LGPL-2.1. Modified for the MineColonies Fabric shim.
/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.ldtteam.shim.neoforge.event.server;

import net.minecraft.server.MinecraftServer;
import com.ldtteam.shim.fml.event.lifecycle.InterModProcessEvent;

/**
 * Called before the server begins loading anything. Called after {@link InterModProcessEvent} on the dedicated
 * server, and after the player has hit "Play Selected World" in the client. Called before {@link ServerStartingEvent}.
 *
 * You can obtain a reference to the server with this event.
 * 
 * @author cpw
 */
public class ServerAboutToStartEvent extends ServerLifecycleEvent {
    public ServerAboutToStartEvent(MinecraftServer server) {
        super(server);
    }
}
