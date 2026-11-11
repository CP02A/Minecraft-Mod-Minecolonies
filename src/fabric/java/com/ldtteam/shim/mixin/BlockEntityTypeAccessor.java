package com.ldtteam.shim.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/**
 * Read access to the valid blocks of a block entity type (BlockEntityTypeAddBlocksEvent).
 */
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeAccessor
{
    @Accessor("validBlocks")
    Set<Block> minecolonies$getValidBlocks();
}
