// Adapted from NeoForge 21.1.80 (event/entity/EntityAttributeCreationEvent.java), licensed under LGPL-2.1. Modified for the MineColonies Fabric shim.
/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.ldtteam.shim.neoforge.event.entity;

import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;

/**
 * EntityAttributeCreationEvent.<br>
 * Use this event to register attributes for your own EntityTypes.
 * This event is fired after registration and before common setup.
 * <br>
 * Fired on the Mod bus {@link IModBusEvent}.<br>
 **/
public class EntityAttributeCreationEvent extends Event implements IModBusEvent {
    private final Map<EntityType<? extends LivingEntity>, AttributeSupplier> map;

    public EntityAttributeCreationEvent(Map<EntityType<? extends LivingEntity>, AttributeSupplier> map) {
        this.map = map;
    }

    public void put(EntityType<? extends LivingEntity> entity, AttributeSupplier map) {
        if (DefaultAttributes.hasSupplier(entity))
            throw new IllegalStateException("Duplicate DefaultAttributes entry: " + entity);
        this.map.put(entity, map);
    }
}
