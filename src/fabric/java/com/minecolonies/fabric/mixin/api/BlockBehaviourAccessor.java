package com.minecolonies.fabric.mixin.api;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes BlockBehaviour#hasCollision (was public through the NeoForge access transformer).
 */
@Mixin(BlockBehaviour.class)
public interface BlockBehaviourAccessor
{
    @Accessor("hasCollision")
    boolean minecolonies$hasCollision();
}
