package com.minecolonies.fabric.mixin.entity;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes AbstractFurnaceBlockEntity#isLit (was public through the NeoForge access transformer).
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityInvoker
{
    @Invoker("isLit")
    boolean minecolonies$isLit();
}
