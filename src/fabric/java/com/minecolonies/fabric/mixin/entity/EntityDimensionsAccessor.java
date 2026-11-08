package com.minecolonies.fabric.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes Entity#dimensions (was public through the NeoForge access transformer).
 */
@Mixin(Entity.class)
public interface EntityDimensionsAccessor
{
    @Accessor("dimensions")
    void minecolonies$setDimensions(EntityDimensions dimensions);
}
