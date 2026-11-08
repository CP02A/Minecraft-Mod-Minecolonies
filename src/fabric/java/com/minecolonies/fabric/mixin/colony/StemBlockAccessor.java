package com.minecolonies.fabric.mixin.colony;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StemBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the NeoForge access transformer entry for StemBlock#fruit (used by the farmer recipe display).
 */
@Mixin(StemBlock.class)
public interface StemBlockAccessor
{
    @Accessor("fruit")
    ResourceKey<Block> minecolonies$getFruit();
}
