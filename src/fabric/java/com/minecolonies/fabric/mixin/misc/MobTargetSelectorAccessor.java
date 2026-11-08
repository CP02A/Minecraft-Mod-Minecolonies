package com.minecolonies.fabric.mixin.misc;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access to the target selector of mobs (replaces the NeoForge access transformer), used to make hostile mobs attack citizens.
 */
@Mixin(Mob.class)
public interface MobTargetSelectorAccessor
{
    @Accessor("targetSelector")
    GoalSelector minecolonies$getTargetSelector();
}
