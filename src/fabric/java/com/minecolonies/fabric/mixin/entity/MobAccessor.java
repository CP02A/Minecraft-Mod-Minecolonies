package com.minecolonies.fabric.mixin.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes Mob#moveControl (was public through the NeoForge access transformer).
 */
@Mixin(Mob.class)
public interface MobAccessor
{
    @Accessor("moveControl")
    void minecolonies$setMoveControl(MoveControl moveControl);
}
