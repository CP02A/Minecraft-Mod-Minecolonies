// Adapted from NeoForge 21.1.80 (event/server/ServerStoppingEvent.java), licensed under LGPL-2.1. Modified for the MineColonies Fabric shim.
/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.ldtteam.shim.neoforge.event.server;

import net.minecraft.server.MinecraftServer;

/**
 * Called when the server begins an orderly shutdown, before {@link ServerStoppedEvent}.
 *
 * @author cpw
 */
public class ServerStoppingEvent extends ServerLifecycleEvent {
    public ServerStoppingEvent(MinecraftServer server) {
        super(server);
    }
}
