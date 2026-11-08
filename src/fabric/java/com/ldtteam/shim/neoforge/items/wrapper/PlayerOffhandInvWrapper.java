// Adapted from NeoForge 21.1 (net.neoforged.neoforge), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.ldtteam.shim.neoforge.items.wrapper;

import net.minecraft.world.entity.player.Inventory;

public class PlayerOffhandInvWrapper extends RangedWrapper {
    public PlayerOffhandInvWrapper(Inventory inv) {
        super(new InvWrapper(inv), Inventory.SLOT_OFFHAND,
                Inventory.SLOT_OFFHAND + 1);
    }
}
