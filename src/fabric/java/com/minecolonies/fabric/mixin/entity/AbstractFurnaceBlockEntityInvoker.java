package com.minecolonies.fabric.mixin.entity;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the furnace burn time. 26.3 inlined the old private AbstractFurnaceBlockEntity#isLit(),
 * which was simply {@code litTimeRemaining > 0}.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityInvoker
{
    @Accessor("litTimeRemaining")
    int minecolonies$getLitTimeRemaining();
}
