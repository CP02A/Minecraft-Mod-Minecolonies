// Adapted from NeoForge 21.1.80 (event/entity/player/AttackEntityEvent.java), licensed under LGPL-2.1. Modified for the MineColonies Fabric shim.
/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.ldtteam.shim.neoforge.event.entity.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import com.ldtteam.shim.bus.api.ICancellableEvent;
import com.ldtteam.shim.neoforge.common.NeoForge;

/**
 * AttackEntityEvent is fired when a player attacks an Entity.<br>
 * This event is fired whenever a player attacks an Entity in
 * {@link Player#attack(Entity)}.<br>
 * <br>
 * {@link #target} contains the Entity that was damaged by the player. <br>
 * <br>
 * This event is {@link ICancellableEvent}.<br>
 * If this event is canceled, the player does not attack the Entity.<br>
 * <br>
 * This event does not have a result. {@link HasResult}<br>
 * <br>
 * This event is fired on the {@link NeoForge#EVENT_BUS}.
 **/
public class AttackEntityEvent extends PlayerEvent implements ICancellableEvent {
    private final Entity target;

    public AttackEntityEvent(Player player, Entity target) {
        super(player);
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }
}
