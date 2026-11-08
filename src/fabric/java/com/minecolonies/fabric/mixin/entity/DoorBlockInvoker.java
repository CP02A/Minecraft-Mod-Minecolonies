package com.minecolonies.fabric.mixin.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes DoorBlock#playSound (was public through the NeoForge access transformer).
 */
@Mixin(DoorBlock.class)
public interface DoorBlockInvoker
{
    @Invoker("playSound")
    void minecolonies$playSound(@Nullable Entity entity, Level level, BlockPos pos, boolean open);
}
