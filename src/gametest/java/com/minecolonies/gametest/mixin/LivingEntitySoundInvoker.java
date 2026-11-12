package com.minecolonies.gametest.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Calls the landing sound the way falling does ({@code LivingEntity#causeFallDamage} → {@code playBlockFallSound}), for the Domum
 * sound tests.
 */
@Mixin(LivingEntity.class)
public interface LivingEntitySoundInvoker
{
    @Invoker("playBlockFallSound")
    void minecoloniesGametest$playBlockFallSound();
}
