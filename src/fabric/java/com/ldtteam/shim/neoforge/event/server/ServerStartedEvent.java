// Adapted from NeoForge 21.1.80 (event/server/ServerStartedEvent.java), licensed under LGPL-2.1. Modified for the MineColonies Fabric shim.
/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.ldtteam.shim.neoforge.event.server;

import net.minecraft.server.MinecraftServer;

/**
 * Called after {@link ServerStartingEvent} when the server is available and ready to play.
 *
 * @author cpw
 */
public class ServerStartedEvent extends ServerLifecycleEvent {
    public ServerStartedEvent(final MinecraftServer server) {
        super(server);
    }
}
