package com.ldtteam.shim.fabric.events;

import com.ldtteam.shim.neoforge.common.damagesource.DamageContainer;

import java.util.Deque;

/**
 * Implemented on LivingEntity by the shim mixin: the stack of damage containers of the hurtServer calls in progress
 * (NeoForge's {@code LivingEntity#damageContainers}).
 */
public interface DamageContainerHolder
{
    Deque<DamageContainer> shim$getDamageContainers();
}
