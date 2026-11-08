package com.minecolonies.fabric.mixin.entity;

import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes BrewingStandBlockEntity#brewTime (was public through the NeoForge access transformer).
 */
@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandBlockEntityAccessor
{
    @Accessor("brewTime")
    int minecolonies$getBrewTime();
}
